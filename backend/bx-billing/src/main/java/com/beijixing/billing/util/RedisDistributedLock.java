package com.beijixing.billing.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("nullness")
public class RedisDistributedLock {
    
    private final StringRedisTemplate redisTemplate;
    private final String lockKey;
    private final String lockValue;
    private final long expireTime;
    
    public RedisDistributedLock(StringRedisTemplate redisTemplate, String lockKey, long expireTimeSeconds) {
        this.redisTemplate = redisTemplate;
        this.lockKey = "lock:" + lockKey;
        this.lockValue = UUID.randomUUID().toString();
        this.expireTime = expireTimeSeconds;
    }
    
    public boolean tryLock() {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireTime, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            return false;
        }
    }
    
    public void unlock() {
        try {
            String script = 
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);
        } catch (Exception e) {
            // ignore
        }
    }
    
    public String getLockValue() {
        return lockValue;
    }
}