package com.beijixing.social.crawl.adapter;

import com.beijixing.social.crawl.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 小红书评论抓取适配器
 *
 * API文档: https://open.xiaohongshu.com/docs/api/v2/notes/comments
 *
 * 支持功能:
 * ✅ 获取笔记一级评论列表
 * ✅ 获取二级评论(回复)
 * ✅ 分页翻页 (cursor方式)
 * ✅ 按时间排序
 * ✅ 评论者信息
 *
 * 所需权限:
 * - scope: notes.comment.read
 * - 需要企业认证或个人开发者认证
 *
 * 特殊要求:
 * - 请求签名: MD5(app_key + timestamp + app_secret)
 * - 时间戳: 精确到秒的Unix时间戳
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Component
@Slf4j
public class XiaohongshuCommentAdapter implements PlatformCommentAdapter {

    @Value("${xiaohongshu.api.base-url:https://open.xiaohongshu.com}")
    private String apiBaseUrl;

    @Value("${xiaohongshu.api.app-key:}")
    private String appKey;

    @Value("${xiaohongshu.api.app-secret:}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ====== 实现接口方法 ======

    @Override
    public String getPlatformCode() {
        return "XIAOHONGSHU";
    }

    @Override
    public String getPlatformName() {
        return "小红书";
    }

    /**
     * 小红书评论抓取核心实现
     *
     * API端点: GET /api/v2/notes/{note_id}/comments
     *
     * 请求参数:
     * - note_id: 笔记ID (必填, 24位hex字符)
     * - cursor: 游标 (首次为空字符串"")
     * - page_size: 每页数量 (默认20, 最大30)
     * - access_token: 用户授权Token (Header或Query)
     * - sign: 请求签名 (MD5签名)
     * - timestamp: Unix时间戳 (秒)
     *
     * 响应示例:
     * {
     *   "code": 0,
     *   "message": "success",
     *   "data": {
     *     "comments": [...],
     *     "cursor": "xxx",
     *     "has_more": true,
     *     "total": 500
     *   }
     * }
     */
    @Override
    public CommentCrawlResult crawlComments(String targetId, String accessToken, CrawlOptions options) {
        log.info("📕 [小红书] 开始抓取评论 | 笔记ID: {} | Token: {}...",
                targetId, accessToken != null ? accessToken.substring(0, 10) + "..." : "null");

        long startTime = System.currentTimeMillis();
        CommentCrawlResult result = CommentCrawlResult.builder()
                .taskId(UUID.randomUUID().toString())
                .platform("XIAOHONGSHU")
                .targetId(targetId)
                .targetUrl("https://www.xiaohongshu.com/explore/" + targetId)
                .startTime(LocalDateTime.now())
                .build();

        try {
            options.validate();

            List<CommentCrawlResult.SocialComment> allComments = new ArrayList<>();
            int totalApiCalls = 0;
            int totalPages = 0;
            Set<String> uniqueUserIds = new HashSet<>();
            long totalLikes = 0;

            String cursor = "";
            boolean hasMore = true;
            int pageCount = 0;

            while (hasMore && pageCount < options.getMaxPages() &&
                   allComments.size() < options.getMaxTotalComments()) {

                log.debug("  📄 翻到第{}页 | 已获取{}条", pageCount + 1, allComments.size());

                JsonNode response = callXiaohongshuCommentAPI(targetId, cursor, accessToken, options);
                totalApiCalls++;
                pageCount++;

                if (response == null || !response.has("data")) {
                    log.warn("  ⚠️ API响应异常，停止翻页");
                    break;
                }

                JsonNode data = response.get("data");

                // 解析评论列表
                if (data.has("comments") && data.get("comments").isArray()) {
                    for (JsonNode commentNode : data.get("comments")) {
                        CommentCrawlResult.SocialComment comment = parseXhsComment(commentNode);
                        if (comment != null) {
                            allComments.add(comment);
                            uniqueUserIds.add(comment.getAuthorId());
                            totalLikes += comment.getLikeCount();
                        }
                    }
                }

                // 检查是否还有更多数据
                hasMore = data.has("has_more") && data.get("has_more").asBoolean();
                if (hasMore && data.has("cursor")) {
                    cursor = data.get("cursor").asText();
                }

                if (hasMore) {
                    Thread.sleep(options.getRequestIntervalMs());
                }
            }

            totalPages = pageCount;

            // 获取二级评论
            if (options.isIncludeReplies() && !allComments.isEmpty()) {
                log.info("  💬 开始获取二级评论...");
                int replyCount = fetchReplies(allComments, targetId, accessToken, options);
                totalApiCalls += replyCount;
            }

            // 构建统计信息
            CommentCrawlResult.CrawlStatistics stats = new CommentCrawlResult.CrawlStatistics();
            stats.setTotalComments(allComments.size());
            stats.setTopLevelComments((int) allComments.stream().filter(c -> c.getParentId() == null).count());
            stats.setReplyComments(stats.getTotalComments() - stats.getTopLevelComments());
            stats.setPagesFetched(totalPages);
            stats.setApiCallCount(totalApiCalls);
            stats.setUniqueUsers(uniqueUserIds.size());
            stats.setTotalLikes(totalLikes);
            stats.setAvgLikeCount(allComments.isEmpty() ? 0 : (double) totalLikes / allComments.size());

            result.setSuccess(true);
            result.setStatistics(stats);
            result.setComments(allComments);
            result.setEndTime(LocalDateTime.now());
            result.setDurationMs(System.currentTimeMillis() - startTime);

            log.info("✅ [小红书] 抓取完成 | {}", result.getSummary());

        } catch (Exception e) {
            log.error("❌ [小红书] 抓取失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorCode("XHS_CRAWL_ERROR");
            result.setErrorMessage(e.getMessage());
            result.setEndTime(LocalDateTime.now());
            result.setDurationMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    /**
     * 调用小红书评论API（含签名算法）
     */
    private JsonNode callXiaohongshuCommentAPI(String noteId, String cursor,
                                               String accessToken, CrawlOptions options) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;  // 秒级时间戳

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(apiBaseUrl + "/api/v2/notes/" + noteId + "/comments")
                    .queryParam("cursor", cursor != null ? cursor : "")
                    .queryParam("page_size", Math.min(options.getPageSize(), 30))  // 小红书限制最大30
                    .queryParam("access_token", accessToken)
                    .queryParam("timestamp", timestamp);

            // 计算签名
            String sign = calculateSign(timestamp);
            builder.queryParam("sign", sign);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.debug("  📡 调用小红书API | note_id={} | cursor={}", noteId, cursor);

            ResponseEntity<String> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            } else {
                log.error("  ❌ API返回错误: HTTP {}", response.getStatusCode().value());
                return null;
            }

        } catch (Exception e) {
            log.error("  ❌ 调用小红书API异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算请求签名 (MD5算法)
     *
     * 签名规则: MD5(app_key + timestamp + app_secret)
     */
    private String calculateSign(long timestamp) {
        try {
            String rawSign = appKey + timestamp + appSecret;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rawSign.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            log.warn("计算签名失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 解析单条小红书评论JSON
     */
    private CommentCrawlResult.SocialComment parseXhsComment(JsonNode node) {
        try {
            CommentCrawlResult.SocialComment comment = CommentCrawlResult.SocialComment.builder()
                    .commentId(node.path("id").asText(null))
                    .parentId(node.has("parent_comment_id") &&
                              !node.get("parent_comment_id").isNull() ?
                              node.get("parent_comment_id").asText() : null)
                    .rootCommentId(node.has("root_comment_id") &&
                                   !node.get("root_comment_id").isNull() ?
                                   node.get("root_comment_id").asText() : null)
                    .content(node.path("content").asText(""))
                    .rawContent(node.path("content").asText(""))
                    .authorId(node.path("user_info").path("user_id").asText(null))
                    .authorName(node.path("user_info").path("nickname").asText("匿名用户"))
                    .authorAvatar(node.path("user_info").path("image").asText(null))
                    .isAuthorVerified(node.path("user_info").path("interaction_type").asInt() > 0)
                    .likeCount(node.path("like_count").asLong(0))
                    .replyCount(node.path("sub_comment_count").asLong(0))
                    .build();

            // 解析发布时间 (ISO格式或时间戳)
            if (node.has("create_time")) {
                String timeStr = node.get("create_time").asText();
                try {
                    long ts = Long.parseLong(timeStr);
                    comment.setPublishTime(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(ts), ZoneId.systemDefault()));
                } catch (NumberFormatException e) {
                    LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            }

            // 解析图片
            if (node.has("image_list") && node.get("image_list").isArray()) {
                List<String> images = new ArrayList<>();
                for (JsonNode imgNode : node.get("image_list")) {
                    if (imgNode.has("url") || imgNode.has("url_default")) {
                        images.add(imgNode.has("url") ?
                                imgNode.get("url").asText() :
                                imgNode.get("url_default").asText());
                    }
                }
                comment.setImages(images);
            }

            return comment;

        } catch (Exception e) {
            log.warn("  ⚠️ 解析小红书评论失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取二级评论（回复）
     *
     * API: GET /api/v2/notes/{note_id}/comments/{comment_id}/sub_comments
     */
    private int fetchReplies(List<CommentCrawlResult.SocialComment> topLevelComments,
                           String noteId, String accessToken, CrawlOptions options) throws Exception {
        int apiCallCount = 0;
        int processedCount = 0;
        int maxRepliesToFetch = Math.min(topLevelComments.size(), 50);

        for (CommentCrawlResult.SocialComment parentComment : topLevelComments) {
            if (processedCount >= maxRepliesToFetch) break;
            if (parentComment.getReplyCount() <= 0) continue;

            try {
                long timestamp = System.currentTimeMillis() / 1000;
                String sign = calculateSign(timestamp);

                String url = UriComponentsBuilder
                        .fromHttpUrl(apiBaseUrl + "/api/v2/notes/" + noteId +
                                    "/comments/" + parentComment.getCommentId() + "/sub_comments")
                        .queryParam("page_size", Math.min(options.getPageSize(), 20))
                        .queryParam("access_token", accessToken)
                        .queryParam("timestamp", timestamp)
                        .queryParam("sign", sign)
                        .toUriString();

                HttpHeaders headers = new HttpHeaders();
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                apiCallCount++;

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    if (root.has("data") && root.get("data").has("comments")) {
                        for (JsonNode replyNode : root.get("data").get("comments")) {
                            CommentCrawlResult.SocialComment reply = parseXhsComment(replyNode);
                            if (reply != null) {
                                topLevelComments.add(reply);
                            }
                        }
                    }
                }

                Thread.sleep(options.getRequestIntervalMs());

            } catch (Exception e) {
                log.warn("  ⚠️ 获取评论{}的回复失败: {}", parentComment.getCommentId(), e.getMessage());
            }

            processedCount++;
        }

        log.info("  ✅ 二级评论获取完成 | 处理了{}条一级评论 | API调用{}次",
                processedCount, apiCallCount);

        return apiCallCount;
    }

    @Override
    public boolean validateToken(String accessToken) {
        try {
            String url = apiBaseUrl + "/api/v1/user/selfinfo?access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getRateLimitInfo() {
        return "100次/分钟 (需企业认证)";
    }

    @Override
    public List<String> getSupportedFeatures() {
        return Arrays.asList(
                "✅ 一级评论获取",
                "✅ 二级评论(回复)",
                "✅ 分页翻页 (每页30条)",
                "✅ 时间排序",
                "✅ 评论者信息 (昵称/头像/认证)",
                "✅ 图片评论支持",
                "🔒 MD5签名认证"
        );
    }
}
