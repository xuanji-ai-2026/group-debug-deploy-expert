package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 意图识别请求
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentRecognitionRequest {
    
    /** 用户ID */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    /** 用户输入文本 */
    @NotBlank(message = "输入文本不能为空")
    private String text;
    
    /** 会话ID */
    private String sessionId;
    
    /** 上下文信息 */
    private List<String> context;
    
    /** 候选意图列表(可选) */
    private List<String> candidateIntents;
    
    /** 是否启用多意图识别 */
    @Builder.Default
    private Boolean multiIntent = false;
    
    /** 扩展参数 */
    private Map<String, Object> extraParams;
}
