package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI图片生成响应
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationResponse {
    
    /** 请求ID */
    private String requestId;
    
    /** 生成的图片列表 */
    private List<GeneratedImage> images;
    
    /** 使用的模型 */
    private String model;
    
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
    public static class GeneratedImage {
        /** 图片索引 */
        private Integer index;
        
        /** 图片URL */
        private String url;
        
        /** Base64编码的图片数据 */
        private String b64Json;
        
        /** 图片宽度 */
        private Integer width;
        
        /** 图片高度 */
        private Integer height;
        
        /** 图片格式 */
        private String format;
        
        /** 修订后的提示词 */
        private String revisedPrompt;
    }
}
