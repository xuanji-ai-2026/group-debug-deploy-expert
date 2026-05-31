package com.beijixing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI模型调用日志实体
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelLog {
    
    private Long id;
    
    /** 请求ID */
    private String requestId;
    
    /** 用户ID */
    private String userId;
    
    /** 模型提供商 */
    private String provider;
    
    /** 模型名称 */
    private String modelName;
    
    /** 请求类型 */
    private String requestType;
    
    /** 输入token数 */
    private Integer inputTokens;
    
    /** 输出token数 */
    private Integer outputTokens;
    
    /** 总token数 */
    private Integer totalTokens;
    
    /** 请求内容摘要 */
    private String requestContent;
    
    /** 响应内容摘要 */
    private String responseContent;
    
    /** 调用状态: SUCCESS, FAILED, TIMEOUT, FALLBACK */
    private String status;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 响应时间(毫秒) */
    private Long responseTime;
    
    /** 费用(USD) */
    private Double cost;
    
    /** 是否使用备用模型 */
    private Boolean isFallback;
    
    /** 客户端IP */
    private String clientIp;
    
    /** 创建时间 */
    private LocalDateTime createTime;
}
