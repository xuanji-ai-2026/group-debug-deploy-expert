package com.beijixing.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 内容审核请求DTO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "内容审核请求")
public class ContentAuditDTO {

    @Schema(description = "内容ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contentId;

    @Schema(description = "审核结果: 1-通过 2-不通过 3-需修改", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer auditResult;

    @Schema(description = "审核意见")
    private String auditOpinion;

    @Schema(description = "违禁词列表")
    private List<String> sensitiveWords;
}
