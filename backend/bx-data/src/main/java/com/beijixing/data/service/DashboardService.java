package com.beijixing.data.service;

import com.beijixing.data.vo.DashboardVO;
import com.beijixing.data.vo.TrendChartVO;

import java.util.List;

/**
 * 数据看板服务接口
 * 
 * @author 陈工（EMP-DATA-001）
 */
public interface DashboardService {

    /**
     * DA-001: 运营数据看板
     * 
     * @param tenantId 租户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 运营数据看板
     */
    DashboardVO getOperationDashboard(Long tenantId, String startDate, String endDate);

    /**
     * DA-002: 获客数据看板
     * 
     * @param tenantId 租户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 商机数据看板
     */
    DashboardVO getLeadDashboard(Long tenantId, String startDate, String endDate);

    /**
     * DA-003: 账号数据看板
     * 
     * @param tenantId 租户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 账号数据看板
     */
    DashboardVO getAccountDashboard(Long tenantId, String startDate, String endDate);

    /**
     * DA-004: 消费数据看板
     * 
     * @param tenantId 租户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 消费数据看板
     */
    DashboardVO getBillingDashboard(Long tenantId, String startDate, String endDate);

    /**
     * 获取运营趋势数据
     */
    List<TrendChartVO> getOperationTrend(Long tenantId, String startDate, String endDate);

    /**
     * 获取商机趋势数据
     */
    List<TrendChartVO> getLeadTrend(Long tenantId, String startDate, String endDate);

    /**
     * 获取账号趋势数据
     */
    List<TrendChartVO> getAccountTrend(Long tenantId, String startDate, String endDate);

    /**
     * 获取消费趋势数据
     */
    List<TrendChartVO> getBillingTrend(Long tenantId, String startDate, String endDate);
}
