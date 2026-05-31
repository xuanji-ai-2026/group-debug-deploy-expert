package com.beijixing.risk.strategy;

import com.beijixing.risk.dto.RiskCheckRequest;

/**
 * 风控策略接口 - 定义各种风控策略的执行逻辑
 *
 * @author 林超 (EMP-SEC-001)
 */
public interface RiskStrategyHandler {

    /**
     * 获取策略类型
     */
    String getStrategyType();

    /**
     * 判断是否支持当前请求
     */
    boolean supports(RiskCheckRequest request);

    /**
     * 执行风控检查
     *
     * @param request 风控检查请求
     * @return 策略执行结果
     */
    StrategyResult execute(RiskCheckRequest request);

    /**
     * 策略执行结果
     */
    class StrategyResult {
        /**
         * 是否通过
         */
        private boolean passed;
        /**
         * 执行的动作
         */
        private String action;
        /**
         * 风险评分
         */
        private int score;
        /**
         * 扣分值
         */
        private int deductScore;
        /**
         * 风险标签
         */
        private String riskTag;
        /**
         * 详细原因
         */
        private String reason;
        /**
         * 建议
         */
        private String suggestion;

        public static StrategyResult pass() {
            StrategyResult result = new StrategyResult();
            result.setPassed(true);
            result.setAction("PASS");
            result.setScore(100);
            result.setDeductScore(0);
            return result;
        }

        public static StrategyResult block(int score, String reason) {
            StrategyResult result = new StrategyResult();
            result.setPassed(false);
            result.setAction("BLOCK");
            result.setScore(score);
            result.setDeductScore(100 - score);
            result.setReason(reason);
            return result;
        }

        public static StrategyResult warn(int score, String reason, String suggestion) {
            StrategyResult result = new StrategyResult();
            result.setPassed(true);
            result.setAction("WARN");
            result.setScore(score);
            result.setDeductScore(Math.max(0, 100 - score));
            result.setReason(reason);
            result.setSuggestion(suggestion);
            return result;
        }

        public static StrategyResult rateLimit(int score, String reason) {
            StrategyResult result = new StrategyResult();
            result.setPassed(true);
            result.setAction("RATE_LIMIT");
            result.setScore(score);
            result.setDeductScore(Math.max(0, 100 - score));
            result.setReason(reason);
            result.setSuggestion("请降低操作频率");
            return result;
        }

        // Getters and Setters
        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public int getDeductScore() {
            return deductScore;
        }

        public void setDeductScore(int deductScore) {
            this.deductScore = deductScore;
        }

        public String getRiskTag() {
            return riskTag;
        }

        public void setRiskTag(String riskTag) {
            this.riskTag = riskTag;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
}
