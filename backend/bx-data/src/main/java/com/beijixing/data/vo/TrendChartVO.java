package com.beijixing.data.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 趋势图表 VO
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Data
public class TrendChartVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期
     */
    private String date;

    /**
     * 数值
     */
    private BigDecimal value;

    /**
     * 同比变化
     */
    private BigDecimal yoyChange;

    /**
     * 环比变化
     */
    private BigDecimal momChange;

    /**
     * 同比变化率(%)
     */
    private BigDecimal yoyRate;

    /**
     * 环比变化率(%)
     */
    private BigDecimal momRate;

    /**
     * 分类标签
     */
    private String category;
}
