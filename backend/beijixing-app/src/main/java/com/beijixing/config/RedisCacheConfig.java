package com.beijixing.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RedisCacheConfig implements CachingConfigurer {

    public static final String CACHE_PREFIX = "bx:";
    public static final String CACHE_VERSION = "v1:";

    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder(CACHE_PREFIX);
            sb.append(CACHE_VERSION);
            sb.append(target.getClass().getSimpleName()).append(":");
            sb.append(method.getName()).append(":");
            if (params.length > 0) {
                for (Object param : params) {
                    if (param != null) {
                        String paramStr = param.toString();
                        String md5Key = DigestUtils.md5DigestAsHex(
                                paramStr.getBytes(StandardCharsets.UTF_8));
                        sb.append(md5Key).append(",");
                    } else {
                        sb.append("null,");
                    }
                }
                sb.deleteCharAt(sb.length() - 1);
            }
            String generatedKey = sb.toString();
            log.debug("[RedisCache] 生成缓存Key: {}", generatedKey);
            return generatedKey;
        };
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        log.info("[RedisCache] RedisTemplate初始化完成 - Key:String / Value:JSON");
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith(CACHE_PREFIX + CACHE_VERSION);

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("userCache", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("dictCache", defaultConfig.entryTtl(Duration.ofHours(12)));
        cacheConfigurations.put("configCache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("leadCache", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("contentCache", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("aiModelCache", defaultConfig.entryTtl(Duration.ofHours(24)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("[RedisCache] CacheManager初始化完成 - 默认TTL:2h | 自定义缓存: {}个",
                cacheConfigurations.size());
        return cacheManager;
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("[RedisCache] 缓存读取异常 - key: {} | error: {}", key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache,
                                            Object key, Object value) {
                log.warn("[RedisCache] 缓存写入异常 - key: {} | error: {}", key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("[RedisCache] 缓存删除异常 - key: {} | error: {}", key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("[RedisCache] 缓存清空异常 - error: {}", e.getMessage());
            }
        };
    }

    public static String buildCacheKey(String cacheName, Object... params) {
        StringBuilder sb = new StringBuilder(CACHE_PREFIX);
        sb.append(CACHE_VERSION).append(cacheName).append(":");
        for (Object param : params) {
            if (param != null) {
                sb.append(DigestUtils.md5DigestAsHex(
                        param.toString().getBytes(StandardCharsets.UTF_8)));
            }
            sb.append(":");
        }
        return sb.toString();
    }
}
