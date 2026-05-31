package com.beijixing.tenant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租户套餐实体类
 * 对应数据库表: tenant_package
 *
 * @author bx-tenant
 */
@Data
@TableName("tenant_package")
public class TenantPackage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 套餐ID（关联billing_package表）
     */
    private Long packageId;

    /**
     * 套餐名称
     */
    private String packageName;

    /**
     * 套餐类型：basic-基础，advanced-高级，annual-年度，lifetime-终身
     */
    private String packageType;

    /**
     * 购买价格
     */
    private BigDecimal price;

    /**
     * 赠送积分
     */
    private BigDecimal pointAmount;

    /**
     * 生效时间
     */
    private LocalDateTime effectiveTime;

    /**
     * 失效时间（NULL表示永久）
     */
    private LocalDateTime expireTime;

    /**
     * 购买方式：1-在线购买，2-线下购买，3-活动赠送
     */
    private Integer purchaseType;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 审核状态：0-待审核，1-已生效，2-已过期，3-已退订
     */
    private Integer auditStatus;

    /**
     * 审核备注
     */
    private String auditRemark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer deleted;

    /**
     * 套餐是否有效
     *
     * @return true-有效
     */
    public boolean isEffective() {
        if (!Integer.valueOf(1).equals(this.auditStatus)) {
            return false;
        }
        if (this.expireTime != null && LocalDateTime.now().isAfter(this.expireTime)) {
            return false;
        }
        return true;
    }
}
