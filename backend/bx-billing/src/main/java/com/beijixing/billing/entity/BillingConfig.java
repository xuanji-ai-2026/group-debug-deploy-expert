package com.beijixing.billing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 扣点标准配置实体
 * BL-010: 扣点标准配置API
 */
@Data
@TableName("bx_billing_config")
public class BillingConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 配置项编码
     */
    private String configCode;
    
    /**
     * 配置项名称
     */
    private String configName;
    
    /**
     * 配置值
     */
    private String configValue;
    
    /**
     * 配置类型: token_price-Token单价, resource_price-资源单价, 
     * package_basic-基础套餐, package_advanced-高级套餐, 
     * package_annual-年度套餐, package_lifetime-终身套餐,
     * recharge_bonus-充值优惠
     */
    private String configType;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 是否启用
     */
    private Integer enabled;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
