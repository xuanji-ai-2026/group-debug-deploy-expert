package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 语音合成响应(TTS)
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechSynthesisResponse {
    
    /** 请求ID */
    private String requestId;
    
    /** 音频URL */
    private String audioUrl;
    
    /** Base64编码的音频数据 */
    private String audioBase64;
    
    /** 音频格式 */
    private String format;
    
    /** 采样率 */
    private Integer sampleRate;
    
    /** 音频时长(秒) */
    private Double duration;
    
    /** 文件大小(字节) */
    private Long fileSize;
    
    /** 响应时间(毫秒) */
    private Long responseTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
}
