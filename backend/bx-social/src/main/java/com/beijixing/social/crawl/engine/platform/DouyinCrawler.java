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
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class DouyinCrawler extends AbstractPlatformCrawler {

    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://open.douyin-oauth.douyin.com";
    private static final String COMMENTS_API = "/video/comments/list/";

    public DouyinCrawler(StringRedisTemplate redisTemplate, 
                         RiskControlEngine riskControlEngine,
                         RestTemplate restTemplate) {
        super(redisTemplate, riskControlEngine);
        this.restTemplate = restTemplate;
    }

    @Override
    public String getPlatformCode() {
        return "DOUYIN";
    }

    @Override
    public String getPlatformName() {
        return "抖音";
    }

    @Override
    public long getDefaultRateLimitDelayMs() {
        return 1500;
    }

    @Override
    protected String getAccessToken(CrawlTask task) {
        String key = "douyin:access_token:" + task.getTargetId();
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    protected String getCookie(CrawlTask task) {
        return null;
    }

    @Override
    protected String getUserAgent(CrawlTask task) {
        return "com.ss.android.ugc.aweme/270100 (Linux; U; Android 12; zh_CN; Pixel 6; Build/SQ3A.220705.003.A1; Cronet/TTNetVersion:fd8b80a4 2022-04-22 QuicVersion:0144d359 2022-03-17)";
    }

    @Override
    protected JSONObject executeSingleRequest(CrawlTaskContext context) {
        try {
            String url = BASE_URL + COMMENTS_API;
            HttpHeaders headers = createDefaultHeaders(context);

            JSONObject requestBody = new JSONObject();
            requestBody.put("item_id", context.getTask().getTargetId());
            requestBody.put("cursor", context.getCursor());
            requestBody.put("count", 20);
            requestBody.put("sort_type", 0);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return JSON.parseObject(response.getBody());
            }
        } catch (Exception e) {
            log.error("抖音API请求失败: {}", e.getMessage());
        }
        return null;
    }

    @Override
    protected List<SocialComment> parseComments(JSONObject response, CrawlTaskContext context) {
        List<SocialComment> comments = new ArrayList<>();

        if (response == null || !response.containsKey("comments")) {
            log.warn("抖音响应为空或缺少comments字段");
            return comments;
        }

        JSONArray commentsArray = response.getJSONArray("comments");
        if (commentsArray == null || commentsArray.isEmpty()) {
            return comments;
        }

        for (int i = 0; i < commentsArray.size(); i++) {
            JSONObject commentObj = commentsArray.getJSONObject(i);
            SocialComment comment = parseDouyinComment(commentObj, context);
            comments.add(comment);
        }

        return comments;
    }

    private SocialComment parseDouyinComment(JSONObject commentObj, CrawlTaskContext context) {
        SocialComment comment = createBaseComment(commentObj, context);
        
        comment.setCommentId(commentObj.getString("cid"));
        comment.setCommentText(commentObj.getString("text"));
        comment.setLikeCount(commentObj.getIntValue("digg_count"));
        comment.setReplyCount(commentObj.getIntValue("reply_comment_total"));
        comment.setPublishTime(LocalDateTime.now());

        JSONObject user = commentObj.getJSONObject("user");
        if (user != null) {
            comment.setAuthorId(user.getString("uid"));
            comment.setAuthorName(user.getString("nickname"));
            comment.setAuthorAvatar(user.getString("avatar_thumb") != null ? 
                    user.getJSONObject("avatar_thumb").getString("url_list") != null ?
                    JSONArray.parseArray(user.getJSONObject("avatar_thumb").getString("url_list")).getString(0) : null : null);
            
            JSONObject levelInfo = user.getJSONObject("level_info");
            if (levelInfo != null) {
                comment.setUserVerified(levelInfo.getIntValue("current_level") >= 5);
            }
            
            comment.setUserFollowerCount(user.getIntValue("follower_count"));
            comment.setUserFollowingCount(user.getIntValue("following_count"));
        }

        JSONObject replyToObj = commentObj.getJSONObject("reply_to_comment");
        if (replyToObj != null) {
            comment.setIsReply(true);
            comment.setParentCommentId(replyToObj.getString("cid"));
            comment.setReplyToUserId(replyToObj.getJSONObject("user") != null ? 
                    replyToObj.getJSONObject("user").getString("uid") : null);
            comment.setReplyToUserName(replyToObj.getJSONObject("user") != null ? 
                    replyToObj.getJSONObject("user").getString("nickname") : null);
        }

        extractContactInfo(comment);

        comment.setRawData(commentObj.toJSONString());

        return comment;
    }

    @Override
    protected void updatePaginationContext(CrawlTaskContext context, JSONObject response) {
        context.setCursor(response.getIntValue("cursor"));
        context.setHasMore(response.getBooleanValue("has_more"));
        context.setLastResponse(response);
    }

    @Override
    public void handleRateLimit(CrawlTaskContext context) {
        log.warn("抖音触发频率限制: taskId={}, platform={}", 
                context.getTask().getId(), getPlatformCode());
        long delay = getDefaultRateLimitDelayMs() * 2;
        safeSleep(delay + ThreadLocalRandom.current().nextLong(2000, 8000));
    }

    @Override
    public boolean validateResponse(CrawlTaskContext context, Object response) {
        if (response == null || !(response instanceof JSONObject)) {
            return false;
        }
        JSONObject json = (JSONObject) response;
        Integer code = json.getInteger("error_code");
        return code != null && code == 0;
    }

    private void extractContactInfo(SocialComment comment) {
        if (comment == null || comment.getRawData() == null) {
            return;
        }
        
        try {
            JSONObject rawDataObj = JSON.parseObject(comment.getRawData());
            if (rawDataObj != null) {
                String phone = rawDataObj.containsKey("phone") ? rawDataObj.getString("phone") : null;
                String wechat = rawDataObj.containsKey("wechat") ? rawDataObj.getString("wechat") : null;
                
                comment.setExtractedPhone(phone);
                comment.setExtractedWechat(wechat);
                comment.setHasPhoneContact(phone != null && !phone.isEmpty());
                comment.setHasWechatContact(wechat != null && !wechat.isEmpty());
            }
        } catch (Exception e) {
            log.warn("提取联系信息失败: {}", e.getMessage());
        }
    }
}
