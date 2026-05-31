package com.beijixing.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容版本历史表
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("content_version")
public class ContentVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 版本ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 内容ID */
    private Long contentId;

    /** 版本号 */
    private Integer version;

    /** 版本标题 */
    private String title;

    /** 版本内容 */
    private String content;

    /** 版本摘要 */
    private String summary;

    /** 版本封面 */
    private String coverImage;

    /** 版本标签 */
    private String tags;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作类型: 1-创建 2-编辑 3-发布 4-撤回 */
    private Integer operationType;

    /** 版本备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
