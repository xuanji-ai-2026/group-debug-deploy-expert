package com.beijixing.tenant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户资源配额实体
 * 对应数据库表: tenant_resource_quota
 * 
 * @author bx-tenant
 */
@Data
@TableName("tenant_resource_quota")
public class TenantResourceQuota implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 资源类型（如 social_account, daily_intercept_task, daily_acquire_task 等）
     */
    private String resourceType;

    /**
     * 配额限制
     */
    private Long quotaLimit;

    /**
     * 已使用量
     */
    private Long usedAmount;

    /**
     * 告警阈值（百分比）
     */
    private Integer alertThreshold;

    /**
     * 重置周期: daily/weekly/monthly/yearly
     */
    private String resetCycle;

    /**
     * 上次重置时间
     */
    private LocalDateTime lastResetTime;

    /**
     * 删除标记
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

    /**
     * 检查配额是否已用完
     */
    public boolean isExceeded() {
        return quotaLimit != null && quotaLimit > 0 && usedAmount != null && usedAmount >= quotaLimit;
    }

    /**
     * 检查是否达到告警阈值
     */
    public boolean isAlert() {
        if (quotaLimit == null || quotaLimit <= 0 || usedAmount == null || alertThreshold == null) {
            return false;
        }
        double usagePercent = (usedAmount.doubleValue() / quotaLimit.doubleValue()) * 100;
        return usagePercent >= alertThreshold;
    }

    /**
     * 剩余可用量
     */
    public long remaining() {
        if (quotaLimit == null || usedAmount == null) return Long.MAX_VALUE;
        return Math.max(0, quotaLimit - usedAmount);
    }
}
