package com.beijixing.risk.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 风控报告VO - 返回风控分析报告数据
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
public class RiskReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报告类型
     */
    private String reportType;

    /**
     * 生成时间
     */
    private String generateTime;

    /**
     * 时间范围
     */
    private String timeRange;

    /**
     * ===== 概览统计 =====
     */
    private SummaryStats summary;

    /**
     * ===== 趋势数据 =====
     */
    private List<TrendData> trendData;

    /**
     * ===== 风险分布 =====
     */
    private Map<String, Integer> riskDistribution;

    /**
     * ===== 风险类型分布 =====
     */
    private Map<String, Integer> riskTypeDistribution;

    /**
     * ===== Top风险账号 =====
     */
    private List<AccountRiskVO> topRiskAccounts;

    /**
     * ===== 触发最多的规则 =====
     */
    private List<RuleTriggerVO> topRules;

    /**
     * ===== 预警列表 =====
     */
    private List<AlertVO> alerts;

    /**
     * 概览统计内部类
     */
    @Data
    public static class SummaryStats implements Serializable {
        private Integer totalChecks;
        private Integer totalPassed;
        private Integer totalBlocked;
        private Integer totalWarned;
        private Integer passRate;
        private Integer blockRate;
        private Integer avgRiskScore;
        private Integer maxRiskScore;
        private Integer minRiskScore;
    }

    /**
     * 趋势数据内部类
     */
    @Data
    public static class TrendData implements Serializable {
        private String date;
        private Integer checkCount;
        private Integer blockCount;
        private Integer passCount;
        private Integer avgScore;
        private Integer alertCount;
    }

    /**
     * 账号风险VO
     */
    @Data
    public static class AccountRiskVO implements Serializable {
        private Long accountId;
        private String accountName;
        private String platform;
        private Integer riskScore;
        private String riskLevel;
        private Integer blockCount;
        private Integer warnCount;
    }

    /**
     * 规则触发VO
     */
    @Data
    public static class RuleTriggerVO implements Serializable {
        private Long ruleId;
        private String ruleName;
        private String ruleCode;
        private Integer triggerCount;
        private Integer avgDeductScore;
    }

    /**
     * 预警VO
     */
    @Data
    public static class AlertVO implements Serializable {
        private Long alertId;
        private String alertType;
        private String alertLevel;
        private String alertContent;
        private String status;
        private String createTime;
    }
}
