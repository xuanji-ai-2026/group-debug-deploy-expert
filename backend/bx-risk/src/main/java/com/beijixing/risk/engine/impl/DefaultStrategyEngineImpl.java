package com.beijixing.risk.engine.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.risk.entity.RiskStrategy;
import com.beijixing.risk.engine.StrategyEngine;
import com.beijixing.risk.enums.RiskAction;
import com.beijixing.risk.repository.RiskStrategyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认策略引擎实现
 *
 * @author 林超 (EMP-SEC-001)
 */
@Slf4j
@Component
public class DefaultStrategyEngineImpl implements StrategyEngine {

    @Autowired
    private RiskStrategyRepository strategyRepository;

    /**
     * 内存策略缓存
     */
    private final Map<String, List<RiskStrategy>> strategyCache = new ConcurrentHashMap<>();
    private final ThreadLocal<StrategyDecisionResult> resultHolder = new ThreadLocal<>();

    @PostConstruct
    public void init() {
        reloadStrategies();
        log.info("策略引擎初始化完成");
    }

    @Override
    public void reloadStrategies() {
        try {
            List<RiskStrategy> allStrategies = strategyRepository.list(
                new LambdaQueryWrapper<RiskStrategy>()
                    .eq(RiskStrategy::getStatus, 1)
                    .orderByAsc(RiskStrategy::getPriority)
            );

            strategyCache.clear();
            for (RiskStrategy strategy : allStrategies) {
                strategyCache.computeIfAbsent(strategy.getStrategyType(), k -> new ArrayList<>())
                            .add(strategy);
            }

            log.info("策略引擎重载完成，共加载 {} 条策略", allStrategies.size());
        } catch (Exception e) {
            log.error("策略引擎重载失败", e);
        }
    }

    @Override
    public List<RiskStrategy> getApplicableStrategies(String strategyType, Long tenantId) {
        List<RiskStrategy> strategies = strategyCache.get(strategyType);
        if (strategies == null || strategies.isEmpty()) {
            return Collections.emptyList();
        }

        return strategies.stream()
            .filter(s -> s.getTenantId() == null || s.getTenantId().equals(tenantId))
            .collect(Collectors.toList());
    }

    @Override
    public StrategyDecisionResult executeStrategies(DecisionContext context) {
        StrategyDecisionResult result = new StrategyDecisionResult();
        result.setRiskTags(new ArrayList<>());

        List<RiskStrategy> strategies = getApplicableStrategies(
            context.getStrategyType(),
            context.getTenantId()
        );

        if (strategies.isEmpty()) {
            // 无策略时默认放行
            result.setFinalAction(RiskAction.PASS.name());
            result.setSuggestion("检查通过");
            result.setNeedReview(false);
            result.setMatchedStrategy("默认策略");
            resultHolder.set(result);
            return result;
        }

        // 按优先级匹配策略
        for (RiskStrategy strategy : strategies) {
            if (matchStrategy(strategy, context)) {
                result = buildResultFromStrategy(strategy, context);
                log.info("策略匹配成功: strategyName={}, action={}",
                    strategy.getStrategyName(), result.getFinalAction());
                break;
            }
        }

        resultHolder.set(result);
        return result;
    }

    @Override
    public StrategyDecisionResult getStrategyResult() {
        return resultHolder.get();
    }

    /**
     * 匹配策略条件
     */
    private boolean matchStrategy(RiskStrategy strategy, DecisionContext context) {
        String triggerCondition = strategy.getTriggerCondition();
        if (!StringUtils.hasText(triggerCondition)) {
            return true;  // 无触发条件则默认匹配
        }

        try {
            JSONObject condition = JSON.parseObject(triggerCondition);

            // 检查分数阈值
            Integer threshold = condition.getInteger("riskThreshold");
            if (threshold != null && context.getCurrentScore() < threshold) {
                return true;
            }

            // 检查触发规则数量
            Integer minRules = condition.getInteger("minTriggeredRules");
            if (minRules != null && context.getTriggeredRuleCount() >= minRules) {
                return true;
            }

            // 检查操作类型
            JSONArray operationTypes = condition.getJSONArray("operationTypes");
            if (operationTypes != null && !operationTypes.isEmpty()) {
                return operationTypes.toList(String.class)
                    .contains(context.getOperationType());
            }

            return false;
        } catch (Exception e) {
            log.error("策略条件解析异常: strategyId={}", strategy.getId(), e);
            return false;
        }
    }

