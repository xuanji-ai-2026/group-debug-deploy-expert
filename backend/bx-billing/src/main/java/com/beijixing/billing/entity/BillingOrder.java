package com.beijixing.billing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 交易订单实体
 * BL-007: 订单管理
 */
@Data
@TableName("bx_billing_order")
public class BillingOrder {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 订单类型: 1-充值, 2-消费, 3-套餐购买, 4-退款
     */
    private Integer orderType;
    
    /**
     * 订单状态: 0-待支付, 1-已支付, 2-已取消, 3-已退款
     */
    private Integer status;
    
    /**
     * 支付类型: 1-微信, 2-支付宝, 3-余额
     */
    private Integer payType;
    
    /**
     * 订单金额（分）
     */
    private Long amount;
    
    /**
     * 实际支付金额（分）
     */
    private Long actualAmount;
    
    /**
     * 赠送金额（分）
     */
    private Long bonusAmount;
    
    /**
     * 套餐ID（套餐购买时）
     */
    private String packageId;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
    /**
     * 支付流水号
     */
    private String transactionId;
    
    /**
     * 订单描述
     */
    private String description;
    
    /**
     * 过期时间（用于待支付订单）
     */
    private LocalDateTime expireTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
