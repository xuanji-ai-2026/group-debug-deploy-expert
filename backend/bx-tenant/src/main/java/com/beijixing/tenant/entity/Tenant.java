package com.beijixing.tenant.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.beijixing.tenant.enums.TenantStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租户实体类
 * 对应数据库表: sys_tenant
 *
 * @author bx-tenant
 */
@Data
@TableName("tenant")
public class Tenant implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户编码（唯一，用于系统间调用）
     */
    private String tenantCode;

    /**
     * 租户名称（企业名称）
     */
    private String tenantName;

    /**
     * 所属行业
     */
    private String industry;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 营业执照号
     */
    private String businessLicense;

    /**
     * 营业执照图片地址
     */
    private String licenseImage;

    /**
     * 租户状态：0-待审核，1-正常，2-禁用，3-注销
     */
    private Integer status;

    /**
     * 风控等级：1-低，2-中，3-高
     */
    private Integer riskLevel;

    /**
     * 套餐类型：basic-基础，advanced-高级，annual-年度，lifetime-终身
     */
    private String packageType;

    /**
     * 套餐过期时间
     */
    private LocalDateTime packageExpireTime;

    /**
     * 积分余额
     */
    private BigDecimal pointBalance;

    /**
     * 累计消费
     */
    private BigDecimal totalConsumption;

    /**
     * 邀请人ID
     */
    private Long inviterId;

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
     * 删除标记：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 获取状态枚举
     *
     * @return TenantStatus
     */
    public TenantStatus getStatusEnum() {
        return TenantStatus.fromCode(this.status);
    }

    /**
     * 是否可用
     *
     * @return true-可用
     */
    public boolean isActive() {
        return TenantStatus.NORMAL.getCode().equals(this.status);
    }
}
