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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class BilibiliCrawler extends AbstractPlatformCrawler {

    private static final String BASE_URL = "https://api.bilibili.com";
    private static final String WEB_BASE = "https://www.bilibili.com";

    private final RestTemplate restTemplate;

    public BilibiliCrawler(org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                           RiskControlEngine riskControlEngine,
                           RestTemplate restTemplate) {
        super(redisTemplate, riskControlEngine);
        this.restTemplate = restTemplate;
    }

    @Override
    public String getPlatformCode() {
        return "BILIBILI";
    }

    @Override
    public String getPlatformName() {
        return "哔哩哔哩";
    }

    @Override
    public long getDefaultRateLimitDelayMs() {
        return 1500;
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
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1.1 Safari/605.1.15"
        };
        return userAgents[ThreadLocalRandom.current().nextInt(userAgents.length)];
    }

    @Override
    protected JSONObject executeSingleRequest(CrawlTaskContext context) {
        try {
            String oid = extractOid(context);
            String bvid = extractBvid(context);
            if (oid == null && bvid == null) {
                log.warn("无法提取B站视频ID");
                return null;
            }

            if (bvid != null && oid == null) {
                oid = resolveOidFromBvid(bvid, context);
            }

            if (oid == null) {
                return null;
            }

            int page = context.getRequestCount() / 20 + 1;
            String sortType = determineSortType(context);
            String apiUrl = String.format("%s/x/v2/reply?type=1&oid=%s&ps=20&pn=%d&sort=%s&nohot=1",
                    BASE_URL, oid, page, sortType);

            org.springframework.http.HttpHeaders headers = createBilibiliHeaders(context);

            log.debug("执行B站评论请求: oid={}, page={}", oid, page);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    apiUrl, org.springframework.http.HttpMethod.GET,
                    new org.springframework.http.HttpEntity<>(headers), String.class);

            if (responseEntity.getBody() != null) {
                return JSON.parseObject(responseEntity.getBody());
            }
            return null;
        } catch (Exception e) {
            log.error("B站请求执行失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    protected List<SocialComment> parseComments(JSONObject response, CrawlTaskContext context) {
        List<SocialComment> comments = new ArrayList<>();

        try {
            if (response == null || !response.containsKey("data")) {
                log.warn("B站响应为空或缺少data字段");
                return comments;
            }
            JSONObject data = response.getJSONObject("data");
            if (data == null) return comments;

            JSONArray replies = data.getJSONArray("replies");
            if (replies == null || replies.isEmpty()) return comments;

            for (int i = 0; i < replies.size(); i++) {
                JSONObject replyObj = replies.getJSONObject(i);

                SocialComment comment = parseSingleComment(replyObj);
                if (comment != null) {
                    comments.add(comment);
                    
                    JSONArray subReplies = replyObj.getJSONArray("replies");
                    if (subReplies != null && !subReplies.isEmpty()) {
                        for (int j = 0; j < subReplies.size(); j++) {
                            SocialComment subComment = parseSingleComment(subReplies.getJSONObject(j));
                            if (subComment != null) {
                                subComment.setParentCommentId(comment.getCommentId());
                                comments.add(subComment);
                            }
                        }
                    }
                }
            }

            log.info("解析B站评论数: {} (含子评论)", comments.size());
        } catch (Exception e) {
            log.error("解析B站评论失败: {}", e.getMessage());
        }

        return comments;
    }

    @Override
    protected void updatePaginationContext(CrawlTaskContext context, JSONObject response) {
        try {
            JSONObject data = response.getJSONObject("data");
            if (data != null) {
                JSONObject page = data.getJSONObject("page");
                if (page != null) {
                    int currentPn = page.getIntValue("num");
                    int totalPage = page.getIntValue("count") / 20 + 1;
                    context.setCursor(currentPn + 1);
                    context.setHasMore(currentPn < totalPage);
                }
            }
        } catch (Exception e) {
            log.warn("更新分页上下文失败: {}", e.getMessage());
        }
    }

    @Override
    public void handleRateLimit(CrawlTaskContext context) {
        log.warn("B站触发频率限制: taskId={}, platform={}", 
                context.getTask().getId(), getPlatformCode());
        long delay = getDefaultRateLimitDelayMs() * 3;
        safeSleep(delay + ThreadLocalRandom.current().nextLong(2000, 8000));
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

    private SocialComment parseSingleComment(JSONObject replyObj) {
        try {
            SocialComment comment = new SocialComment();
            comment.setCommentId(String.valueOf(replyObj.getIntValue("rpid")));
            
            JSONObject content = replyObj.getJSONObject("content");
            if (content != null) {
                comment.setCommentText(content.getString("message"));
            }

            JSONObject member = replyObj.getJSONObject("member");
            if (member != null) {
                comment.setAuthorId(String.valueOf(member.getIntValue("mid")));
                comment.setAuthorName(member.getString("uname"));
            }

            comment.setLikeCount(replyObj.getIntValue("like"));
            comment.setPublishTime(LocalDateTime.now());
            comment.setCrawlSource(getPlatformCode());
            comment.setPlatformCode(getPlatformCode());
            comment.setCreateTime(LocalDateTime.now());

            return comment;
        } catch (Exception e) {
            log.error("解析单条B站评论失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractOid(CrawlTaskContext context) {
        return context.getTask().getTargetId();
    }

    private String extractBvid(CrawlTaskContext context) {
        String targetUrl = context.getTask().getTargetUrl();
        if (targetUrl != null && targetUrl.contains("/video/BV")) {
            int start = targetUrl.indexOf("/video/BV") + "/video/BV".length();
            int end = targetUrl.indexOf("?", start);
            return end > 0 ? targetUrl.substring(start, end) : targetUrl.substring(start);
        }
        return null;
    }

    private String resolveOidFromBvid(String bvid, CrawlTaskContext context) {
        try {
            String apiUrl = String.format("%s/x/web-interface/view?bvid=%s", BASE_URL, bvid);
            org.springframework.http.HttpHeaders headers = createBilibiliHeaders(context);
            
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    apiUrl, org.springframework.http.HttpMethod.GET,
                    new org.springframework.http.HttpEntity<>(headers), String.class);
            
            if (responseEntity.getBody() != null) {
                JSONObject response = JSON.parseObject(responseEntity.getBody());
                if (response.getJSONObject("data") != null) {
                    return String.valueOf(response.getJSONObject("data").getIntValue("aid"));
                }
            }
        } catch (Exception e) {
            log.error("转换BV号到AID失败: {}", e.getMessage());
        }
        return null;
    }

    private String determineSortType(CrawlTaskContext context) {
        return "2"; // 默认按最新排序
    }

    private org.springframework.http.HttpHeaders createBilibiliHeaders(CrawlTaskContext context) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("User-Agent", context.getUserAgent());
        headers.set("Cookie", context.getCookie());
        headers.set("Referer", WEB_BASE + "/");
        headers.set("Origin", WEB_BASE);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9");
        return headers;
    }
}
