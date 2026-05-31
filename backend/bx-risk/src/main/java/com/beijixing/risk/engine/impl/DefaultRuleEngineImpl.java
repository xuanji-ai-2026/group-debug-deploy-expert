package com.beijixing.risk.engine.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.entity.RiskRule;
import com.beijixing.risk.engine.RuleEngine;
import com.beijixing.risk.repository.RiskRuleRepository;
import com.beijixing.risk.vo.RiskDecisionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认规则引擎实现
 *
 * @author 林超 (EMP-SEC-001)
 */
@Slf4j
@Component
@SuppressWarnings("nullness")
public class DefaultRuleEngineImpl implements RuleEngine {

    @Autowired
    private RiskRuleRepository riskRuleRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 内存规则缓存（按操作类型分组）
     */
    private final Map<String, List<RiskRule>> ruleCache = new ConcurrentHashMap<>();

    /**
     * 线程本地变量，存储当前检查结果
     */
    private final ThreadLocal<RuleCheckResult> resultHolder = new ThreadLocal<>();

    @PostConstruct
    public void init() {
        reloadRules();
        log.info("规则引擎初始化完成");
    }

    @Override
    public void reloadRules() {
        try {
            List<RiskRule> allRules = riskRuleRepository.list(
                new LambdaQueryWrapper<RiskRule>()
                    .eq(RiskRule::getStatus, 1)
                    .le(RiskRule::getEffectiveTime, LocalDateTime.now())
                    .ge(RiskRule::getExpireTime, LocalDateTime.now())
                    .or()
                    .isNull(RiskRule::getEffectiveTime)
                    .isNull(RiskRule::getExpireTime)
                    .orderByAsc(RiskRule::getPriority)
            );

            // 按操作类型分组缓存
            ruleCache.clear();
            for (RiskRule rule : allRules) {
                ruleCache.computeIfAbsent(rule.getRuleType(), k -> new ArrayList<>()).add(rule);
            }

            log.info("规则引擎重载完成，共加载 {} 条规则", allRules.size());
        } catch (Exception e) {
            log.error("规则引擎重载失败", e);
        }
    }

    @Override
    public List<RiskRule> getApplicableRules(String operationType, Long platformId) {
        List<RiskRule> rules = ruleCache.get(operationType);
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        // 过滤出平台适用的规则
        return rules.stream()
            .filter(r -> r.getPlatformId() == null || r.getPlatformId().equals(platformId))
            .collect(Collectors.toList());
    }

    @Override
    public RuleCheckResult checkRules(RiskCheckRequest request) {
        RuleCheckResult result = new RuleCheckResult();
        result.setBaseScore(100);
        result.setTotalDeductScore(0);
        result.setTriggeredRules(new ArrayList<>());

        List<RiskRule> applicableRules = getApplicableRules(
            request.getOperationType(),
            request.getPlatformCode() != null ? null : null  // platformId解析逻辑
        );

        int highestDeduct = 0;
        String primaryRiskType = null;

        for (RiskRule rule : applicableRules) {
            try {
                // 解析规则配置
                JSONObject config = StringUtils.hasText(rule.getRuleConfig())
                    ? JSON.parseObject(rule.getRuleConfig())
                    : new JSONObject();

                // 执行规则条件判断
                boolean triggered = evaluateRule(rule, request, config);

                if (triggered) {
                    int deductScore = rule.getDeductScore() != null ? rule.getDeductScore() : 10;

                    RiskDecisionVO.TriggeredRuleVO triggeredRule = new RiskDecisionVO.TriggeredRuleVO();
                    triggeredRule.setRuleId(rule.getId());
                    triggeredRule.setRuleName(rule.getRuleName());
                    triggeredRule.setRuleCode(rule.getRuleCode());
                    triggeredRule.setRiskType(rule.getRiskType());
                    triggeredRule.setDeductScore(deductScore);
                    triggeredRule.setTriggerReason(buildTriggerReason(rule, config));

                    result.getTriggeredRules().add(triggeredRule);
                    result.setTotalDeductScore(result.getTotalDeductScore() + deductScore);

                    // 记录最严重的风险类型
                    if (deductScore > highestDeduct) {
                        highestDeduct = deductScore;
                        primaryRiskType = rule.getRiskType();
                    }

                    log.debug("规则触发: ruleName={}, deductScore={}", rule.getRuleName(), deductScore);
                }
            } catch (Exception e) {
                log.error("规则执行异常: ruleId={}", rule.getId(), e);
            }
        }

        // 计算最终分数
        int finalScore = Math.max(0, result.getBaseScore() - result.getTotalDeductScore());
        result.setFinalScore(finalScore);
        result.setPassed(finalScore >= 40);  // 低于40分认为不通过
        result.setPrimaryRiskType(primaryRiskType);

        resultHolder.set(result);
        return result;
    }

    @Override
    public RuleCheckResult getRuleCheckResult() {
        return resultHolder.get();
    }

    /**
     * 评估规则是否触发
     */
    private boolean evaluateRule(RiskRule rule, RiskCheckRequest request, JSONObject config) {
        String category = rule.getRuleCategory();
        if (category == null) {
            return false;
        }

        return switch (category.toLowerCase()) {
            // 频率限制规则
            case "frequency" -> evaluateFrequencyRule(request, config);
            // 内容合规规则
            case "content" -> evaluateContentRule(request, config);
            // IP风险规则
            case "ip" -> evaluateIpRule(request, config);
            // 设备指纹规则
            case "device" -> evaluateDeviceRule(request, config);
            // 行为模拟规则
            case "behavior" -> evaluateBehaviorRule(request, config);
            // 账号风险规则
            case "account" -> evaluateAccountRule(request, config);
            default -> false;
        };
    }

