package com.beijixing.ai.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * AI模型网关服务
 * 统一的AI模型调用入口,负责路由、熔断、限流、日志
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@Service
public class AiModelGateway {
    
    @Autowired
    private ModelRouter modelRouter;
    
    @Autowired
    private CircuitBreakerManager circuitBreakerManager;
    
    @Autowired
    private RateLimitManager rateLimitManager;
    
    @Autowired
    private IdempotencyManager idempotencyManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 模型提供商列表
    private List<ModelProvider> providers;
    
    /**
     * 执行带网关保护的AI调用
     */
    public <T> GatewayResult<T> executeWithGateway(
            String userId,
            String requestType,
            String idempotencyKey,
            ModelCall<T> modelCall) {
        
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 检查幂等性
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                if (idempotencyManager.isProcessed(idempotencyKey)) {
                    String cachedResponse = idempotencyManager.getProcessedResponse(idempotencyKey);
                    log.info("[{}] 命中幂等缓存,直接返回", requestId);
                    return GatewayResult.<T>builder()
                            .requestId(requestId)
                            .data(deserializeResponse(cachedResponse))
                            .fromCache(true)
                            .build();
                }
            }
            
            // 2. 限流检查
            String rateLimitKey = "user:" + userId + ":" + requestType;
            if (!rateLimitManager.allowRequest(rateLimitKey)) {
                log.warn("[{}] 触发限流:userId={}, requestType={}", requestId, userId, requestType);
                return GatewayResult.<T>builder()
                        .requestId(requestId)
                        .success(false)
                        .errorCode("RATE_LIMITED")
                        .errorMessage("请求过于频繁,请稍后再试")
                        .build();
            }
            
            // 3. 选择模型提供商
            ModelProvider provider = selectProvider();
            if (provider == null) {
                log.error("[{}] 无可用模型提供商", requestId);
                return GatewayResult.<T>builder()
                        .requestId(requestId)
                        .success(false)
                        .errorCode("NO_PROVIDER_AVAILABLE")
                        .errorMessage("当前无可用AI服务,请稍后再试")
                        .build();
            }
            
            // 4. 检查熔断器
            if (!circuitBreakerManager.allowRequest(provider.getId())) {
                log.warn("[{}] 熔断器打开,尝试备用提供商", requestId);
                provider = selectFallbackProvider();
                if (provider == null) {
                    return GatewayResult.<T>builder()
                            .requestId(requestId)
                            .success(false)
                            .errorCode("CIRCUIT_BREAKER_OPEN")
                            .errorMessage("服务暂时不可用,请稍后再试")
                            .build();
                }
            }
            
            // 5. 执行模型调用
            log.info("[{}] 开始调用模型:provider={}, requestType={}", requestId, provider.getName(), requestType);
            T result = modelCall.call(provider);
            
            // 6. 记录成功
            circuitBreakerManager.recordSuccess(provider.getId());
            provider.recordSuccess();
            
            // 7. 缓存幂等结果
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                idempotencyManager.markProcessed(idempotencyKey, serializeResponse(result));
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 模型调用成功,响应时间:{}ms", requestId, responseTime);
            
            return GatewayResult.<T>builder()
                    .requestId(requestId)
                    .success(true)
                    .data(result)
                    .providerId(provider.getId())
                    .providerName(provider.getName())
                    .responseTime(responseTime)
                    .isFallback(!provider.getHealthy())
                    .build();
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("[{}] 模型调用失败,响应时间:{}ms", requestId, responseTime, e);
            
            // 记录失败
            if (providers != null && !providers.isEmpty()) {
                ModelProvider provider = providers.get(0);
                circuitBreakerManager.recordFailure(provider.getId());
                provider.recordFailure();
            }
            
            return GatewayResult.<T>builder()
                    .requestId(requestId)
                    .success(false)
                    .errorCode("MODEL_CALL_FAILED")
                    .errorMessage(e.getMessage())
                    .responseTime(responseTime)
                    .build();
        }
    }
    
    /**
     * 选择主模型提供商
     */
    private ModelProvider selectProvider() {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        return modelRouter.selectProvider(providers, ModelRouter.RoutingStrategy.PRIORITY);
    }
    
    /**
     * 选择备用提供商
     */
    private ModelProvider selectFallbackProvider() {
        if (providers == null || providers.size() < 2) {
            return null;
        }
        // 选择第二个优先级最高的
        return providers.stream()
                .filter(ModelProvider::getEnabled)
                .skip(1)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 序列化响应
     */
    private String serializeResponse(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("序列化响应失败", e);
            return "";
        }
    }
    
    /**
     * 反序列化响应
     */
    @SuppressWarnings("unchecked")
    private <T> T deserializeResponse(String json) {
        try {
            return (T) objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.warn("反序列化响应失败", e);
            return null;
        }
    }
    
    /**
     * 设置提供商列表
     */
    public void setProviders(List<ModelProvider> providers) {
        this.providers = providers;
    }
    
    /**
     * 模型调用接口
     */
    @FunctionalInterface
    public interface ModelCall<T> {
        T call(ModelProvider provider) throws Exception;
    }
    
    /**
     * 网关结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GatewayResult<T> {
        private String requestId;
        private boolean success;
        private T data;
        private String providerId;
        private String providerName;
        private Long responseTime;
        private Boolean isFallback;
        private Boolean fromCache;
        private String errorCode;
        private String errorMessage;
    }
}
