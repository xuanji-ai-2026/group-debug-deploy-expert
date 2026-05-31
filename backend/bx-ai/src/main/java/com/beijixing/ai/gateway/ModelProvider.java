package com.beijixing.ai.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI模型提供商配置
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProvider {
    
    /** 提供商ID */
    private String id;
    
    /** 提供商名称 */
    private String name;
    
    /** 提供商类型: VOLCENGINE, WENXIN, TONGYI, HUNYUAN, XINGHUO */
    private String type;
    
    /** API基础URL */
    private String baseUrl;
    
    /** API密钥 */
    private String apiKey;
    
    /** 额外配置 */
    private String extraConfig;
    
    /** 优先级(数字越小优先级越高) */
    private Integer priority;
    
    /** 权重(负载均衡用) */
    @Builder.Default
    private Integer weight = 1;
    
    /** 是否启用 */
    @Builder.Default
    private Boolean enabled = true;
    
    /** 是否健康 */
    @Builder.Default
    private Boolean healthy = true;
    
    /** 连续失败次数 */
    @Builder.Default
    private Integer consecutiveFailures = 0;
    
    /** 失败阈值 */
    @Builder.Default
    private Integer failureThreshold = 3;
    
    /** 最后检查时间 */
    private LocalDateTime lastCheckTime;
    
    /** 平均响应时间(毫秒) */
    @Builder.Default
    private Long avgResponseTime = 0L;
    
    /**
     * 检查是否需要熔断
     */
    public boolean shouldCircuitBreak() {
        return consecutiveFailures >= failureThreshold;
    }
    
    /**
     * 记录失败
     */
    public void recordFailure() {
        this.consecutiveFailures++;
        if (shouldCircuitBreak()) {
            this.healthy = false;
        }
    }
    
    /**
     * 记录成功
     */
    public void recordSuccess() {
        this.consecutiveFailures = 0;
        this.healthy = true;
    }
}
