package com.beijixing.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容列表项VO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "内容列表项")
public class ContentListVO {

    @Schema(description = "内容ID")
    private Long id;

    @Schema(description = "内容标题")
    private String title;

    @Schema(description = "内容摘要")
    private String summary;

    @Schema(description = "内容类型: 1-文章 2-图文 3-视频 4-短内容")
    private Integer contentType;

    @Schema(description = "内容类型名称")
    private String contentTypeName;

    @Schema(description = "内容状态")
    private Integer status;

    @Schema(description = "内容状态名称")
    private String statusName;

    @Schema(description = "发布状态")
    private Integer publishStatus;

    @Schema(description = "发布状态名称")
    private String publishStatusName;

    @Schema(description = "作者名称")
    private String authorName;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "封面图片")
    private String coverImage;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "阅读量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "计划发布时间")
    private LocalDateTime scheduledTime;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
