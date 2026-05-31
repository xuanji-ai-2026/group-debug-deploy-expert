package com.beijixing.ai.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流管理器
 * 支持本地内存限流和分布式Redis限流
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@Component
public class RateLimitManager {
    
    private final StringRedisTemplate redisTemplate;
    
    // 本地限流缓存
    private final ConcurrentHashMap<String, AtomicInteger> localCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> localResetTimes = new ConcurrentHashMap<>();
    
    // 默认限流配置
    private static final int DEFAULT_LIMIT = 100;  // 每分钟100次
    private static final long WINDOW_MILLIS = 60000; // 1分钟窗口
    
    public RateLimitManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 检查是否允许请求(本地限流)
     */
    public boolean allowRequestLocal(String key, int limit) {
        long now = System.currentTimeMillis();
        long windowStart = now - (now % WINDOW_MILLIS) + WINDOW_MILLIS;
        
        Long resetTime = localResetTimes.get(key);
        if (resetTime == null || now >= resetTime) {
            // 新窗口
            localCounters.put(key, new AtomicInteger(0));
            localResetTimes.put(key, windowStart);
            resetTime = windowStart;
        }
        
        AtomicInteger counter = localCounters.get(key);
        int current = counter.incrementAndGet();
        
        if (current > limit) {
            log.warn("本地限流触发:key={}, current={}, limit={}", key, current, limit);
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查是否允许请求(Redis分布式限流 - 滑动窗口)
     */
    public boolean allowRequestRedis(String key, int limit, Duration window) {
        try {
            String redisKey = "rate_limit:" + key;
            long now = System.currentTimeMillis();
            long windowStart = now - window.toMillis();
            
            // 使用Redis ZSet实现滑动窗口
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
            
            Long current = redisTemplate.opsForZSet().zCard(redisKey);
            if (current != null && current >= limit) {
                log.warn("Redis限流触发:key={}, current={}, limit={}", key, current, limit);
                return false;
            }
            
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
            redisTemplate.expire(redisKey, window);
            
            return true;
        } catch (Exception e) {
            log.error("Redis限流检查失败,降级到本地限流", e);
            return allowRequestLocal(key, limit);
        }
    }
    
    /**
     * 检查是否允许请求(默认配置)
     */
    public boolean allowRequest(String key) {
        return allowRequestLocal(key, DEFAULT_LIMIT);
    }
    
    /**
     * 检查是否允许请求(指定限制)
     */
    public boolean allowRequest(String key, int limit) {
        return allowRequestLocal(key, limit);
    }
    
    /**
     * 获取当前计数
     */
    public int getCurrentCount(String key) {
        AtomicInteger counter = localCounters.get(key);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * 重置计数器
     */
    public void reset(String key) {
        localCounters.remove(key);
        localResetTimes.remove(key);
        try {
            redisTemplate.delete("rate_limit:" + key);
        } catch (Exception e) {
            log.warn("重置Redis限流key失败", e);
        }
    }
}
