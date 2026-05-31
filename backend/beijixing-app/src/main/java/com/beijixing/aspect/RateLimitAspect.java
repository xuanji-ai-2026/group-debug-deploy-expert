package com.beijixing.aspect;

import com.beijixing.annotation.RateLimit;
import com.beijixing.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_SCRIPT =
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local current = redis.call('incr', key) " +
            "if current == 1 then " +
            "    redis.call('expire', key, window) " +
            "end " +
            "return current ";

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String rateLimitKey = buildRateLimitKey(joinPoint, rateLimit, method);
        int maxCount = rateLimit.count();
        long windowSeconds = rateLimit.timeUnit().toSeconds(rateLimit.window());

        Long currentCount = executeRateLimitScript(rateLimitKey, maxCount, windowSeconds);

        if (currentCount != null && currentCount > maxCount) {
            log.warn("[RateLimit] 触发限流 - Key: {} | 当前次数: {}/{} | 窗口: {}s | UserID: {} | IP: {} | TraceId: {}",
                    rateLimitKey, currentCount, maxCount, windowSeconds,
                    RequestContext.getUserId(),
                    RequestContext.getClientIp(),
                    RequestContext.getTraceId());
            throw new RuntimeException(rateLimit.message());
        }

        log.debug("[RateLimit] 请求通过 - Key: {} | 当前次数: {}/{} | TraceId: {}",
                rateLimitKey, currentCount, maxCount, RequestContext.getTraceId());

        return joinPoint.proceed();
    }

    private String buildRateLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit,
                                     Method method) {
        StringBuilder keyBuilder = new StringBuilder("bx:rate_limit:");

        switch (rateLimit.limitType()) {
            case IP -> {
                keyBuilder.append("ip:").append(RequestContext.getClientIp());
            }
            case USER -> {
                String userId = RequestContext.getUserId();
                keyBuilder.append("user:").append(userId != null ? userId : "anonymous");
            }
            case CUSTOM -> {
                String customKey = rateLimit.key();
                if (!customKey.isEmpty()) {
                    keyBuilder.append(customKey);
                } else {
                    keyBuilder.append("method:")
                            .append(method.getDeclaringClass().getSimpleName())
                            .append(":").append(method.getName());
                }
            }
            default -> {
                String customKey = rateLimit.key();
                if (!customKey.isEmpty()) {
                    keyBuilder.append(customKey);
                } else {
                    keyBuilder.append("method:")
                            .append(method.getDeclaringClass().getSimpleName())
                            .append(":").append(method.getName())
                            .append(":ip:").append(RequestContext.getClientIp())
                            .append(":user:").append(RequestContext.getUserId() != null ?
                                    RequestContext.getUserId() : "anonymous");
                }
            }
        }

        return keyBuilder.toString();
    }

    private Long executeRateLimitScript(String key, long limit, long windowSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(key),
                String.valueOf(limit),
                String.valueOf(windowSeconds));
        return result;
    }
}
