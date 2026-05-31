package com.beijixing.bxlead.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商机漏斗分析VO
 * @author 朱怡
 * @since 1.0.0
 */
@Data
public class LeadFunnelVO {
    
    /** 阶段编码 */
    private String stageCode;
    
    /** 阶段名称 */
    private String stageName;
    
    /** 商机数量 */
    private Integer leadCount;
    
    /** 预计金额 */
    private BigDecimal totalAmount;
    
    /** 上一阶段转化率 */
    private BigDecimal conversionRate;
    
    /** 总转化率（相对于新建） */
    private BigDecimal totalConversionRate;
    
    /** 平均停留天数 */
    private Double avgStayDays;
}