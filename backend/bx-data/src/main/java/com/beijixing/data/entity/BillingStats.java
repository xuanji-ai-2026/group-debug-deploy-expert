package com.beijixing.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消费统计实体
 * 
 * @author 陈工（EMP-DATA-001）
 */
@Data
@TableName("bx_billing_stats")
public class BillingStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 统计日期
     */
    private String statDate;

    /**
     * 消耗积分
     */
    private BigDecimal pointsConsumed;

    /**
     * 消耗金额
     */
    private BigDecimal amountConsumed;

    /**
     * 获客成本（每线索成本）
     */
    private BigDecimal costPerLead;

    /**
     * 消费类型（PUBLISH/AI_SERVICE/SMS/PUSH/OTHER）
     */
    private String billingType;

    /**
     * 消费数量
     */
    private Integer quantity;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
