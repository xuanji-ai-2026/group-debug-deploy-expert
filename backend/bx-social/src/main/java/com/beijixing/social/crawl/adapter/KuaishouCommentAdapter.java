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
 * 快手评论抓取适配器
 *
 * API文档: https://open.kuaishou.com/platform/openApi?tab=5
 *
 * 支持功能:
 * ✅ 获取视频一级评论列表
 * ✅ 获取二级评论(回复)
 * ✅ 分页翻页 (cursor方式)
 *
 * 所需权限: comment.read
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Component
@Slf4j
public class KuaishouCommentAdapter implements PlatformCommentAdapter {

    @Value("${kuaishou.api.base-url:https://open.kuaishou.com}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getPlatformCode() { return "KUAISHOU"; }

    @Override
    public String getPlatformName() { return "快手"; }

    @Override
    public CommentCrawlResult crawlComments(String targetId, String accessToken, CrawlOptions options) {
        log.info("⚡ [快手] 开始抓取评论 | 视频ID: {}", targetId);

        long startTime = System.currentTimeMillis();
        CommentCrawlResult result = CommentCrawlResult.builder()
                .taskId(UUID.randomUUID().toString())
                .platform("KUAISHOU")
                .targetId(targetId)
                .startTime(LocalDateTime.now())
                .build();

        try {
            List<CommentCrawlResult.SocialComment> allComments = new ArrayList<>();
            String cursor = "";
            boolean hasMore = true;
            int pageCount = 0;
            int totalApiCalls = 0;
            Set<String> uniqueUsers = new HashSet<>();
            long totalLikes = 0;

            while (hasMore && pageCount < options.getMaxPages() &&
                   allComments.size() < options.getMaxTotalComments()) {

                JsonNode response = callKuaishouAPI(targetId, cursor, accessToken, options);
                totalApiCalls++;
                pageCount++;

                if (response != null && response.has("data")) {
                    JsonNode data = response.get("data");

                    if (data.has("commentList") && data.get("commentList").isArray()) {
                        for (JsonNode node : data.get("commentList")) {
                            CommentCrawlResult.SocialComment comment = parseKuaishouComment(node);
                            if (comment != null) {
                                allComments.add(comment);
                                uniqueUsers.add(comment.getAuthorId());
                                totalLikes += comment.getLikeCount();
                            }
                        }
                    }

                    hasMore = data.has("hasMore") && data.get("hasMore").asBoolean();
                    cursor = data.has("cursor") ? data.get("cursor").asText("") : "";
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

            log.info("✅ [快手] 抓取完成 | {}", result.getSummary());

        } catch (Exception e) {
            log.error("❌ [快手] 抓取失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorCode("KUAISHOU_ERROR");
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private JsonNode callKuaishouAPI(String photoId, String cursor,
                                    String accessToken, CrawlOptions options) {
        try {
            String url = apiBaseUrl + "/openapi/photo/comment/list";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("photoId", photoId);
            body.put("cursor", cursor.isEmpty() ? "0" : cursor);
            body.put("count", Math.min(options.getPageSize(), 20));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            }
            return null;
        } catch (Exception e) {
            log.error("快手API调用失败: {}", e.getMessage());
            return null;
        }
    }

    private CommentCrawlResult.SocialComment parseKuaishouComment(JsonNode node) {
        return CommentCrawlResult.SocialComment.builder()
                .commentId(node.path("commentId").asText(null))
                .parentId(node.has("parentCommentId") && !node.get("parentCommentId").isNull() ?
                          node.get("parentCommentId").asText() : null)
                .content(node.path("content").asText(""))
                .authorId(node.path("authorId").asText(null))
                .authorName(node.path("authorName").asText("匿名用户"))
                .likeCount(node.path("likeCount").asLong(0))
                .replyCount(node.path("replyCount").asLong(0))
                .publishTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(node.path("timestamp").asLong(0)), ZoneId.systemDefault()))
                .build();
    }

    @Override
    public String getRateLimitInfo() { return "100次/分钟"; }

    @Override
    public List<String> getSupportedFeatures() {
        return Arrays.asList("✅ 一级评论", "✅ 二级回复", "✅ 分页翻页");
    }
}
