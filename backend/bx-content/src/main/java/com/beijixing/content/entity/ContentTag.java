package com.beijixing.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容标签表
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("content_tag")
public class ContentTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标签ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称 */
    private String name;

    /** 标签别名 */
    private String alias;

    /** 标签描述 */
    private String description;

    /** 标签颜色 */
    private String color;

    /** 父标签ID */
    private Long parentId;

    /** 使用次数 */
    private Integer usageCount;

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
