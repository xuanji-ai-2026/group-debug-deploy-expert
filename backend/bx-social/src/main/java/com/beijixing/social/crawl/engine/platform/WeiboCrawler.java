package com.beijixing.social.crawl.engine.platform;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.beijixing.social.crawl.engine.AbstractPlatformCrawler;
import com.beijixing.social.crawl.engine.CrawlTaskContext;
import com.beijixing.social.crawl.engine.RiskControlEngine;
import com.beijixing.social.crawl.entity.CrawlTask;
import com.beijixing.social.crawl.entity.SocialComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class WeiboCrawler extends AbstractPlatformCrawler {

    private static final String BASE_URL = "https://weibo.com";
    private static final String API_BASE = "https://m.weibo.cn";

    private final RestTemplate restTemplate;

    public WeiboCrawler(org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                        RiskControlEngine riskControlEngine,
                        RestTemplate restTemplate) {
        super(redisTemplate, riskControlEngine);
        this.restTemplate = restTemplate;
    }

    @Override
    public String getPlatformCode() {
        return "WEIBO";
    }

    @Override
    public String getPlatformName() {
        return "微博";
    }

    @Override
    public long getDefaultRateLimitDelayMs() {
        return 3000;
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
                "Mozilla/5.0 (Linux; Android 14; SM-G998B) Chrome/125.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 Version/17.1 Mobile/15E148 Safari/604.1"
        };
        return userAgents[ThreadLocalRandom.current().nextInt(userAgents.length)];
    }

    @Override
    protected JSONObject executeSingleRequest(CrawlTaskContext context) {
        try {
            String mid = extractMid(context);
            if (mid == null) {
                log.warn("无法提取微博ID");
                return null;
            }

            String apiUrl = String.format("%s/comments/hotflow?id=%s&mid=%s&max_id=%s&max_id_type=0",
                    API_BASE, mid, mid, context.getCursor() > 0 ? 
                    String.valueOf(context.getCursor()) : "0");

            org.springframework.http.HttpHeaders headers = createWeiboHeaders(context);

            log.debug("执行微博评论请求: mid={}, max_id={}", mid, context.getCursor());

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    apiUrl, org.springframework.http.HttpMethod.GET,
                    new org.springframework.http.HttpEntity<>(headers), String.class);

            if (responseEntity.getBody() != null) {
                return com.alibaba.fastjson2.JSON.parseObject(responseEntity.getBody());
            }
            return null;
        } catch (Exception e) {
            log.error("微博请求执行失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    protected List<SocialComment> parseComments(JSONObject response, CrawlTaskContext context) {
        List<SocialComment> comments = new ArrayList<>();

        try {
            if (response == null || !response.containsKey("data")) {
                log.warn("微博响应为空或缺少data字段");
                return comments;
            }
            JSONObject data = response.getJSONObject("data");
            if (data == null) return comments;

            JSONArray commentArray = data.getJSONArray("data");
            if (commentArray == null || commentArray.isEmpty()) return comments;

            for (int i = 0; i < commentArray.size(); i++) {
                JSONObject commentObj = commentArray.getJSONObject(i);

                SocialComment comment = new SocialComment();
                comment.setCommentId(commentObj.getString("id"));
                comment.setCommentText(cleanWeiboText(commentObj.getString("text")));

                JSONObject user = commentObj.getJSONObject("user");
                if (user != null) {
                    comment.setAuthorId(user.getString("id"));
                    comment.setAuthorName(user.getString("screen_name"));
                }

                comment.setLikeCount(commentObj.getIntValue("like_count"));
                comment.setPublishTime(java.time.LocalDateTime.now());
                comment.setCrawlSource(getPlatformCode());
                comment.setPlatformCode(getPlatformCode());
                comment.setCreateTime(java.time.LocalDateTime.now());

                comments.add(comment);
            }

            log.info("解析微博评论数: {}", comments.size());
        } catch (Exception e) {
            log.error("解析微博评论失败: {}", e.getMessage());
        }

        return comments;
    }

    @Override
    protected void updatePaginationContext(CrawlTaskContext context, JSONObject response) {
        try {
            JSONObject data = response.getJSONObject("data");
            if (data != null) {
                String maxId = data.getString("max_id");
                context.setCursor(maxId != null && !maxId.isEmpty() ? 
                                 Integer.parseInt(maxId) : 0);
                context.setHasMore(data.getBooleanValue("has_next") && 
                                  !"0".equals(maxId));
            }
        } catch (Exception e) {
            log.warn("更新分页上下文失败: {}", e.getMessage());
        }
    }

    private String extractMid(CrawlTaskContext context) {
        String targetUrl = context.getTask().getTargetUrl();
        if (targetUrl != null) {
            if (targetUrl.contains("/detail/")) {
                int start = targetUrl.indexOf("/detail/") + "/detail/".length();
                int end = targetUrl.indexOf("?", start);
                return end > 0 ? targetUrl.substring(start, end) : targetUrl.substring(start);
            } else if (targetUrl.contains("?mid=")) {
                int start = targetUrl.indexOf("?mid=") + "?mid=".length();
                int end = targetUrl.indexOf("&", start);
                return end > 0 ? targetUrl.substring(start, end) : targetUrl.substring(start);
            }
        }
        return context.getTask().getTargetId();
    }

    @Override
    public void handleRateLimit(CrawlTaskContext context) {
        log.warn("微博触发频率限制: taskId={}, platform={}", 
                context.getTask().getId(), getPlatformCode());
        long delay = getDefaultRateLimitDelayMs() * 3;
        safeSleep(delay + ThreadLocalRandom.current().nextLong(5000, 15000));
    }

    @Override
    public boolean validateResponse(CrawlTaskContext context, Object response) {
        if (response == null || !(response instanceof JSONObject)) {
            return false;
        }
        JSONObject json = (JSONObject) response;
        Integer ok = json.getInteger("ok");
        return ok != null && ok == 1;
    }

    private org.springframework.http.HttpHeaders createWeiboHeaders(CrawlTaskContext context) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("User-Agent", context.getUserAgent());
        headers.set("Cookie", context.getCookie());
        headers.set("X-Requested-With", "XMLHttpRequest");
        headers.set("Referer", BASE_URL + "/");
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        return headers;
    }

    private String cleanWeiboText(String text) {
        if (text == null) return "";
        text = text.replaceAll("<[^>]+>", "");
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&amp;", "&");
        return text.trim();
    }
}
