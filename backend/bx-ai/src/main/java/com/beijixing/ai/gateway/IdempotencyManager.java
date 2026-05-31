package com.beijixing.ai.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等性管理器
 * 防止重复请求
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@Component
public class IdempotencyManager {
    
    private final StringRedisTemplate redisTemplate;
    
    // 本地缓存(作为Redis的备份)
    private final ConcurrentHashMap<String, IdempotencyRecord> localCache = new ConcurrentHashMap<>();
    
    // 默认幂等key过期时间
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    
    public IdempotencyManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 检查请求是否已处理(Redis)
     */
    public boolean isProcessed(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return false;
        }
        
        try {
            String redisKey = "idempotency:" + idempotencyKey;
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            log.warn("Redis幂等检查失败,降级到本地缓存", e);
            return isProcessedLocal(idempotencyKey);
        }
    }
    
    /**
     * 检查请求是否已处理(本地)
     */
    public boolean isProcessedLocal(String idempotencyKey) {
        IdempotencyRecord record = localCache.get(idempotencyKey);
        if (record == null) {
            return false;
        }
        
        // 检查是否过期
        if (record.isExpired()) {
            localCache.remove(idempotencyKey);
            return false;
        }
        
        return true;
    }
    
    /**
     * 标记请求已处理
     */
    public void markProcessed(String idempotencyKey, String response) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return;
        }
        
        try {
            String redisKey = "idempotency:" + idempotencyKey;
            redisTemplate.opsForValue().set(redisKey, response != null ? response : "", DEFAULT_TTL);
        } catch (Exception e) {
            log.warn("Redis幂等标记失败,使用本地缓存", e);
            markProcessedLocal(idempotencyKey, response);
        }
    }
    
    /**
     * 本地标记请求已处理
     */
    public void markProcessedLocal(String idempotencyKey, String response) {
        localCache.put(idempotencyKey, new IdempotencyRecord(response, System.currentTimeMillis()));
    }
    
    /**
     * 获取已处理请求的响应
     */
    public String getProcessedResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return null;
        }
        
        try {
            String redisKey = "idempotency:" + idempotencyKey;
            return redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.warn("Redis获取幂等响应失败", e);
            IdempotencyRecord record = localCache.get(idempotencyKey);
            return record != null ? record.getResponse() : null;
        }
    }
    
    /**
     * 清理过期记录
     */
    public void cleanup() {
        localCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * 生成幂等key
     */
    public static String generateKey(String userId, String requestType, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append(userId).append(":").append(requestType);
        for (Object param : params) {
            sb.append(":").append(param);
        }
        return sb.toString();
    }
    
    /**
     * 幂等记录
     */
    private static class IdempotencyRecord {
        private final String response;
        private final long timestamp;
        
        IdempotencyRecord(String response, long timestamp) {
            this.response = response;
            this.timestamp = timestamp;
        }
        
        String getResponse() {
            return response;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > DEFAULT_TTL.toMillis();
        }
    }
}
