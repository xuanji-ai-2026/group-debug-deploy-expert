package com.beijixing.data.service;

import com.beijixing.data.vo.TrendChartVO;

import java.util.List;

/**
 * 趋势分析服务接口
 * 
 * @author 陈工（EMP-DATA-001）
 */
public interface TrendService {

    /**
     * DA-006: 趋势分析
     * 
     * @param tenantId 租户ID
     * @param type 趋势类型（OPERATION/LEAD/ACCOUNT/BILLING）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 趋势数据列表
     */
    List<TrendChartVO> getTrend(Long tenantId, String type, String startDate, String endDate);

    /**
     * 获取同比数据
     * 
     * @param tenantId 租户ID
     * @param type 趋势类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 同比趋势数据
     */
    List<TrendChartVO> getYearOverYearTrend(Long tenantId, String type, String startDate, String endDate);

    /**
     * 获取环比数据
     * 
     * @param tenantId 租户ID
     * @param type 趋势类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 环比趋势数据
     */
    List<TrendChartVO> getMonthOverMonthTrend(Long tenantId, String type, String startDate, String endDate);
}
