package com.beijixing.risk.service;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.dto.RiskReportRequest;
import com.beijixing.risk.vo.RiskDecisionVO;
import com.beijixing.risk.vo.RiskReportVO;

import java.util.List;

/**
 * 风控服务接口
 *
 * @author 林超 (EMP-SEC-001)
 */
public interface RiskService {

    /**
     * 执行风控检查
     *
     * @param request 风控检查请求
     * @return 风控决策结果
     */
    RiskDecisionVO checkRisk(RiskCheckRequest request);

    /**
     * 快速风控检查（仅规则检查）
     *
     * @param request 风控检查请求
     * @return 快速检查结果
     */
    RiskDecisionVO quickCheck(RiskCheckRequest request);

    /**
     * 生成风控报告
     *
     * @param request 报告请求参数
     * @return 风控报告
     */
    RiskReportVO generateReport(RiskReportRequest request);

    /**
     * 获取风控记录列表
     *
     * @param tenantId 租户ID
     * @param accountId 账号ID（可选）
     * @param operationType 操作类型（可选）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 风控记录列表
     */
    List<RiskDecisionVO> getRiskRecords(Long tenantId, Long accountId, String operationType,
                                       Integer pageNum, Integer pageSize);

    /**
     * 获取账号风险评分
     *
     * @param accountId 账号ID
     * @return 当前评分
     */
    Integer getAccountRiskScore(Long accountId);

    /**
     * 批量风控检查
     *
     * @param requests 批量请求
     * @return 批量结果
     */
    List<RiskDecisionVO> batchCheck(List<RiskCheckRequest> requests);

    /**
     * 获取未处理预警数量
     *
     * @param tenantId 租户ID
     * @return 预警数量
     */
    Long getUnhandledAlertCount(Long tenantId);
}
