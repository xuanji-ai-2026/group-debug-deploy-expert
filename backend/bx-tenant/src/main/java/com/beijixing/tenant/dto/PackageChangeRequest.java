package com.beijixing.tenant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 套餐购买/变更请求DTO
 * 用于套餐管理（TM-005）
 *
 * @author bx-tenant
 */
@Data
public class PackageChangeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /**
     * 目标套餐ID
     */
    @NotNull(message = "套餐ID不能为空")
    private Long packageId;

    /**
     * 购买方式：1-在线购买，2-线下购买，3-活动赠送
     */
    private Integer purchaseType;

    /**
     * 关联订单ID（在线购买时）
     */
    private Long orderId;

    /**
     * 操作人ID
     */
    private Long operatorId;
}
