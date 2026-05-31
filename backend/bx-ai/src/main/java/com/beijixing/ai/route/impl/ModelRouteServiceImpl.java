package com.beijixing.ai.route.impl;

import com.beijixing.ai.adapter.ModelAdapter;
import com.beijixing.ai.config.AutoRefreshConfig;
import com.beijixing.ai.dto.ModelInfoDTO;
import com.beijixing.ai.prompt.enums.AiSceneEnum;
import com.beijixing.ai.route.ModelAdapterFactory;
import com.beijixing.ai.route.ModelRouteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ModelRouteServiceImpl implements ModelRouteService {

    @Resource
    private ModelAdapterFactory modelAdapterFactory;

    @Resource
    private AutoRefreshConfig autoRefreshConfig;

    @Override
    public ModelAdapter selectBestModel(AiSceneEnum scene) {
        // 获取该场景下所有可用的模型，按优先级排序
        List<ModelInfoDTO> availableModels = getAvailableModelsByScene(scene);
        if (availableModels.isEmpty()) {
            // 场景没有匹配的模型，使用全局默认模型
            ModelAdapter defaultAdapter = modelAdapterFactory.getAdapter(autoRefreshConfig.getDefaultModelId());
            if (defaultAdapter != null && modelAdapterFactory.isModelAvailable(autoRefreshConfig.getDefaultModelId())) {
                return defaultAdapter;
            }
            throw new RuntimeException("当前场景没有可用的大模型，请检查配置");
        }
        // 按优先级排序，取优先级最高的
        availableModels.sort(Comparator.comparingInt(ModelInfoDTO::getPriority));
        return modelAdapterFactory.getAdapter(availableModels.get(0).getModelId());
    }

    @Override
    public ModelAdapter selectFallbackModel(AiSceneEnum scene, String failedModelId) {
        if (!autoRefreshConfig.getDegradeEnable()) {
            throw new RuntimeException("模型调用失败，降级已关闭");
        }
        // 获取该场景下所有可用的模型，排除失败的模型
        List<ModelInfoDTO> availableModels = getAvailableModelsByScene(scene);
        availableModels.removeIf(model -> model.getModelId().equals(failedModelId));
        if (availableModels.isEmpty()) {
            // 没有可用的备用模型，尝试用全局默认模型
            if (!failedModelId.equals(autoRefreshConfig.getDefaultModelId())
                    && modelAdapterFactory.isModelAvailable(autoRefreshConfig.getDefaultModelId())) {
                return modelAdapterFactory.getAdapter(autoRefreshConfig.getDefaultModelId());
            }
            throw new RuntimeException("所有模型均调用失败，请稍后重试");
        }
        // 按优先级排序，取最高优先级的备用模型
        availableModels.sort(Comparator.comparingInt(ModelInfoDTO::getPriority));
        log.warn("模型{}调用失败，自动降级到备用模型{}", failedModelId, availableModels.get(0).getModelId());
        return modelAdapterFactory.getAdapter(availableModels.get(0).getModelId());
    }

    @Override
    public boolean switchDefaultModel(String modelId) {
        if (modelAdapterFactory.isModelAvailable(modelId)) {
            // 这里后续对接配置中心后直接修改配置即可，当前暂存在内存中，支持热更新
            autoRefreshConfig.setDefaultModelId(modelId);
            log.info("全局默认模型已切换为{}", modelId);
            return true;
        }
        return false;
    }

    /**
     * 获取场景下所有可用的模型
     */
    private List<ModelInfoDTO> getAvailableModelsByScene(AiSceneEnum scene) {
        List<ModelInfoDTO> result = new ArrayList<>();
        // 遍历所有已注册的模型，筛选支持该场景且启用的
        for (Map.Entry<String, ModelInfoDTO> entry : modelAdapterFactory.getAllModelConfigs().entrySet()) {
            ModelInfoDTO model = entry.getValue();
            if (model.getStatus() == 1
                    && model.getType().equals(scene.getType())
                    && model.getSupportScenes().contains(scene.getCode())) {
                result.add(model);
            }
        }
        return result;
    }
}
