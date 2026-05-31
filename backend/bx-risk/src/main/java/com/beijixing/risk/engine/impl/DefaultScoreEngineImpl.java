package com.beijixing.risk.engine.impl;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.entity.RiskScore;
import com.beijixing.risk.engine.ScoreEngine;
import com.beijixing.risk.repository.RiskScoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;

/**
 * 默认评分引擎实现
 *
 * @author 林超 (EMP-SEC-001)
 * 评分维度权重：
 * - 操作频率: 30%
 * - 内容合规: 25%
 * - 触达成功: 25%
 * - 账号活跃: 20%
 */
@Slf4j
@Component
@SuppressWarnings("nullness")
public class DefaultScoreEngineImpl implements ScoreEngine {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RiskScoreRepository riskScoreRepository;

    private static final String SCORE_KEY_PREFIX = "risk:score:";
    private static final String OPERATION_COUNT_PREFIX = "risk:opcount:";

    @PostConstruct
    public void init() {
        log.info("评分引擎初始化完成");
    }

    @Override
    public ScoreResult calculateScore(RiskCheckRequest request) {
        ScoreResult result = new ScoreResult();

        // 1. 计算操作频率评分 (权重30%)
        int frequencyScore = calculateFrequencyScore(request);
        result.setFrequencyScore(frequencyScore);

        // 2. 计算内容合规评分 (权重25%)
        int complianceScore = calculateComplianceScore(request);
        result.setComplianceScore(complianceScore);

        // 3. 计算触达成功评分 (权重25%)
        int touchScore = calculateTouchScore(request);
        result.setTouchScore(touchScore);

        // 4. 计算账号活跃评分 (权重20%)
        int activityScore = calculateActivityScore(request);
        result.setActivityScore(activityScore);

        // 5. 计算设备指纹评分
        int deviceScore = calculateDeviceScore(request);
        result.setDeviceScore(deviceScore);

        // 6. 计算IP评分
        int ipScore = calculateIpScore(request);
        result.setIpScore(ipScore);

        // 7. 综合评分
        int totalScore = (frequencyScore * 30 + complianceScore * 25 +
                         touchScore * 25 + activityScore * 20) / 100;
        totalScore = Math.min(100, (totalScore + deviceScore + ipScore) / 3);
        result.setTotalScore(totalScore);

        // 8. 子维度评分Map
        Map<String, Integer> subScores = new HashMap<>();
        subScores.put("frequency", frequencyScore);
        subScores.put("compliance", complianceScore);
        subScores.put("touch", touchScore);
        subScores.put("activity", activityScore);
        subScores.put("device", deviceScore);
        subScores.put("ip", ipScore);
        result.setSubScores(subScores);

        // 更新评分缓存
        updateScoreCache(request, result);

        log.debug("评分计算完成: accountId={}, totalScore={}", request.getAccountId(), totalScore);
        return result;
    }

    /**
     * 计算操作频率评分
     * 根据当前时间窗口内的操作次数与平台限制对比计算
     */
    private int calculateFrequencyScore(RiskCheckRequest request) {
        int score = 100;
        if (request.getAccountId() == null) {
            return score - 10;
        }

        String key = OPERATION_COUNT_PREFIX + request.getAccountId() + ":" + request.getOperationType();
        Object countObj = redisTemplate.opsForValue().get(key);
        int count = countObj != null ? Integer.parseInt(countObj.toString()) : 0;

        // 获取平台限制（简化处理，实际应从数据库读取）
        int limit = getOperationLimit(request.getOperationType());
        if (limit > 0) {
            double usageRate = (double) count / limit;
            if (usageRate > 1.0) {
                score -= 40;  // 超限
            } else if (usageRate > 0.8) {
                score -= 20;  // 接近上限
            } else if (usageRate > 0.5) {
                score -= 10;  // 使用较多
            }
        }

        return Math.max(0, score);
    }

    /**
     * 计算内容合规评分
     */
    private int calculateComplianceScore(RiskCheckRequest request) {
        int score = 100;
        Map<String, Object> params = request.getRequestParams();
        if (params == null) {
            return score;
        }

        Object content = params.get("content");
        if (content == null) {
            return score;
        }

        String contentStr = content.toString();

        // AI生成内容检测
        Boolean aiGenerated = (Boolean) params.get("aiGenerated");
        if (Boolean.TRUE.equals(aiGenerated)) {
            score -= 15;
        }

        // 违禁词检测
        List<String> sensitiveWords = Arrays.asList("违禁", "违法", "敏感");
        for (String word : sensitiveWords) {
            if (contentStr.contains(word)) {
                score -= 20;
                break;
            }
        }

        return Math.max(0, score);
    }

