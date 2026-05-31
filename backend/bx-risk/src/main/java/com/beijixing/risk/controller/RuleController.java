package com.beijixing.risk.controller;

import com.beijixing.common.core.Result;
import com.beijixing.risk.entity.RiskRule;
import com.beijixing.risk.entity.RiskStrategy;
import com.beijixing.risk.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则管理控制器 - 提供规则和策略的管理接口
 *
 * @author 林超 (EMP-SEC-001)
 */
@Slf4j
@RestController
@RequestMapping("/risk/rule")
public class RuleController {

    @Autowired
    private RuleService ruleService;

    // ==================== 规则管理接口 ====================

    /**
     * RC-007: 获取所有启用的风控规则
     *
     * @param operationType 操作类型过滤（可选）
     * @return 规则列表
     */
    @GetMapping("/list")
    public Result<List<RiskRule>> getRules(
            @RequestParam(required = false) String operationType) {
        List<RiskRule> rules;
        if (operationType != null && !operationType.isEmpty()) {
            rules = ruleService.getRulesByType(operationType);
        } else {
            rules = ruleService.getAllActiveRules();
        }
        return Result.success(rules);
    }

    /**
     * RC-008: 获取规则详情
     *
     * @param ruleId 规则ID
     * @return 规则详情
     */
    @GetMapping("/{ruleId}")
    public Result<RiskRule> getRuleById(@PathVariable Long ruleId) {
        RiskRule rule = ruleService.getRuleById(ruleId);
        return Result.success(rule);
    }

    /**
     * RC-009: 创建风控规则
     *
     * @param rule 规则信息
     * @return 创建后的规则
     */
    @PostMapping
    public Result<RiskRule> createRule(@RequestBody RiskRule rule) {
        log.info("创建风控规则: ruleName={}, ruleType={}", rule.getRuleName(), rule.getRuleType());
        RiskRule created = ruleService.createRule(rule);
        return Result.success(created);
    }

    /**
     * RC-010: 更新风控规则
     *
     * @param ruleId 规则ID
     * @param rule 规则信息
     * @return 更新后的规则
     */
    @PutMapping("/{ruleId}")
    public Result<RiskRule> updateRule(@PathVariable Long ruleId, @RequestBody RiskRule rule) {
        rule.setId(ruleId);
        log.info("更新风控规则: ruleId={}", ruleId);
        RiskRule updated = ruleService.updateRule(rule);
        return Result.success(updated);
    }

    /**
     * RC-011: 删除风控规则
     *
     * @param ruleId 规则ID
     * @return 操作结果
     */
    @DeleteMapping("/{ruleId}")
    public Result<Void> deleteRule(@PathVariable Long ruleId) {
        log.info("删除风控规则: ruleId={}", ruleId);
        ruleService.deleteRule(ruleId);
        return Result.success(null);
    }

    /**
     * RC-012: 启用/禁用风控规则
     *
     * @param ruleId 规则ID
     * @param status 状态：1-启用，0-禁用
     * @return 操作结果
     */
    @PatchMapping("/{ruleId}/status")
    public Result<Void> toggleRuleStatus(@PathVariable Long ruleId, @RequestParam Integer status) {
        log.info("切换规则状态: ruleId={}, status={}", ruleId, status);
        ruleService.toggleRuleStatus(ruleId, status);
        return Result.success(null);
    }

    /**
     * RC-013: 重载规则缓存
     *
     * @return 操作结果
     */
    @PostMapping("/reload")
    public Result<Void> reloadRules() {
        log.info("重载规则缓存");
        ruleService.reloadRules();
        return Result.success(null);
    }

    // ==================== 策略管理接口 ====================

    /**
     * 获取所有启用的风控策略
     *
     * @param strategyType 策略类型过滤（可选）
     * @return 策略列表
     */
    @GetMapping("/strategy/list")
    public Result<List<RiskStrategy>> getStrategies(
            @RequestParam(required = false) String strategyType) {
        List<RiskStrategy> strategies;
        if (strategyType != null && !strategyType.isEmpty()) {
            strategies = ruleService.getStrategiesByType(strategyType);
        } else {
            strategies = ruleService.getAllActiveStrategies();
        }
        return Result.success(strategies);
    }

    /**
     * 获取策略详情
     *
     * @param strategyId 策略ID
     * @return 策略详情
     */
    @GetMapping("/strategy/{strategyId}")
    public Result<RiskStrategy> getStrategyById(@PathVariable Long strategyId) {
        RiskStrategy strategy = ruleService.getStrategyById(strategyId);
        return Result.success(strategy);
    }

    /**
     * 创建风控策略
     *
     * @param strategy 策略信息
     * @return 创建后的策略
     */
    @PostMapping("/strategy")
    public Result<RiskStrategy> createStrategy(@RequestBody RiskStrategy strategy) {
        log.info("创建风控策略: strategyName={}", strategy.getStrategyName());
        RiskStrategy created = ruleService.createStrategy(strategy);
        return Result.success(created);
    }

    /**
     * 更新风控策略
     *
     * @param strategyId 策略ID
     * @param strategy 策略信息
     * @return 更新后的策略
     */
    @PutMapping("/strategy/{strategyId}")
    public Result<RiskStrategy> updateStrategy(
            @PathVariable Long strategyId, @RequestBody RiskStrategy strategy) {
        strategy.setId(strategyId);
        log.info("更新风控策略: strategyId={}", strategyId);
        RiskStrategy updated = ruleService.updateStrategy(strategy);
        return Result.success(updated);
    }

    /**
     * 删除风控策略
     *
     * @param strategyId 策略ID
     * @return 操作结果
     */
    @DeleteMapping("/strategy/{strategyId}")
    public Result<Void> deleteStrategy(@PathVariable Long strategyId) {
        log.info("删除风控策略: strategyId={}", strategyId);
        ruleService.deleteStrategy(strategyId);
        return Result.success(null);
    }

    /**
     * 启用/禁用风控策略
     *
     * @param strategyId 策略ID
     * @param status 状态：1-启用，0-禁用
     * @return 操作结果
     */
    @PatchMapping("/strategy/{strategyId}/status")
    public Result<Void> toggleStrategyStatus(
            @PathVariable Long strategyId, @RequestParam Integer status) {
        log.info("切换策略状态: strategyId={}, status={}", strategyId, status);
        ruleService.toggleStrategyStatus(strategyId, status);
        return Result.success(null);
    }

    /**
     * 重载策略缓存
     *
     * @return 操作结果
     */
    @PostMapping("/strategy/reload")
    public Result<Void> reloadStrategies() {
        log.info("重载策略缓存");
        ruleService.reloadStrategies();
        return Result.success(null);
    }
}
