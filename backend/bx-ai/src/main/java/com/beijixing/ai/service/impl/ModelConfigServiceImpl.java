package com.beijixing.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.ai.adapter.ModelAdapter;
import com.beijixing.ai.adapter.VolcengineAdapter;
import com.beijixing.ai.adapter.WenxinAdapter;
import com.beijixing.ai.dto.ModelInfoDTO;
import com.beijixing.ai.entity.ModelConfig;
import com.beijixing.ai.mapper.ModelConfigMapper;
import com.beijixing.ai.route.ModelAdapterFactory;
import com.beijixing.ai.service.ModelConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ModelAdapterFactory modelAdapterFactory;

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, ModelAdapter> adapterInstanceCache = new HashMap<>();

    /**
     * 启动时加载所有模型配置到内存
     */
    @PostConstruct
    public void init() {
        refreshCache();
        log.info("模型配置初始化完成，共加载{}个模型", modelAdapterFactory.getAllModelConfigs().size());
    }

    @Override
    public ModelInfoDTO convertToDTO(ModelConfig modelConfig) {
        ModelInfoDTO dto = new ModelInfoDTO();
        dto.setModelId(modelConfig.getModelId());
        dto.setModelName(modelConfig.getModelName());
        dto.setProvider(modelConfig.getProvider());
        dto.setType(modelConfig.getType());
        dto.setStatus(modelConfig.getStatus());
        dto.setPriority(modelConfig.getPriority());
        dto.setApiKey(modelConfig.getApiKey());
        dto.setApiSecret(modelConfig.getApiSecret());
        dto.setEndpoint(modelConfig.getEndpoint());
        dto.setMaxTokens(modelConfig.getMaxTokens());
        dto.setTemperature(modelConfig.getTemperature());
        dto.setRateLimit(modelConfig.getRateLimit());
        dto.setTimeout(modelConfig.getTimeout());
        // 转换JSON为List
        try {
            if (modelConfig.getSupportScenes() != null) {
                dto.setSupportScenes(objectMapper.readValue(modelConfig.getSupportScenes(), new TypeReference<List<String>>() {}));
            }
        } catch (Exception e) {
            log.error("解析模型支持场景失败，modelId={}", modelConfig.getModelId(), e);
        }
        return dto;
    }

    @Override
    public ModelConfig convertToEntity(ModelInfoDTO dto) {
        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setModelId(dto.getModelId());
        modelConfig.setModelName(dto.getModelName());
        modelConfig.setProvider(dto.getProvider());
        modelConfig.setType(dto.getType());
        modelConfig.setStatus(dto.getStatus());
        modelConfig.setPriority(dto.getPriority());
        modelConfig.setApiKey(dto.getApiKey());
        modelConfig.setApiSecret(dto.getApiSecret());
        modelConfig.setEndpoint(dto.getEndpoint());
        modelConfig.setMaxTokens(dto.getMaxTokens());
        modelConfig.setTemperature(dto.getTemperature());
        modelConfig.setRateLimit(dto.getRateLimit());
        modelConfig.setTimeout(dto.getTimeout());
        // 转换List为JSON
        try {
            if (dto.getSupportScenes() != null) {
                modelConfig.setSupportScenes(objectMapper.writeValueAsString(dto.getSupportScenes()));
            }
        } catch (Exception e) {
            log.error("序列化模型支持场景失败，modelId={}", dto.getModelId(), e);
        }
        return modelConfig;
    }

    @Override
    public List<ModelConfig> listEnabledModels() {
        return list(new LambdaQueryWrapper<ModelConfig>().eq(ModelConfig::getStatus, 1));
    }

    @Override
    public boolean saveModel(ModelInfoDTO modelInfoDTO) {
        // 先删除旧的配置
        remove(new LambdaQueryWrapper<ModelConfig>().eq(ModelConfig::getModelId, modelInfoDTO.getModelId()));
        // 保存新的配置
        ModelConfig entity = convertToEntity(modelInfoDTO);
        boolean success = save(entity);
        if (success) {
            // 刷新缓存
            refreshCache();
        }
        return success;
    }

    @Override
    public boolean deleteModel(String modelId) {
        boolean success = remove(new LambdaQueryWrapper<ModelConfig>().eq(ModelConfig::getModelId, modelId));
        if (success) {
            // 刷新缓存
            refreshCache();
        }
        return success;
    }

    @Override
    public void refreshCache() {
        // 清空旧缓存
        modelAdapterFactory.getAllModelConfigs().clear();
        // 加载所有启用的模型
        List<ModelConfig> modelConfigs = listEnabledModels();
        for (ModelConfig config : modelConfigs) {
            try {
                // 根据厂商初始化对应的Adapter
                ModelAdapter adapter = createAdapter(config);
                if (adapter != null) {
                    ModelInfoDTO dto = convertToDTO(config);
                    modelAdapterFactory.registerAdapter(config.getModelId(), adapter, dto);
                }
            } catch (Exception e) {
                log.error("初始化模型Adapter失败，modelId={}", config.getModelId(), e);
            }
        }
        log.info("模型配置缓存刷新完成，共加载{}个可用模型", modelAdapterFactory.getAllModelConfigs().size());
    }

    /**
     * 根据厂商创建对应的Adapter
     * 后续新增厂商只需要在这里加case，实现对应的Adapter即可
     */
    private ModelAdapter createAdapter(ModelConfig config) {
        String provider = config.getProvider();
        
        return switch (provider.toLowerCase()) {
            case "volcano", "volcengine" -> getOrCreateAdapter("volcengine", VolcengineAdapter.class);
            case "wenxin", "baidu" -> getOrCreateAdapter("wenxin", WenxinAdapter.class);
            case "aliyun", "ali" -> createAliyunAdapter(config);
            case "openai" -> createOpenAiAdapter(config);
            default -> {
                log.warn("不支持的模型厂商: {}, modelId={}", provider, config.getModelId());
                yield null;
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    private <T extends ModelAdapter> T getOrCreateAdapter(String beanName, Class<T> adapterClass) {
        return (T) adapterInstanceCache.computeIfAbsent(beanName, k -> {
            try {
                return applicationContext.getBean(adapterClass);
            } catch (Exception e) {
                log.error("获取{} Adapter实例失败", beanName, e);
                return null;
            }
        });
    }
    
    private ModelAdapter createAliyunAdapter(ModelConfig config) {
        log.warn("阿里云通义千问适配器尚未实现, modelId={}", config.getModelId());
        return null;
    }
    
    private ModelAdapter createOpenAiAdapter(ModelConfig config) {
        log.warn("OpenAI适配器尚未实现, modelId={}", config.getModelId());
        return null;
    }
}
