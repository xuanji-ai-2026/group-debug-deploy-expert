package com.beijixing.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置参数实体
 * 对应数据库表: sys_config
 *
 * 功能：SM-001 参数配置（系统参数、动态配置）
 *
 * @author bx-system
 */
@Data
@TableName("sys_config")
public class SysConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置分组（如：system、business、security）
     */
    private String groupCode;

    /**
     * 配置项Key（唯一）
     */
    private String configKey;

    /**
     * 配置项名称
     */
    private String configName;

    /**
     * 配置项值
     */
    private String configValue;

    /**
     * 配置类型：string、number、boolean、json
     */
    private String configType;

    /**
     * 是否内置：0-否，1-是（内置不可删除）
     */
    private Integer builtIn;

    /**
     * 描述说明
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 删除标记：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
