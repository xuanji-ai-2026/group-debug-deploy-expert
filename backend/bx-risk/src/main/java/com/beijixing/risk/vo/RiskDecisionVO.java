package com.beijixing.risk.vo;

import com.beijixing.risk.enums.RiskAction;
import com.beijixing.risk.enums.RiskLevel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 风控决策VO - 返回风控检查的决策结果
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
public class RiskDecisionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 执行动作
     */
    private String action;

    /**
     * 动作描述
     */
    private String actionDesc;

    /**
     * 风险评分（0-100）
     */
    private Integer riskScore;

    /**
     * 风险等级
     */
    private String riskLevel;

    /**
     * 风险类型
     */
    private String riskType;

    /**
     * 决策ID（关联风控记录）
     */
    private Long recordId;

    /**
     * 触发规则列表
     */
    private List<TriggeredRuleVO> triggeredRules;

    /**
     * 子维度评分
     */
    private Map<String, Integer> subScores;

    /**
     * 风险标签列表
     */
    private List<String> riskTags;

    /**
     * 是否需要人工审核
     */
    private Boolean needReview;

    /**
     * 处理建议
     */
    private String suggestion;

    /**
     * 错误信息（如有）
     */
    private String errorMessage;

    /**
     * 决策耗时（毫秒）
     */
    private Long decisionTimeMs;

    /**
     * 决策时间
     */
    private String decisionTime;

    /**
     * 触发规则VO
     */
    @Data
    public static class TriggeredRuleVO implements Serializable {
        private Long ruleId;
        private String ruleName;
        private String ruleCode;
        private String riskType;
        private Integer deductScore;
        private String triggerReason;
    }

    /**
     * 构建通过结果
     */
    public static RiskDecisionVO pass(int score, String riskType) {
        RiskDecisionVO vo = new RiskDecisionVO();
        vo.setPassed(true);
        vo.setAction(RiskAction.PASS.name());
        vo.setActionDesc(RiskAction.PASS.getDescription());
        vo.setRiskScore(score);
        vo.setRiskLevel(RiskLevel.fromScore(score).name());
        vo.setRiskType(riskType);
        return vo;
    }

    /**
     * 构建拒绝结果
     */
    public static RiskDecisionVO block(int score, String reason) {
        RiskDecisionVO vo = new RiskDecisionVO();
        vo.setPassed(false);
        vo.setAction(RiskAction.BLOCK.name());
        vo.setActionDesc(RiskAction.BLOCK.getDescription());
        vo.setRiskScore(score);
        vo.setRiskLevel(RiskLevel.fromScore(score).name());
        vo.setSuggestion(reason);
        return vo;
    }

    /**
     * 构建警告结果
     */
    public static RiskDecisionVO warn(int score, String suggestion) {
        RiskDecisionVO vo = new RiskDecisionVO();
        vo.setPassed(true);
        vo.setAction(RiskAction.WARN.name());
        vo.setActionDesc(RiskAction.WARN.getDescription());
        vo.setRiskScore(score);
        vo.setRiskLevel(RiskLevel.fromScore(score).name());
        vo.setSuggestion(suggestion);
        return vo;
    }
}
