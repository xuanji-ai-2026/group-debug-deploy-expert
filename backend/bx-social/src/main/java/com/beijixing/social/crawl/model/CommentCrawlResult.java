package com.beijixing.social.crawl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评论抓取统一结果模型
 *
 * 支持平台: 抖音、小红书、快手、微博、B站、YouTube等10+平台
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCrawlResult {

    private String taskId;                    // 任务ID
    private String platform;                  // 平台代码 (DOUYIN/XIAOHONGSHU/KUAISHOU/WEIBO/BILIBILI)
    private String targetId;                  // 目标ID (视频ID/笔记ID/BV号)
    private String targetUrl;                 // 目标URL (原始输入URL)
    private String targetTitle;               // 标题 (视频标题/笔记标题)

    private boolean success;                  // 是否成功
    private String errorCode;                 // 错误码 (null表示成功)
    private String errorMessage;              // 错误信息

    // ====== 抓取统计 ======
    @Builder.Default
    private CrawlStatistics statistics = new CrawlStatistics();

    // ====== 评论数据列表 ======
    private List<SocialComment> comments;     // 一级评论列表

    // ====== 元信息 ======
    private LocalDateTime startTime;          // 开始时间
    private LocalDateTime endTime;            // 结束时间
    private long durationMs;                  // 耗时(毫秒)
    private Map<String, Object> metadata;     // 额外元信息 (平台特定字段)

    /**
     * 评论统计信息
     */
    @Data
    public static class CrawlStatistics {
        private int totalComments;             // 总评论数 (含二级评论)
        private int topLevelComments;          // 一级评论数
        private int replyComments;             // 回复评论数
        private int pagesFetched;              // 获取的页数
        private int apiCallCount;              // API调用次数
        private int uniqueUsers;               // 独立用户数
        private long totalLikes;               // 总点赞数
        private double avgLikeCount;           // 平均点赞数
    }

    /**
     * 社交评论通用模型
     */
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialComment {
        private String commentId;              // 评论唯一ID
        private String parentId;               // 父评论ID (一级评论时为null)
        private String rootCommentId;          // 根评论ID (用于二级评论关联)
        private String content;                // 评论内容 (纯文本，已脱敏)
        private String rawContent;             // 原始内容 (含@、#等特殊标记)
        private String authorId;               // 评论者ID
        private String authorName;             // 评论者昵称
        private String authorAvatar;           // 头像URL
        private boolean isAuthorVerified;      // 是否认证用户
        private LocalDateTime publishTime;     // 发布时间
        private long likeCount;                // 点赞数
        private long replyCount;               // 回复数
        private List<String> images;           // 图片URL列表
        private Map<String, Object> extraInfo; // 平台额外信息

        // ====== 意图分析字段 (AI后处理) ======
        private Double purchaseIntentScore;    // 购买意向评分 (0-100)
        private IntentCategory intentCategory; // 意向分类
        private List<String> keywords;         // 提取的关键词
        private String sentiment;              // 情感倾向 (POSITIVE/NEGATIVE/NEUTRAL)

        public String getCommentId() { return commentId; }
        public void setCommentId(String commentId) { this.commentId = commentId; }
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        public String getRootCommentId() { return rootCommentId; }
        public void setRootCommentId(String rootCommentId) { this.rootCommentId = rootCommentId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getRawContent() { return rawContent; }
        public void setRawContent(String rawContent) { this.rawContent = rawContent; }
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getAuthorAvatar() { return authorAvatar; }
        public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
        public boolean isAuthorVerified() { return isAuthorVerified; }
        public void setAuthorVerified(boolean authorVerified) { isAuthorVerified = authorVerified; }
        public LocalDateTime getPublishTime() { return publishTime; }
        public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
        public long getLikeCount() { return likeCount; }
        public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
        public long getReplyCount() { return replyCount; }
        public void setReplyCount(long replyCount) { this.replyCount = replyCount; }
        public List<String> getImages() { return images; }
        public void setImages(List<String> images) { this.images = images; }
        public Map<String, Object> getExtraInfo() { return extraInfo; }
        public void setExtraInfo(Map<String, Object> extraInfo) { this.extraInfo = extraInfo; }
        public Double getPurchaseIntentScore() { return purchaseIntentScore; }
        public void setPurchaseIntentScore(Double purchaseIntentScore) { this.purchaseIntentScore = purchaseIntentScore; }
        public IntentCategory getIntentCategory() { return intentCategory; }
        public void setIntentCategory(IntentCategory intentCategory) { this.intentCategory = intentCategory; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        public String getSentiment() { return sentiment; }
        public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    }

    /**
     * 意向分类枚举
     */
    public enum IntentCategory {
        STRONG_PURCHASE_INTENT("强购买意向"),      // "多少钱？哪里买？"
        MODERATE_PURCHASE_INTENT("中等购买意图"),   // "看起来不错"
        WEAK_PURCHASE_INTENT("弱购买意向"),         // "收藏了"
        INQUIRY("咨询询问"),                        // "有货吗？"
        COMPARISON("比较对比"),                     // "比XX怎么样？"
        NEGATIVE_FEEDBACK("负面反馈"),              // "太贵了/质量差")
        NEUTRAL_COMMENT("中性评论"),                // "哈哈/不错"
        SPAM_OR_AD("垃圾广告");                      // "加微信买..."

        private final String description;

        IntentCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取格式化的统计摘要
     */
    public String getSummary() {
        return String.format("[%s] %s | 一级:%d | 总计:%d | 用户:%d | 耗时:%dms",
                this.platform,
                this.success ? "✅ 成功" : "❌ 失败:" + this.errorMessage,
                this.statistics.getTopLevelComments(),
                this.statistics.getTotalComments(),
                this.statistics.getUniqueUsers(),
                this.durationMs);
    }
}
