package com.beijixing.billing.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 套餐购买DTO
 * BL-005: 套餐购买
 */
@Data
public class PackagePurchaseDTO {
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long orderId;
    private String packageType;
    private String packageName;
    private Long tokenQuota;
    private Long usedTokens;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer status;
    private LocalDateTime createTime;
}
