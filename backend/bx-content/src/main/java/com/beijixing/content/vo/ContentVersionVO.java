package com.beijixing.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容版本VO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "内容版本")
public class ContentVersionVO {

    @Schema(description = "版本ID")
    private Long id;

    @Schema(description = "内容ID")
    private Long contentId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "版本标题")
    private String title;

    @Schema(description = "版本摘要")
    private String summary;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "操作类型: 1-创建 2-编辑 3-发布 4-撤回")
    private Integer operationType;

    @Schema(description = "操作类型名称")
    private String operationTypeName;

    @Schema(description = "版本备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
