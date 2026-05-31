package com.beijixing.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoRefreshConfig {

    /**
     * 全局默认模型ID
     */
    @Value("${ai.model.default:volcano}")
    private String defaultModelId;

    /**
     * 是否开启自动降级
     */
    @Value("${ai.route.degrade.enable:true}")
    private Boolean degradeEnable;

    /**
     * 全局最大重试次数
     */
    @Value("${ai.route.retry.max:2}")
    private Integer maxRetryCount;

    /**
     * 提示词自动优化开关
     */
    @Value("${ai.prompt.optimize.enable:true}")
    private Boolean promptOptimizeEnable;

    public String getDefaultModelId() {
        return defaultModelId;
    }

    public void setDefaultModelId(String defaultModelId) {
        this.defaultModelId = defaultModelId;
    }

    public Boolean getDegradeEnable() {
        return degradeEnable;
    }

    public void setDegradeEnable(Boolean degradeEnable) {
        this.degradeEnable = degradeEnable;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Boolean getPromptOptimizeEnable() {
        return promptOptimizeEnable;
    }

    public void setPromptOptimizeEnable(Boolean promptOptimizeEnable) {
        this.promptOptimizeEnable = promptOptimizeEnable;
    }
}
