package com.beijixing.billing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消费明细实体
 * BL-004: 消费明细记录（精确到每次AI调用）
 */
@Data
@TableName("bx_consumption_record")
public class ConsumptionRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 关联订单ID
     */
    private Long orderId;
    
    /**
     * AI调用ID（追踪每次调用）
     */
    private String callId;
    
    /**
     * 功能模块
     */
    private String module;
    
    /**
     * 消耗Token数
     */
    private Long tokenCount;
    
    /**
     * 资源使用时长（分钟）
     */
    private Integer resourceUsageMinutes;
    
    /**
     * Token费用（分）
     */
    private Long tokenCost;
    
    /**
     * 资源占用费用（分）
     */
    private Long resourceCost;
    
    /**
     * 总费用（分）
     */
    private Long totalCost;
    
    /**
     * 单价（分/Token）
     */
    private Integer unitPrice;
    
    /**
     * 请求内容摘要
     */
    private String requestSummary;
    
    /**
     * 响应内容摘要
     */
    private String responseSummary;
    
    /**
     * 调用时间
     */
    private LocalDateTime callTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