    /**
     * 根据策略构建决策结果
     */
    private StrategyDecisionResult buildResultFromStrategy(RiskStrategy strategy, DecisionContext context) {
        StrategyDecisionResult result = new StrategyDecisionResult();
        result.setMatchedStrategy(strategy.getStrategyName());
        result.setRiskTags(new ArrayList<>());

        // 解析策略配置
        String config = strategy.getStrategyConfig();
        JSONObject strategyConfig = StringUtils.hasText(config)
            ? JSON.parseObject(config)
            : new JSONObject();

        // 确定执行动作
        String defaultAction = strategy.getDefaultAction();
        if (StringUtils.hasText(defaultAction)) {
            result.setFinalAction(defaultAction);
        } else {
            // 根据评分自动决策
            int score = context.getCurrentScore();
            if (score < 20) {
                result.setFinalAction(RiskAction.BAN.name());
            } else if (score < 40) {
                result.setFinalAction(RiskAction.BLOCK.name());
            } else if (score < 60) {
                result.setFinalAction(RiskAction.REVIEW.name());
            } else if (score < 80) {
                result.setFinalAction(RiskAction.WARN.name());
            } else {
                result.setFinalAction(RiskAction.PASS.name());
            }
        }

        // 是否需要人工审核
        Boolean needReview = strategyConfig.getBoolean("needReview");
        result.setNeedReview(Boolean.TRUE.equals(needReview) ||
                           result.getFinalAction().equals(RiskAction.REVIEW.name()));

        // 生成建议
        result.setSuggestion(generateSuggestion(strategy, context));

        // 添加风险标签
        addRiskTags(result, strategyConfig);

        return result;
    }

    /**
     * 生成处理建议
     */
    private String generateSuggestion(RiskStrategy strategy, DecisionContext context) {
        String action = resultHolder.get() != null ? resultHolder.get().getFinalAction() : "";

        return switch (action) {
            case "BAN" -> "账号已被封禁，请联系客服处理";
            case "BLOCK" -> "操作被拦截，建议降低操作频率或优化内容";
            case "REVIEW" -> "内容需要人工审核，请等待审核结果";
            case "WARN" -> "操作已记录，建议关注账号评分变化";
            case "RATE_LIMIT" -> "操作频率过高，建议间隔更长时间后重试";
            case "OPTIMIZE" -> "内容需要优化，建议检查是否包含敏感词";
            default -> "检查通过，请继续操作";
        };
    }

    /**
     * 添加风险标签
     */
    private void addRiskTags(StrategyDecisionResult result, JSONObject config) {
        List<String> tags = result.getRiskTags();
        if (tags == null) {
            tags = new ArrayList<>();
        }

        // 从策略配置中读取风险标签
        JSONArray tagArray = config.getJSONArray("riskTags");
        if (tagArray != null) {
            tags.addAll(tagArray.toList(String.class));
        }

        // 根据触发规则数添加标签
        StrategyDecisionResult currentResult = resultHolder.get();
        if (currentResult != null && currentResult.getFinalAction() != null) {
            switch (currentResult.getFinalAction()) {
                case "BAN" -> tags.add("高危账号");
                case "BLOCK" -> tags.add("风险操作");
                case "REVIEW" -> tags.add("待审核");
                case "WARN" -> tags.add("轻微异常");
            }
        }

        result.setRiskTags(tags);
    }
}
