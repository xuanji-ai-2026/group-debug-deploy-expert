package com.beijixing.billing.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发票申请DTO
 * BL-008: 发票申请
 */
@Data
public class InvoiceRequestDTO {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "发票类型不能为空")
    private Integer invoiceType;
    
    @NotBlank(message = "发票抬头不能为空")
    private String title;
    
    @NotBlank(message = "纳税人识别号不能为空")
    private String taxNumber;
    
    private String address;
    private String phone;
    private String bankName;
    private String bankAccount;
    
    @NotBlank(message = "接收邮箱不能为空")
    private String receiveEmail;
    
    @NotNull(message = "关联订单不能为空")
    private String orderIds;
}
