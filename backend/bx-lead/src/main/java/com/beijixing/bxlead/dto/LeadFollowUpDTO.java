package com.beijixing.bxlead.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 创建跟进记录DTO
 * @author 朱怡
 * @since 1.0.0
 */
@Data
public class LeadFollowUpDTO {
    
    @NotNull(message = "商机ID不能为空")
    private Long leadId;
    
    @NotBlank(message = "跟进方式不能为空")
    private String followType;
    
    @NotBlank(message = "跟进内容不能为空")
    @Size(max = 2000, message = "跟进内容长度不能超过2000")
    private String followContent;
    
    private String followResult;
    
    private LocalDateTime nextFollowTime;
    
    private Boolean nextFollowRemind;
    
    private String customerFeedback;
    
    private String attachments;
}