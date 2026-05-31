package com.beijixing.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容创建/更新请求DTO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "内容创建/更新请求")
public class ContentDTO {

    @Schema(description = "内容ID (更新时必填)")
    private Long id;

    @NotBlank(message = "内容标题不能为空")
    @Schema(description = "内容标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "内容摘要")
    private String summary;

    @NotBlank(message = "内容正文不能为空")
    @Schema(description = "内容正文 (支持Markdown)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @NotNull(message = "内容类型不能为空")
    @Schema(description = "内容类型: 1-文章 2-图文 3-视频 4-短内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer contentType;

    @Schema(description = "所属栏目/分类ID")
    private Long categoryId;

    @Schema(description = "封面图片")
    private String coverImage;

    @Schema(description = "内容标签列表")
    private List<String> tags;

    @Schema(description = "计划发布时间 (定时发布)")
    private LocalDateTime scheduledTime;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "来源类型: 1-原创 2-转载 3-投稿")
    private Integer sourceType;

    @Schema(description = "原文链接")
    private String sourceUrl;

    @Schema(description = "是否开启评论")
    private Boolean allowComment;

    @Schema(description = "SEO标题")
    private String seoTitle;

    @Schema(description = "SEO关键词")
    private String seoKeywords;

    @Schema(description = "SEO描述")
    private String seoDescription;

    @Schema(description = "发布平台列表: 1-微信公众号 2-微博 3-抖音 4-小红书 5-B站 6-官网")
    private List<Integer> publishPlatforms;
}
