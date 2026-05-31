package com.beijixing.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容审核记录表
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("content_audit_record")
public class ContentAuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审核ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 内容ID */
    private Long contentId;

    /** 审核类型: 1-AI审核 2-人工审核 */
    private Integer auditType;

    /** 审核结果: 0-待审核 1-通过 2-不通过 3-需修改 */
    private Integer auditResult;

    /** 审核意见 */
    private String auditOpinion;

    /** 违禁词检测结果 */
    private String sensitiveResult;

    /** AI审核分数 (0-100) */
    private Integer aiScore;

    /** 审核人ID */
    private Long auditorId;

    /** 审核人名称 */
    private String auditorName;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
