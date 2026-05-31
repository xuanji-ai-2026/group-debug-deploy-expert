package com.beijixing.social.crawl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.social.crawl.entity.SocialComment;
import com.beijixing.social.crawl.mapper.SocialCommentMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class CommentFilterEngine {

    private static final Logger log = LoggerFactory.getLogger(CommentFilterEngine.class);

    private final SocialCommentMapper commentMapper;
    private final AiIntentAnalysisV2Service aiAnalysisService;

    public FilterResult filterComments(Long crawlTaskId, FilterCriteria criteria) {
        log.info("开始筛选评论: taskId={}, criteria={}", crawlTaskId, criteria);

        LambdaQueryWrapper<SocialComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SocialComment::getCrawlTaskId, crawlTaskId)
               .eq(SocialComment::getDeleted, 0);

        if (criteria.getMinIntentScore() != null) {
            wrapper.ge(SocialComment::getAiIntentScore, criteria.getMinIntentScore());
        }
        
        if (criteria.getMaxIntentScore() != null) {
            wrapper.le(SocialComment::getAiIntentScore, criteria.getMaxIntentScore());
        }

        if (criteria.getIntentLevel() != null && !criteria.getIntentLevel().isEmpty()) {
            wrapper.in(SocialComment::getAiIntentLevel, criteria.getIntentLevel());
        }

        if (criteria.getOnlyHighIntent() != null && criteria.getOnlyHighIntent()) {
            wrapper.eq(SocialComment::getIsHighIntent, true);
        }

        if (criteria.getOnlyWithContact() != null && criteria.getOnlyWithContact()) {
            wrapper.and(w -> w.eq(SocialComment::getHasPhoneContact, true)
                             .or()
                             .eq(SocialComment::getHasWechatContact, true));
        }

        if (criteria.getOnlyUnprocessed() != null && criteria.getOnlyUnprocessed()) {
            wrapper.eq(SocialComment::getLeadGenerated, false);
        }

        if (criteria.getKeywords() != null && !criteria.getKeywords().isEmpty()) {
            wrapper.and(w -> {
                for (int i = 0; i < criteria.getKeywords().size(); i++) {
                    String keyword = criteria.getKeywords().get(i);
                    if (i == 0) {
                        w.like(SocialComment::getCommentText, keyword);
                    } else {
                        w.or(wp -> wp.like(SocialComment::getCommentText, keyword));
                    }
                }
            });
        }

        if (criteria.getExcludeKeywords() != null && !criteria.getExcludeKeywords().isEmpty()) {
            for (String keyword : criteria.getExcludeKeywords()) {
                wrapper.notLike(SocialComment::getCommentText, keyword);
            }
        }

        if (criteria.getMinLikeCount() != null) {
            wrapper.ge(SocialComment::getLikeCount, criteria.getMinLikeCount());
        }

        if (criteria.getPlatformCode() != null) {
            wrapper.eq(SocialComment::getPlatformCode, criteria.getPlatformCode());
        }

        wrapper.orderByDesc(SocialComment::getAiIntentScore)
               .orderByDesc(SocialComment::getLikeCount)
               .orderByDesc(SocialComment::getPublishTime);

        List<SocialComment> allComments = commentMapper.selectList(wrapper);

        List<SocialComment> filtered = allComments.stream()
                .filter(c -> applyAdvancedFilters(c, criteria))
                .collect(Collectors.toList());

        if (criteria.getLimit() != null && filtered.size() > criteria.getLimit()) {
            filtered = filtered.subList(0, criteria.getLimit());
        }

        log.info("评论筛选完成: 总数={}, 筛选后={}", allComments.size(), filtered.size());

        return FilterResult.builder()
                .totalCount(allComments.size())
                .filteredCount(filtered.size())
                .comments(filtered)
                .statistics(calculateStatistics(filtered))
                .build();
    }

    private boolean applyAdvancedFilters(SocialComment comment, FilterCriteria criteria) {
        if (criteria.getMinFollowerCount() != null && 
            comment.getUserFollowerCount() != null && 
            comment.getUserFollowerCount() < criteria.getMinFollowerCount()) {
            return false;
        }

        if (criteria.getRequireVerified() != null && criteria.getRequireVerified() &&
            (comment.getUserVerified() == null || !comment.getUserVerified())) {
            return false;
        }

        if (criteria.getSince() != null && 
            comment.getPublishTime() != null && 
            comment.getPublishTime().isBefore(criteria.getSince())) {
            return false;
        }

        return true;
    }

    public void analyzeAndFilterBatch(Long crawlTaskId) {
        log.info("开始批量分析评论: taskId={}", crawlTaskId);

        List<SocialComment> unanalyzed = findUnanalyzedComments(crawlTaskId, 100);

        int processed = 0;
        int highIntent = 0;

        for (SocialComment comment : unanalyzed) {
            try {
                AiIntentAnalysisV2Service.IntentAnalysisResult result = 
                        aiAnalysisService.analyzeComment(comment);
                
                commentMapper.updateById(comment);
                processed++;

                if (result.isHighIntent()) {
                    highIntent++;
                }

                if (processed % 10 == 0) {
                    log.info("已分析: {}/{}, 高意向: {}", processed, unanalyzed.size(), highIntent);
                }

            } catch (Exception e) {
                log.error("分析评论失败: commentId={}", comment.getId(), e);
            }
        }

        log.info("批量分析完成: taskId={}, 已分析={}, 高意向={}", crawlTaskId, processed, highIntent);
    }

    private List<SocialComment> findUnanalyzedComments(Long crawlTaskId, int limit) {
        LambdaQueryWrapper<SocialComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SocialComment::getCrawlTaskId, crawlTaskId)
               .isNull(SocialComment::getAiIntentScore)
               .eq(SocialComment::getStatus, 1)
               .eq(SocialComment::getDeleted, 0)
               .last("LIMIT " + limit);
        return commentMapper.selectList(wrapper);
    }

    private CommentStatistics calculateStatistics(List<SocialComment> comments) {
        if (comments.isEmpty()) {
            return new CommentStatistics();
        }

        CommentStatistics stats = new CommentStatistics();

        stats.totalCount = comments.size();
        stats.highIntentCount = (int) comments.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsHighIntent()))
                .count();
        stats.withPhoneCount = (int) comments.stream()
                .filter(c -> Boolean.TRUE.equals(c.getHasPhoneContact()))
                .count();
        stats.withWechatCount = (int) comments.stream()
                .filter(c -> Boolean.TRUE.equals(c.getHasWechatContact()))
                .count();
        stats.avgIntentScore = comments.stream()
                .filter(c -> c.getAiIntentScore() != null)
                .mapToInt(SocialComment::getAiIntentScore)
                .average()
                .orElse(0.0);

        Map<String, Long> platformDistribution = comments.stream()
                .collect(Collectors.groupingBy(
                        SocialComment::getPlatformCode,
                        Collectors.counting()
                ));
        stats.platformDistribution = platformDistribution;

        Map<String, Long> levelDistribution = comments.stream()
                .filter(c -> c.getAiIntentLevel() != null)
                .collect(Collectors.groupingBy(
                        SocialComment::getAiIntentLevel,
                        Collectors.counting()
                ));
        stats.levelDistribution = levelDistribution;

        Map<String, Long> intentTagStats = new HashMap<>();
        for (SocialComment comment : comments) {
            if (comment.getAiIntentTags() != null) {
                String[] tags = comment.getAiIntentTags().replace("[", "").replace("]", "").split(",");
                for (String tag : tags) {
                    tag = tag.trim().replaceAll("\"", "");
                    intentTagStats.merge(tag, 1L, Long::sum);
                }
            }
        }
        stats.topTags = intentTagStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return stats;
    }

    public static class FilterCriteria {
        private Integer minIntentScore;
        private Integer maxIntentScore;
        private List<String> intentLevel;
        private Boolean onlyHighIntent;
        private Boolean onlyWithContact;
        private Boolean onlyUnprocessed;
        private List<String> keywords;
        private List<String> excludeKeywords;
        private Integer minLikeCount;
        private String platformCode;
        private Integer minFollowerCount;
        private Boolean requireVerified;
        private java.time.LocalDateTime since;
        private Integer limit;

        public Integer getMinIntentScore() { return minIntentScore; }
        public void setMinIntentScore(Integer minIntentScore) { this.minIntentScore = minIntentScore; }
        public Integer getMaxIntentScore() { return maxIntentScore; }
        public void setMaxIntentScore(Integer maxIntentScore) { this.maxIntentScore = maxIntentScore; }
        public List<String> getIntentLevel() { return intentLevel; }
        public void setIntentLevel(List<String> intentLevel) { this.intentLevel = intentLevel; }
        public Boolean getOnlyHighIntent() { return onlyHighIntent; }
        public void setOnlyHighIntent(Boolean onlyHighIntent) { this.onlyHighIntent = onlyHighIntent; }
        public Boolean getOnlyWithContact() { return onlyWithContact; }
        public void setOnlyWithContact(Boolean onlyWithContact) { this.onlyWithContact = onlyWithContact; }
        public Boolean getOnlyUnprocessed() { return onlyUnprocessed; }
        public void setOnlyUnprocessed(Boolean onlyUnprocessed) { this.onlyUnprocessed = onlyUnprocessed; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        public List<String> getExcludeKeywords() { return excludeKeywords; }
        public void setExcludeKeywords(List<String> excludeKeywords) { this.excludeKeywords = excludeKeywords; }
        public Integer getMinLikeCount() { return minLikeCount; }
        public void setMinLikeCount(Integer minLikeCount) { this.minLikeCount = minLikeCount; }
        public String getPlatformCode() { return platformCode; }
        public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
        public Integer getMinFollowerCount() { return minFollowerCount; }
        public void setMinFollowerCount(Integer minFollowerCount) { this.minFollowerCount = minFollowerCount; }
        public Boolean getRequireVerified() { return requireVerified; }
        public void setRequireVerified(Boolean requireVerified) { this.requireVerified = requireVerified; }
        public java.time.LocalDateTime getSince() { return since; }
        public void setSince(java.time.LocalDateTime since) { this.since = since; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }

    public static class FilterResult {
        private int totalCount;
        private int filteredCount;
        private List<SocialComment> comments;
        private CommentStatistics statistics;

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getFilteredCount() { return filteredCount; }
        public void setFilteredCount(int filteredCount) { this.filteredCount = filteredCount; }
        public List<SocialComment> getComments() { return comments; }
        public void setComments(List<SocialComment> comments) { this.comments = comments; }
        public CommentStatistics getStatistics() { return statistics; }
        public void setStatistics(CommentStatistics statistics) { this.statistics = statistics; }

        public static class Builder {
            private FilterResult result = new FilterResult();
            public Builder totalCount(int count) { result.totalCount = count; return this; }
            public Builder filteredCount(int count) { result.filteredCount = count; return this; }
            public Builder comments(List<SocialComment> comments) { result.comments = comments; return this; }
            public Builder statistics(CommentStatistics stats) { result.statistics = stats; return this; }
            public FilterResult build() { return result; }
        }
        public static Builder builder() { return new Builder(); }
    }

    public static class CommentStatistics {
        private int totalCount;
        private int highIntentCount;
        private int withPhoneCount;
        private int withWechatCount;
        private double avgIntentScore;
        private Map<String, Long> platformDistribution;
        private Map<String, Long> levelDistribution;
        private Map<String, Long> topTags;

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getHighIntentCount() { return highIntentCount; }
        public void setHighIntentCount(int highIntentCount) { this.highIntentCount = highIntentCount; }
        public int getWithPhoneCount() { return withPhoneCount; }
        public void setWithPhoneCount(int withPhoneCount) { this.withPhoneCount = withPhoneCount; }
        public int getWithWechatCount() { return withWechatCount; }
        public void setWithWechatCount(int withWechatCount) { this.withWechatCount = withWechatCount; }
        public double getAvgIntentScore() { return avgIntentScore; }
        public void setAvgIntentScore(double avgIntentScore) { this.avgIntentScore = avgIntentScore; }
        public Map<String, Long> getPlatformDistribution() { return platformDistribution; }
        public void setPlatformDistribution(Map<String, Long> platformDistribution) { this.platformDistribution = platformDistribution; }
        public Map<String, Long> getLevelDistribution() { return levelDistribution; }
        public void setLevelDistribution(Map<String, Long> levelDistribution) { this.levelDistribution = levelDistribution; }
        public Map<String, Long> getTopTags() { return topTags; }
        public void setTopTags(Map<String, Long> topTags) { this.topTags = topTags; }
    }
}
