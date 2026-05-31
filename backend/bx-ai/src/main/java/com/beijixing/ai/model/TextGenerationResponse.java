package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI文本生成响应
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextGenerationResponse {
    
    /** 请求ID */
    private String requestId;
    
    /** 生成结果列表 */
    private List<GeneratedContent> contents;
    
    /** 使用的模型 */
    private String model;
    
    /** token使用量 */
    private TokenUsage tokenUsage;
    
    /** 响应时间(毫秒) */
    private Long responseTime;
    
    /** 是否使用备用模型 */
    private Boolean isFallback;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedContent {
        /** 内容索引 */
        private Integer index;
        
        /** 生成的文本内容 */
        private String text;
        
        /** 标题(可选) */
        private String title;
        
        /** 预估字数 */
        private Integer wordCount;
        
        /** 违禁词检测结果 */
        private SensitiveCheckResult sensitiveCheck;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensitiveCheckResult {
        private Boolean hasSensitive;
        private List<String> sensitiveWords;
    }
}
