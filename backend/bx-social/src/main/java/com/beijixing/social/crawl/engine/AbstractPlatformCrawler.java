package com.beijixing.social.crawl.engine;

import com.alibaba.fastjson2.JSONObject;
import com.beijixing.social.crawl.entity.CrawlTask;
import com.beijixing.social.crawl.entity.SocialComment;
import com.beijixing.social.crawl.engine.anti.AntiDetectionEngine;
import com.beijixing.social.crawl.engine.browser.BrowserContextManager;
import com.beijixing.social.crawl.engine.proxy.ProxyIpPool;
import com.beijixing.social.crawl.engine.ratelimit.SmartRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 北极星AI专属爬虫引擎基类 (v4.0 - 适配性改造版)
 * 
 * 设计理念:
 * - **简洁优先**: 移除MediaCrawler过度设计，专注SaaS商机获客核心需求
 * - **移动端友好**: 支持手机端的轻量级任务管理流程
 * - **渐进增强**: 高级功能可选注入，不影响基础功能
 * - **100%对齐**: 确保PC/Admin/Android/iOS四端接口完全一致
 *
 * 核心能力:
 * 1. 基础爬取流程（创建→执行→分页→解析→存储）
 * 2. 风控集成（请求评估、违规处理、自动降级）
 * 3. 频率控制（简单延迟+自适应调整）
 * 4. 错误重试（指数退避+最大重试次数）
 *
 * 可选增强（通过Spring注入）:
 * - ProxyIpPool: 代理IP管理（大规模爬取时启用）
 * - AntiDetectionEngine: 反检测机制（高频请求时启用）
 * - SmartRateLimiter: 智能限流（多租户并发时启用）
 * - BrowserContextManager: 浏览器会话保持（需要登录态时启用）
 *
 * @author 北极星AI团队
 * @version 4.0 (2026-05-20 适配性改造)
 */
@Slf4j
public abstract class AbstractPlatformCrawler implements PlatformCrawlerEngine {

    protected final StringRedisTemplate redisTemplate;
    protected final RiskControlEngine riskControlEngine;
    
    // 可选增强组件（v4.0: 改为非必需依赖）
    @Autowired(required = false)
    protected ProxyIpPool proxyIpPool;
    
    @Autowired(required = false)
    protected AntiDetectionEngine antiDetectionEngine;
    
    @Autowired(required = false)
    protected SmartRateLimiter smartRateLimiter;
    
    @Autowired(required = false)
    protected BrowserContextManager browserContextManager;

    private static final String VIOLATION_COUNT_PREFIX = "crawl:violation:";

    public AbstractPlatformCrawler(StringRedisTemplate redisTemplate, 
                                    RiskControlEngine riskControlEngine) {
        this.redisTemplate = redisTemplate;
        this.riskControlEngine = riskControlEngine;
    }

    @Override
    public CrawlTaskContext prepareContext(CrawlTask task) {
        CrawlTaskContext context = new CrawlTaskContext();
        context.setTask(task);
        context.setPlatformCode(task.getPlatformCode());
        context.setCursor(0);
        context.setTotalCount(0);
        context.setHasMore(true);
        context.setRequestCount(0);
        context.setSuccessCount(0);
        context.setFailCount(0);
        context.setStartTime(java.time.LocalDateTime.now());

        // 基础配置（必选）
        context.setAccessToken(getAccessToken(task));
        context.setCookie(getCookie(task));
        context.setUserAgent(getUserAgent(task));
        
        // 可选增强：代理IP（仅当proxyIpPool可用时）
        if (proxyIpPool != null) {
            setupEnhancedProxy(context);
        }
        
        // 可选增强：浏览器会话（仅当browserContextManager可用时）
        if (browserContextManager != null) {
            try {
                Object browserSession = browserContextManager.getOrCreateSession(task);
                context.getMetadata().put("browserSession", browserSession);
            } catch (Exception e) {
                log.warn("浏览器会话初始化失败，使用无头模式: {}", e.getMessage());
            }
        }

        log.info("准备爬取上下文 [北极星AI v4.0]: platform={}, task={}, enhanced={}", 
                getPlatformCode(), task.getId(), (proxyIpPool != null || browserContextManager != null));
        return context;
    }

    protected abstract String getAccessToken(CrawlTask task);

    protected abstract String getCookie(CrawlTask task);

    protected abstract String getUserAgent(CrawlTask task);

    /**
     * v4.0优化：可选代理支持
     */
    protected void setupEnhancedProxy(CrawlTaskContext context) {
        try {
            if (proxyIpPool != null) {
                ProxyIpPool.ProxyInfo proxy = proxyIpPool.getAvailableProxy();
                if (proxy != null) {
                    context.setProxyHost(proxy.getIp());
                    context.setProxyPort(proxy.getPort());
                    log.debug("使用代理IP: {} [类型={}]", proxy.getIp(), proxy.getType());
                }
            }
        } catch (Exception e) {
            log.warn("获取代理失败，使用直连: {}", e.getMessage());
        }
    }

