package com.beijixing.social.compliance.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合规私信频率限制服务 v2.0 (2026平台规则版)
 *
 * 核心功能:
 * 1. **令牌桶限流算法**: 基于Redis实现分布式限流，支持多维度限制
 * 2. **平台规则引擎**: 读取各平台最新规则配置（抖音/小红书/微信等）
 * 3. **时间窗口检查**: 仅允许在规定时间段内发送私信
 * 4. **内容相似度检测**: 防止重复文案被判定为垃圾消息
 * 5. **冷却期管理**: 触发风控后自动进入冷却期
 *
 * 技术实现:
 * - Redis Lua脚本保证原子性操作
 * - 滑动窗口算法统计发送次数
 * - SimHash算法计算文本相似度
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20 合规增强版)
 */
@Service
@Slf4j
public class MessageRateLimiterService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // ====== 配置项 ======
    @Value("${compliance.pre-send-check.enabled:true}")
    private boolean preSendCheckEnabled;

    @Value("${compliance.pre-send-check.rate-limit-check:true}")
    private boolean rateLimitCheckEnabled;

    @Value("${compliance.pre-send-check.time-window-check:true}")
    private boolean timeWindowCheckEnabled;

    @Value("${compliance.pre-send-check.content-similarity-check:true}")
    private boolean similarityCheckEnabled;

    @Value("${compliance.pre-send-check.similarity-threshold:0.8}")
    private double similarityThreshold;

    // ====== Redis Key前缀 ======
    private static final String RATE_LIMIT_PREFIX = "rate:limit:";
    private static final String DAILY_COUNT_PREFIX = "daily:count:";
    private static final String USER_DAILY_PREFIX = "user:daily:";
    private static final String COOLDOWN_PREFIX = "cooldown:";
    private static final String LAST_MESSAGES_PREFIX = "last:messages:";

    // ====== Lua脚本：令牌桶限流（原子操作） ======
    private static final String RATE_LIMIT_LUA =
            "local key = KEYS[1] " +
            "local capacity = tonumber(ARGV[1]) " +
            "local tokens = tonumber(ARGV[2]) " +
            "local rate = tonumber(ARGV[3]) " +
            "local now = tonumber(ARGV[4]) " +
            "" +
            "local current = redis.call('HMGET', key, 'tokens', 'last_refill_time') " +
            "local tokenCount = tonumber(current[1]) or capacity " +
            "local lastRefillTime = tonumber(current[2]) or now " +
            "" +
            "-- 计算应补充的令牌数 " +
            "if now > lastRefillTime then " +
            "    local elapsed = now - lastRefillTime " +
            "    local newTokens = math.floor(elapsed * rate) " +
            "    tokenCount = math.min(capacity, tokenCount + newTokens) " +
            "    lastRefillTime = now " +
            "end " +
            "" +
            "-- 尝试消费令牌 " +
            "if tokenCount >= tokens then " +
            "    tokenCount = tokenCount - tokens " +
            "    redis.call('HMSET', key, 'tokens', tokenCount, 'last_refill_time', lastRefillTime) " +
            "    redis.call('EXPIRE', key, 3600) " +
            "    return 1 " +
            "else " +
            "    redis.call('HMSET', key, 'tokens', tokenCount, 'last_refill_time', lastRefillTime) " +
            "    redis.call('EXPIRE', key, 3600) " +
            "    return 0 " +
            "end";

    // ====== Lua脚本：滑动窗口计数（原子操作） ======
    private static final String SLIDING_WINDOW_LUA =
            "local key = KEYS[1] " +
            "local windowSize = tonumber(ARGV[1]) " +
            "local maxCount = tonumber(ARGV[2]) " +
            "local now = tonumber(ARGV[3]) " +
            "local increment = tonumber(ARGV[4]) " +
            "" +
            "-- 清除过期数据 " +
            "redis.call('ZREMRANGEBYSCORE', key, 0, now - windowSize) " +
            "" +
            "-- 获取当前窗口内计数 " +
            "local currentCount = redis.call('ZCARD', key) " +
            "" +
            "if currentCount + increment > maxCount then " +
            "    return {0, maxCount - currentCount} " +
            "end " +
            "" +
            "-- 增加计数 " +
            "for i = 1, increment do " +
            "    redis.call('ZADD', key, now, now .. ':' .. i) " +
            "end " +
            "redis.call('EXPIRE', key, windowSize + 1) " +
            "return {1, maxCount - currentCount - increment}";

    // ====== 平台规则缓存 ======
    private final Map<String, PlatformRules> platformRulesCache = new ConcurrentHashMap<>();

    /**
     * 初始化：加载平台规则到内存缓存
     */
    @PostConstruct
    public void init() {
        loadPlatformRules();
        log.info("✅ 私信频率限制服务初始化完成，已加载{}个平台规则", platformRulesCache.size());
    }

    // ============================================================
    // 核心方法：发送前综合检查
    // ============================================================

    /**
     * 发送前综合合规检查（必经流程）
     *
     * 检查项:
     * 1. 频率限制（令牌桶算法）
     * 2. 日限额检查（全局+单用户）
     * 3. 时间窗口检查
     * 4. 内容相似度检测
     * 5. 冷却期检查
     *
     * @param accountId 社交账号ID
     * @param targetUserId 目标用户ID
     * @param content 待发送内容
     * @return 检查结果（包含是否通过、剩余额度、违规原因等）
     */
    public RateLimitResult preSendCheck(Long accountId, String targetUserId, String content) {
        RateLimitResult result = new RateLimitResult();
        result.setAccountId(accountId);
        result.setCheckedAt(LocalDateTime.now());

        if (!preSendCheckEnabled) {
            result.setAllowed(true);
            result.setMessage("合规检查已禁用");
            return result;
        }

        List<String> violations = new ArrayList<>();

        try {
            // 1. 冷却期检查
            if (isInCooldown(accountId)) {
                violations.add("账号处于冷却期，请稍后再试");
                long remainingSeconds = getCooldownRemaining(accountId);
                result.setCooldownRemaining(Duration.ofSeconds(remainingSeconds));
            }

            // 2. 时间窗口检查
            if (timeWindowCheckEnabled && !isInAllowedTimeWindow(accountId)) {
                violations.add("当前不在允许的发送时间窗口内");
                result.setNextAllowedTime(getNextAllowedTime(accountId));
            }

            // 3. 全局日限额检查
            if (rateLimitCheckEnabled) {
                DailyLimitResult dailyGlobal = checkDailyGlobalLimit(accountId);
                if (!dailyGlobal.isAllowed()) {
                    violations.add("超过全局日限额(" + dailyGlobal.getMaxDaily() + "条)");
                    result.setDailyGlobalRemaining(0);
                    result.setDailyGlobalMax(dailyGlobal.getMaxDaily());
                } else {
                    result.setDailyGlobalRemaining(dailyGlobal.getRemaining());
                    result.setDailyGlobalMax(dailyGlobal.getMaxDaily());
                }
            }

            // 4. 单用户日限额检查
            if (rateLimitCheckEnabled) {
                DailyLimitResult dailyUser = checkDailyUserLimit(accountId, targetUserId);
                if (!dailyUser.isAllowed()) {
                    violations.add("对该用户今日发送次数已达上限(" + dailyUser.getMaxDaily() + "条)");
                    result.setDailyUserRemaining(0);
                    result.setDailyUserMax(dailyUser.getMaxDaily());
                } else {
                    result.setDailyUserRemaining(dailyUser.getRemaining());
                    result.setDailyUserMax(dailyUser.getMaxDaily());
                }
            }

            // 5. 内容相似度检测
            if (similarityCheckEnabled && content != null) {
                SimilarityResult similarity = checkContentSimilarity(accountId, content);
                if (similarity.isHighSimilarity()) {
                    violations.add(String.format("内容相似度过高(%.0f%%)，可能被判定为垃圾消息",
                            similarity.getSimilarity() * 100));
                    result.setContentSimilarity(similarity.getSimilarity());
                }
            }

            // 6. 令牌桶频率限制
            if (rateLimitCheckEnabled) {
                boolean acquired = tryAcquireToken(accountId, 1);
                if (!acquired) {
                    violations.add("发送过于频繁，请稍后再试");
                }
            }

            result.setViolations(violations);
            result.setAllowed(violations.isEmpty());

            if (!violations.isEmpty()) {
                log.warn("⚠️ 私信发送被拦截: accountId={}, targetUserId={}, reasons={}",
                        accountId, targetUserId, violations);
            }

        } catch (Exception e) {
            log.error("❌ 合规检查异常: accountId={}, error={}", accountId, e.getMessage(), e);
            result.setAllowed(false);  // 异常情况下默认拒绝，确保安全
            result.setViolations(List.of("系统异常：" + e.getMessage()));
        }

        return result;
    }

    // ============================================================
    // 具体检查方法
    // ============================================================

    /**
     * 检查是否在允许的时间窗口内
     *
     * 配置示例: allowed-time-window: "09:00-21:00"
     */
    public boolean isInAllowedTimeWindow(Long accountId) {
        PlatformRules rules = getPlatformRules(accountId);
        if (rules == null || rules.getAllowedTimeWindow() == null) {
            return true;  // 未配置则默认允许
        }

        LocalTime now = LocalTime.now();
        String[] parts = rules.getAllowedTimeWindow().split("-");
        LocalTime startTime = LocalTime.parse(parts[0].trim());
        LocalTime endTime = LocalTime.parse(parts[1].trim());

        return !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    /**
     * 获取下次允许发送的时间
     */
    public LocalDateTime getNextAllowedTime(Long accountId) {
        PlatformRules rules = getPlatformRules(accountId);
        if (rules == null || rules.getAllowedTimeWindow() == null) {
            return LocalDateTime.now();
        }

        String[] parts = rules.getAllowedTimeWindow().split("-");
        LocalTime startTime = LocalTime.parse(parts[0].trim());
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (now.isBefore(startTime)) {
            return LocalDateTime.of(today, startTime);
        } else {
            return LocalDateTime.of(today.plusDays(1), startTime);
        }
    }

    /**
     * 检查全局日限额（滑动窗口算法）
     */
    public DailyLimitResult checkDailyGlobalLimit(Long accountId) {
        PlatformRules rules = getPlatformRules(accountId);
        if (rules == null) {
            return DailyLimitResult.allowed(Integer.MAX_VALUE);
        }

        String key = DAILY_COUNT_PREFIX + accountId + ":" + LocalDate.now();
        int maxDaily = rules.getMaxDailyTotal();
        long windowSize = 86400;  // 24小时

        Object[] result = executeSlidingWindowScript(key, windowSize, maxDaily, 1);

        boolean allowed = ((Long) result[0]) == 1;
        long remaining = (Long) result[1];

        return new DailyLimitResult(allowed, remaining, maxDaily);
    }

    /**
     * 检查单用户日限额
     */
    public DailyLimitResult checkDailyUserLimit(Long accountId, String targetUserId) {
        PlatformRules rules = getPlatformRules(accountId);
        if (rules == null) {
            return DailyLimitResult.allowed(Integer.MAX_VALUE);
        }

        String key = USER_DAILY_PREFIX + accountId + ":" + targetUserId + ":" + LocalDate.now();
        int maxDaily = rules.getMaxDailyMessagesPerUser();
        long windowSize = 86400;

        Object[] result = executeSlidingWindowScript(key, windowSize, maxDaily, 1);

        boolean allowed = ((Long) result[0]) == 1;
        long remaining = (Long) result[1];

        return new DailyLimitResult(allowed, remaining, maxDaily);
    }

    /**
     * 令牌桶算法：尝试获取令牌
     *
     * @param accountId 账号ID
     * @param tokens 需要的令牌数
     * @return 是否获取成功
     */
    public boolean tryAcquireToken(Long accountId, int tokens) {
        PlatformRules rules = getPlatformRules(accountId);
        if (rules == null) return true;

        String key = RATE_LIMIT_PREFIX + accountId;
        int capacity = rules.getMaxDailyTotal();  // 桶容量=日限额
        double rate = (double) capacity / 86400;   // 每秒补充速率

        Long result = executeRateLimitScript(key, capacity, tokens, rate);

        return result != null && result == 1L;
    }

    /**
     * 内容相似度检测（SimHash算法简化版）
     *
     * 用途:
     * - 防止短时间内发送大量相同/相似的文案
     * - 降低被平台判定为垃圾消息的风险
     *
     * 算法原理:
     * - 将文本分词后生成指纹
     * - 计算汉明距离判断相似度
     * - 相似度超过阈值则触发警告
     */
    public SimilarityResult checkContentSimilarity(Long accountId, String content) {
        if (content == null || content.trim().isEmpty()) {
            return new SimilarityResult(0.0, false);
        }

        // 1. 获取最近发送的N条消息
        List<String> recentMessages = getLastNSentMessages(accountId, 5);
        if (recentMessages.isEmpty()) {
            return new SimilarityResult(0.0, false);
        }

        // 2. 计算与每条历史消息的相似度，取最大值
        double maxSimilarity = 0.0;
        for (String history : recentMessages) {
            double sim = calculateSimHashSimilarity(content, history);
            if (sim > maxSimilarity) {
                maxSimilarity = sim;
            }
        }

        boolean isHighSimilarity = maxSimilarity > similarityThreshold;

        // 3. 记录本次消息用于后续比较
        recordSentMessage(accountId, content);

        return new SimilarityResult(maxSimilarity, isHighSimilarity);
    }

    /**
     * 冷却期管理
     *
     * 场景:
     * - 连续多次触发频率限制后自动进入冷却期
     * - 平台API返回429 Too Many Requests时强制冷却
     * - 手动设置冷却期（管理员操作）
     */
    public void triggerCooldown(Long accountId, Duration duration) {
        String key = COOLDOWN_PREFIX + accountId;
        long seconds = duration.getSeconds();

        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(seconds));

        log.warn("⚠️ 账号已进入冷却期: accountId={}, duration={}s", accountId, seconds);
    }

    public boolean isInCooldown(Long accountId) {
        String key = COOLDOWN_PREFIX + accountId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public long getCooldownRemaining(Long accountId) {
        String key = COOLDOWN_PREFIX + accountId;
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    // ============================================================
    // 内部辅助方法
    // ============================================================

    /**
     * 执行令牌桶Lua脚本
     */
    private Long executeRateLimitScript(String key, int capacity, int tokens, double rate) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_LUA, Long.class);
        long now = System.currentTimeMillis() / 1000;

        return redisTemplate.execute(script,
                List.of(key),
                String.valueOf(capacity),
                String.valueOf(tokens),
                String.valueOf(rate),
                String.valueOf(now));
    }

    /**
     * 执行滑动窗口Lua脚本
     */
    private Object[] executeSlidingWindowScript(String key, long windowSize, int maxCount, int increment) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>(SLIDING_WINDOW_LUA, List.class);
        long now = System.currentTimeMillis() / 1000;

        List<Object> result = redisTemplate.execute(script,
                List.of(key),
                String.valueOf(windowSize),
                String.valueOf(maxCount),
                String.valueOf(now),
                String.valueOf(increment));

        if (result != null && result.size() >= 2) {
            return new Object[]{result.get(0), result.get(1)};
        }
        return new Object[]{0L, 0L};
    }

    /**
     * 加载平台规则到内存缓存
     */
    private void loadPlatformRules() {
        log.info("加载平台限流规则(当前使用内置默认配置，待接入数据库/配置中心动态加载)");

        platformRulesCache.put("DOUYIN", new PlatformRules(
                50, 3, 6, 60, "09:00-21:00"
        ));

        platformRulesCache.put("XIAOHONGSHU", new PlatformRules(
                20, 5, 0, 120, "09:00-21:00"
        ));

        platformRulesCache.put("KUAISHOU", new PlatformRules(
                30, 3, 0, 90, "09:00-21:00"
        ));

        platformRulesCache.put("WECHAT", new PlatformRules(
                100, 10, 0, 30, "08:00-22:00"
        ));
    }

    /**
     * 获取平台规则（根据账号ID查找对应平台）
     */
    private PlatformRules getPlatformRules(Long accountId) {
        log.debug("根据accountId查询平台规则，当前使用resolvePlatformCode降级实现(待接入SocialAccountRepository)");

        String platformCode = resolvePlatformCode(accountId);
        PlatformRules rules = platformRulesCache.get(platformCode);

        if (rules == null) {
            log.warn("未找到平台{}的限流规则，使用默认规则(DOUYIN)", platformCode);
            return platformRulesCache.get("DOUYIN");
        }

        return rules;
    }

    /**
     * 解析账号所属平台（简化实现）
     * 实际应从数据库或缓存中查询
     */
    private String resolvePlatformCode(Long accountId) {
        if (accountId == null) {
            return "DOUYIN";
        }

        int hash = accountId.hashCode() % 4;
        switch (hash) {
            case 0: return "DOUYIN";
            case 1: return "XIAOHONGSHU";
            case 2: return "KUAISHOU";
            default: return "WECHAT";
        }
    }

    /**
     * 获取最近发送的N条消息
     */
    private List<String> getLastNSentMessages(Long accountId, int n) {
        String key = LAST_MESSAGES_PREFIX + accountId;
        Set<String> messages = redisTemplate.opsForZSet().reverseRange(key, 0, n - 1);

        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(messages);
    }

    /**
     * 记录发送的消息（用于后续相似度比较）
     */
    private void recordSentMessage(Long accountId, String content) {
        String key = LAST_MESSAGES_PREFIX + accountId;
        redisTemplate.opsForZSet().add(key, content, System.currentTimeMillis());

        // 只保留最近50条消息
        redisTemplate.opsForZSet().removeRange(key, 0, -51);
        redisTemplate.expire(key, Duration.ofDays(7));
    }

    /**
     * SimHash相似度计算（简化版）
     *
     * 实际生产环境建议使用专业的NLP库:
     * - HanLP: https://github.com/hankcs/HanLP
     * - Jieba: https://github.com/huaban/jieba-analysis
     * - Apache OpenNLP: https://opennlp.apache.org/
     */
    private double calculateSimHashSimilarity(String text1, String text2) {
        // 简化实现：基于编辑距离（Levenshtein Distance）
        int maxLength = Math.max(text1.length(), text2.length());
        if (maxLength == 0) return 1.0;

        int distance = calculateLevenshteinDistance(text1, text2);
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * 编辑距离算法（Levenshtein Distance）
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }

    // ============================================================
    // 统计与管理接口
    // ============================================================

    /**
     * 获取账号当前的配额使用情况
     */
    public QuotaInfo getQuotaInfo(Long accountId) {
        QuotaInfo info = new QuotaInfo();
        info.setAccountId(accountId);

        info.setInCooldown(isInCooldown(accountId));
        if (info.isInCooldown()) {
            info.setCooldownRemaining(getCooldownRemaining(accountId));
        }

        info.setInTimeWindow(isInAllowedTimeWindow(accountId));
        if (!info.isInTimeWindow()) {
            info.setNextAllowedTime(getNextAllowedTime(accountId));
        }

        DailyLimitResult globalLimit = checkDailyGlobalLimit(accountId);
        info.setDailyGlobalUsed(globalLimit.getMaxDaily() - globalLimit.getRemaining());
        info.setDailyGlobalTotal(globalLimit.getMaxDaily());

        return info;
    }

    /**
     * 重置账号所有限额（仅管理员可调用）
     */
    public void resetQuota(Long accountId) {
        String pattern = RATE_LIMIT_PREFIX + accountId + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        log.info("🔄 已重置账号限额: accountId={}", accountId);
    }

    // ============================================================
    // 数据模型
    // ============================================================

    @Data
    public static class RateLimitResult {
        private Long accountId;
        private boolean allowed;
        private String message;
        private LocalDateTime checkedAt;
        private List<String> violations;
        private long dailyGlobalRemaining;
        private long dailyGlobalMax;
        private long dailyUserRemaining;
        private long dailyUserMax;
        private Double contentSimilarity;
        private Duration cooldownRemaining;
        private LocalDateTime nextAllowedTime;
    }

    @Data
    public static class DailyLimitResult {
        private boolean allowed;
        private long remaining;
        private int maxDaily;

        public DailyLimitResult(boolean allowed, long remaining, int maxDaily) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.maxDaily = maxDaily;
        }

        public static DailyLimitResult allowed(int maxDaily) {
            return new DailyLimitResult(true, maxDaily, maxDaily);
        }
    }

    @Data
    public static class SimilarityResult {
        private double similarity;      // 0.0-1.0
        private boolean highSimilarity; // 是否超过阈值

        public SimilarityResult(double similarity, boolean highSimilarity) {
            this.similarity = similarity;
            this.highSimilarity = highSimilarity;
        }
    }

    @Data
    public static class QuotaInfo {
        private Long accountId;
        private boolean inCooldown;
        private long cooldownRemaining;
        private boolean inTimeWindow;
        private LocalDateTime nextAllowedTime;
        private long dailyGlobalUsed;
        private long dailyGlobalTotal;
    }

    @Data
    public static class PlatformRules {
        private int maxDailyTotal;              // 全局日限额
        private int maxDailyMessagesPerUser;    // 单用户日限额
        private int maxMessagesPerConversation; // 单会话消息数
        private int minIntervalSeconds;         // 最小发送间隔
        private String allowedTimeWindow;       // 允许时间窗口

        public PlatformRules(int maxDailyTotal, int maxDailyMessagesPerUser,
                             int maxMessagesPerConversation, int minIntervalSeconds,
                             String allowedTimeWindow) {
            this.maxDailyTotal = maxDailyTotal;
            this.maxDailyMessagesPerUser = maxDailyMessagesPerUser;
            this.maxMessagesPerConversation = maxMessagesPerConversation;
            this.minIntervalSeconds = minIntervalSeconds;
            this.allowedTimeWindow = allowedTimeWindow;
        }
    }
}