    /**
     * 评估频率规则
     */
    private boolean evaluateFrequencyRule(RiskCheckRequest request, JSONObject config) {
        String key = "risk:frequency:" + request.getAccountId() + ":" + request.getOperationType();
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }

        Integer maxCount = config.getInteger("maxCount");
        if (maxCount != null && count != null && count > maxCount) {
            return true;
        }

        // 检查时间窗口内频率
        Integer windowSeconds = config.getInteger("windowSeconds");
        if (windowSeconds != null && count != null) {
            Long windowMax = config.getLong("windowMaxCount");
            if (windowMax != null && count > windowMax) {
                return true;
            }
        }

        return false;
    }

    /**
     * 评估内容规则
     */
    private boolean evaluateContentRule(RiskCheckRequest request, JSONObject config) {
        Map<String, Object> params = request.getRequestParams();
        if (params == null) {
            return false;
        }

        // 检查违禁词
        List<String> sensitiveWords = config.getJSONArray("sensitiveWords")
            .toList(String.class);
        Object contentObj = params.get("content");
        if (contentObj != null && sensitiveWords != null) {
            String content = contentObj.toString().toLowerCase();
            for (String word : sensitiveWords) {
                if (content.contains(word.toLowerCase())) {
                    return true;
                }
            }
        }

        // 检查内容长度
        Integer minLength = config.getInteger("minLength");
        Integer maxLength = config.getInteger("maxLength");
        if (contentObj != null) {
            int contentLength = contentObj.toString().length();
            if (minLength != null && contentLength < minLength) {
                return true;
            }
            if (maxLength != null && contentLength > maxLength) {
                return true;
            }
        }

        return false;
    }

    /**
     * 评估IP规则
     */
    private boolean evaluateIpRule(RiskCheckRequest request, JSONObject config) {
        String ip = request.getIpAddress();
        if (ip == null) {
            return false;
        }

        // 检查IP黑名单
        String blacklistKey = "risk:ip:blacklist";
        Boolean isBlacklisted = redisTemplate.opsForValue().getBit(blacklistKey, ip.hashCode());
        if (Boolean.TRUE.equals(isBlacklisted)) {
            return true;
        }

        // 检查高风险地区
        List<String> highRiskRegions = config.getJSONArray("highRiskRegions")
            .toList(String.class);
        if (highRiskRegions != null && !highRiskRegions.isEmpty()) {
            // 实际生产中应调用IP地理位置服务
            // 这里简化处理
        }

        return false;
    }

    /**
     * 评估设备指纹规则
     */
    private boolean evaluateDeviceRule(RiskCheckRequest request, JSONObject config) {
        String fingerprint = request.getDeviceFingerprint();
        if (fingerprint == null) {
            return true; // 无设备指纹视为高风险
        }

        // 检查设备指纹是否在黑名单
        String deviceBlacklistKey = "risk:device:blacklist";
        Boolean isBlacklisted = redisTemplate.opsForValue().getBit(deviceBlacklistKey, fingerprint.hashCode());
        return Boolean.TRUE.equals(isBlacklisted);
    }

    /**
     * 评估行为规则
     */
    private boolean evaluateBehaviorRule(RiskCheckRequest request, JSONObject config) {
        // 检查操作时间分布（夜间操作）
        Boolean checkNightOperation = config.getBoolean("checkNightOperation");
        if (Boolean.TRUE.equals(checkNightOperation)) {
            int hour = LocalDateTime.now().getHour();
            if (hour >= 23 || hour <= 5) {
                return true;
            }
        }

        // 检查操作间隔
        Integer minInterval = config.getInteger("minIntervalSeconds");
        if (minInterval != null && request.getAccountId() != null) {
            String lastOpKey = "risk:lastop:" + request.getAccountId() + ":" + request.getOperationType();
            Object lastOpTime = redisTemplate.opsForValue().get(lastOpKey);
            if (lastOpTime != null) {
                long lastOp = Long.parseLong(lastOpTime.toString());
                long now = System.currentTimeMillis();
                if ((now - lastOp) < minInterval * 1000L) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 评估账号规则
     */
    private boolean evaluateAccountRule(RiskCheckRequest request, JSONObject config) {
        // 检查账号状态
        Integer minAccountScore = config.getInteger("minAccountScore");
        if (minAccountScore != null && request.getAccountId() != null) {
            String scoreKey = "risk:account:score:" + request.getAccountId();
            Object score = redisTemplate.opsForValue().get(scoreKey);
            if (score != null) {
                int accountScore = Integer.parseInt(score.toString());
                if (accountScore < minAccountScore) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 构建规则触发原因
     */
    private String buildTriggerReason(RiskRule rule, JSONObject config) {
        StringBuilder reason = new StringBuilder();
        reason.append(rule.getDescription() != null ? rule.getDescription() : rule.getRuleName());
        reason.append("，扣").append(rule.getDeductScore()).append("分");
        return reason.toString();
    }
}
