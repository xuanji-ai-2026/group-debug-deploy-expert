package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音识别响应(ASR)
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionResponse {
    
    /** 请求ID */
    private String requestId;
    
    /** 识别文本 */
    private String text;
    
    /** 分段识别结果 */
    private List<Segment> segments;
    
    /** 语言 */
    private String language;
    
    /** 音频时长(秒) */
    private Double duration;
    
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
    public static class Segment {
        /** 片段ID */
        private Integer id;
        
        /** 开始时间(秒) */
        private Double start;
        
        /** 结束时间(秒) */
        private Double end;
        
        /** 识别文本 */
        private String text;
        
        /** 说话人ID(启用说话人分离时) */
        private String speaker;
        
        /** 置信度 */
        private Double confidence;
    }
}
