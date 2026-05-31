package com.beijixing.billing.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 账户信息DTO
 * BL-001: 积分账户管理
 */
@Data
public class CreditAccountDTO {
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long balance;
    private Long frozenAmount;
    private Long totalRecharge;
    private Long totalConsumption;
    private Integer status;
    private LocalDateTime lastTransactionTime;
    private LocalDateTime createTime;
}
