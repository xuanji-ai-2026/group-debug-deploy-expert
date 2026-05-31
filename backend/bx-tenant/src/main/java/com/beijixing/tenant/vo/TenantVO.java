package com.beijixing.tenant.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租户视图对象
 *
 * @author bx-tenant
 */
@Data
public class TenantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    private Long id;

    /**
     * 租户编码
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
     * 联系电话（脱敏显示）
     */
    private String contactPhone;

    /**
     * 联系邮箱（脱敏显示）
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
     * 状态：0-待审核，1-正常，2-禁用，3-注销
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 风控等级：1-低，2-中，3-高
     */
    private Integer riskLevel;

    /**
     * 风控等级描述
     */
    private String riskLevelDesc;

    /**
     * 套餐类型
     */
    private String packageType;

    /**
     * 套餐类型描述
     */
    private String packageTypeDesc;

    /**
     * 套餐过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
