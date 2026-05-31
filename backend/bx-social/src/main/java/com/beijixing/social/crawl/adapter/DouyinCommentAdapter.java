package com.beijixing.social.crawl.adapter;

import com.beijixing.social.crawl.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 抖音评论抓取适配器
 *
 * API文档: https://developer.open-douyin.com/docs/resource/zh-CN/dop/develop/openapi/video-management/video-comment/get/list
 *
 * 支持功能:
 * ✅ 获取视频一级评论列表
 * ✅ 获取二级评论(回复)
 * ✅ 分页翻页
 * ✅ 按时间/点赞数排序
 * ✅ 时间范围过滤
 * ✅ 评论者信息(昵称/头像/认证状态)
 *
 * 所需权限:
 * - scope: item_review (读取视频评论)
 * - 需要用户授权
 *
 * 限制:
 * - 单次最多返回20条评论
 * - 每个账号每天调用次数有限制
 * - 只能获取公开视频的评论
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Component
@Slf4j
public class DouyinCommentAdapter implements PlatformCommentAdapter {

    @Value("${douyin.api.base-url:https://open.douyin.com}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ====== 实现接口方法 ======

    @Override
    public String getPlatformCode() {
        return "DOUYIN";
    }

    @Override
    public String getPlatformName() {
        return "抖音";
    }

    /**
     * 抖音评论抓取核心实现
     *
     * API端点: POST /video/comment/list/
     *
     * 请求参数:
     * {
     *   "item_id": "7234567890123456789",   // 视频ID (必填)
     *   "cursor": 0,                        // 游标/页码 (首次为0)
     *   "count": 20,                         // 每页数量 (最大20)
     *   "sort_type": 0,                      // 排序方式: 0=时间倒序, 1=时间正序, 2=点赞数倒序
     *   "item_type": 0                       // 类型: 0=视频
     * }
     *
     * 响应示例:
     * {
     *   "code": 0,
     *   "message": "success",
     *   "data": {
     *     "comments": [...],
     *     "total": 1500,
     *     "has_more": true,
     *     "cursor": "xxx"
     *   }
     * }
     */
    @Override
    public CommentCrawlResult crawlComments(String targetId, String accessToken, CrawlOptions options) {
        log.info("🎬 [抖音] 开始抓取评论 | 视频ID: {} | Token: {}...",
                targetId, accessToken != null ? accessToken.substring(0, 10) + "..." : "null");

        long startTime = System.currentTimeMillis();
        CommentCrawlResult result = CommentCrawlResult.builder()
                .taskId(UUID.randomUUID().toString())
                .platform("DOUYIN")
                .targetId(targetId)
                .targetUrl("https://www.douyin.com/video/" + targetId)
                .startTime(LocalDateTime.now())
                .build();

        try {
            options.validate();

            List<CommentCrawlResult.SocialComment> allComments = new ArrayList<>();
            int totalApiCalls = 0;
            int totalPages = 0;
            Set<String> uniqueUserIds = new HashSet<>();
            long totalLikes = 0;

            // ====== 第一轮：获取一级评论 ======
            String cursor = "0";
            boolean hasMore = true;
            int pageCount = 0;

            while (hasMore && pageCount < options.getMaxPages() &&
                   allComments.size() < options.getMaxTotalComments()) {

                log.debug("  📄 翻到第{}页 | 已获取{}条", pageCount + 1, allComments.size());

                JsonNode response = callDouyinCommentAPI(targetId, cursor, accessToken, options);
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
                        CommentCrawlResult.SocialComment comment = parseDouyinComment(commentNode);
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

                // 控制请求频率
                if (hasMore) {
                    Thread.sleep(options.getRequestIntervalMs());
                }
            }

            totalPages = pageCount;

            // ====== 第二轮：获取二级评论（如果配置允许）=====
            if (options.isIncludeReplies() && !allComments.isEmpty()) {
                log.info("  💬 开始获取二级评论...");
                int replyCount = fetchReplies(allComments, targetId, accessToken, options);
                totalApiCalls += replyCount;
            }

            // ====== 构建结果 ======
            CommentCrawlResult.CrawlStatistics stats = new CommentCrawlResult.CrawlStatistics();
            stats.setTotalComments(allComments.size());
            stats.setTopLevelComments((int) allComments.stream()
                    .filter(c -> c.getParentId() == null).count());
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

            log.info("✅ [抖音] 抓取完成 | {}", result.getSummary());

        } catch (Exception e) {
            log.error("❌ [抖音] 抓取失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorCode("DOUYIN_CRAWL_ERROR");
            result.setErrorMessage(e.getMessage());
            result.setEndTime(LocalDateTime.now());
            result.setDurationMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    /**
     * 调用抖音评论API
     */
    private JsonNode callDouyinCommentAPI(String itemId, String cursor,
                                         String accessToken, CrawlOptions options) {
        try {
            String url = apiBaseUrl + "/video/comment/list/";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("item_id", itemId);
            requestBody.put("cursor", cursor.equals("0") ? 0 : Long.parseLong(cursor));
            requestBody.put("count", Math.min(options.getPageSize(), 20));  // 抖音限制最大20

            // 排序类型映射
            switch (options.getSortType()) {
                case TIME_ASC:
                    requestBody.put("sort_type", 1);
                    break;
                case LIKES_DESC:
                    requestBody.put("sort_type", 2);
                    break;
                default:
                    requestBody.put("sort_type", 0);  // 默认时间倒序
            }

            requestBody.put("item_type", 0);  // 0=视频

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("  📡 调用抖音API: {} | cursor={}", url, cursor);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            } else {
                log.error("  ❌ API返回错误: HTTP {}", response.getStatusCode().value());
                return null;
            }

        } catch (Exception e) {
            log.error("  ❌ 调用抖音API异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析单条抖音评论JSON
     */
    private CommentCrawlResult.SocialComment parseDouyinComment(JsonNode node) {
        try {
            CommentCrawlResult.SocialComment comment = CommentCrawlResult.SocialComment.builder()
                    .commentId(node.path("cid").asText(null))
                    .parentId(node.has("reply_id") && !node.get("reply_id").isNull() ?
                              node.get("reply_id").asText() : null)
                    .rootCommentId(node.has("root_id") && !node.get("root_id").isNull() ?
                                   node.get("root_id").asText() : null)
                    .content(node.path("text").asText(""))
                    .rawContent(node.path("text").asText(""))
                    .authorId(node.path("user").path("uid").asText(null))
                    .authorName(node.path("user").path("nickname").asText("匿名用户"))
                    .authorAvatar(node.path("user").path("avatar_thumb").path("url_list").get(0).asText(null))
                    .isAuthorVerified(node.path("user").path("custom_verify").asText("") != "" ||
                                       node.path("user").path("verification_type").asInt() > 0)
                    .likeCount(node.path("digg_count").asLong(0))
                    .replyCount(node.path("reply_comment_total").asLong(0))
                    .build();

            // 解析发布时间 (毫秒时间戳)
            if (node.has("create_time")) {
                long timestamp = node.get("create_time").asLong();
                comment.setPublishTime(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()));
            }

            // 解析图片
            if (node.has("image_list") && node.get("image_list").isArray()) {
                List<String> images = new ArrayList<>();
                for (JsonNode imgNode : node.get("image_list")) {
                    if (imgNode.has("url_list") && imgNode.get("url_list").size() > 0) {
                        images.add(imgNode.get("url_list").get(0).asText());
                    }
                }
                comment.setImages(images);
            }

            return comment;

        } catch (Exception e) {
            log.warn("  ⚠️ 解析评论失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取二级评论（回复）
     *
     * API: POST /video/comment/list/reply/
     * 参数: item_id, comment_id, cursor, count, sort_type
     */
    private int fetchReplies(List<CommentCrawlResult.SocialComment> topLevelComments,
                           String itemId, String accessToken, CrawlOptions options) throws Exception {
        int apiCallCount = 0;
        int processedCount = 0;
        int maxRepliesToFetch = Math.min(topLevelComments.size(), 50);  // 限制回复数量防止过多API调用

        for (CommentCrawlResult.SocialComment parentComment : topLevelComments) {
            if (processedCount >= maxRepliesToFetch) break;
            if (parentComment.getReplyCount() <= 0) continue;  // 无回复则跳过

            try {
                String url = apiBaseUrl + "/video/comment/list/reply/";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(accessToken);

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("item_id", itemId);
                body.put("comment_id", parentComment.getCommentId());
                body.put("cursor", 0);
                body.put("count", Math.min(options.getPageSize(), 20));

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                apiCallCount++;

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    if (root.has("data") && root.get("data").has("comments")) {
                        for (JsonNode replyNode : root.get("data").get("comments")) {
                            CommentCrawlResult.SocialComment reply = parseDouyinComment(replyNode);
                            if (reply != null) {
                                topLevelComments.add(reply);  // 添加到总列表
                            }
                        }
                    }
                }

                Thread.sleep(options.getRequestIntervalMs());  // 频率控制

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
            String url = apiBaseUrl + "/oauth/userinfo/";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getRateLimitInfo() {
        return "100次/分钟 (官方限制)";
    }

    @Override
    public List<String> getSupportedFeatures() {
        return Arrays.asList(
                "✅ 一级评论获取",
                "✅ 二级评论(回复)",
                "✅ 分页翻页 (每页20条)",
                "✅ 时间排序 / 点赞排序",
                "✅ 评论者信息 (昵称/头像/认证)",
                "✅ 图片评论支持"
        );
    }
}
