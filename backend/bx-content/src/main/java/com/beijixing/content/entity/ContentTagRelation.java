package com.beijixing.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容与标签关联表
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("content_tag_relation")
public class ContentTagRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 内容ID */
    private Long contentId;

    /** 标签ID */
    private Long tagId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
