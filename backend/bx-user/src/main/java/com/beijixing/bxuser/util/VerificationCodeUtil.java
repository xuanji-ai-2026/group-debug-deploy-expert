package com.beijixing.bxuser.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@SuppressWarnings("nullness")
public class VerificationCodeUtil {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;
    
    private static final String CODE_PREFIX = "verify:code:";
    private static final String SMS_COOLDOWN_PREFIX = "verify:cooldown:";
    
    @SuppressWarnings("nullness")
    public String generateCode(String phone, String purpose, int expireSeconds) {
        if (redisTemplate == null) {
            log.warn("Redis未配置，使用内存验证码模式");
            return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        }
        
        String cooldownKey = SMS_COOLDOWN_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new RuntimeException("请稍后再试，短信发送过于频繁");
        }
        
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        
        String codeKey = CODE_PREFIX + phone + ":" + purpose;
        redisTemplate.opsForValue().set(codeKey, code, expireSeconds, TimeUnit.SECONDS);
        
        redisTemplate.opsForValue().set(cooldownKey, "1", expireSeconds, TimeUnit.SECONDS);
        
        log.info("生成验证码 - 手机号: {}, 用途: {}, 验证码: {} (开发环境显示)", phone, purpose, code);
        
        return code;
    }
    
    public boolean verifyCode(String phone, String purpose, String code) {
        if (redisTemplate == null) {
            log.warn("Redis未配置，跳过验证码校验");
            return true;
        }
        
        String codeKey = CODE_PREFIX + phone + ":" + purpose;
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        
        if (storedCode == null) {
            return false;
        }
        
        boolean valid = storedCode.equals(code);
        if (valid) {
            redisTemplate.delete(codeKey);
        }
        
        return valid;
    }
    
    public void deleteCode(String phone, String purpose) {
        if (redisTemplate == null) {
            return;
        }
        
        String codeKey = CODE_PREFIX + phone + ":" + purpose;
        redisTemplate.delete(codeKey);
    }
}
