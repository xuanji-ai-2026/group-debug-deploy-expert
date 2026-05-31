package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 语音识别请求(ASR)
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionRequest {
    
    /** 用户ID */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    /** 音频文件URL */
    private String audioUrl;
    
    /** Base64编码的音频数据 */
    private String audioBase64;
    
    /** 音频格式: mp3, mp4, mpeg, mpga, m4a, wav, webm */
    @Builder.Default
    private String format = "mp3";
    
    /** 语言: zh, en, ja等 */
    @Builder.Default
    private String language = "zh";
    
    /** 是否启用标点预测 */
    @Builder.Default
    private Boolean enablePunctuation = true;
    
    /** 是否启用说话人分离 */
    @Builder.Default
    private Boolean speakerDiarization = false;
}
