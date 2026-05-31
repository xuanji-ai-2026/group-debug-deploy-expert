package com.beijixing.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容详情VO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "内容详情")
public class ContentVO {

    @Schema(description = "内容ID")
    private Long id;

    @Schema(description = "内容标题")
    private String title;

    @Schema(description = "内容摘要")
    private String summary;

    @Schema(description = "内容正文")
    private String content;

    @Schema(description = "内容类型: 1-文章 2-图文 3-视频 4-短内容")
    private Integer contentType;

    @Schema(description = "内容类型名称")
    private String contentTypeName;

    @Schema(description = "内容状态: 0-草稿 1-审核中 2-已发布 3-已撤回")
    private Integer status;

    @Schema(description = "内容状态名称")
    private String statusName;

    @Schema(description = "发布状态: 0-未发布 1-发布成功 2-发布失败 3-发布中")
    private Integer publishStatus;

    @Schema(description = "发布状态名称")
    private String publishStatusName;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者名称")
    private String authorName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "封面图片")
    private String coverImage;

    @Schema(description = "内容标签列表")
    private List<String> tags;

    @Schema(description = "阅读量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "分享数")
    private Integer shareCount;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "计划发布时间")
    private LocalDateTime scheduledTime;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "当前版本号")
    private Integer version;

    @Schema(description = "是否开启评论")
    private Boolean allowComment;

    @Schema(description = "SEO标题")
    private String seoTitle;

    @Schema(description = "SEO关键词")
    private String seoKeywords;

    @Schema(description = "SEO描述")
    private String seoDescription;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
