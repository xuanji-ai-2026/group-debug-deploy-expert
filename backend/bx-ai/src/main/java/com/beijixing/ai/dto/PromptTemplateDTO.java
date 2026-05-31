package com.beijixing.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 提示词模板DTO
 * 内置全行业预设模板，支持自定义扩展
 */
@Data
public class PromptTemplateDTO {

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 所属场景
     */
    private String sceneCode;

    /**
     * 模板内容，变量用{{变量名}}表示
     */
    private String content;

    /**
     * 变量列表
     */
    private List<String> variables;

    /**
     * 优化指令，自动追加到提示词末尾，提升生成效果
     */
    private String optimizeInstruction;

    /**
     * 适用行业
     */
    private List<String> industries;

    /**
     * 是否是系统预设模板
     */
    private Boolean isSystem;

    /**
     * 状态：0=禁用,1=启用
     */
    private Integer status;

    /**
     * 渲染模板
     * @param params 变量参数
     * @return 渲染后的完整提示词
     */
    public String render(Map<String, String> params) {
        String result = content;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        // 自动追加优化指令
        if (optimizeInstruction != null && !optimizeInstruction.isEmpty()) {
            result += "\n\n优化要求：" + optimizeInstruction;
        }
        return result;
    }
}
