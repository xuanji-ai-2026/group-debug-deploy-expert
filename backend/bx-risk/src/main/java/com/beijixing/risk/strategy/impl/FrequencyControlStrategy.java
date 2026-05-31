package com.beijixing.risk.strategy.impl;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.strategy.RiskStrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 频率控制策略 - 操作频率限制，防止过度操作导致封号
 *
 * @author 林超 (EMP-SEC-001)
 * 控制维度：
 * 1. 日操作次数限制
 * 2. 小时操作次数限制
 * 3. 操作间隔控制
 * 4. 时间窗口滑动限流
 */
@Slf4j
@Component
@SuppressWarnings("nullness")
public class FrequencyControlStrategy implements RiskStrategyHandler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 各操作类型的日限制
     */
    private static final Map<String, Integer> DAILY_LIMITS = Map.of(
        "publish", 10,
        "message", 50,
        "follow", 100,
        "comment", 200,
        "like", 500
    );

    /**
     * 各操作类型的最小间隔（秒）
     */
    private static final Map<String, Integer> MIN_INTERVALS = Map.of(
        "publish", 300,     // 5分钟
        "message", 60,      // 1分钟
        "follow", 30,       // 30秒
        "comment", 120,     // 2分钟
        "like", 5          // 5秒
    );

    /**
     * 小时限制（日限制的20%）
     */
    private static final double HOURLY_RATIO = 0.2;

    @Override
    public String getStrategyType() {
        return "frequency_control";
    }

    @Override
    public boolean supports(RiskCheckRequest request) {
        return request.getAccountId() != null && DAILY_LIMITS.containsKey(request.getOperationType());
    }

    @Override
    public StrategyResult execute(RiskCheckRequest request) {
        String operationType = request.getOperationType();
        Long accountId = request.getAccountId();

        // 1. 检查日操作次数
        StrategyResult dailyResult = checkDailyLimit(accountId, operationType);
        if (!dailyResult.isPassed()) {
            return dailyResult;
        }

        // 2. 检查小时操作次数
        StrategyResult hourlyResult = checkHourlyLimit(accountId, operationType);
        if (!hourlyResult.isPassed()) {
            return hourlyResult;
        }

        // 3. 检查操作间隔
        StrategyResult intervalResult = checkMinInterval(accountId, operationType);
        if (!intervalResult.isPassed()) {
            return intervalResult;
        }

        // 4. 滑动窗口限流检查
        StrategyResult slidingResult = checkSlidingWindow(accountId, operationType);
        if (!slidingResult.isPassed()) {
            return slidingResult;
        }

        // 通过所有检查，记录本次操作
        recordOperation(accountId, operationType);

        return StrategyResult.pass();
    }

    /**
     * 检查日操作次数限制
     */
    private StrategyResult checkDailyLimit(Long accountId, String operationType) {
        Integer dailyLimit = DAILY_LIMITS.get(operationType);
        if (dailyLimit == null) {
            return StrategyResult.pass();
        }

        String key = buildKey(accountId, operationType, "daily");
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 设置当天过期
            long secondsUntilMidnight = getSecondsUntilMidnight();
            redisTemplate.expire(key, Duration.ofSeconds(secondsUntilMidnight));
        }

        if (count != null && count > dailyLimit) {
            log.warn("日操作次数超限: accountId={}, operation={}, count={}, limit={}",
                accountId, operationType, count, dailyLimit);
            return StrategyResult.rateLimit(30,
                String.format("今日%s次数已达上限（%d次）", getOperationName(operationType), dailyLimit));
        }

        // 接近上限时发出警告
        if (count != null && count > dailyLimit * 0.8) {
            int remaining = dailyLimit - count.intValue();
            return StrategyResult.warn(60,
                String.format("%s剩余次数: %d", getOperationName(operationType), remaining),
                "操作频率建议适当降低");
        }

        return StrategyResult.pass();
    }

    /**
     * 检查小时操作次数限制
     */
    private StrategyResult checkHourlyLimit(Long accountId, String operationType) {
        Integer dailyLimit = DAILY_LIMITS.get(operationType);
        if (dailyLimit == null) {
            return StrategyResult.pass();
        }

        int hourlyLimit = (int) (dailyLimit * HOURLY_RATIO);
        String key = buildKey(accountId, operationType, "hourly");
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofHours(1));
        }

        if (count != null && count > hourlyLimit) {
            log.warn("小时操作次数超限: accountId={}, operation={}, count={}, limit={}",
                accountId, operationType, count, hourlyLimit);
            return StrategyResult.rateLimit(40, "操作过于频繁，请稍后再试");
        }

        return StrategyResult.pass();
    }

    /**
     * 检查最小操作间隔
     */
    private StrategyResult checkMinInterval(Long accountId, String operationType) {
        Integer minInterval = MIN_INTERVALS.get(operationType);
        if (minInterval == null) {
            return StrategyResult.pass();
        }

        String lastOpKey = buildKey(accountId, operationType, "lastop");
        Object lastOpTime = redisTemplate.opsForValue().get(lastOpKey);
        if (lastOpTime != null) {
            long lastTime = Long.parseLong(lastOpTime.toString());
            long now = System.currentTimeMillis();
            long elapsed = (now - lastTime) / 1000;

            if (elapsed < minInterval) {
                long waitTime = minInterval - elapsed;
                log.warn("操作间隔不足: accountId={}, operation={}, elapsed={}s, required={}s",
                    accountId, operationType, elapsed, minInterval);
                return StrategyResult.rateLimit(50,
                    String.format("操作间隔需≥%d秒，请等待%d秒后重试", minInterval, waitTime));
            }
        }

        return StrategyResult.pass();
    }

    /**
     * 滑动窗口限流检查
     */
    private StrategyResult checkSlidingWindow(Long accountId, String operationType) {
        String key = buildKey(accountId, operationType, "window");
        long now = System.currentTimeMillis();
        int windowSeconds = 60;  // 1分钟窗口

        // 移除超出窗口的记录
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, now - windowSeconds * 1000L);

        // 获取窗口内请求数
        Long windowCount = redisTemplate.opsForZSet().zCard(key);
        int maxInWindow = 10;  // 1分钟内最多10次

        if (windowCount != null && windowCount >= maxInWindow) {
            return StrategyResult.rateLimit(45, "请求过于频繁，请稍后重试");
        }

        // 添加当前请求到窗口
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        redisTemplate.expire(key, Duration.ofSeconds(windowSeconds * 2));

        return StrategyResult.pass();
    }

    /**
     * 记录操作（增加计数）
     */
    private void recordOperation(Long accountId, String operationType) {
        // 更新日计数
        String dailyKey = buildKey(accountId, operationType, "daily");
        redisTemplate.opsForValue().increment(dailyKey);

        // 更新小时计数
        String hourlyKey = buildKey(accountId, operationType, "hourly");
        redisTemplate.opsForValue().increment(hourlyKey);

        // 更新最后操作时间
        String lastOpKey = buildKey(accountId, operationType, "lastop");
        redisTemplate.opsForValue().set(lastOpKey, System.currentTimeMillis());

        // 更新最后操作类型（用于日志追踪）
        String lastTypeKey = "risk:lastop:type:" + accountId;
        redisTemplate.opsForValue().set(lastTypeKey, operationType);
    }

    /**
     * 构建Redis Key
     */
    private String buildKey(Long accountId, String operationType, String suffix) {
        return String.format("risk:freq:%s:%s:%s", accountId, operationType, suffix);
    }

    /**
     * 获取距当天午夜剩余秒数
     */
    private long getSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return Duration.between(now, midnight).getSeconds();
    }

    /**
     * 获取操作类型名称
     */
    private String getOperationName(String operationType) {
        return switch (operationType) {
            case "publish" -> "发布";
            case "message" -> "私信";
            case "follow" -> "关注";
            case "comment" -> "评论";
            case "like" -> "点赞";
            default -> operationType;
        };
    }
}
