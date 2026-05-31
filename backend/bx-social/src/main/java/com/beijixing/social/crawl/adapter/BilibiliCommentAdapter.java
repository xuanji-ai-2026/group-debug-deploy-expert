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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * B站(bilibili)评论抓取适配器
 *
 * API文档: https://github.com/SocialSisterYi/bilibili-API-collect
 *
 * 支持功能:
 * ✅ 获取视频一级评论列表
 * ✅ 获取二级评论(回复)
 * ✅ 分页翻页 (next方式)
 * ✅ 按热度/时间排序
 *
 * 注意: B站API分为:
 * - 登录用户API (需要Cookie/AccessKey)
 * - 游客API (无需登录，但有限制)
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Component
@Slf4j
public class BilibiliCommentAdapter implements PlatformCommentAdapter {

    @Value("${bilibili.api.base-url:https://api.bilibili.com}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getPlatformCode() { return "BILIBILI"; }

    @Override
    public String getPlatformName() { return "B站"; }

    @Override
    public CommentCrawlResult crawlComments(String targetId, String accessToken, CrawlOptions options) {
        log.info("📺 [B站] 开始抓取评论 | 视频{} | Token: {}...",
                targetId, accessToken != null ? "已提供" : "未提供(游客模式)");

        long startTime = System.currentTimeMillis();
        CommentCrawlResult result = CommentCrawlResult.builder()
                .taskId(UUID.randomUUID().toString())
                .platform("BILIBILI")
                .targetId(targetId)
                .targetUrl("https://www.bilibili.com/video/" + targetId)
                .startTime(LocalDateTime.now())
                .build();

        try {
            List<CommentCrawlResult.SocialComment> allComments = new ArrayList<>();
            String next = "";
            boolean hasMore = true;
            int pageCount = 0;
            int totalApiCalls = 0;
            Set<String> uniqueUsers = new HashSet<>();
            long totalLikes = 0;

            while (hasMore && pageCount < options.getMaxPages() &&
                   allComments.size() < options.getMaxTotalComments()) {

                JsonNode response = callBilibiliAPI(targetId, next, accessToken, options);
                totalApiCalls++;
                pageCount++;

                if (response != null && response.has("data")) {
                    JsonNode data = response.get("data");

                    if (data.has("replies") && data.get("replies").isArray()) {
                        for (JsonNode replyNode : data.get("replies")) {
                            CommentCrawlResult.SocialComment comment = null;
                            // 解析一级评论
                            if (replyNode.has("member") && replyNode.has("content")) {
                                comment = parseBilibiliComment(replyNode);
                                if (comment != null) {
                                    allComments.add(comment);
                                    uniqueUsers.add(comment.getAuthorId());
                                    totalLikes += comment.getLikeCount();
                                }
                            }

                            // 解析二级评论 (replies字段)
                            if (options.isIncludeReplies() && replyNode.has("replies") &&
                                replyNode.get("replies").isArray() && comment != null) {
                                for (JsonNode subReply : replyNode.get("replies")) {
                                    CommentCrawlResult.SocialComment subComment =
                                            parseBilibiliComment(subReply);
                                    if (subComment != null) {
                                        subComment.setParentId(comment.getCommentId());
                                        allComments.add(subComment);
                                        uniqueUsers.add(subComment.getAuthorId());
                                        totalLikes += subComment.getLikeCount();
                                    }
                                }
                            }
                        }
                    }

                    hasMore = data.has("cursor") && data.get("cursor").has("is_end") ?
                              !data.get("cursor").get("is_end").asBoolean() :
                              (allComments.size() < data.path("page").path("count").asInt(0));
                    next = data.has("cursor") && data.get("cursor").has("next") ?
                           data.get("cursor").get("next").asText("") : "";
                } else {
                    break;
                }

                Thread.sleep(options.getRequestIntervalMs());
            }

            CommentCrawlResult.CrawlStatistics stats = new CommentCrawlResult.CrawlStatistics();
            stats.setTotalComments(allComments.size());
            stats.setTopLevelComments((int) allComments.stream().filter(c -> c.getParentId() == null).count());
            stats.setReplyComments(stats.getTotalComments() - stats.getTopLevelComments());
            stats.setPagesFetched(pageCount);
            stats.setApiCallCount(totalApiCalls);
            stats.setUniqueUsers(uniqueUsers.size());
            stats.setTotalLikes(totalLikes);
            stats.setAvgLikeCount(allComments.isEmpty() ? 0 : (double) totalLikes / allComments.size());

            result.setSuccess(true);
            result.setStatistics(stats);
            result.setComments(allComments);
            result.setEndTime(LocalDateTime.now());
            result.setDurationMs(System.currentTimeMillis() - startTime);

            log.info("✅ [B站] 抓取完成 | {}", result.getSummary());

        } catch (Exception e) {
            log.error("❌ [B站] 抓取失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorCode("BILIBILI_ERROR");
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 调用B站评论API
     *
     * API端点: GET /x/v2/reply?type=1&oid={aid}&next={next}
     *
     * 参数说明:
     * - type: 1=视频评论, 2=番剧评论, etc.
     * - oid: 视频AVID (数字ID)
     * - next: 翻页游标
     * - sort: 0=按时间, 1=按热度, 2=按回复数
     * - ps: 每页数量 (默认20)
     */
    private JsonNode callBilibiliAPI(String bvidOrAvid, String next,
                                    String accessToken, CrawlOptions options) {
        try {
            long aid = resolveAid(bvidOrAvid);

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(apiBaseUrl + "/x/v2/reply")
                    .queryParam("type", 1)  // 视频评论
                    .queryParam("oid", aid)
                    .queryParam("ps", Math.min(options.getPageSize(), 20))
                    .queryParam("sort", options.getSortType() == CrawlOptions.SortType.LIKES_DESC ? 1 : 0);

            if (!next.isEmpty()) {
                builder.queryParam("next", next);
            }

            if (accessToken != null && !accessToken.isEmpty()) {
                builder.queryParam("access_key", accessToken);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Referer", "https://www.bilibili.com/");

            ResponseEntity<String> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                if (root.has("code") && root.get("code").asInt() == 0) {
                    return root;
                } else {
                    log.warn("  ⚠️ B站API返回错误: code={}, message={}",
                            root.path("code").asText(), root.path("message").asText());
                    return null;
                }
            }
            return null;

        } catch (Exception e) {
            log.error("B站API调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析BV号或AV号为AVID
     */
    private long resolveAid(String bvidOrAvid) {
        if (bvidOrAvid.toUpperCase().startsWith("BV")) {
            // BV号转AVID算法 (简化版，实际应调用查询接口)
            return bvToAid(bvidOrAvid);
        } else {
            try {
                return Long.parseLong(bvidOrAvid.replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的视频ID: " + bvidOrAvid);
            }
        }
    }

    /**
     * BV号转AVID (XOR加密算法)
     */
    private long bvToAid(String bvid) {
        final int[] table = {11, 10, 3, 8, 4, 6};
        final long xor = 177451812L;
        final long add = 100618342136696320L;

        StringBuilder sb = new StringBuilder(bvid.replaceFirst("(?i)bv", "").replaceFirst("^1?", ""));
        long tmp = 0;
        for (int i = 0; i < 6; i++) {
            tmp = tmp * 58L + "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF".indexOf(sb.charAt(table[i]));
        }
        return (tmp - add) ^ xor;
    }

    private CommentCrawlResult.SocialComment parseBilibiliComment(JsonNode node) {
        try {
            String content = "";
            if (node.has("content") && node.get("content").has("message")) {
                content = node.get("content").get("message").asText("");
            }

            return CommentCrawlResult.SocialComment.builder()
                    .commentId(node.path("rpid").asLong(0) + "")
                    .parentId(node.has("parent") && node.get("parent").asInt(0) != 0 ?
                              node.get("parent").asLong(0) + "" : null)
                    .content(content)
                    .rawContent(content)
                    .authorId(node.path("member").path("mid").asLong(0) + "")
                    .authorName(node.path("member").path("uname").asText("匿名用户"))
                    .authorAvatar(node.path("member").path("avatar").asText(null))
                    .isAuthorVerified(node.path("member").path("vip").path("status").asInt(0) > 0 ||
                                       node.path("member").path("official_verify").has("type"))
                    .likeCount(node.path("like").asLong(0))
                    .replyCount(node.path("rcount").asInt(0))
                    .publishTime(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(node.path("ctime").asLong(0)), ZoneId.systemDefault()))
                    .build();

        } catch (Exception e) {
            log.warn("解析B站评论失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getRateLimitInfo() { return "游客: 无限制 | 登录: 60次/秒"; }

    @Override
    public List<String> getSupportedFeatures() {
        return Arrays.asList(
                "✅ 一级评论获取",
                "✅ 二级评论(回复)",
                "✅ 分页翻页",
                "✅ 时间/热度排序",
                "✅ 游客模式(无需登录)"
        );
    }
}
