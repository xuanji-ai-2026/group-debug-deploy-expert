package com.beijixing.risk.engine.impl;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.engine.DecisionEngine;
import com.beijixing.risk.engine.RuleEngine;
import com.beijixing.risk.engine.ScoreEngine;
import com.beijixing.risk.engine.StrategyEngine;
import com.beijixing.risk.enums.RiskAction;
import com.beijixing.risk.enums.RiskLevel;
import com.beijixing.risk.vo.RiskDecisionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 默认决策引擎实现 - 综合规则、评分、策略引擎做出最终决策
 *
 * @author 林超 (EMP-SEC-001)
 */
@Slf4j
@Component
public class DefaultDecisionEngineImpl implements DecisionEngine {

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private ScoreEngine scoreEngine;

    @Autowired
    private StrategyEngine strategyEngine;

    private long decisionTimestamp;

    @Override
    public RiskDecisionVO makeDecision(RiskCheckRequest request) {
        long startTime = System.currentTimeMillis();
        decisionTimestamp = startTime;

        RiskDecisionVO vo = new RiskDecisionVO();

        try {
            // ========== 1. 规则引擎检查 ==========
            RuleEngine.RuleCheckResult ruleResult = ruleEngine.checkRules(request);
            vo.setTriggeredRules(ruleResult.getTriggeredRules());
            int ruleScore = ruleResult.getFinalScore();
            String primaryRiskType = ruleResult.getPrimaryRiskType();

            // ========== 2. 评分引擎计算评分 ==========
            ScoreEngine.ScoreResult scoreResult = scoreEngine.calculateScore(request);
            int totalScore = scoreResult.getTotalScore();
            vo.setSubScores(scoreResult.getSubScores());

            // 综合规则评分和计算评分（取较低值）
            int finalScore = Math.min(ruleScore, totalScore);

            // ========== 3. 策略引擎匹配策略 ==========
            StrategyEngine.DecisionContext context = new StrategyEngine.DecisionContext();
            context.setTenantId(request.getTenantId());
            context.setAccountId(request.getAccountId());
            context.setOperationType(request.getOperationType());
            context.setStrategyType(determineStrategyType(request.getOperationType()));
            context.setCurrentScore(finalScore);
            context.setTriggeredRuleCount(ruleResult.getTriggeredRules().size());

            StrategyEngine.StrategyDecisionResult strategyResult = strategyEngine.executeStrategies(context);

            // ========== 4. 综合决策 ==========
            vo.setRiskScore(finalScore);
            vo.setRiskLevel(RiskLevel.fromScore(finalScore).name());
            vo.setRiskType(primaryRiskType);
            vo.setSuggestion(strategyResult.getSuggestion());
            vo.setRiskTags(strategyResult.getRiskTags());

            // 确定最终动作（策略引擎结果优先）
            String action = strategyResult.getFinalAction();
            if (action == null || action.equals(RiskAction.PASS.name())) {
                if (finalScore < 20) {
                    action = RiskAction.BAN.name();
                } else if (finalScore < 40) {
                    action = RiskAction.BLOCK.name();
                } else if (finalScore < 60) {
                    action = RiskAction.REVIEW.name();
                } else if (finalScore < 80) {
                    action = RiskAction.WARN.name();
                } else {
                    action = RiskAction.PASS.name();
                }
            }

            vo.setAction(action);
            vo.setActionDesc(RiskAction.valueOf(action).getDescription());
            vo.setPassed(!RiskAction.valueOf(action).isBlocking());
            vo.setNeedReview(strategyResult.isNeedReview());

            // ========== 5. 记录决策耗时 ==========
            long decisionTimeMs = System.currentTimeMillis() - startTime;
            vo.setDecisionTimeMs(decisionTimeMs);
            vo.setDecisionTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            log.info("风控决策完成: tenantId={}, accountId={}, operation={}, score={}, action={}, triggeredRules={}, costMs={}",
                request.getTenantId(), request.getAccountId(), request.getOperationType(),
                finalScore, action, ruleResult.getTriggeredRules().size(), decisionTimeMs);

        } catch (Exception e) {
            log.error("风控决策异常", e);
            // 异常时默认放行但记录日志
            vo.setPassed(true);
            vo.setAction(RiskAction.PASS.name());
            vo.setActionDesc(RiskAction.PASS.getDescription());
            vo.setRiskScore(100);
            vo.setRiskLevel(RiskLevel.LOW.name());
            vo.setErrorMessage("决策引擎异常: " + e.getMessage());
            vo.setDecisionTimeMs(System.currentTimeMillis() - startTime);
            vo.setDecisionTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        return vo;
    }

    @Override
    public RiskDecisionVO quickCheck(RiskCheckRequest request) {
        long startTime = System.currentTimeMillis();

        RuleEngine.RuleCheckResult ruleResult = ruleEngine.checkRules(request);
        int score = ruleResult.getFinalScore();

        RiskDecisionVO vo = new RiskDecisionVO();
        vo.setRiskScore(score);
        vo.setRiskLevel(RiskLevel.fromScore(score).name());
        vo.setTriggeredRules(ruleResult.getTriggeredRules());
        vo.setPassed(score >= 40);
        vo.setAction(score >= 40 ? RiskAction.PASS.name() : RiskAction.BLOCK.name());
        vo.setActionDesc(score >= 40 ? RiskAction.PASS.getDescription() : RiskAction.BLOCK.getDescription());
        vo.setDecisionTimeMs(System.currentTimeMillis() - startTime);
        vo.setDecisionTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return vo;
    }

    @Override
    public long getDecisionTimestamp() {
        return decisionTimestamp;
    }

    @Override
    public boolean needsLogging(RiskCheckRequest request) {
        // 以下情况需要记录：
        // 1. 账户ID存在
        // 2. 非预检模式
        // 3. 包含内容检查
        return request.getAccountId() != null
            && !Boolean.TRUE.equals(request.getDryRun())
            && request.getRequestParams() != null;
    }

    /**
     * 根据操作类型确定策略类型
     */
    private String determineStrategyType(String operationType) {
        return switch (operationType) {
            case "publish", "comment" -> "content";
            case "message", "follow" -> "operation";
            case "login" -> "account";
            case "access" -> "crawler";
            default -> "account";
        };
    }
}
