package com.beijixing.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容查询请求DTO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "内容查询请求")
public class ContentQueryDTO {

    @Schema(description = "关键词 (标题/摘要)")
    private String keyword;

    @Schema(description = "内容类型: 1-文章 2-图文 3-视频 4-短内容")
    private Integer contentType;

    @Schema(description = "内容状态: 0-草稿 1-审核中 2-已发布 3-已撤回")
    private Integer status;

    @Schema(description = "发布状态: 0-未发布 1-发布成功 2-发布失败 3-发布中")
    private Integer publishStatus;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "当前页")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "排序字段")
    private String sortField;

    @Schema(description = "排序方式: asc/desc")
    private String sortOrder = "desc";
}
