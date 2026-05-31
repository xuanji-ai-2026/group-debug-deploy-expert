package com.beijixing.social.crawl.engine.platform;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.beijixing.social.crawl.engine.AbstractPlatformCrawler;
import com.beijixing.social.crawl.engine.CrawlTaskContext;
import com.beijixing.social.crawl.engine.RiskControlEngine;
import com.beijixing.social.crawl.entity.CrawlTask;
import com.beijixing.social.crawl.entity.SocialComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class KuaishouCrawler extends AbstractPlatformCrawler {

    private static final String BASE_URL = "https://www.kuaishou.com";
    private static final String COMMENTS_API = "/graphql";

    private final RestTemplate restTemplate;

    public KuaishouCrawler(org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                           RiskControlEngine riskControlEngine,
                           RestTemplate restTemplate) {
        super(redisTemplate, riskControlEngine);
        this.restTemplate = restTemplate;
    }

    @Override
    public String getPlatformCode() {
        return "KUAISHOU";
    }

    @Override
    public String getPlatformName() {
        return "快手";
    }

    @Override
    public long getDefaultRateLimitDelayMs() {
        return 2000;
    }

    @Override
    protected String getAccessToken(CrawlTask task) {
        return task.getKeywords();
    }

    @Override
    protected String getCookie(CrawlTask task) {
        return task.getFilterConditions();
    }

    @Override
    protected String getUserAgent(CrawlTask task) {
        String[] userAgents = {
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) Chrome/125.0.0.0 Mobile Safari/537.36"
        };
        return userAgents[ThreadLocalRandom.current().nextInt(userAgents.length)];
    }

    @Override
    protected JSONObject executeSingleRequest(CrawlTaskContext context) {
        try {
            String photoId = extractPhotoId(context);
            if (photoId == null) {
                log.warn("无法提取快手视频ID");
                return null;
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("operationName", "commentListQuery");
            requestBody.put("variables", buildVariables(photoId, context.getCursor()));
            requestBody.put("query", getGraphQLQuery());

            org.springframework.http.HttpHeaders headers = createKuaishouHeaders(context);
            
            log.debug("执行快手评论请求: photoId={}, cursor={}", photoId, context.getCursor());

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    BASE_URL + COMMENTS_API, entity, String.class);

            if (responseEntity.getBody() != null) {
                return JSON.parseObject(responseEntity.getBody());
            }
            return null;
        } catch (Exception e) {
            log.error("快手请求执行失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    protected List<SocialComment> parseComments(JSONObject response, CrawlTaskContext context) {
        List<SocialComment> comments = new ArrayList<>();

        try {
            if (response == null || !response.containsKey("data")) {
                log.warn("快手响应为空或缺少data字段");
                return comments;
            }
            JSONObject data = response.getJSONObject("data");
            if (data == null) return comments;

            JSONObject commentList = data.getJSONObject("commentList");
            if (commentList == null) return comments;

            JSONArray commentArray = commentList.getJSONArray("comments");
            if (commentArray == null || commentArray.isEmpty()) return comments;

            for (int i = 0; i < commentArray.size(); i++) {
                JSONObject commentObj = commentArray.getJSONObject(i);
                
                SocialComment comment = new SocialComment();
                comment.setCommentId(commentObj.getString("commentId"));
                comment.setCommentText(commentObj.getString("content"));
                comment.setAuthorId(commentObj.getString("authorId"));
                comment.setAuthorName(commentObj.getString("authorName"));
                comment.setLikeCount(commentObj.getIntValue("likeCount"));
                comment.setPublishTime(LocalDateTime.now());
                comment.setParentCommentId(commentObj.getString("parentCommentId"));
                comment.setReplyToUserId(commentObj.getString("replyToUserId"));
                comment.setCrawlSource(getPlatformCode());
                comment.setPlatformCode(getPlatformCode());
                comment.setCreateTime(LocalDateTime.now());

                comments.add(comment);
            }

            log.info("解析快手评论数: {}", comments.size());
        } catch (Exception e) {
            log.error("解析快手评论失败: {}", e.getMessage());
        }

        return comments;
    }

    @Override
    protected void updatePaginationContext(CrawlTaskContext context, JSONObject response) {
        try {
            JSONObject data = response.getJSONObject("data");
            if (data != null) {
                JSONObject commentList = data.getJSONObject("commentList");
                if (commentList != null) {
                    String nextCursor = commentList.getString("nextCursor");
                    context.setCursor(nextCursor != null && !nextCursor.isEmpty() ? 
                                     Integer.parseInt(nextCursor) : 0);
                    context.setHasMore(commentList.getBooleanValue("hasMore"));
                }
            }
        } catch (Exception e) {
            log.warn("更新分页上下文失败: {}", e.getMessage());
        }
    }

    @Override
    public void handleRateLimit(CrawlTaskContext context) {
        log.warn("快手触发频率限制: taskId={}, platform={}", 
                context.getTask().getId(), getPlatformCode());
        long delay = getDefaultRateLimitDelayMs() * 4;
        safeSleep(delay + ThreadLocalRandom.current().nextLong(3000, 10000));
    }

    @Override
    public boolean validateResponse(CrawlTaskContext context, Object response) {
        if (response == null || !(response instanceof JSONObject)) {
            return false;
        }
        JSONObject json = (JSONObject) response;
        return json.containsKey("data") && json.getJSONObject("data") != null;
    }

    private String extractPhotoId(CrawlTaskContext context) {
        String targetUrl = context.getTask().getTargetUrl();
        if (targetUrl != null && targetUrl.contains("/short-video/")) {
            int start = targetUrl.indexOf("/short-video/") + "/short-video/".length();
            int end = targetUrl.indexOf("?", start);
            return end > 0 ? targetUrl.substring(start, end) : targetUrl.substring(start);
        }
        return context.getTask().getTargetId();
    }

    private JSONObject buildVariables(String photoId, int cursor) {
        JSONObject variables = new JSONObject();
        variables.put("photoId", photoId);
        variables.put("cursor", String.valueOf(cursor));
        variables.put("count", 20);
        return variables;
    }

    private String getGraphQLQuery() {
        return "query commentListQuery($photoId: String, $cursor: String, $count: Int) { " +
               "commentList(photoId: $photoId, cursor: $cursor, count: $count) { " +
               "comments { commentId content authorId authorName likeCount timestamp parentCommentId replyToUserId } " +
               "nextCursor hasMore } }";
    }

    private org.springframework.http.HttpHeaders createKuaishouHeaders(CrawlTaskContext context) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("User-Agent", context.getUserAgent());
        headers.set("Cookie", context.getCookie());
        headers.set("Referer", BASE_URL + "/");
        headers.setOrigin(BASE_URL);
        headers.set("Accept", "*/*");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9");
        return headers;
    }
}
