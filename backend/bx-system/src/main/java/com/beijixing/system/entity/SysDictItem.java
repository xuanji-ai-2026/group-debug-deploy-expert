package com.beijixing.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据字典项实体
 * 对应数据库表: sys_dict_item
 *
 * 功能：SM-002 字典管理
 *
 * @author bx-system
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字典项ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属字典ID
     */
    private Long dictId;

    /**
     * 字典项键值
     */
    private String itemValue;

    /**
     * 字典项标签（显示文本）
     */
    private String itemLabel;

    /**
     * 字典项类型（与字典类型一致）
     */
    private String itemType;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 默认值：0-否，1-是
     */
    private Integer isDefault;

    /**
     * 描述说明
     */
    private String description;

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
