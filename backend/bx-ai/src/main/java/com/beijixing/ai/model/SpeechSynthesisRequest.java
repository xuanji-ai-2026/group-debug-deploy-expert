package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 语音合成请求(TTS)
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechSynthesisRequest {
    
    /** 用户ID */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    /** 待合成文本 */
    @NotBlank(message = "文本不能为空")
    @Size(max = 5000, message = "文本长度不能超过5000字符")
    private String text;
    
    /** 声音类型: zh-CN-Xiaoxiao, zh-CN-Yunxi, zh-CN-Yunjian */
    @Builder.Default
    private String voice = "zh-CN-Xiaoxiao";
    
    /** 语速: 0.5-2.0, 默认1.0 */
    @Builder.Default
    private Double speed = 1.0;
    
    /** 音调: 0.5-2.0, 默认1.0 */
    @Builder.Default
    private Double pitch = 1.0;
    
    /** 音量: 0.0-1.0, 默认1.0 */
    @Builder.Default
    private Double volume = 1.0;
    
    /** 输出格式: mp3, wav, pcm */
    @Builder.Default
    private String format = "mp3";
    
    /** 采样率: 8000, 16000, 24000, 48000 */
    @Builder.Default
    private Integer sampleRate = 24000;
}
