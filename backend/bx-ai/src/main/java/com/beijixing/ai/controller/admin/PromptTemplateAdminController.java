package com.beijixing.ai.controller.admin;

import com.beijixing.ai.dto.PromptTemplateDTO;
import com.beijixing.ai.prompt.enums.AiSceneEnum;
import com.beijixing.ai.prompt.service.PromptTemplateService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 管理后台-提示词模板配置接口
 * 所有模板修改后实时生效，不需要重启服务
 */
@RestController
@RequestMapping("/admin/ai/prompt-template")
public class PromptTemplateAdminController {

    @Resource
    private PromptTemplateService promptTemplateService;

    /**
     * 获取场景下的模板列表
     * @param sceneCode 场景编码
     * @return 模板列表
     */
    @GetMapping("/list/{sceneCode}")
    public List<PromptTemplateDTO> listTemplates(@PathVariable String sceneCode) {
        return promptTemplateService.listTemplatesByScene(sceneCode);
    }

    /**
     * 获取模板详情
     * @param templateId 模板ID
     * @return 模板详情
     */
    @GetMapping("/{templateId}")
    public PromptTemplateDTO getTemplate(@PathVariable Long templateId) {
        return promptTemplateService.getTemplateById(templateId);
    }

    /**
     * 渲染模板
     * @param templateId 模板ID
     * @param params 变量参数
     * @return 渲染后的提示词
     */
    @PostMapping("/render/{templateId}")
    public String renderTemplate(@PathVariable Long templateId, @RequestBody Map<String, String> params) {
        return promptTemplateService.renderTemplate(templateId, params);
    }

    /**
     * 保存自定义模板
     * @param template 模板信息
     * @return 是否成功
     */
    @PostMapping("/save")
    public Boolean saveTemplate(@RequestBody PromptTemplateDTO template) {
        return promptTemplateService.saveTemplate(template);
    }

    /**
     * 删除模板
     * @param templateId 模板ID
     * @return 是否成功
     */
    @DeleteMapping("/{templateId}")
    public Boolean deleteTemplate(@PathVariable Long templateId) {
        return promptTemplateService.deleteTemplate(templateId);
    }

    /**
     * 优化提示词
     * @param prompt 原始提示词
     * @param sceneCode 场景编码
     * @return 优化后的提示词
     */
    @PostMapping("/optimize")
    public String optimizePrompt(@RequestParam String prompt, @RequestParam String sceneCode) {
        return promptTemplateService.optimizePrompt(prompt, AiSceneEnum.getByCode(sceneCode));
    }
}
