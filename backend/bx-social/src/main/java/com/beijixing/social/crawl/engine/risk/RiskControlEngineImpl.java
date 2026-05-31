package com.beijixing.social.crawl.engine.risk;

import com.beijixing.social.crawl.engine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskControlEngineImpl implements RiskControlEngine {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, List<RiskControlRule>> platformRules = new ConcurrentHashMap<>();

    @Override
    public List<RiskControlRule> getActiveRules(String platformCode) {
        return platformRules.getOrDefault(platformCode.toUpperCase(), Collections.emptyList())
                .stream()
                .filter(RiskControlRule::isActive)
                .sorted(Comparator.comparingInt(RiskControlRule::getPriority).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public RiskControlEvaluationResult evaluateRequest(CrawlTaskContext context) {
        String platformCode = context.getPlatformCode();
        List<RiskControlRule> activeRules = getActiveRules(platformCode);

        for (RiskControlRule rule : activeRules) {
            try {
                if (rule.evaluate(context)) {
                    log.warn("触发风控规则: platform={}, rule={}, action={}", 
                            platformCode, rule.getRuleId(), rule.getActionOnTrigger());
                    
                    recordViolation(platformCode, rule.getRuleId(), context);
                    
                    long recommendedDelay = calculateRecommendedDelay(platformCode, rule);
                    
                    RiskControlEvaluationResult result = RiskControlEvaluationResult.blocked(
                            rule.getActionOnTrigger(),
                            rule.getRuleId(),
                            "触发规则: " + rule.getRuleName()
                    );
                    result.setRecommendedDelayMs(recommendedDelay);
                    return result;
                }
            } catch (Exception e) {
                log.error("风控规则执行失败: rule={}, error={}", rule.getRuleId(), e.getMessage());
            }
        }

        RiskControlEvaluationResult result = RiskControlEvaluationResult.allowed();
        result.setRecommendedDelayMs(getRecommendedDelayMs(platformCode));
        return result;
    }

    @Override
    public void recordViolation(String platformCode, String ruleId, CrawlTaskContext context) {
        String violationKey = "violation:" + platformCode + ":" + ruleId;
        Long count = redisTemplate.opsForValue().increment(violationKey);
        
        if (count != null && count == 1) {
            redisTemplate.expire(violationKey, java.time.Duration.ofHours(1));
        }

        log.info("记录违规: platform={}, rule={}, count={}", platformCode, ruleId, count);

        String taskKey = "task:violation:" + context.getTask().getId();
        redisTemplate.opsForValue().increment(taskKey);
    }

    @Override
    public void updateRulesFromMonitor(String platformCode, RuleUpdatePayload payload) {
        log.info("更新风控规则: platform={}, reason={}", 
                platformCode, payload.getChangeReason());

        List<RiskControlRule> updatedRules = payload.getUpdatedRules();
        if (updatedRules != null && !updatedRules.isEmpty()) {
            platformRules.put(platformCode.toUpperCase(), updatedRules);
            
            String updateKey = "rule_update:" + platformCode;
            redisTemplate.opsForValue().set(updateKey, 
                    com.alibaba.fastjson2.JSON.toJSONString(payload));
            redisTemplate.expire(updateKey, java.time.Duration.ofDays(7));
        }
    }

    @Override
    public boolean shouldExecuteTask(CrawlTaskContext context) {
        String platformCode = context.getPlatformCode();
        
        String banKey = "ban:" + platformCode;
        String banned = redisTemplate.opsForValue().get(banKey);
        if ("true".equals(banned)) {
            log.warn("平台已被封禁: platform={}", platformCode);
            return false;
        }

        String taskViolationKey = "task:violation:" + context.getTask().getId();
        String violationCountStr = redisTemplate.opsForValue().get(taskViolationKey);
        if (violationCountStr != null) {
            int violations = Integer.parseInt(violationCountStr);
            if (violations >= 10) {
                log.warn("任务违规次数过多，暂停执行: taskId={}, violations={}", 
                        context.getTask().getId(), violations);
                return false;
            }
        }

        return true;
    }

    @Override
    public long getRecommendedDelayMs(String platformCode) {
        String delayKey = "recommended_delay:" + platformCode;
        String delayStr = redisTemplate.opsForValue().get(delayKey);
        
        if (delayStr != null) {
            try {
                return Long.parseLong(delayStr);
            } catch (NumberFormatException e) {
                log.warn("解析推荐延迟失败: {}", delayStr);
            }
        }

        switch (platformCode.toUpperCase()) {
            case "DOUYIN": return 1500L;
            case "XIAOHONGSHU": return 2500L;
            case "KUAISHOU": return 2000L;
            case "WEIBO": return 1200L;
            case "BILIBILI": return 1800L;
            default: return 2000L;
        }
    }

    private long calculateRecommendedDelay(String platformCode, RiskControlRule triggeredRule) {
        long baseDelay = getRecommendedDelayMs(platformCode);
        
        switch (triggeredRule.getActionOnTrigger()) {
            case DELAY_AND_RETRY:
                return baseDelay * 3;
            case REDUCE_RATE:
                return baseDelay * 5;
            case SWITCH_PROXY:
                return baseDelay * 2;
            default:
                return baseDelay * 2;
        }
    }

    public void registerDefaultRules() {
        registerDouyinRules();
        registerXiaohongshuRules();
        registerKuaishouRules();
        registerWeiboRules();
        registerBilibiliRules();

        log.info("注册默认风控规则完成");
    }

    private void registerDouyinRules() {
        List<RiskControlRule> rules = new ArrayList<>();

        rules.add(new RateLimitRule("DOUYIN_RATE_LIMIT", "DOUYIN", "请求频率限制",
                "RATE_LIMIT", 10, () -> 30));

        rules.add(new BanDetectionRule("DOUYIN_BAN_DETECTION", "DOUYIN", "封禁检测",
                "BAN_DETECTION", 9));

        rules.add(new BehaviorAnalysisRule("DOUYIN_BEHAVIOR", "DOUYIN", "行为分析",
                "BEHAVIOR", 7));

        platformRules.put("DOUYIN", rules);
    }

    private void registerXiaohongshuRules() {
        List<RiskControlRule> rules = new ArrayList<>();

        rules.add(new RateLimitRule("XHS_RATE_LIMIT", "XIAOHONGSHU", "请求频率限制",
                "RATE_LIMIT", 10, () -> 20));

        rules.add(new SignatureValidationRule("XHS_SIGNATURE", "XIAOHONGSHU", "签名验证",
                "SIGNATURE_VALIDATION", 10));

        rules.add(new CookieExpiryRule("XHS_COOKIE_EXPIRY", "XIAOHONGSHU", "Cookie过期检测",
                "COOKIE_EXPIRY", 8));

        platformRules.put("XIAOHONGSHU", rules);
    }

    private void registerKuaishouRules() {
        List<RiskControlRule> rules = new ArrayList<>();

        rules.add(new RateLimitRule("KS_RATE_LIMIT", "KUAISHOU", "请求频率限制",
                "RATE_LIMIT", 9, () -> 25));

        rules.add(new BanDetectionRule("KS_BAN_DETECTION", "KUAISHOU", "封禁检测",
                "BAN_DETECTION", 8));

        platformRules.put("KUAISHOU", rules);
    }

    private void registerWeiboRules() {
        List<RiskControlRule> rules = new ArrayList<>();

        rules.add(new RateLimitRule("WB_RATE_LIMIT", "WEIBO", "请求频率限制",
                "RATE_LIMIT", 8, () -> 40));

        rules.add(new BanDetectionRule("WB_BAN_DETECTION", "WEIBO", "封禁检测",
                "BAN_DETECTION", 7));

        platformRules.put("WEIBO", rules);
    }

    private void registerBilibiliRules() {
        List<RiskControlRule> rules = new ArrayList<>();

        rules.add(new RateLimitRule("BILI_RATE_LIMIT", "BILIBILI", "请求频率限制",
                "RATE_LIMIT", 8, () -> 35));

        rules.add(new BanDetectionRule("BILI_BAN_DETECTION", "BILIBILI", "封禁检测",
                "BAN_DETECTION", 7));

        platformRules.put("BILIBILI", rules);
    }
}
