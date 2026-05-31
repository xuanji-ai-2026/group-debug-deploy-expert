package com.beijixing.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.beijixing.ai.entity.ModelConfig;
import com.beijixing.ai.dto.ModelInfoDTO;
import java.util.List;

public interface ModelConfigService extends IService<ModelConfig> {

    /**
     * 转换实体为DTO
     */
    ModelInfoDTO convertToDTO(ModelConfig modelConfig);

    /**
     * 转换DTO为实体
     */
    ModelConfig convertToEntity(ModelInfoDTO modelInfoDTO);

    /**
     * 获取所有启用的模型
     */
    List<ModelConfig> listEnabledModels();

    /**
     * 保存模型配置
     */
    boolean saveModel(ModelInfoDTO modelInfoDTO);

    /**
     * 删除模型配置
     */
    boolean deleteModel(String modelId);

    /**
     * 刷新内存缓存
     */
    void refreshCache();
}
