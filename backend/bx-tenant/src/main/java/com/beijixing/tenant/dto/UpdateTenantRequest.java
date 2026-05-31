package com.beijixing.tenant.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新租户请求DTO
 * 用于租户信息修改
 *
 * @author bx-tenant
 */
@Data
public class UpdateTenantRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户名称（企业名称）
     */
    @Size(max = 100, message = "租户名称不能超过100个字符")
    private String tenantName;

    /**
     * 所属行业
     */
    @Size(max = 50, message = "所属行业不能超过50个字符")
    private String industry;

    /**
     * 联系人姓名
     */
    @Size(max = 50, message = "联系人姓名不能超过50个字符")
    private String contactName;

    /**
     * 联系电话
     */
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    /**
     * 联系邮箱
     */
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String contactEmail;

    /**
     * 营业执照号
     */
    @Size(max = 100, message = "营业执照号不能超过100个字符")
    private String businessLicense;

    /**
     * 营业执照图片地址
     */
    @Size(max = 255, message = "营业执照图片地址不能超过255个字符")
    private String licenseImage;

    /**
     * 风控等级：1-低，2-中，3-高
     */
    private Integer riskLevel;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
