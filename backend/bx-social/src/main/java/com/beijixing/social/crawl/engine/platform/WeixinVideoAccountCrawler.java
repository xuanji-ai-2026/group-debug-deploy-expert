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
public class WeixinVideoAccountCrawler extends AbstractPlatformCrawler {

    private static final String BASE_URL = "https://channels.weixin.qq.com";
    private static final String API_BASE = "https://mp.weixin.qq.com";

    private final RestTemplate restTemplate;

    public WeixinVideoAccountCrawler(org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                                     RiskControlEngine riskControlEngine,
                                     RestTemplate restTemplate) {
        super(redisTemplate, riskControlEngine);
        this.restTemplate = restTemplate;
    }

    @Override
    public String getPlatformCode() {
        return "WEIXIN_VIDEO_ACCOUNT";
    }

    @Override
    public String getPlatformName() {
        return "微信视频号";
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
                "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/125.0.0.0 Mobile Safari/537.36 MicroMessenger/8.0.50",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.50"
        };
        return userAgents[ThreadLocalRandom.current().nextInt(userAgents.length)];
    }

    @Override
    protected JSONObject executeSingleRequest(CrawlTaskContext context) {
        try {
            String feedId = extractFeedId(context);
            if (feedId == null) {
                log.warn("无法提取视频号ID");
                return null;
            }

            String lastCommentId = context.getCursor() > 0 ? 
                                   String.valueOf(context.getCursor()) : "0";
            String apiUrl = String.format(
                    "%s/cgi-bin/homebiz/sns/webpage/comment/list?feedid=%s&last_comment_id=%s&count=20",
                    API_BASE, feedId, lastCommentId);

            org.springframework.http.HttpHeaders headers = createWeixinHeaders(context);

            log.debug("执行微信视频号评论请求: feedId={}, last_id={}", feedId, lastCommentId);

            HttpEntity<String> entity = new HttpEntity<>(new JSONObject().toJSONString(), headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    apiUrl, entity, String.class);
            
            if (responseEntity.getBody() != null) {
                JSONObject response = JSON.parseObject(responseEntity.getBody());
                if (response != null && response.getIntValue("base_resp") == 0) {
                    return response;
                } else {
                    log.warn("微信视频号API返回错误: {}", response != null ? response.toJSONString() : "null");
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("微信视频号请求执行失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    protected List<SocialComment> parseComments(JSONObject response, CrawlTaskContext context) {
        List<SocialComment> comments = new ArrayList<>();

        try {
            if (response == null || !response.containsKey("comment_list")) {
                log.warn("微信视频号响应为空或缺少comment_list字段");
                return comments;
            }
            JSONArray commentList = response.getJSONArray("comment_list");
            if (commentList == null || commentList.isEmpty()) return comments;

            for (int i = 0; i < commentList.size(); i++) {
                JSONObject commentObj = commentList.getJSONObject(i);

                SocialComment comment = new SocialComment();
                comment.setCommentId(commentObj.getString("comment_id"));
                comment.setCommentText(commentObj.getString("content"));

                JSONObject user = commentObj.getJSONObject("user");
                if (user != null) {
                    comment.setAuthorId(user.getString("openid"));
                    comment.setAuthorName(user.getString("nickname"));
                    comment.setAuthorAvatar(user.getString("head_img_url"));
                }

                comment.setLikeCount(commentObj.getIntValue("like_num"));
                comment.setPublishTime(LocalDateTime.now());
                comment.setParentCommentId(commentObj.getString("ref_comment_id"));
                comment.setCrawlSource(getPlatformCode());
                comment.setPlatformCode(getPlatformCode());
                comment.setCreateTime(LocalDateTime.now());

                comments.add(comment);
            }

            log.info("解析微信视频号评论数: {}", comments.size());
        } catch (Exception e) {
            log.error("解析微信视频号评论失败: {}", e.getMessage());
        }

        return comments;
    }

    @Override
    protected void updatePaginationContext(CrawlTaskContext context, JSONObject response) {
        try {
            String lastCommentId = response.getString("last_comment_id");
            context.setCursor(lastCommentId != null && !lastCommentId.isEmpty() ? 
                             Integer.parseInt(lastCommentId) : 0);
            context.setHasMore(response.getBooleanValue("has_more"));
        } catch (Exception e) {
            log.warn("更新分页上下文失败: {}", e.getMessage());
        }
    }

    @Override
    public void handleRateLimit(CrawlTaskContext context) {
        log.warn("微信视频号触发频率限制: taskId={}, platform={}", 
                context.getTask().getId(), getPlatformCode());
        long delay = getDefaultRateLimitDelayMs() * 5;
        safeSleep(delay + ThreadLocalRandom.current().nextLong(5000, 15000));
    }

    @Override
    public boolean validateResponse(CrawlTaskContext context, Object response) {
        if (response == null || !(response instanceof JSONObject)) {
            return false;
        }
        JSONObject json = (JSONObject) response;
        Integer baseResp = json.getInteger("base_resp");
        return baseResp != null && baseResp == 0;
    }

    private String extractFeedId(CrawlTaskContext context) {
        String targetUrl = context.getTask().getTargetUrl();
        if (targetUrl != null && targetUrl.contains("/finder/")) {
            int start = targetUrl.indexOf("/finder/") + "/finder/".length();
            int end = targetUrl.indexOf("?", start);
            return end > 0 ? targetUrl.substring(start, end) : targetUrl.substring(start);
        }
        return context.getTask().getTargetId();
    }

    private org.springframework.http.HttpHeaders createWeixinHeaders(CrawlTaskContext context) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("User-Agent", context.getUserAgent());
        headers.set("Cookie", context.getCookie());
        
        if (context.getAccessToken() != null && !context.getAccessToken().isEmpty()) {
            headers.set("Authorization", "Bearer " + context.getAccessToken());
        }
        
        headers.set("Referer", BASE_URL + "/");
        headers.set("Accept", "application/json");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9");
        return headers;
    }
}
