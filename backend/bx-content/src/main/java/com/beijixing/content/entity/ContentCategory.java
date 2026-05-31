package com.beijixing.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容分类/栏目表
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("content_category")
public class ContentCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类描述 */
    private String description;

    /** 父分类ID */
    private Long parentId;

    /** 分类层级 */
    private Integer level;

    /** 分类路径 */
    private String path;

    /** 分类图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
