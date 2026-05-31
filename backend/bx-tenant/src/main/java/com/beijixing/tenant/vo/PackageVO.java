package com.beijixing.tenant.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐视图对象
 *
 * @author bx-tenant
 */
@Data
public class PackageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户套餐记录ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 套餐ID
     */
    private Long packageId;

    /**
     * 套餐名称
     */
    private String packageName;

    /**
     * 套餐类型
     */
    private String packageType;

    /**
     * 套餐类型描述
     */
    private String packageTypeDesc;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveTime;

    /**
     * 失效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /**
     * 购买方式：1-在线购买，2-线下购买，3-活动赠送
     */
    private Integer purchaseType;

    /**
     * 购买方式描述
     */
    private String purchaseTypeDesc;

    /**
     * 审核状态：0-待审核，1-已生效，2-已过期，3-已退订
     */
    private Integer auditStatus;

    /**
     * 审核状态描述
     */
    private String auditStatusDesc;

    /**
     * 审核备注
     */
    private String auditRemark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 是否当前生效套餐
     */
    private Boolean isCurrent;
}
