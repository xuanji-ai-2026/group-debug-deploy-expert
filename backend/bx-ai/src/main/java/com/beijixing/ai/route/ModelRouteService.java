package com.beijixing.ai.route;

import com.beijixing.ai.adapter.ModelAdapter;
import com.beijixing.ai.prompt.enums.AiSceneEnum;

/**
 * 大模型智能路由服务
 * 负责根据场景选择最优模型，处理降级、重试、流量分配
 */
public interface ModelRouteService {

    /**
     * 根据场景选择最优可用模型
     * @param scene 场景枚举
     * @return 模型适配器
     */
    ModelAdapter selectBestModel(AiSceneEnum scene);

    /**
     * 选择当前场景的备用模型
     * @param scene 场景枚举
     * @param failedModelId 失败的模型ID
     * @return 备用模型适配器
     */
    ModelAdapter selectFallbackModel(AiSceneEnum scene, String failedModelId);

    /**
     * 切换全局默认模型
     * @param modelId 模型ID
     * @return 是否切换成功
     */
    boolean switchDefaultModel(String modelId);
}
