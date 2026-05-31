package com.beijixing.tenant.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量审核请求DTO
 * 用于批量审核（TM-003）
 *
 * @author bx-tenant
 */
@Data
public class BatchAuditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID列表
     */
    @NotEmpty(message = "租户ID列表不能为空")
    private List<Long> tenantIds;

    /**
     * 审核是否通过
     */
    private Boolean approved;

    /**
     * 审核备注/理由
     */
    private String reason;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 分配的套餐类型（审核通过时可指定套餐）
     */
    private String packageType;
}
