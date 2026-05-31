package com.beijixing.billing.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 充值请求DTO
 * BL-002: 在线充值
 */
@Data
public class RechargeRequestDTO {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "充值金额不能为空")
    @Positive(message = "充值金额必须大于0")
    private Long amount;
    
    @NotNull(message = "支付方式不能为空")
    private Integer payType;
    
    private String description;
}
