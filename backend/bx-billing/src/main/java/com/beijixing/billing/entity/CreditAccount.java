package com.beijixing.billing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 积分账户实体
 * BL-001: 积分账户管理
 */
@Data
@TableName("bx_credit_account")
public class CreditAccount {
    
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
     * 当前余额（分）
     */
    private Long balance;
    
    /**
     * 冻结金额（分）
     */
    private Long frozenAmount;
    
    /**
     * 累计充值金额（分）
     */
    private Long totalRecharge;
    
    /**
     * 累计消费金额（分）
     */
    private Long totalConsumption;
    
    /**
     * 账户状态: 0-禁用, 1-正常, 2-冻结
     */
    private Integer status;
    
    /**
     * 最后交易时间
     */
    private LocalDateTime lastTransactionTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
