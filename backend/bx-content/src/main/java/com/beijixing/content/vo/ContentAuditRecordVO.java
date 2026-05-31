package com.beijixing.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容审核记录VO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "内容审核记录")
public class ContentAuditRecordVO {

    @Schema(description = "审核ID")
    private Long id;

    @Schema(description = "内容ID")
    private Long contentId;

    @Schema(description = "审核类型: 1-AI审核 2-人工审核")
    private Integer auditType;

    @Schema(description = "审核类型名称")
    private String auditTypeName;

    @Schema(description = "审核结果: 0-待审核 1-通过 2-不通过 3-需修改")
    private Integer auditResult;

    @Schema(description = "审核结果名称")
    private String auditResultName;

    @Schema(description = "审核意见")
    private String auditOpinion;

    @Schema(description = "违禁词检测结果")
    private String sensitiveResult;

    @Schema(description = "AI审核分数")
    private Integer aiScore;

    @Schema(description = "审核人名称")
    private String auditorName;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
