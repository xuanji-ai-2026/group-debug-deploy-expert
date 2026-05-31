package com.beijixing.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 内容标签DTO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "内容标签请求")
public class ContentTagDTO {

    @Schema(description = "标签ID (更新时必填)")
    private Long id;

    @NotBlank(message = "标签名称不能为空")
    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "标签别名")
    private String alias;

    @Schema(description = "标签描述")
    private String description;

    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "父标签ID")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态: 0-禁用 1-启用")
    private Integer status;
}
