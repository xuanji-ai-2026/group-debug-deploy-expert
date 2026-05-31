package com.beijixing.ai.prompt.service;

import com.beijixing.ai.dto.PromptTemplateDTO;
import com.beijixing.ai.prompt.enums.AiSceneEnum;
import java.util.List;
import java.util.Map;

/**
 * 提示词模板服务
 * 内置全行业预设模板，支持自定义扩展，自动优化提示词
 */
public interface PromptTemplateService {

    /**
     * 获取场景下的所有模板
     * @param sceneCode 场景编码
     * @return 模板列表
     */
    List<PromptTemplateDTO> listTemplatesByScene(String sceneCode);

    /**
     * 根据模板ID获取模板
     * @param templateId 模板ID
     * @return 模板
     */
    PromptTemplateDTO getTemplateById(Long templateId);

    /**
     * 渲染模板
     * @param templateId 模板ID
     * @param params 变量参数
     * @return 渲染后的完整提示词（包含自动优化指令）
     */
    String renderTemplate(Long templateId, Map<String, String> params);

    /**
     * 自动优化用户输入的原始提示词
     * @param prompt 原始提示词
     * @param scene 场景
     * @return 优化后的提示词
     */
    String optimizePrompt(String prompt, AiSceneEnum scene);

    /**
     * 新增/修改自定义模板
     * @param template 模板信息
     * @return 是否成功
     */
    boolean saveTemplate(PromptTemplateDTO template);

    /**
     * 删除模板
     * @param templateId 模板ID
     * @return 是否成功
     */
    boolean deleteTemplate(Long templateId);
}
