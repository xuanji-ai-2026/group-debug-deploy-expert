package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * AI图片生成请求
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationRequest {
    
    /** 用户ID */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    /** 图片描述/提示词 */
    @NotBlank(message = "图片描述不能为空")
    private String prompt;
    
    /** 反向提示词 */
    private String negativePrompt;
    
    /** 图片尺寸: 1024x1024, 1024x768, 768x1024, 1792x1024, 1024x1792 */
    @Pattern(regexp = "^\\d+x\\d+$", message = "尺寸格式应为: 1024x1024")
    @Builder.Default
    private String size = "1024x1024";
    
    /** 图片风格: realistic, cartoon, watercolor, oil_painting, sketch */
    private String style;
    
    /** 生成数量(1-4) */
    @Builder.Default
    private Integer n = 1;
    
    /** 质量: standard, hd */
    @Builder.Default
    private String quality = "standard";
    
    /** 响应格式: url, b64_json */
    @Builder.Default
    private String responseFormat = "url";
    
    /** 参考图片URL(用于图生图) */
    private String referenceImageUrl;
    
    /** 幂等key */
    private String idempotencyKey;
}
