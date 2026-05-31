package com.beijixing.social.crawl.engine.risk;

import com.beijixing.social.crawl.engine.CrawlTaskContext;
import com.beijixing.social.crawl.engine.RiskControlAction;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RateLimitRule extends AbstractRiskControlRule {

    private final java.util.function.Supplier<Integer> maxRequestsPerMinuteSupplier;

    public RateLimitRule(String ruleId, String platformCode, String ruleName,
                         String ruleType, int priority,
                         java.util.function.Supplier<Integer> maxRequestsPerMinuteSupplier) {
        super(ruleId, platformCode, ruleName, ruleType, priority);
        this.maxRequestsPerMinuteSupplier = maxRequestsPerMinuteSupplier;
        this.setActionOnTrigger(RiskControlAction.DELAY_AND_RETRY);
    }

    @Override
    public boolean evaluate(CrawlTaskContext context) {
        String key = "rate_limit:" + getPlatformCode() + ":" + context.getTask().getId();
        
        StringRedisTemplate redisTemplate = getRedisTemplate(context);
        if (redisTemplate == null) return false;

        Long currentCount = redisTemplate.opsForValue().get(key) != null ?
                Long.parseLong(redisTemplate.opsForValue().get(key)) : 0L;

        int maxRequests = maxRequestsPerMinuteSupplier.get();

        if (currentCount >= maxRequests) {
            logWarning(context, "达到频率限制: current={}, max={}", currentCount, maxRequests);
            return true;
        }

        redisTemplate.opsForValue().increment(key);
        if (currentCount == 0) {
            redisTemplate.expire(key, java.time.Duration.ofMinutes(1));
        }

        return false;
    }

    private StringRedisTemplate getRedisTemplate(CrawlTaskContext context) {
        try {
            return (StringRedisTemplate) context.getMetadata().get("redisTemplate");
        } catch (Exception e) {
            return null;
        }
    }

    private void logWarning(CrawlTaskContext context, String format, Object... args) {
        String message = String.format(format, args);
        System.out.println("[RateLimitRule] " + message);
    }
}
