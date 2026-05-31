package com.beijixing.billing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 套餐购买记录实体
 * BL-005: 套餐购买
 */
@Data
@TableName("bx_package_purchase")
public class PackagePurchase {
    
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
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 套餐类型: basic/advanced/annual/lifetime
     */
    private String packageType;
    
    /**
     * 套餐名称
     */
    private String packageName;
    
    /**
     * 套餐包含Token数
     */
    private Long tokenQuota;
    
    /**
     * 已使用Token数
     */
    private Long usedTokens;
    
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;
    
    /**
     * 到期日期（lifetime为null）
     */
    private LocalDate expireDate;
    
    /**
     * 状态: 0-未生效, 1-生效中, 2-已过期, 3-已用完
     */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
