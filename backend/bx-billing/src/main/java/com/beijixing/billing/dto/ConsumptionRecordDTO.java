package com.beijixing.billing.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消费明细DTO
 * BL-004: 消费明细记录
 */
@Data
public class ConsumptionRecordDTO {
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long orderId;
    private String callId;
    private String module;
    private Long tokenCount;
    private Integer resourceUsageMinutes;
    private Long tokenCost;
    private Long resourceCost;
    private Long totalCost;
    private Integer unitPrice;
    private String requestSummary;
    private String responseSummary;
    private LocalDateTime callTime;
    private LocalDateTime createTime;
}
