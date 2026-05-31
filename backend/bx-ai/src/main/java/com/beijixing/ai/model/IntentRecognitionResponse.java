package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 意图识别响应
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentRecognitionResponse {
    
    /** 请求ID */
    private String requestId;
    
    /** 主要意图 */
    private Intent primaryIntent;
    
    /** 所有识别到的意图(多意图场景) */
    private List<Intent> allIntents;
    
    /** 实体提取结果 */
    private Map<String, Object> entities;
    
    /** 置信度 */
    private Double confidence;
    
    /** 响应时间(毫秒) */
    private Long responseTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Intent {
        /** 意图类型 */
        private String type;
        
        /** 意图名称 */
        private String name;
        
        /** 置信度(0-1) */
        private Double confidence;
        
        /** 意图描述 */
        private String description;
    }
    
    /**
     * 意图类型枚举
     */
    public enum IntentType {
        LEAD_INQUIRY("lead_inquiry", "商机咨询"),
        PRODUCT_QUERY("product_query", "产品查询"),
        PRICE_QUERY("price_query", "价格查询"),
        COMPLAINT("complaint", "投诉反馈"),
        SUPPORT_REQUEST("support_request", "技术支持"),
        APPOINTMENT("appointment", "预约服务"),
        GENERAL_CHAT("general_chat", "闲聊"),
        UNKNOWN("unknown", "未知意图");
        
        private final String code;
        private final String description;
        
        IntentType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
}
