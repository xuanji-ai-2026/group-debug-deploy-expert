package com.beijixing.social.crawl.adapter;

import com.beijixing.social.crawl.model.CommentCrawlResult;
import com.beijixing.social.crawl.model.CrawlOptions;

/**
 * 平台评论抓取适配器接口 (Strategy Pattern)
 *
 * 每个社交平台需要实现此接口，提供统一的评论抓取能力
 *
 * 支持平台:
 * - DOUYIN (抖音) - DouyinCommentAdapter
 * - XIAOHONGSHU (小红书) - XiaohongshuCommentAdapter
 * - KUAISHOU (快手) - KuaishouCommentAdapter
 * - WEIBO (微博) - WeiboCommentAdapter
 * - BILIBILI (B站) - BilibiliCommentAdapter
 * - YOUTUBE (YouTube) - YouTubeCommentAdapter
 * - TIKTOK (国际版抖音) - TiktokCommentAdapter
 * - TWITTER/X (推特) - TwitterCommentAdapter
 * - INSTAGRAM (Instagram) - InstagramCommentAdapter
 * - REDDIT (Reddit) - RedditCommentAdapter
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
public interface PlatformCommentAdapter {

    /**
     * 获取平台代码
     */
    String getPlatformCode();

    /**
     * 获取平台显示名称
     */
    String getPlatformName();

    /**
     * 抓取指定目标的评论（核心方法）
     *
     * @param targetId 目标ID (视频ID/笔记ID/BV号等)
     * @param accessToken 用户授权的Access Token
     * @param options 抓取配置选项
     * @return 评论抓取结果
     */
    CommentCrawlResult crawlComments(String targetId, String accessToken, CrawlOptions options);

    /**
     * 从URL抓取评论 (自动解析目标ID)
     *
     * @param url 目标URL
     * @param accessToken Access Token
     * @param options 抓取配置
     * @return 抓取结果
     */
    default CommentCrawlResult crawlFromUrl(String url, String accessToken, CrawlOptions options) {
        throw new UnsupportedOperationException("此平台不支持从URL直接抓取");
    }

    /**
     * 验证Token是否有效且有足够权限
     *
     * @param accessToken Access Token
     * @return true=有效, false=无效或权限不足
     */
    default boolean validateToken(String accessToken) {
        return true;
    }

    /**
     * 获取API调用频率限制信息
     *
     * @return 频率限制描述 (如: "100次/分钟")
     */
    default String getRateLimitInfo() {
        return "未设置频率限制";
    }

    /**
     * 获取支持的功能特性列表
     *
     * @return 特性列表 (如: ["分页", "二级评论", "时间过滤", "点赞排序"])
     */
    default java.util.List<String> getSupportedFeatures() {
        return java.util.Collections.singletonList("基础评论获取");
    }
}