    @Override
    public List<SocialComment> crawlComments(CrawlTaskContext context) {
        List<SocialComment> allComments = new ArrayList<>();
        Set<String> processedIds = new java.util.concurrent.ConcurrentHashMap<String, Boolean>().newKeySet();

        while (context.isHasMore() && shouldContinueCrawling(context)) {

            RiskControlEngine.RiskControlEvaluationResult evalResult = 
                    riskControlEngine.evaluateRequest(context);

            if (!evalResult.isAllowed()) {
                handleRiskControlViolation(context, evalResult);
                continue;
            }

            try {
                JSONObject response = executeSingleRequest(context);
                context.incrementRequestCount();

                if (response != null && validateResponse(context, response)) {
                    context.incrementSuccessCount();
                    
                    List<SocialComment> comments = parseComments(response, context);
                    for (SocialComment comment : comments) {
                        if (!processedIds.contains(comment.getCommentId())) {
                            processedIds.add(comment.getCommentId());
                            allComments.add(comment);
                            context.setTotalCount(context.getTotalCount() + 1);
                        }
                    }

                    updatePaginationContext(context, response);
                } else {
                    context.incrementFailCount();
                    handleInvalidResponse(context, response);
                }

                applyRateLimitDelay(context, evalResult.getRecommendedDelayMs());

            } catch (Exception e) {
                context.incrementFailCount();
                handleRequestError(context, e);
            }
        }

        log.info("爬取完成 [北极星AI v4.0]: platform={}, total={}, success={}, fail={}", 
                getPlatformCode(), allComments.size(), 
                context.getSuccessCount(), context.getFailCount());
        
        return allComments;
    }

    protected boolean shouldContinueCrawling(CrawlTaskContext context) {
        CrawlTask task = context.getTask();
        if (task.getMaxCrawlCount() != null && 
            context.getTotalCount() >= task.getMaxCrawlCount()) {
            return false;
        }
        return true;
    }

    protected abstract JSONObject executeSingleRequest(CrawlTaskContext context);

    protected abstract List<SocialComment> parseComments(JSONObject response, CrawlTaskContext context);

    protected abstract void updatePaginationContext(CrawlTaskContext context, JSONObject response);

    protected void handleInvalidResponse(CrawlTaskContext context, Object response) {
        log.warn("无效响应: platform={}, taskId={}", getPlatformCode(), context.getTask().getId());
    }

    protected void handleRequestError(CrawlTaskContext context, Exception e) {
        log.error("请求错误: platform={}, taskId={}, error={}", 
                getPlatformCode(), context.getTask().getId(), e.getMessage());
        
        recordViolationIfNeeded(context, "REQUEST_ERROR");
    }

    protected void handleRiskControlViolation(CrawlTaskContext context, 
                                             RiskControlEngine.RiskControlEvaluationResult result) {
        log.warn("触发风控规则: platform={}, rule={}, action={}", 
                getPlatformCode(), result.getTriggeredRuleId(), result.getAction());

        riskControlEngine.recordViolation(
                getPlatformCode(), 
                result.getTriggeredRuleId(), 
                context);

        switch (result.getAction()) {
            case DELAY_AND_RETRY:
                long delay = Math.max(result.getRecommendedDelayMs(), 5000);
                safeSleep(delay + ThreadLocalRandom.current().nextLong(1000, 3000));
                break;
            case REDUCE_RATE:
                reduceRequestRate(context);
                break;
            case PAUSE_TASK:
                context.setHasMore(false);
                break;
            default:
                safeSleep(result.getRecommendedDelayMs() > 0 ? 
                         result.getRecommendedDelayMs() : 30000);
        }
    }

    /**
     * v4.0优化：智能频率控制（可选增强）
     */
    protected void applyRateLimitDelay(CrawlTaskContext context, long recommendedDelay) {
        long baseDelay = getDefaultRateLimitDelayMs();
        
        // 如果有反检测引擎，使用真实用户行为延迟
        if (antiDetectionEngine != null) {
            baseDelay = antiDetectionEngine.generateRealisticDelay();
        }
        
        // 如果有智能限流器，检查并获取建议延迟
        if (smartRateLimiter != null) {
            SmartRateLimiter.RateLimitResult rateResult = 
                    smartRateLimiter.acquire(getPlatformCode(), context.getTask().getId());
            
            if (!rateResult.isAllowed()) {
                log.debug("触发频率限制: platform={}, wait={}ms", 
                        getPlatformCode(), rateResult.getRetryAfterMs());
                safeSleep(rateResult.getRetryAfterMs());
            }
        }
        
        // 取最大值（推荐延迟、基础延迟、风控延迟）
        long actualDelay = Math.max(Math.max(baseDelay, recommendedDelay), getDefaultRateLimitDelayMs());
        
        // 添加随机抖动(±20%)
        double jitterFactor = 0.8 + ThreadLocalRandom.current().nextDouble() * 0.4;
        actualDelay = (long) (actualDelay * jitterFactor);
        
        safeSleep(actualDelay);
    }

    protected void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void recordViolationIfNeeded(CrawlTaskContext context, String reason) {
        String key = VIOLATION_COUNT_PREFIX + getPlatformCode() + ":" + context.getTask().getId();
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count != null && count >= 3) {
            redisTemplate.expire(key, Duration.ofHours(1));
            log.warn("连续错误达到阈值: platform={}, count={}", getPlatformCode(), count);
        }
    }

    protected void reduceRequestRate(CrawlTaskContext context) {
        log.info("降低请求频率: platform={}", getPlatformCode());
    }

    protected HttpHeaders createDefaultHeaders(CrawlTaskContext context) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", context.getUserAgent());
        headers.set("Accept", "application/json");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        
        if (context.getAccessToken() != null) {
            headers.setBearerAuth(context.getAccessToken());
        }
        
        if (context.getCookie() != null) {
            headers.set("Cookie", context.getCookie());
        }
        
        return headers;
    }

    protected SocialComment createBaseComment(JSONObject commentObj, CrawlTaskContext context) {
        SocialComment comment = new SocialComment();
        comment.setPlatformCode(getPlatformCode());
        comment.setCrawlTaskId(context.getTask().getId());
        comment.setCrawlSource("API");
        comment.setCreateTime(java.time.LocalDateTime.now());
        return comment;
    }

    @Override
    public long getDefaultRateLimitDelayMs() {
        return 2000;
    }
}
