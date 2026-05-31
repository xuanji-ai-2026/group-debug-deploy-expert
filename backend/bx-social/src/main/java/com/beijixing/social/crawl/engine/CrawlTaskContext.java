package com.beijixing.social.crawl.engine;

import com.alibaba.fastjson2.JSONObject;
import com.beijixing.social.crawl.entity.CrawlTask;

import java.time.LocalDateTime;
import java.util.Map;

public class CrawlTaskContext {

    private CrawlTask task;
    private String platformCode;
    private String accessToken;
    private String cookie;
    private String userAgent;
    private String proxyHost;
    private Integer proxyPort;
    private int cursor;
    private int totalCount;
    private boolean hasMore;
    private JSONObject lastResponse;
    private LocalDateTime startTime;
    private LocalDateTime lastRequestTime;
    private int requestCount;
    private int successCount;
    private int failCount;
    private Map<String, Object> metadata;

    public CrawlTask getTask() { return task; }
    public void setTask(CrawlTask task) { this.task = task; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getCookie() { return cookie; }
    public void setCookie(String cookie) { this.cookie = cookie; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getProxyHost() { return proxyHost; }
    public void setProxyHost(String proxyHost) { this.proxyHost = proxyHost; }
    public Integer getProxyPort() { return proxyPort; }
    public void setProxyPort(Integer proxyPort) { this.proxyPort = proxyPort; }
    public int getCursor() { return cursor; }
    public void setCursor(int cursor) { this.cursor = cursor; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    public JSONObject getLastResponse() { return lastResponse; }
    public void setLastResponse(JSONObject lastResponse) { this.lastResponse = lastResponse; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getLastRequestTime() { return lastRequestTime; }
    public void setLastRequestTime(LocalDateTime lastRequestTime) { this.lastRequestTime = lastRequestTime; }
    public int getRequestCount() { return requestCount; }
    public void setRequestCount(int requestCount) { this.requestCount = requestCount; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public void incrementRequestCount() {
        this.requestCount++;
        this.lastRequestTime = LocalDateTime.now();
    }

    public void incrementSuccessCount() {
        this.successCount++;
    }

    public void incrementFailCount() {
        this.failCount++;
    }

    public long getTimeSinceLastRequestMs() {
        if (lastRequestTime == null) return Long.MAX_VALUE;
        return java.time.Duration.between(lastRequestTime, LocalDateTime.now()).toMillis();
    }
}
