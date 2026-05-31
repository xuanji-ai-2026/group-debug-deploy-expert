package com.beijixing.risk.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控报告请求DTO - 用于生成风控分析报告
 *
 * @author 林超 (EMP-SEC-001)
 */
@Data
public class RiskReportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 账号ID（可选，用于查看单个账号的报告）
     */
    private Long accountId;

    /**
     * 报告类型：summary/detailed/trend/rule
     */
    private String reportType;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 风险类型过滤
     */
    private String riskType;

    /**
     * 风险等级过滤
     */
    private String riskLevel;

    /**
     * 操作类型过滤
     */
    private String operationType;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;
}
