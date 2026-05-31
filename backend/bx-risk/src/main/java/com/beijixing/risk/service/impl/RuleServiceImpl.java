package com.beijixing.risk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.risk.engine.RuleEngine;
import com.beijixing.risk.engine.StrategyEngine;
import com.beijixing.risk.entity.RiskRule;
import com.beijixing.risk.entity.RiskStrategy;
import com.beijixing.risk.exception.RiskException;
import com.beijixing.risk.repository.RiskStrategyRepository;
import com.beijixing.risk.repository.mapper.RiskRuleMapper;
import com.beijixing.risk.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 规则管理服务实现
 *
 * @author 林超 (EMP-SEC-001)
 */
@Slf4j
@Service
public class RuleServiceImpl extends ServiceImpl<RiskRuleMapper, RiskRule> implements RuleService {

    @Autowired
    @Lazy
    private RuleEngine ruleEngine;

    @Autowired
    @Lazy
    private StrategyEngine strategyEngine;

    @Autowired
    private RiskStrategyRepository strategyRepository;

    // ==================== 规则管理 ====================

    @Override
    public List<RiskRule> getAllActiveRules() {
        return this.list(
            new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getStatus, 1)
                .orderByAsc(RiskRule::getPriority)
        );
    }

    @Override
    public List<RiskRule> getRulesByType(String operationType) {
        return this.list(
            new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getStatus, 1)
                .eq(StringUtils.hasText(operationType), RiskRule::getRuleType, operationType)
                .orderByAsc(RiskRule::getPriority)
        );
    }

    @Override
    public RiskRule getRuleById(Long ruleId) {
        RiskRule rule = this.getById(ruleId);
        if (rule == null) {
            throw RiskException.ruleNotFound(ruleId);
        }
        return rule;
    }

    @Override
    @Transactional
    public RiskRule createRule(RiskRule rule) {
        LambdaQueryWrapper<RiskRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRule::getRuleCode, rule.getRuleCode());
        if (this.count(wrapper) > 0) {
            throw new RiskException("RULE_CODE_EXISTS", "规则编码已存在: " + rule.getRuleCode());
        }

        rule.setStatus(1);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setDeleted(0);

        this.save(rule);
        log.info("创建风控规则: ruleId={}, ruleName={}", rule.getId(), rule.getRuleName());

        ruleEngine.reloadRules();
        return rule;
    }

    @Override
    @Transactional
    public RiskRule updateRule(RiskRule rule) {
        RiskRule existing = getRuleById(rule.getId());
        if (existing.getBuiltIn() != null && existing.getBuiltIn() == 1) {
            if (!Objects.equals(existing.getRuleConfig(), rule.getRuleConfig())) {
                throw new RiskException("BUILT_IN_RULE_PROTECTED", "内置规则不允许修改配置");
            }
        }
        rule.setUpdateTime(LocalDateTime.now());
        this.updateById(rule);
        log.info("更新风控规则: ruleId={}", rule.getId());
        ruleEngine.reloadRules();
        return rule;
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId) {
        RiskRule rule = getRuleById(ruleId);
        if (rule.getBuiltIn() != null && rule.getBuiltIn() == 1) {
            throw new RiskException("BUILT_IN_RULE_PROTECTED", "内置规则不允许删除");
        }
        this.removeById(ruleId);
        log.info("删除风控规则: ruleId={}", ruleId);
        ruleEngine.reloadRules();
    }

    @Override
    @Transactional
    public void toggleRuleStatus(Long ruleId, Integer status) {
        RiskRule rule = getRuleById(ruleId);
        rule.setStatus(status);
        rule.setUpdateTime(LocalDateTime.now());
        this.updateById(rule);
        log.info("切换规则状态: ruleId={}, status={}", ruleId, status);
        ruleEngine.reloadRules();
    }

    @Override
    public void reloadRules() {
        ruleEngine.reloadRules();
    }

    // ==================== 策略管理 ====================

    @Override
    public List<RiskStrategy> getAllActiveStrategies() {
        return strategyRepository.list(
            new LambdaQueryWrapper<RiskStrategy>()
                .eq(RiskStrategy::getStatus, 1)
                .orderByAsc(RiskStrategy::getPriority)
        );
    }

    @Override
    public List<RiskStrategy> getStrategiesByType(String strategyType) {
        LambdaQueryWrapper<RiskStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskStrategy::getStatus, 1)
               .eq(StringUtils.hasText(strategyType), RiskStrategy::getStrategyType, strategyType)
               .orderByAsc(RiskStrategy::getPriority);
        return strategyRepository.list(wrapper);
    }

    @Override
    public RiskStrategy getStrategyById(Long strategyId) {
        RiskStrategy strategy = strategyRepository.getById(strategyId);
        if (strategy == null) {
            throw RiskException.strategyNotFound(String.valueOf(strategyId));
        }
        return strategy;
    }

    @Override
    @Transactional
    public RiskStrategy createStrategy(RiskStrategy strategy) {
        LambdaQueryWrapper<RiskStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskStrategy::getStrategyCode, strategy.getStrategyCode());
        if (strategyRepository.count(wrapper) > 0) {
            throw new RiskException("STRATEGY_CODE_EXISTS", "策略编码已存在: " + strategy.getStrategyCode());
        }

        strategy.setStatus(1);
        strategy.setCreateTime(LocalDateTime.now());
        strategy.setUpdateTime(LocalDateTime.now());
        strategy.setDeleted(0);

        strategyRepository.save(strategy);
        log.info("创建风控策略: strategyId={}, strategyName={}", strategy.getId(), strategy.getStrategyName());
        strategyEngine.reloadStrategies();
        return strategy;
    }

    @Override
    @Transactional
    public RiskStrategy updateStrategy(RiskStrategy strategy) {
        strategy.setUpdateTime(LocalDateTime.now());
        strategyRepository.updateById(strategy);
        log.info("更新风控策略: strategyId={}", strategy.getId());
        strategyEngine.reloadStrategies();
        return strategy;
    }

    @Override
    @Transactional
    public void deleteStrategy(Long strategyId) {
        strategyRepository.removeById(strategyId);
        log.info("删除风控策略: strategyId={}", strategyId);
        strategyEngine.reloadStrategies();
    }

    @Override
    @Transactional
    public void toggleStrategyStatus(Long strategyId, Integer status) {
        RiskStrategy strategy = getStrategyById(strategyId);
        strategy.setStatus(status);
        strategy.setUpdateTime(LocalDateTime.now());
        strategyRepository.updateById(strategy);
        log.info("切换策略状态: strategyId={}, status={}", strategyId, status);
        strategyEngine.reloadStrategies();
    }

    @Override
    public void reloadStrategies() {
        strategyEngine.reloadStrategies();
    }
}
