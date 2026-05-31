package com.beijixing.social.crawl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论抓取配置选项
 *
 * 用于控制抓取行为、深度、过滤条件等
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlOptions {

    // ====== 分页配置 ======
    @Builder.Default
    private int pageSize = 20;                  // 每页数量 (10-100, 默认20)
    @Builder.Default
    private int maxPages = 50;                  // 最大页数 (防止无限翻页)
    @Builder.Default
    private int maxTotalComments = 5000;        // 最大总评论数限制

    // ====== 深度配置 ======
    @Builder.Default
    private boolean includeReplies = true;      // 是否包含二级评论(回复)
    @Builder.Default
    private int replyDepth = 2;                 // 回复层级深度 (1-3)
    @Builder.Default
    private boolean includeSubComments = true;  // 是否包含子评论

    // ====== 时间过滤 ======
    private String startTime;                   // 开始时间 (ISO格式: 2026-01-01T00:00:00)
    private String endTime;                     // 结束时间

    // ====== 排序方式 ======
    @Builder.Default
    private SortType sortType = SortType.TIME_DESC;  // 排序类型
    private String sortField;                   // 自定义排序字段 (部分平台支持)

    // ====== 过滤条件 ======
    @Builder.Default
    private long minLikeCount = 0;              // 最小点赞数过滤
    private String keywordFilter;               // 关键词过滤 (只保留含此关键词的评论)
    private String excludeKeywordFilter;        // 排除关键词 (排除含此关键词的评论)
    @Builder.Default
    private boolean onlyVerifiedUsers = false;  // 只获取认证用户评论
    @Builder.Default
    private boolean excludeAuthorComments = false; // 排除作者自己的评论

    // ====== 性能控制 ======
    @Builder.Default
    private int requestIntervalMs = 200;         // API请求间隔 (毫秒, 防频率限制)
    @Builder.Default
    private int timeoutSeconds = 30;            // 单次请求超时时间 (秒)
    @Builder.Default
    private int maxRetryCount = 3;              // 失败重试次数
    @Builder.Default
    private boolean enableRateLimiting = true;  // 是否启用自动限流保护

    // ====== 数据处理 ======
    @Builder.Default
    private boolean autoDeduplication = true;   // 自动去重
    @Builder.Default
    private boolean anonymizeUsers = true;      // 用户信息脱敏
    @Builder.Default
    private boolean extractEmojis = true;       // 提取Emoji表情
    @Builder.Default
    private boolean removeMentions = true;      // 移除@用户名标记

    // ====== 输出格式 ======
    @Builder.Default
    private OutputFormat outputFormat = OutputFormat.JSON;  // 输出格式
    @Builder.Default
    private boolean includeMetadata = true;     // 包含元数据 (平台特定字段)

    /**
     * 获取默认配置
     */
    public static CrawlOptions defaults() {
        return new CrawlOptions();
    }

    /**
     * 验证配置参数合法性
     */
    public void validate() {
        if (pageSize < 10 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize必须在10-100之间");
        }
        if (maxPages < 1 || maxPages > 100) {
            throw new IllegalArgumentException("maxPages必须在1-100之间");
        }
        if (maxTotalComments < 1 || maxTotalComments > 100000) {
            throw new IllegalArgumentException("maxTotalComments必须在1-100000之间");
        }
        if (replyDepth < 1 || replyDepth > 5) {
            throw new IllegalArgumentException("replyDepth必须在1-5之间");
        }
        if (requestIntervalMs < 50 || requestIntervalMs > 10000) {
            throw new IllegalArgumentException("requestIntervalMs必须在50-10000ms之间");
        }
        if (timeoutSeconds < 5 || timeoutSeconds > 120) {
            throw new IllegalArgumentException("timeoutSeconds必须在5-120秒之间");
        }
        if (maxRetryCount < 0 || maxRetryCount > 10) {
            throw new IllegalArgumentException("maxRetryCount必须在0-10之间");
        }
    }

    /**
     * 排序类型枚举
     */
    public enum SortType {
        TIME_DESC("按时间倒序"),          // 最新优先
        TIME_ASC("按时间正序"),            // 最旧优先
        LIKES_DESC("按点赞数倒序"),        // 热门优先
        LIKES_ASC("按点赞数正序"),
        REPLY_COUNT_DESC("按回复数倒序");   // 讨论度优先

        private final String description;
        SortType(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    /**
     * 输出格式枚举
     */
    public enum OutputFormat {
        JSON("JSON格式"),
        CSV("CSV格式"),
        EXCEL("Excel格式"),
        DATABASE("直接入库");

        private final String description;
        OutputFormat(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    @Override
    public String toString() {
        return "CrawlOptions{" +
                "pageSize=" + pageSize +
                ", maxPages=" + maxPages +
                ", maxTotalComments=" + maxTotalComments +
                ", includeReplies=" + includeReplies +
                ", requestIntervalMs=" + requestIntervalMs +
                ", sortType=" + sortType +
                '}';
    }
}