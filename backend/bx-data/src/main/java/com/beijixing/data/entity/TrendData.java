package com.beijixing.data.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 趋势数据实体
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Data
public class TrendData implements Serializable {

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
    private BigDecimal yearOverYearChange;

    /**
     * 环比变化
     */
    private BigDecimal monthOverMonthChange;

    /**
     * 同比变化率(%)
     */
    private BigDecimal yearOverYearRate;

    /**
     * 环比变化率(%)
     */
    private BigDecimal monthOverMonthRate;
}
