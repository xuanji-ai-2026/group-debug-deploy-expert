package com.beijixing.data.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报表 VO
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Data
public class ReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期
     */
    private String date;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 指标值
     */
    private String metricValue;

    /**
     * 指标类型
     */
    private String metricType;

    /**
     * 数值
     */
    private Double numericValue;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
