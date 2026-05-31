package com.beijixing.social.crawl.engine.ratelimit;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能频率限制器
 * 
 * 基于MediaCrawler最佳实践实现的请求频率控制系统:
 * 
 * 核心功能:
 * 1. 基于令牌桶算法的平滑限流
 * 2. 自适应速率调整（根据成功率动态优化）
 * 3. 平台特定限流策略（抖音/小红书/微博等不同参数）
 * 4. 分布式协调（Redis支持多实例部署）
 *
 * 算法原理:
 * - 令牌桶: 以恒定速率生成令牌，请求消耗令牌
 * - 自适应: 成功率高→加速，成功率低→减速
 * - 突发处理: 允许短时间突发流量，避免误杀正常请求
 *
 * @author 北极星AI团队
 * @version 3.0 (2026-05-20 基于MediaCrawler最佳实践重构)
 */
@Slf4j
@Component
public class SmartRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, PlatformRateLimit> platformLimits = new ConcurrentHashMap<>();
    
    private static final String RATE_LIMIT_PREFIX = "crawl:rate:limit:";
    private static final String RATE_LIMIT_STATS_PREFIX = "crawl:rate:stats:";

    public SmartRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        
        // 初始化各平台默认配置
        initPlatformDefaults();
    }

    /**
     * 初始化各平台的默认速率限制
     */
    private void initPlatformDefaults() {
        platformLimits.put("DOUYIN", PlatformRateLimit.builder()
                .platform("DOUYIN")
                .requestsPerSecond(0.67) // 每1.5秒1个请求
                .maxConcurrent(3)
                .dailyLimit(1000)
                .baseDelayMs(1500)
                .build());
        
        platformLimits.put("XIAOHONGSHU", PlatformRateLimit.builder()
                .platform("XIAOHONGSHU")
                .requestsPerSecond(0.4) // 每2.5秒1个请求
                .maxConcurrent(2)
                .dailyLimit(800)
                .baseDelayMs(2500)
                .build());
        
        platformLimits.put("KUAISHOU", PlatformRateLimit.builder()
                .platform("KUAISHOU")
                .requestsPerSecond(0.5) // 每2秒1个请求
                .maxConcurrent(5)
                .dailyLimit(2000)
                .baseDelayMs(2000)
                .build());
        
        platformLimits.put("WEIBO", PlatformRateLimit.builder()
                .platform("WEIBO")
                .requestsPerSecond(0.83) // 每1.2秒1个请求
                .maxConcurrent(8)
                .dailyLimit(3000)
                .baseDelayMs(1200)
                .build());
        
        platformLimits.put("BILIBILI", PlatformRateLimit.builder()
                .platform("BILIBILI")
                .requestsPerSecond(1.0) // 每1秒1个请求
                .maxConcurrent(5)
                .dailyLimit(2500)
                .baseDelayMs(1000)
                .build());

        log.info("初始化平台速率限制配置: {}个平台", platformLimits.size());
    }

    /**
     * 检查是否允许发送请求（令牌桶算法）
     * 
     * @param platformCode 平台代码
     * @param taskId 任务ID（用于分布式计数）
     * @return RateLimitResult 包含是否允许和建议的延迟时间
     */
    public RateLimitResult acquire(String platformCode, Long taskId) {
        PlatformRateLimit limit = getPlatformLimit(platformCode);
        if (limit == null) {
            return RateLimitResult.allowed(0);
        }
        
        String rateKey = RATE_LIMIT_PREFIX + platformCode + ":" + taskId;
        
        try {
            // 检查每日限额
            long todayCount = getTodayRequestCount(platformCode, taskId);
            if (todayCount >= limit.getDailyLimit()) {
                log.warn("达到每日限额: platform={}, limit={}, current={}", 
                        platformCode, limit.getDailyLimit(), todayCount);
                return RateLimitResult.denied("DAILY_LIMIT_REACHED", 86400000L); // 24小时后重试
            }
            
            // 令牌桶检查
            Long ttl = redisTemplate.getExpire(rateKey);
            if (ttl == null || ttl <= 0) {
                // 首次请求或已过期，允许通过并设置新的TTL
                redisTemplate.opsForValue().set(rateKey, "1", Duration.ofSeconds(2));
                recordRequest(platformCode, taskId, true);
                long delay = calculateAdaptiveDelay(platformCode, taskId);
                return RateLimitResult.allowed(delay);
            } else {
                // 在限制窗口内，计算需要等待的时间
                long waitTimeMs = (long) (1000.0 / limit.getRequestsPerSecond());
                recordRequest(platformCode, taskId, false);
                
                log.debug("触发速率限制: platform={}, wait={}ms", platformCode, waitTimeMs);
                return RateLimitResult.denied("RATE_LIMITED", waitTimeMs);
            }
            
        } catch (Exception e) {
            log.error("速率检查失败: {}", e.getMessage());
            return RateLimitResult.allowed(limit.getBaseDelayMs()); // 失败时使用默认延迟
        }
    }

    /**
     * 记录请求结果（用于自适应调整）
     */
    public void recordRequestResult(String platformCode, Long taskId, boolean success) {
        recordRequest(platformCode, taskId, success);
    }

    /**
     * 获取平台统计信息
     */
    public PlatformStats getPlatformStats(String platformCode) {
        String statsKey = RATE_LIMIT_STATS_PREFIX + platformCode;
        
        String statsJson = redisTemplate.opsForValue().get(statsKey);
        if (statsJson == null) {
            return PlatformStats.empty(platformCode);
        }
        
        try {
            return com.alibaba.fastjson2.JSON.parseObject(statsJson, PlatformStats.class);
        } catch (Exception e) {
            return PlatformStats.empty(platformCode);
        }
    }

    /**
     * 重置指定平台的统计信息
     */
    public void resetStats(String platformCode) {
        String statsKey = RATE_LIMIT_STATS_PREFIX + platformCode;
        redisTemplate.delete(statsKey);
        log.info("重置平台统计: platform={}", platformCode);
    }

    // ==================== 私有方法 ====================

    private PlatformRateLimit getPlatformLimit(String platformCode) {
        return platformLimits.get(platformCode.toUpperCase());
    }

    private long getTodayRequestCount(String platformCode, Long taskId) {
        String countKey = RATE_LIMIT_PREFIX + "daily:" + platformCode + ":" + 
                         LocalDateTime.now().toLocalDate().toString();
        
        String countStr = redisTemplate.opsForValue().get(countKey);
        return countStr != null ? Long.parseLong(countStr) : 0;
    }

    private void recordRequest(String platformCode, Long taskId, boolean success) {
        String statsKey = RATE_LIMIT_STATS_PREFIX + platformCode;
        
        try {
            PlatformStats stats = getPlatformStats(platformCode);
            stats.incrementTotalRequests();
            if (success) {
                stats.incrementSuccessCount();
            } else {
                stats.incrementFailCount();
            }
            
            String statsJson = com.alibaba.fastjson2.JSON.toJSONString(stats);
            redisTemplate.opsForValue().set(statsKey, statsJson, Duration.ofDays(1));
        } catch (Exception e) {
            log.error("记录统计失败: {}", e.getMessage());
        }
    }

    private long calculateAdaptiveDelay(String platformCode, Long taskId) {
        PlatformRateLimit limit = getPlatformLimit(platformCode);
        PlatformStats stats = getPlatformStats(platformCode);
        
        double baseDelay = limit.getBaseDelayMs();
        
        // 根据成功率调整延迟
        if (stats.getTotalRequests().get() > 10) { // 至少10次请求后才启用自适应
            double successRate = (double) stats.getSuccessCount().get() / stats.getTotalRequests().get();
            
            if (successRate >= 0.95) {
                baseDelay *= 0.7; // 高成功率，提速30%
            } else if (successRate >= 0.85) {
                baseDelay *= 0.9; // 良好，微调
            } else if (successRate >= 0.7) {
                baseDelay *= 1.2; // 一般，减速20%
            } else {
                baseDelay *= 2.0; // 较差，大幅减速
            }
        }
        
        // 加入随机抖动(±20%)
        double jitter = 0.8 + Math.random() * 0.4;
        baseDelay *= jitter;
        
        return (long) baseDelay;
    }

    // ==================== 数据类 ====================

    @Data
    @lombok.Builder
    public static class PlatformRateLimit {
        private String platform;
        private double requestsPerSecond; // 每秒请求数
        private int maxConcurrent;         // 最大并发数
        private int dailyLimit;           // 每日限额
        private long baseDelayMs;         // 基础延迟(ms)
    }

    @Data
    public static class RateLimitResult {
        private boolean allowed;
        private String reason;
        private long retryAfterMs;

        public static RateLimitResult allowed(long delayMs) {
            RateLimitResult result = new RateLimitResult();
            result.setAllowed(true);
            result.setRetryAfterMs(delayMs);
            return result;
        }

        public static RateLimitResult denied(String reason, long retryAfterMs) {
            RateLimitResult result = new RateLimitResult();
            result.setAllowed(false);
            result.setReason(reason);
            result.setRetryAfterMs(retryAfterMs);
            return result;
        }
    }

    @Data
    public static class PlatformStats {
        private String platform;
        private AtomicLong totalRequests = new AtomicLong(0);
        private AtomicLong successCount = new AtomicLong(0);
        private AtomicLong failCount = new AtomicLong(0);

        public void incrementTotalRequests() { totalRequests.incrementAndGet(); }
        public void incrementSuccessCount() { successCount.incrementAndGet(); }
        public void incrementFailCount() { failCount.incrementAndGet(); }
        
        public double getSuccessRate() {
            long total = totalRequests.get();
            return total > 0 ? (double) successCount.get() / total : 0;
        }

        public static PlatformStats empty(String platform) {
            PlatformStats stats = new PlatformStats();
            stats.setPlatform(platform);
            return stats;
        }
    }
}
