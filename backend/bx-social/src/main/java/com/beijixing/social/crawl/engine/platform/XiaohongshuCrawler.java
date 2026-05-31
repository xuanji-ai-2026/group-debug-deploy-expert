package com.beijixing.social.crawl.engine.platform;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.beijixing.social.crawl.entity.CrawlTask;
import com.beijixing.social.crawl.entity.SocialComment;
import com.beijixing.social.crawl.engine.AbstractPlatformCrawler;
import com.beijixing.social.crawl.engine.CrawlTaskContext;
import com.beijixing.social.crawl.engine.RiskControlEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class XiaohongshuCrawler extends AbstractPlatformCrawler {

    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://edith.xiaohongshu.com";

    public XiaohongshuCrawler(StringRedisTemplate redisTemplate, 
                               RiskControlEngine riskControlEngine,
                               RestTemplate restTemplate) {
        super(redisTemplate, riskControlEngine);
        this.restTemplate = restTemplate;
    }

    @Override
    public String getPlatformCode() {
        return "XIAOHONGSHU";
    }

    @Override
    public String getPlatformName() {
        return "小红书";
    }

    @Override
    public long getDefaultRateLimitDelayMs() {
        return 2500;
    }

    @Override
    protected String getAccessToken(CrawlTask task) {
        String key = "xhs:cookie:" + task.getTargetId();
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    protected String getCookie(CrawlTask task) {
        return getAccessToken(task);
    }

    @Override
    protected String getUserAgent(CrawlTask task) {
        return "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1";
    }

    protected void setupProxy(CrawlTaskContext context) {
    }

    @Override
    protected JSONObject executeSingleRequest(CrawlTaskContext context) {
        try {
            String noteId = context.getTask().getTargetId();
            String url = BASE_URL + "/api/sns/v2/comment/page";
            
            HttpHeaders headers = createDefaultHeaders(context);
            headers.set("Referer", "https://www.xiaohongshu.com/explore/" + noteId);
            headers.set("Origin", "https://www.xiaohongshu.com");

            JSONObject requestBody = new JSONObject();
            requestBody.put("note_id", noteId);
            requestBody.put("cursor", context.getCursor() > 0 ? 
                              String.valueOf(context.getCursor()) : "0");
            requestBody.put("top_comment_id", "");
            requestBody.put("image_formats", "jpg,webp,avif");

            String xSign = generateXSign(url, context.getCookie(), System.currentTimeMillis());
            headers.set("X-s", xSign);
            headers.set("X-t", String.valueOf(System.currentTimeMillis()));

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getBody() != null) {
                return JSON.parseObject(response.getBody());
            }
        } catch (Exception e) {
            log.error("小红书API请求失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateXSign(String url, String cookie, long timestamp) {
        String signKey = redisTemplate.opsForValue().get("xhs:sign_key");
        if (signKey == null) {
            return "default_sign_" + timestamp;
        }
        
        try {
            String data = url + cookie + timestamp;
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "fallback_sign_" + timestamp;
        }
    }

    @Override
    protected List<SocialComment> parseComments(JSONObject response, CrawlTaskContext context) {
        List<SocialComment> comments = new ArrayList<>();

        if (response == null || !response.containsKey("data")) {
            log.warn("小红书响应为空或缺少data字段");
            return comments;
        }

        JSONObject data = response.getJSONObject("data");
        if (data == null) return comments;

        JSONArray commentsArray = data.getJSONArray("comments");
        if (commentsArray == null || commentsArray.isEmpty()) {
            return comments;
        }

        for (int i = 0; i < commentsArray.size(); i++) {
            JSONObject commentObj = commentsArray.getJSONObject(i);
            SocialComment comment = parseXhsComment(commentObj, context);
            comments.add(comment);
        }

        return comments;
    }

    private SocialComment parseXhsComment(JSONObject commentObj, CrawlTaskContext context) {
        SocialComment comment = createBaseComment(commentObj, context);
        
        comment.setCommentId(commentObj.getString("id"));
        comment.setCommentText(commentObj.getString("content"));
        comment.setLikeCount(commentObj.getIntValue("like_count"));
        comment.setReplyCount(commentObj.getIntValue("sub_comment_count"));

        long createTimeStamp = commentObj.getLongValue("create_time");
        if (createTimeStamp > 0) {
            comment.setPublishTime(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(createTimeStamp), ZoneId.systemDefault()));
        }

        JSONObject userInfo = commentObj.getJSONObject("user_info");
        if (userInfo != null) {
            comment.setAuthorId(userInfo.getString("user_id"));
            comment.setAuthorName(userInfo.getString("nickname"));
            comment.setAuthorAvatar(userInfo.getString("image"));

            JSONObject interactInfo = userInfo.getJSONObject("interact_info");
            if (interactInfo != null) {
                comment.setUserFollowerCount(interactInfo.getIntValue("follower_count"));
                comment.setUserFollowingCount(interactInfo.getIntValue("following_count"));
            }
        }

        extractContactInfo(comment);

        comment.setRawData(commentObj.toJSONString());

        return comment;
    }

    @Override
    protected void updatePaginationContext(CrawlTaskContext context, JSONObject response) {
        JSONObject data = response.getJSONObject("data");
        if (data != null) {
            context.setCursor(data.getIntValue("cursor"));
            context.setHasMore(data.getBooleanValue("has_more"));
        }
        context.setLastResponse(response);
    }

    @Override
    public void handleRateLimit(CrawlTaskContext context) {
        log.warn("小红书触发频率限制: taskId={}, platform={}", 
                context.getTask().getId(), getPlatformCode());
        long delay = getDefaultRateLimitDelayMs() * 5;
        safeSleep(delay);
    }

    private void extractContactInfo(SocialComment comment) {
        if (comment == null || comment.getAuthorName() == null) {
            return;
        }
        
        String rawText = comment.getRawData();
        if (rawText != null) {
            JSONObject rawDataObj = com.alibaba.fastjson2.JSON.parseObject(rawText);
            if (rawDataObj != null) {
                String phone = extractPhoneNumber(rawDataObj);
                String wechat = extractWechatId(rawDataObj);
                
                comment.setExtractedPhone(phone);
                comment.setExtractedWechat(wechat);
                comment.setHasPhoneContact(phone != null && !phone.isEmpty());
                comment.setHasWechatContact(wechat != null && !wechat.isEmpty());
            }
        }
    }

    private String extractPhoneNumber(JSONObject obj) {
        if (obj.containsKey("phone")) return obj.getString("phone");
        if (obj.containsKey("mobile")) return obj.getString("mobile");
        return null;
    }

    private String extractWechatId(JSONObject obj) {
        if (obj.containsKey("wechat")) return obj.getString("wechat");
        if (obj.containsKey("wx_id")) return obj.getString("wx_id");
        return null;
    }

    @Override
    public boolean validateResponse(CrawlTaskContext context, Object response) {
        if (response == null || !(response instanceof JSONObject)) {
            return false;
        }
        JSONObject json = (JSONObject) response;
        Integer code = json.getInteger("code");
        return code != null && code == 0;
    }
}
