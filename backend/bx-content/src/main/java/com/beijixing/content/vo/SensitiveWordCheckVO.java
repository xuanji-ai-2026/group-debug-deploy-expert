package com.beijixing.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 违禁词检测结果VO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "违禁词检测结果")
public class SensitiveWordCheckVO {

    @Schema(description = "是否包含违禁词")
    private Boolean hasSensitive;

    @Schema(description = "风险等级: 0-无风险 1-低风险 2-中风险 3-高风险")
    private Integer riskLevel;

    @Schema(description = "风险等级名称")
    private String riskLevelName;

    @Schema(description = "检测到的违禁词列表")
    private java.util.List<String> sensitiveWords;

    @Schema(description = "处理建议")
    private String suggestion;

    @Schema(description = "检测耗时(ms)")
    private Long checkTime;
}
