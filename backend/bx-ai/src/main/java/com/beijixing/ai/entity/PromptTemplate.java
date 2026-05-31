package com.beijixing.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 提示词模板实体
 * 持久化到MySQL数据库
 */
@Data
@TableName("ai_prompt_template")
public class PromptTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 变量列表，JSON数组存储
     */
    private String variables;

    /**
     * 优化指令，自动追加到提示词末尾
     */
    private String optimizeInstruction;

    /**
     * 适用行业，JSON数组存储
     */
    private String industries;

    /**
     * 是否是系统预设模板：0=自定义,1=系统预设
     */
    private Integer isSystem;

    /**
     * 状态：0=禁用,1=启用
     */
    private Integer status;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
