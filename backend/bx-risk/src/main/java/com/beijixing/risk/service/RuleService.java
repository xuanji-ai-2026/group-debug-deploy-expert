package com.beijixing.risk.service;

import com.beijixing.risk.entity.RiskRule;
import com.beijixing.risk.entity.RiskStrategy;

import java.util.List;

/**
 * 规则管理服务接口
 *
 * @author 林超 (EMP-SEC-001)
 */
public interface RuleService {

    // ==================== 规则管理 ====================

    /**
     * 获取所有启用的规则
     */
    List<RiskRule> getAllActiveRules();

    /**
     * 根据操作类型获取规则
     */
    List<RiskRule> getRulesByType(String operationType);

    /**
     * 获取规则详情
     */
    RiskRule getRuleById(Long ruleId);

    /**
     * 创建规则
     */
    RiskRule createRule(RiskRule rule);

    /**
     * 更新规则
     */
    RiskRule updateRule(RiskRule rule);

    /**
     * 删除规则
     */
    void deleteRule(Long ruleId);

    /**
     * 启用/禁用规则
     */
    void toggleRuleStatus(Long ruleId, Integer status);

    /**
     * 重载规则缓存
     */
    void reloadRules();

    // ==================== 策略管理 ====================

    /**
     * 获取所有启用的策略
     */
    List<RiskStrategy> getAllActiveStrategies();

    /**
     * 根据类型获取策略
     */
    List<RiskStrategy> getStrategiesByType(String strategyType);

    /**
     * 获取策略详情
     */
    RiskStrategy getStrategyById(Long strategyId);

    /**
     * 创建策略
     */
    RiskStrategy createStrategy(RiskStrategy strategy);

    /**
     * 更新策略
     */
    RiskStrategy updateStrategy(RiskStrategy strategy);

    /**
     * 删除策略
     */
    void deleteStrategy(Long strategyId);

    /**
     * 启用/禁用策略
     */
    void toggleStrategyStatus(Long strategyId, Integer status);

    /**
     * 重载策略缓存
     */
    void reloadStrategies();
}
