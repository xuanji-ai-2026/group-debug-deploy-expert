package com.beijixing.risk.engine;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.entity.RiskRule;
import com.beijixing.risk.vo.RiskDecisionVO;

import java.util.List;

/**
 * 规则引擎接口 - 负责规则的配置管理和执行
 *
 * @author 林超 (EMP-SEC-001)
 */
public interface RuleEngine {

    /**
     * 执行风控规则检查
     *
     * @param request 风控检查请求
     * @return 规则检查结果（包含触发规则列表和扣分）
     */
    RuleCheckResult checkRules(RiskCheckRequest request);

    /**
     * 根据操作类型获取适用规则
     *
     * @param operationType 操作类型
     * @param platformId   平台ID（可为null）
     * @return 适用规则列表
     */
    List<RiskRule> getApplicableRules(String operationType, Long platformId);

    /**
     * 加载所有启用的规则
     */
    void reloadRules();

    /**
     * 获取规则检查结果
     */
    RuleCheckResult getRuleCheckResult();

    /**
     * 规则检查结果内部类
     */
    class RuleCheckResult {
        /**
         * 是否通过所有规则
         */
        private boolean passed;
        /**
         * 总扣分
         */
        private int totalDeductScore;
        /**
         * 基础分数（100分制）
         */
        private int baseScore;
        /**
         * 最终分数
         */
        private int finalScore;
        /**
         * 触发的规则列表
         */
        private List<RiskDecisionVO.TriggeredRuleVO> triggeredRules;
        /**
         * 主要风险类型
         */
        private String primaryRiskType;

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public int getTotalDeductScore() {
            return totalDeductScore;
        }

        public void setTotalDeductScore(int totalDeductScore) {
            this.totalDeductScore = totalDeductScore;
        }

        public int getBaseScore() {
            return baseScore;
        }

        public void setBaseScore(int baseScore) {
            this.baseScore = baseScore;
        }

        public int getFinalScore() {
            return finalScore;
        }

        public void setFinalScore(int finalScore) {
            this.finalScore = finalScore;
        }

        public List<RiskDecisionVO.TriggeredRuleVO> getTriggeredRules() {
            return triggeredRules;
        }

        public void setTriggeredRules(List<RiskDecisionVO.TriggeredRuleVO> triggeredRules) {
            this.triggeredRules = triggeredRules;
        }

        public String getPrimaryRiskType() {
            return primaryRiskType;
        }

        public void setPrimaryRiskType(String primaryRiskType) {
            this.primaryRiskType = primaryRiskType;
        }
    }
}
