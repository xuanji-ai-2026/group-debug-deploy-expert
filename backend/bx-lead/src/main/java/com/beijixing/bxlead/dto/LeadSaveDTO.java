package com.beijixing.bxlead.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建/更新商机DTO
 * @author 朱怡
 * @since 1.0.0
 */
@Data
public class LeadSaveDTO {
    
    private Long id;
    
    @NotBlank(message = "商机标题不能为空")
    @Size(max = 200, message = "商机标题长度不能超过200")
    private String title;
    
    @NotBlank(message = "商机来源不能为空")
    private String source;
    
    private String channel;
    
    @NotBlank(message = "客户名称不能为空")
    private String customerName;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String customerPhone;
    
    @Email(message = "邮箱格式不正确")
    private String customerEmail;
    
    private String customerCompany;
    
    private String industry;
    
    private String region;
    
    @Size(max = 2000, message = "需求描述长度不能超过2000")
    private String requirementDesc;
    
    private BigDecimal budgetAmount;
    
    private LocalDateTime expectedDealTime;
    
    private String status;
    
    private Integer intentScore;
    
    private String level;
    
    private Long ownerId;
    
    private String ownerName;
    
    private String remark;
}