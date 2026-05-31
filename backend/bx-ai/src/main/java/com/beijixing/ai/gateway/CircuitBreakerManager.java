package com.beijixing.ai.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断器管理器
 * 实现熔断降级逻辑
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@Component
public class CircuitBreakerManager {
    
    private final Map<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();
    
    /**
     * 获取熔断器状态
     */
    public CircuitBreakerState getState(String providerId) {
        return circuitBreakers.computeIfAbsent(providerId, 
                k -> new CircuitBreakerState(providerId));
    }
    
    /**
     * 记录成功
     */
    public void recordSuccess(String providerId) {
        CircuitBreakerState state = getState(providerId);
        state.recordSuccess();
        log.debug("提供商[{}]调用成功,连续失败次数重置", providerId);
    }
    
    /**
     * 记录失败
     */
    public void recordFailure(String providerId) {
        CircuitBreakerState state = getState(providerId);
        state.recordFailure();
        log.warn("提供商[{}]调用失败,连续失败次数:{}", providerId, state.getConsecutiveFailures());
    }
    
    /**
     * 检查是否允许请求通过
     */
    public boolean allowRequest(String providerId) {
        CircuitBreakerState state = getState(providerId);
        return state.allowRequest();
    }
    
    /**
     * 重置熔断器
     */
    public void reset(String providerId) {
        circuitBreakers.remove(providerId);
        log.info("熔断器[{}]已重置", providerId);
    }
    
    /**
     * 熔断器状态类
     */
    public static class CircuitBreakerState {
        
        private final String providerId;
        private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
        private volatile CircuitBreakerStatus status = CircuitBreakerStatus.CLOSED;
        private volatile LocalDateTime openTime;
        
        // 熔断阈值配置
        private static final int FAILURE_THRESHOLD = 5;
        private static final int HALF_OPEN_MAX_CALLS = 3;
        private static final long OPEN_DURATION_SECONDS = 30;
        
        private final AtomicInteger halfOpenCalls = new AtomicInteger(0);
        
        public CircuitBreakerState(String providerId) {
            this.providerId = providerId;
        }
        
        /**
         * 检查是否允许请求通过
         */
        public synchronized boolean allowRequest() {
            switch (status) {
                case CLOSED:
                    return true;
                case OPEN:
                    if (shouldAttemptReset()) {
                        status = CircuitBreakerStatus.HALF_OPEN;
                        halfOpenCalls.set(0);
                        log.info("熔断器[{}]进入半开状态", providerId);
                        return true;
                    }
                    return false;
                case HALF_OPEN:
                    if (halfOpenCalls.incrementAndGet() <= HALF_OPEN_MAX_CALLS) {
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        }
        
        /**
         * 记录成功
         */
        public synchronized void recordSuccess() {
            consecutiveFailures.set(0);
            
            if (status == CircuitBreakerStatus.HALF_OPEN) {
                status = CircuitBreakerStatus.CLOSED;
                log.info("熔断器[{}]关闭,服务恢复正常", providerId);
            }
        }
        
        /**
         * 记录失败
         */
        public synchronized void recordFailure() {
            int failures = consecutiveFailures.incrementAndGet();
            
            if (status == CircuitBreakerStatus.HALF_OPEN) {
                status = CircuitBreakerStatus.OPEN;
                openTime = LocalDateTime.now();
                log.warn("熔断器[{}]在半开状态下再次失败,重新打开", providerId);
            } else if (failures >= FAILURE_THRESHOLD && status == CircuitBreakerStatus.CLOSED) {
                status = CircuitBreakerStatus.OPEN;
                openTime = LocalDateTime.now();
                log.error("熔断器[{}]打开,连续失败次数达到阈值:{}", providerId, failures);
            }
        }
        
        /**
         * 是否尝试重置熔断器
         */
        private boolean shouldAttemptReset() {
            if (openTime == null) return true;
            return java.time.Duration.between(openTime, LocalDateTime.now()).getSeconds() >= OPEN_DURATION_SECONDS;
        }
        
        public int getConsecutiveFailures() {
            return consecutiveFailures.get();
        }
        
        public CircuitBreakerStatus getStatus() {
            return status;
        }
    }
    
    /**
     * 熔断器状态枚举
     */
    public enum CircuitBreakerStatus {
        CLOSED,     // 关闭-正常服务
        OPEN,       // 打开-拒绝服务
        HALF_OPEN   // 半开-尝试恢复
    }
}
