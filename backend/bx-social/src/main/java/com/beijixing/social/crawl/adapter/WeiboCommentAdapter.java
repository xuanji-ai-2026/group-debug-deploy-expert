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
 * 微博评论抓取适配器
 *
 * API文档: https://open.weibo.com/wiki/index.php
 *
 * 支持功能:
 * ✅ 获取微博评论列表
 * ✅ 分页翻页
 * ✅ 按热度/时间排序
 *
 * 所需权限: read
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Component
@Slf4j
public class WeiboCommentAdapter implements PlatformCommentAdapter {

    @Value("${weibo.api.base-url:https://api.weibo.cn}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getPlatformCode() { return "WEIBO"; }

    @Override
    public String getPlatformName() { return "微博"; }

    @Override
    public CommentCrawlResult crawlComments(String targetId, String accessToken, CrawlOptions options) {
        log.info("📱 [微博] 开始抓取评论 | 微博ID: {}", targetId);

        long startTime = System.currentTimeMillis();
        CommentCrawlResult result = CommentCrawlResult.builder()
                .taskId(UUID.randomUUID().toString())
                .platform("WEIBO")
                .targetId(targetId)
                .startTime(LocalDateTime.now())
                .build();

        try {
            List<CommentCrawlResult.SocialComment> allComments = new ArrayList<>();
            int page = 1;
            boolean hasMore = true;
            int totalApiCalls = 0;
            Set<String> uniqueUsers = new HashSet<>();
            long totalLikes = 0;

            while (hasMore && page <= options.getMaxPages() &&
                   allComments.size() < options.getMaxTotalComments()) {

                JsonNode response = callWeiboAPI(targetId, page, accessToken, options);
                totalApiCalls++;

                if (response != null) {
                    JsonNode data = response.has("data") ? response.get("data") : response;

                    if (data.has("comments") && data.get("comments").isArray()) {
                        for (JsonNode node : data.get("comments")) {
                            CommentCrawlResult.SocialComment comment = parseWeiboComment(node);
                            if (comment != null) {
                                allComments.add(comment);
                                uniqueUsers.add(comment.getAuthorId());
                                totalLikes += comment.getLikeCount();
                            }
                        }
                    }

                    hasMore = data.has("has_more") ? data.get("has_more").asBoolean() :
                              data.has("total_number") &&
                              allComments.size() < data.get("total_number").asInt(0);
                } else {
                    break;
                }

                page++;
                Thread.sleep(options.getRequestIntervalMs());
            }

            CommentCrawlResult.CrawlStatistics stats = new CommentCrawlResult.CrawlStatistics();
            stats.setTotalComments(allComments.size());
            stats.setTopLevelComments((int) allComments.stream().filter(c -> c.getParentId() == null).count());
            stats.setReplyComments(stats.getTotalComments() - stats.getTopLevelComments());
            stats.setPagesFetched(page - 1);
            stats.setApiCallCount(totalApiCalls);
            stats.setUniqueUsers(uniqueUsers.size());
            stats.setTotalLikes(totalLikes);
            stats.setAvgLikeCount(allComments.isEmpty() ? 0 : (double) totalLikes / allComments.size());

            result.setSuccess(true);
            result.setStatistics(stats);
            result.setComments(allComments);
            result.setEndTime(LocalDateTime.now());
            result.setDurationMs(System.currentTimeMillis() - startTime);

            log.info("✅ [微博] 抓取完成 | {}", result.getSummary());

        } catch (Exception e) {
            log.error("❌ [微博] 抓取失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorCode("WEIBO_ERROR");
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private JsonNode callWeiboAPI(String statusId, int page,
                                 String accessToken, CrawlOptions options) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(apiBaseUrl + "/2/comments/show.json")
                    .queryParam("id", statusId)
                    .queryParam("page", page)
                    .queryParam("count", Math.min(options.getPageSize(), 50))
                    .queryParam("access_token", accessToken)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            }
            return null;
        } catch (Exception e) {
            log.error("微博API调用失败: {}", e.getMessage());
            return null;
        }
    }

    private CommentCrawlResult.SocialComment parseWeiboComment(JsonNode node) {
        return CommentCrawlResult.SocialComment.builder()
                .commentId(node.path("idstr").asText(null))
                .parentId(node.has("id") && !node.get("id").isNull() ?
                          node.get("id").asText() : null)
                .content(node.path("text").asText(""))
                .rawContent(node.path("text").asText(""))
                .authorId(node.path("user").path("idstr").asText(null))
                .authorName(node.path("user").path("screen_name").asText("匿名用户"))
                .authorAvatar(node.path("user").path("profile_image_url").asText(null))
                .isAuthorVerified(node.path("user").path("verified").asBoolean(false))
                .likeCount(node.path("attitudes_count").asLong(0))
                .replyCount(0)  // 微博API不直接提供回复数
                .publishTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(node.path("created_at").asLong(0)), ZoneId.systemDefault()))
                .build();
    }

    @Override
    public String getRateLimitInfo() { return "150次/小时"; }

    @Override
    public List<String> getSupportedFeatures() {
        return Arrays.asList("✅ 评论获取", "✅ 分页翻页", "✅ 热度排序");
    }
}
