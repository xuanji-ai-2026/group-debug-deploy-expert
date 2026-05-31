package com.beijixing.tenant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 邀请奖励实体类
 * 对应数据库表: invite_reward
 * 邀请人获得被邀请人充值金额10%奖励，可提现或折算积分
 *
 * @author bx-tenant
 */
@Data
@TableName("invite_reward")
public class InviteReward implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 邀请人ID（租户ID）
     */
    private Long inviterId;

    /**
     * 被邀请人ID（租户ID）
     */
    private Long inviteeId;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 奖励金额（充值金额 × 10%）
     */
    private BigDecimal rewardAmount;

    /**
     * 奖励类型: CASH-现金, POINTS-积分
     */
    private String rewardType;

    /**
     * 奖励状态: PENDING-待发放, APPROVED-已审批, PAID-已发放
     */
    private String status;

    /**
     * 删除标记: 0-未删除, 1-已删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 奖励类型常量 ====================

    public static final String REWARD_TYPE_CASH = "CASH";
    public static final String REWARD_TYPE_POINTS = "POINTS";

    // ==================== 状态常量 ====================

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PAID = "PAID";

    /**
     * 奖励比例：10%
     */
    public static final BigDecimal REWARD_RATE = new BigDecimal("0.10");
}
