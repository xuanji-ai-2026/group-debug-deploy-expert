package com.beijixing.billing.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 订单DTO
 * BL-007: 订单管理
 */
@Data
public class BillingOrderDTO {
    private Long id;
    private String orderNo;
    private Long tenantId;
    private Long userId;
    private Integer orderType;
    private Integer status;
    private Integer payType;
    private Long amount;
    private Long actualAmount;
    private Long bonusAmount;
    private String packageId;
    private LocalDateTime payTime;
    private String transactionId;
    private String description;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
