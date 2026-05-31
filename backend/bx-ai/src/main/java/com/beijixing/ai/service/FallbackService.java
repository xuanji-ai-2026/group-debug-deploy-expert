package com.beijixing.ai.service;

import com.beijixing.ai.gateway.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 降级策略服务
 * 管理主备模型切换
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@Service
public class FallbackService {
    
    // 主模型提供商
    private ModelProvider primaryProvider;
    
    // 备用模型提供商列表
    private final List<ModelProvider> backupProviders = new CopyOnWriteArrayList<>();
    
    /**
     * 设置主提供商
     */
    public void setPrimaryProvider(ModelProvider provider) {
        this.primaryProvider = provider;
        log.info("设置主模型提供商:{}", provider.getName());
    }
    
    /**
     * 添加备用提供商
     */
    public void addBackupProvider(ModelProvider provider) {
        backupProviders.add(provider);
        log.info("添加备用模型提供商:{}", provider.getName());
    }
    
    /**
     * 获取可用的提供商
     * 优先返回主提供商,如果不可用则返回备用提供商
     */
    public ModelProvider getAvailableProvider() {
        // 检查主提供商
        if (primaryProvider != null && primaryProvider.getHealthy() && primaryProvider.getEnabled()) {
            return primaryProvider;
        }
        
        log.warn("主模型提供商不可用,尝试使用备用提供商");
        
        // 查找健康的备用提供商
        for (ModelProvider backup : backupProviders) {
            if (backup.getHealthy() && backup.getEnabled()) {
                log.info("使用备用模型提供商:{}", backup.getName());
                return backup;
            }
        }
        
        log.error("没有可用的模型提供商");
        return null;
    }
    
    /**
     * 获取所有备用提供商
     */
    public List<ModelProvider> getBackupProviders() {
        return backupProviders;
    }
    
    /**
     * 强制切换到备用提供商
     */
    public ModelProvider forceFallback() {
        if (primaryProvider != null) {
            primaryProvider.setHealthy(false);
            log.warn("强制切换到备用模型,主模型{}已标记为不健康", primaryProvider.getName());
        }
        return getAvailableProvider();
    }
    
    /**
     * 恢复主提供商
     */
    public void restorePrimary() {
        if (primaryProvider != null) {
            primaryProvider.setHealthy(true);
            primaryProvider.setConsecutiveFailures(0);
            log.info("主模型提供商{}已恢复", primaryProvider.getName());
        }
    }
    
    /**
     * 检查是否需要降级
     */
    public boolean shouldFallback(String providerId, int failureCount, int threshold) {
        return failureCount >= threshold;
    }
}
