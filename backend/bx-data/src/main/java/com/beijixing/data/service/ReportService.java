package com.beijixing.data.service;

import com.beijixing.data.vo.ReportVO;

import java.util.List;

/**
 * 报表服务接口
 * 
 * @author 陈工（EMP-DATA-001）
 */
public interface ReportService {

    /**
     * DA-005: 导出Excel报表
     * 
     * @param tenantId 租户ID
     * @param reportType 报表类型（OPERATION/LEAD/ACCOUNT/BILLING）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 报表数据列表
     */
    List<ReportVO> exportReport(Long tenantId, String reportType, String startDate, String endDate);

    /**
     * 导出CSV格式报表
     * 
     * @param tenantId 租户ID
     * @param reportType 报表类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return CSV格式数据
     */
    String exportCsvReport(Long tenantId, String reportType, String startDate, String endDate);
}
