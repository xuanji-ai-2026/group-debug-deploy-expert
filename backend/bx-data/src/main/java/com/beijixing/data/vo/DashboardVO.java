package com.beijixing.data.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 数据看板 VO
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Data
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总计
     */
    private BigDecimal total;

    /**
     * 今日
     */
    private BigDecimal today;

    /**
     * 昨日
     */
    private BigDecimal yesterday;

    /**
     * 同比变化率(%)
     */
    private BigDecimal yearOverYearRate;

    /**
     * 环比变化率(%)
     */
    private BigDecimal monthOverMonthRate;

    /**
     * 趋势数据列表
     */
    private List<TrendChartVO> trendData;

    /**
     * 附加数据
     */
    private Object extra;
}
