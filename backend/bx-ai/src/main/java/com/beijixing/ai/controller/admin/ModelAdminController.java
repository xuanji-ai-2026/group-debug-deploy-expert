package com.beijixing.ai.controller.admin;

import com.beijixing.ai.dto.ModelInfoDTO;
import com.beijixing.ai.route.ModelAdapterFactory;
import com.beijixing.ai.route.ModelRouteService;
import com.beijixing.ai.service.ModelConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 管理后台 - 大模型配置接口
 * 所有接口支持热更新，修改后立即生效，不需要重启服务
 * 权限：需要管理员角色才能调用
 */
@RestController
@RequestMapping("/admin/ai/model")
public class ModelAdminController {

    @Resource
    private ModelAdapterFactory modelAdapterFactory;

    @Resource
    private ModelRouteService modelRouteService;

    @Resource
    private ModelConfigService modelConfigService;

    /**
     * 获取所有已配置的模型列表
     * @return 模型配置Map，key=模型ID，value=模型配置信息
     */
    @GetMapping("/list")
    public Map<String, ModelInfoDTO> listModels() {
        return modelAdapterFactory.getAllModelConfigs();
    }

    /**
     * 新增/更新模型配置
     * 保存后自动刷新内存缓存，立即生效
     * @param modelInfo 模型配置信息
     * @return true=成功，false=失败
     */
    @PostMapping("/save")
    public Boolean saveModel(@RequestBody ModelInfoDTO modelInfo) {
        return modelConfigService.saveModel(modelInfo);
    }

    /**
     * 删除模型配置
     * 删除后自动刷新内存缓存，立即生效
     * @param modelId 模型唯一ID
     * @return true=成功，false=失败
     */
    @DeleteMapping("/{modelId}")
    public Boolean deleteModel(@PathVariable String modelId) {
        return modelConfigService.deleteModel(modelId);
    }

    /**
     * 切换全局默认模型
     * 没有指定场景时，默认使用该模型
     * @param modelId 模型唯一ID
     * @return true=成功，false=失败
     */
    @PostMapping("/switch-default/{modelId}")
    public Boolean switchDefaultModel(@PathVariable String modelId) {
        return modelRouteService.switchDefaultModel(modelId);
    }

    /**
     * 手动刷新配置缓存
     * 数据库配置被外部修改时调用，强制同步数据库配置到内存缓存
     * @return true=成功
     */
    @PostMapping("/refresh-cache")
    public Boolean refreshCache() {
        modelConfigService.refreshCache();
        return true;
    }
}