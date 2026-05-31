package com.beijixing.billing.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Token消耗计算DTO
 * BL-003: Token消耗计算
 */
@Data
public class TokenConsumptionDTO {
    
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "Token数量不能为空")
    @Positive(message = "Token数量必须大于0")
    private Long tokenCount;
    
    /**
     * 资源使用时长（分钟）
     */
    private Integer resourceUsageMinutes;
    
    /**
     * AI调用ID
     */
    private String callId;
    
    /**
     * 功能模块
     */
    private String module;
    
    /**
     * 请求摘要
     */
    private String requestSummary;
    
    /**
     * 响应摘要
     */
    private String responseSummary;
}
