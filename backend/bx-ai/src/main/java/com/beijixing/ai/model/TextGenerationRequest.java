package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * AI文本生成请求
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextGenerationRequest {
    
    /** 用户ID */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    /** 提示词/指令 */
    @NotBlank(message = "提示词不能为空")
    @Size(max = 8000, message = "提示词长度不能超过8000字符")
    private String prompt;
    
    /** 系统提示词 */
    private String systemPrompt;
    
    /** 内容类型: social_media(社媒), ad_copy(广告), article(文章), script(脚本) */
    @NotBlank(message = "内容类型不能为空")
    private String contentType;
    
    /** 目标平台: wechat, weibo, douyin, xiaohongshu等 */
    private String targetPlatform;
    
    /** 语气风格: professional, casual, humorous, formal */
    private String tone;
    
    /** 字数限制 */
    private Integer maxLength;
    
    /** 生成数量 */
    @Builder.Default
    private Integer count = 1;
    
    /** 温度参数(0-1) */
    @Builder.Default
    private Double temperature = 0.7;
    
    /** 扩展参数 */
    private Map<String, Object> extraParams;
    
    /** 幂等key */
    private String idempotencyKey;
}