    /**
     * 计算触达成功评分
     */
    private int calculateTouchScore(RiskCheckRequest request) {
        int score = 100;
        if (request.getAccountId() == null) {
            return score;
        }

        // 从Redis获取触达成功率
        String key = "risk:touch:rate:" + request.getAccountId();
        Object rate = redisTemplate.opsForValue().get(key);
        if (rate != null) {
            double touchRate = Double.parseDouble(rate.toString());
            if (touchRate < 0.5) {
                score -= 30;
            } else if (touchRate < 0.7) {
                score -= 15;
            }
        }

        return Math.max(0, score);
    }

    /**
     * 计算账号活跃评分
     */
    private int calculateActivityScore(RiskCheckRequest request) {
        int score = 100;
        if (request.getAccountId() == null) {
            return score;
        }

        String key = "risk:lastop:" + request.getAccountId();
        Object lastOp = redisTemplate.opsForValue().get(key);
        if (lastOp != null) {
            long lastOpTime = Long.parseLong(lastOp.toString());
            long now = System.currentTimeMillis();
            long hoursSinceLastOp = (now - lastOpTime) / (1000 * 60 * 60);

            if (hoursSinceLastOp > 72) {  // 超过3天未操作
                score -= 30;
            } else if (hoursSinceLastOp > 24) {  // 超过1天未操作
                score -= 10;
            }
        }

        return Math.max(0, score);
    }

    /**
     * 计算设备指纹评分
     */
    private int calculateDeviceScore(RiskCheckRequest request) {
        String fingerprint = request.getDeviceFingerprint();
        if (fingerprint == null) {
            return 60;  // 无设备指纹降分
        }

        // 检查设备是否在黑名单
        String key = "risk:device:blacklist";
        Boolean isBlacklisted = redisTemplate.opsForValue().getBit(key, fingerprint.hashCode());
        if (Boolean.TRUE.equals(isBlacklisted)) {
            return 10;
        }

        // 检查设备使用频率（同一设备绑定多个账号）
        String deviceKey = "risk:device:accounts:" + fingerprint.hashCode();
        Object accountCount = redisTemplate.opsForValue().get(deviceKey);
        if (accountCount != null) {
            int count = Integer.parseInt(accountCount.toString());
            if (count > 5) {
                return 50;  // 同一设备账号过多
            }
        }

        return 100;
    }

    /**
     * 计算IP评分
     */
    private int calculateIpScore(RiskCheckRequest request) {
        String ip = request.getIpAddress();
        if (ip == null) {
            return 80;
        }

        // 检查IP黑名单
        String key = "risk:ip:blacklist";
        Boolean isBlacklisted = redisTemplate.opsForValue().getBit(key, ip.hashCode());
        if (Boolean.TRUE.equals(isBlacklisted)) {
            return 5;
        }

        // 检查IP代理风险
        String proxyKey = "risk:ip:proxy:" + ip;
        Boolean isProxy = redisTemplate.opsForValue().getBit(proxyKey, 0);
        if (Boolean.TRUE.equals(isProxy)) {
            return 60;
        }

        // 检查IP使用频率
        String usageKey = "risk:ip:usage:" + ip;
        Object usage = redisTemplate.opsForValue().get(usageKey);
        if (usage != null) {
            int usageCount = Integer.parseInt(usage.toString());
            if (usageCount > 50) {
                return 70;  // IP使用频率过高
            }
        }

        return 100;
    }

    @Override
    public RiskScore getAccountScore(Long accountId) {
        return riskScoreRepository.getAccountScore(accountId);
    }

    @Override
    public void updateAccountScore(Long accountId, int score) {
        String key = SCORE_KEY_PREFIX + "account:" + accountId;
        redisTemplate.opsForValue().set(key, score, Duration.ofHours(1));
    }

    @Override
    public Map<String, Integer> calculateSubScores(RiskCheckRequest request) {
        ScoreResult result = calculateScore(request);
        return result.getSubScores();
    }

    /**
     * 更新评分缓存
     */
    private void updateScoreCache(RiskCheckRequest request, ScoreResult result) {
        if (request.getAccountId() != null) {
            String key = SCORE_KEY_PREFIX + "account:" + request.getAccountId();
            redisTemplate.opsForValue().set(key, result.getTotalScore(), Duration.ofHours(1));

            // 更新操作计数
            String countKey = OPERATION_COUNT_PREFIX + request.getAccountId() + ":" + request.getOperationType();
            redisTemplate.opsForValue().increment(countKey);
            redisTemplate.expire(countKey, Duration.ofDays(1));

            // 更新最后操作时间
            String lastOpKey = "risk:lastop:" + request.getAccountId();
            redisTemplate.opsForValue().set(lastOpKey, System.currentTimeMillis());
        }
    }

    /**
     * 获取操作限制
     */
    private int getOperationLimit(String operationType) {
        return switch (operationType) {
            case "publish" -> 10;
            case "message" -> 50;
            case "follow" -> 100;
            case "comment" -> 200;
            default -> 100;
        };
    }
}
