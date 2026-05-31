package com.beijixing.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容实体类 - 核心内容表
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("content")
public class Content implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 内容ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 内容标题 */
    private String title;

    /** 内容摘要 */
    private String summary;

    /** 内容正文 (支持Markdown) */
    private String content;

    /** 内容类型: 1-文章 2-图文 3-视频 4-短内容 */
    private Integer contentType;

    /** 内容状态: 0-草稿 1-审核中 2-已发布 3-已撤回 4-已删除 */
    private Integer status;

    /** 发布状态: 0-未发布 1-发布成功 2-发布失败 3-发布中 */
    private Integer publishStatus;

    /** 作者ID */
    private Long authorId;

    /** 作者名称 */
    private String authorName;

    /** 所属栏目/分类ID */
    private Long categoryId;

    /** 封面图片 */
    private String coverImage;

    /** 视频URL (contentType=3时使用) */
    private String videoUrl;

    /** 作者名称 */
    private String author;

    /** 图片URL列表 (JSON数组格式) */
    private String imageUrls;

    /** 内容标签JSON数组 */
    private String tags;

    /** 阅读量 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 分享数 */
    private Integer shareCount;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 计划发布时间 (定时发布) */
    private LocalDateTime scheduledTime;

    /** 是否置顶 */
    private Boolean isTop;

    /** 排序权重 */
    private Integer sortOrder;

    /** 来源平台: 1-原创 2-转载 3-投稿 */
    private Integer sourceType;

    /** 原文链接 */
    private String sourceUrl;

    /** 当前版本号 */
    private Integer version;

    /** 是否开启评论 */
    private Boolean allowComment;

    /** SEO标题 */
    private String seoTitle;

    /** SEO关键词 */
    private String seoKeywords;

    /** SEO描述 */
    private String seoDescription;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
