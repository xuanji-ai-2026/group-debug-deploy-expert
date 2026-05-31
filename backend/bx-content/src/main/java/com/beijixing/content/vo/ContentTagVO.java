package com.beijixing.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容标签VO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "内容标签")
public class ContentTagVO {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "标签别名")
    private String alias;

    @Schema(description = "标签描述")
    private String description;

    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "父标签ID")
    private Long parentId;

    @Schema(description = "父标签名称")
    private String parentName;

    @Schema(description = "使用次数")
    private Integer usageCount;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态: 0-禁用 1-启用")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
