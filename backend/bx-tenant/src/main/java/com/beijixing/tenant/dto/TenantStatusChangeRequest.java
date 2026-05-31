package com.beijixing.tenant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 租户状态变更请求DTO
 * 用于租户状态管理（TM-004）
 *
 * @author bx-tenant
 */
@Data
public class TenantStatusChangeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标状态：1-正常，2-禁用，3-注销
     */
    @NotNull(message = "目标状态不能为空")
    private Integer targetStatus;

    /**
     * 操作原因
     */
    private String reason;

    /**
     * 操作人ID
     */
    private Long operatorId;
}
