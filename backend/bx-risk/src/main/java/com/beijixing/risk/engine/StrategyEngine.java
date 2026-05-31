package com.beijixing.risk.engine;

import com.beijixing.risk.entity.RiskStrategy;

import java.util.List;

/**
 * 策略引擎接口 - 负责风控策略的加载、匹配和执行
 *
 * @author 林超 (EMP-SEC-001)
 */
public interface StrategyEngine {

    /**
     * 根据类型获取适用策略
     *
     * @param strategyType 策略类型
     * @param tenantId     租户ID
     * @return 适用策略列表
     */
    List<RiskStrategy> getApplicableStrategies(String strategyType, Long tenantId);

    /**
     * 执行策略决策
     *
     * @param context 决策上下文
     * @return 策略决策结果
     */
    StrategyDecisionResult executeStrategies(DecisionContext context);

    /**
     * 加载所有策略
     */
    void reloadStrategies();

    /**
     * 获取策略执行结果
     */
    StrategyDecisionResult getStrategyResult();

    /**
     * 决策上下文
     */
    class DecisionContext {
        /**
         * 租户ID
         */
        private Long tenantId;
        /**
         * 账号ID
         */
        private Long accountId;
        /**
         * 操作类型
         */
        private String operationType;
        /**
         * 策略类型
         */
        private String strategyType;
        /**
         * 当前风险评分
         */
        private int currentScore;
        /**
         * 触发的规则数
         */
        private int triggeredRuleCount;

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public String getStrategyType() {
            return strategyType;
        }

        public void setStrategyType(String strategyType) {
            this.strategyType = strategyType;
        }

        public int getCurrentScore() {
            return currentScore;
        }

        public void setCurrentScore(int currentScore) {
            this.currentScore = currentScore;
        }

        public int getTriggeredRuleCount() {
            return triggeredRuleCount;
        }

        public void setTriggeredRuleCount(int triggeredRuleCount) {
            this.triggeredRuleCount = triggeredRuleCount;
        }
    }

    /**
     * 策略决策结果
     */
    class StrategyDecisionResult {
        /**
         * 最终执行动作
         */
        private String finalAction;
        /**
         * 决策建议
         */
        private String suggestion;
        /**
         * 是否需要人工审核
         */
        private boolean needReview;
        /**
         * 匹配的策略名称
         */
        private String matchedStrategy;
        /**
         * 风险标签
         */
        private List<String> riskTags;

        public String getFinalAction() {
            return finalAction;
        }

        public void setFinalAction(String finalAction) {
            this.finalAction = finalAction;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public boolean isNeedReview() {
            return needReview;
        }

        public void setNeedReview(boolean needReview) {
            this.needReview = needReview;
        }

        public String getMatchedStrategy() {
            return matchedStrategy;
        }

        public void setMatchedStrategy(String matchedStrategy) {
            this.matchedStrategy = matchedStrategy;
        }

        public List<String> getRiskTags() {
            return riskTags;
        }

        public void setRiskTags(List<String> riskTags) {
            this.riskTags = riskTags;
        }
    }
}
