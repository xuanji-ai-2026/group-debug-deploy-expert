package com.beijixing.ai.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI模型路由器
 * 负责请求路由、负载均衡和故障转移
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@Component
public class ModelRouter {
    
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    
    /**
     * 轮询策略选择提供商
     */
    public ModelProvider roundRobin(List<ModelProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        
        String key = "default";
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % providers.size();
        return providers.get(index);
    }
    
    /**
     * 加权轮询策略选择提供商
     */
    public ModelProvider weightedRoundRobin(List<ModelProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        
        int totalWeight = providers.stream().mapToInt(ModelProvider::getWeight).sum();
        if (totalWeight == 0) {
            return roundRobin(providers);
        }
        
        String key = "weighted";
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int current = counter.getAndIncrement() % totalWeight;
        
        int weightSum = 0;
        for (ModelProvider provider : providers) {
            weightSum += provider.getWeight();
            if (current < weightSum) {
                return provider;
            }
        }
        
        return providers.get(0);
    }
    
    /**
     * 优先级策略选择提供商(选择优先级最高的健康提供商)
     */
    public ModelProvider priority(List<ModelProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        
        return providers.stream()
                .filter(ModelProvider::getHealthy)
                .filter(ModelProvider::getEnabled)
                .min((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .orElse(providers.get(0));
    }
    
    /**
     * 延迟最低策略选择提供商
     */
    public ModelProvider lowestLatency(List<ModelProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        
        return providers.stream()
                .filter(ModelProvider::getHealthy)
                .filter(ModelProvider::getEnabled)
                .min((a, b) -> Long.compare(a.getAvgResponseTime(), b.getAvgResponseTime()))
                .orElse(providers.get(0));
    }
    
    /**
     * 根据策略选择提供商
     */
    public ModelProvider selectProvider(List<ModelProvider> providers, RoutingStrategy strategy) {
        if (providers == null || providers.isEmpty()) {
            log.warn("没有可用的模型提供商");
            return null;
        }
        
        // 过滤出健康的提供商
        List<ModelProvider> healthyProviders = providers.stream()
                .filter(ModelProvider::getHealthy)
                .filter(ModelProvider::getEnabled)
                .toList();
        
        if (healthyProviders.isEmpty()) {
            log.warn("没有健康的模型提供商,将尝试使用备用");
            return providers.get(0); // 降级:使用第一个提供商
        }
        
        return switch (strategy) {
            case ROUND_ROBIN -> roundRobin(healthyProviders);
            case WEIGHTED -> weightedRoundRobin(healthyProviders);
            case PRIORITY -> priority(healthyProviders);
            case LATENCY -> lowestLatency(healthyProviders);
        };
    }
    
    /**
     * 路由策略枚举
     */
    public enum RoutingStrategy {
        ROUND_ROBIN,    // 轮询
        WEIGHTED,       // 加权
        PRIORITY,       // 优先级
        LATENCY         // 最低延迟
    }
}
