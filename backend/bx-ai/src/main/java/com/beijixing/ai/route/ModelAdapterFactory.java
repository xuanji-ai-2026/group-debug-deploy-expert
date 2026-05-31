package com.beijixing.ai.route;

import com.beijixing.ai.adapter.ModelAdapter;
import com.beijixing.ai.dto.ModelInfoDTO;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型适配器工厂 - 核心路由组件
 * 动态管理所有接入的大模型，支持热新增/修改/删除模型，不需要重启服务
 * 所有模型配置缓存到内存，路由时直接从缓存读取，性能极高
 */
@Component
public class ModelAdapterFactory {

    /**
     * 已注册的模型适配器缓存：key=模型ID，value=适配器实例
     * 每个厂商的模型对应一个适配器实现，统一ModelAdapter接口
     */
    private final Map<String, ModelAdapter> adapterCache = new ConcurrentHashMap<>();

    /**
     * 已注册的模型配置缓存：key=模型ID，value=模型配置信息
     * 和数据库中的ai_model_config表同步，修改数据库后自动刷新缓存
     */
    private final Map<String, ModelInfoDTO> modelConfigCache = new ConcurrentHashMap<>();

    /**
     * 注册模型适配器
     * 新增/修改模型时调用，自动刷新缓存
     * @param modelId 模型唯一ID
     * @param adapter 模型适配器实例
     * @param modelInfo 模型配置信息
     */
    public void registerAdapter(String modelId, ModelAdapter adapter, ModelInfoDTO modelInfo) {
        adapterCache.put(modelId, adapter);
        modelConfigCache.put(modelId, modelInfo);
    }

    /**
     * 移除模型适配器
     * 删除模型时调用，自动清理缓存
     * @param modelId 模型唯一ID
     */
    public void removeAdapter(String modelId) {
        adapterCache.remove(modelId);
        modelConfigCache.remove(modelId);
    }

    /**
     * 根据模型ID获取适配器实例
     * @param modelId 模型唯一ID
     * @return 模型适配器实例，不存在返回null
     */
    public ModelAdapter getAdapter(String modelId) {
        return adapterCache.get(modelId);
    }

    /**
     * 根据模型ID获取模型配置信息
     * @param modelId 模型唯一ID
     * @return 模型配置信息，不存在返回null
     */
    public ModelInfoDTO getModelConfig(String modelId) {
        return modelConfigCache.get(modelId);
    }

    /**
     * 检查模型是否可用（存在且已启用）
     * @param modelId 模型唯一ID
     * @return true=可用，false=不可用
     */
    public boolean isModelAvailable(String modelId) {
        ModelInfoDTO modelInfo = modelConfigCache.get(modelId);
        return modelInfo != null && modelInfo.getStatus() == 1;
    }

    /**
     * 获取所有已注册的模型配置
     * @return 所有模型配置的Map，key=模型ID，value=模型配置
     */
    public Map<String, ModelInfoDTO> getAllModelConfigs() {
        return new ConcurrentHashMap<>(modelConfigCache);
    }
}
