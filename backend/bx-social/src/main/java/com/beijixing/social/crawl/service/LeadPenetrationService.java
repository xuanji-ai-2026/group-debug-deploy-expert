package com.beijixing.social.crawl.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.bxlead.dto.LeadSaveDTO;
import com.beijixing.bxlead.entity.Lead;
import com.beijixing.bxlead.enums.LeadSource;
import com.beijixing.bxlead.enums.LeadStatus;
import com.beijixing.bxlead.mapper.LeadMapper;
import com.beijixing.bxlead.service.LeadService;
import com.beijixing.social.crawl.entity.CrawlTask;
import com.beijixing.social.crawl.entity.SocialComment;
import com.beijixing.social.crawl.mapper.CrawlTaskMapper;
import com.beijixing.social.crawl.mapper.SocialCommentMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LeadPenetrationService {

    private static final Logger log = LoggerFactory.getLogger(LeadPenetrationService.class);

    private final SocialCommentMapper commentMapper;
    private final LeadMapper leadMapper;
    private final CrawlTaskMapper crawlTaskMapper;
    private final LeadService leadService;
    private final AiIntentAnalysisV2Service aiAnalysisService;

    public PenetrationResult generateLeadsFromComments(Long crawlTaskId, 
                                                        LeadGenerationCriteria criteria) {
        log.info("开始商机穿透: taskId={}, criteria={}", crawlTaskId, criteria);

        LambdaQueryWrapper<SocialComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SocialComment::getCrawlTaskId, crawlTaskId)
               .eq(SocialComment::getIsHighIntent, true)
               .eq(SocialComment::getLeadGenerated, false)
               .eq(SocialComment::getDeleted, 0)
               .orderByDesc(SocialComment::getAiIntentScore);

        if (criteria.getMinScore() != null) {
            wrapper.ge(SocialComment::getAiIntentScore, criteria.getMinScore());
        }

        List<SocialComment> highIntentComments = commentMapper.selectList(wrapper);
        
        log.info("发现高意向评论: {}条", highIntentComments.size());

        PenetrationResult result = new PenetrationResult();
        result.setTotalHighIntentComments(highIntentComments.size());
        result.setStartTime(LocalDateTime.now());
        result.setGeneratedLeads(new ArrayList<>());
        result.setSkippedComments(new ArrayList<>());

        int generatedCount = 0;
        int skippedCount = 0;

        for (SocialComment comment : highIntentComments) {
            try {
                boolean shouldGenerate = shouldGenerateLead(comment, criteria);
                
                if (shouldGenerate) {
                    Lead lead = generateSingleLeadFromComment(comment, crawlTaskId, criteria);
                    
                    if (lead != null) {
                        result.getGeneratedLeads().add(lead);
                        
                        comment.setLeadGenerated(true);
                        comment.setGeneratedLeadId(lead.getId());
                        commentMapper.updateById(comment);
                        
                        generatedCount++;
                        
                        updateCrawlTaskStats(crawlTaskId, null, null, 1, null);
                    }
                } else {
                    result.getSkippedComments().add(comment.getId());
                    skippedCount++;
                }

            } catch (Exception e) {
                log.error("生成商机失败: commentId={}, error={}", comment.getId(), e.getMessage(), e);
                result.addError(comment.getId(), e.getMessage());
                skippedCount++;
            }
        }

        result.setGeneratedCount(generatedCount);
        result.setSkippedCount(skippedCount);
        result.setEndTime(LocalDateTime.now());
        result.setSuccess(true);

        log.info("商机穿透完成: taskId={}, 生成商机={}, 跳过={}", 
                crawlTaskId, generatedCount, skippedCount);

        return result;
    }

    @Async("leadGenerationExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void asyncGenerateLeadsFromComment(Long commentId, Long crawlTaskId) {
        SocialComment comment = commentMapper.selectById(commentId);
        if (comment == null || Boolean.TRUE.equals(comment.getLeadGenerated())) {
            return;
        }

        try {
            AiIntentAnalysisV2Service.IntentAnalysisResult analysis = 
                    JSON.parseObject(comment.getAiAnalysisResult(), 
                            AiIntentAnalysisV2Service.IntentAnalysisResult.class);
            
            if (analysis == null || !analysis.isHighIntent()) {
                return;
            }

            LeadGenerationCriteria defaultCriteria = new LeadGenerationCriteria();
            defaultCriteria.setAutoAssign(true);
            defaultCriteria.setGenerateFollowUpTask(true);
            
            generateSingleLeadFromComment(comment, crawlTaskId, defaultCriteria);
            
        } catch (Exception e) {
            log.error("异步生成商机失败: commentId={}", commentId, e);
        }
    }

    private Lead generateSingleLeadFromComment(SocialComment comment, Long crawlTaskId, 
                                                LeadGenerationCriteria criteria) {
        log.info("从评论生成商机: commentId={}, score={}, level={}", 
                comment.getCommentId(), comment.getAiIntentScore(), comment.getAiIntentLevel());

        AiIntentAnalysisV2Service.IntentAnalysisResult analysis = null;
        try {
            analysis = JSON.parseObject(comment.getAiAnalysisResult(), 
                    AiIntentAnalysisV2Service.IntentAnalysisResult.class);
        } catch (Exception e) {
            log.warn("解析AI分析结果失败，重新分析");
            analysis = aiAnalysisService.analyzeComment(comment);
        }

        LeadSaveDTO dto = new LeadSaveDTO();

        String title = buildLeadTitle(comment, analysis);
        dto.setTitle(title);

        dto.setSource(LeadSource.SOCIAL.getCode());
        dto.setChannel(comment.getPlatformCode() + "_评论抓取");

        dto.setCustomerName(comment.getAuthorName());
        
        if (comment.getExtractedPhone() != null && !comment.getExtractedPhone().isEmpty()) {
            dto.setCustomerPhone(comment.getExtractedPhone());
        }

        StringBuilder requirementDesc = new StringBuilder();
        requirementDesc.append("【原始评论】\n").append(comment.getCommentText()).append("\n\n");
        
        if (analysis != null) {
            requirementDesc.append("【AI意向分析】\n");
            requirementDesc.append("- 意向评分：").append(analysis.getFinalScore()).append("分\n");
            requirementDesc.append("- 意向等级：").append(analysis.getLevel()).append("级\n");
            
            if (analysis.getAiSummary() != null) {
                requirementDesc.append("- 用户意图：").append(analysis.getAiSummary()).append("\n");
            }
            
            if (!analysis.getUrgencySignals().isEmpty()) {
                requirementDesc.append("- 紧急信号：").append(String.join(", ", analysis.getUrgencySignals())).append("\n");
            }
            
            if (analysis.getPainPoints() != null && !analysis.getPainPoints().isEmpty()) {
                requirementDesc.append("- 痛点：").append(String.join(", ", analysis.getPainPoints())).append("\n");
            }
            
            if (analysis.getCompetitorMentioned() != null) {
                requirementDesc.append("- 竞品提及：").append(analysis.getCompetitorMentioned()).append("\n");
            }
            
            if (analysis.getEstimatedBudget() != null) {
                requirementDesc.append("- 预估预算：").append(analysis.getEstimatedBudget()).append("元\n");
            }
            
            if (analysis.getPurchaseStage() != null) {
                requirementDesc.append("- 购买阶段：").append(analysis.getPurchaseStage()).append("\n");
            }
            
            if (analysis.getAiSuggestedAction() != null) {
                requirementDesc.append("- 建议动作：").append(analysis.getAiSuggestedAction()).append("\n");
            }
            
            if (analysis.getAllTags() != null && !analysis.getAllTags().isEmpty()) {
                requirementDesc.append("- 标签：").append(String.join(", ", analysis.getAllTags())).append("\n");
            }
        }

        requirementDesc.append("\n【用户信息】\n");
        requirementDesc.append("- 平台：").append(getPlatformName(comment.getPlatformCode())).append("\n");
        requirementDesc.append("- 用户ID：").append(comment.getAuthorId()).append("\n");
        
        if (comment.getUserFollowerCount() != null) {
            requirementDesc.append("- 粉丝数：").append(comment.getUserFollowerCount()).append("\n");
        }
        
        if (comment.getUserVerified() != null && comment.getUserVerified()) {
            requirementDesc.append("- 认证用户：是\n");
        }
        
        if (comment.getLikeCount() != null) {
            requirementDesc.append("- 评论点赞：").append(comment.getLikeCount()).append("\n");
        }
        
        requirementDesc.append("- 评论时间：").append(comment.getPublishTime()).append("\n");
        requirementDesc.append("- 评论链接：").append(buildCommentUrl(comment)).append("\n");

        dto.setRequirementDesc(requirementDesc.toString());

        if (analysis != null) {
            dto.setLevel(analysis.getLevel());
            dto.setIntentScore(analysis.getFinalScore());
        } else {
            dto.setLevel(comment.getAiIntentLevel() != null ? comment.getAiIntentLevel() : "C");
            dto.setIntentScore(comment.getAiIntentScore() != null ? comment.getAiIntentScore() : 50);
        }

        dto.setStatus(LeadStatus.NEW.getCode());
        dto.setRemark("来源：社媒评论抓取 | 抓取任务ID：" + crawlTaskId + " | 评论ID：" + comment.getCommentId());

        Long leadId = leadService.createLead(dto);

        Lead lead = leadMapper.selectById(leadId);
        
        if (criteria.isAutoAssign()) {
            autoAssignLead(lead, criteria);
        }

        if (criteria.isGenerateFollowUpTask()) {
            generateInitialFollowUpTask(lead, comment, analysis);
        }

        log.info("商机生成成功: leadId={}, title={}, level={}", 
                leadId, title, dto.getLevel());

        return lead;
    }

    private String buildLeadTitle(SocialComment comment, 
                                   AiIntentAnalysisV2Service.IntentAnalysisResult analysis) {
        StringBuilder title = new StringBuilder();
        
        title.append("[").append(getPlatformName(comment.getPlatformCode())).append("]");
        
        if (analysis != null && analysis.getLevel() != null) {
            switch (analysis.getLevel()) {
                case "A":
                    title.append("[A级-超高意向]");
                    break;
                case "B":
                    title.append("[B级-高意向]");
                    break;
                case "C":
                    title.append("[C级-中意向]");
                    break;
                case "D":
                    title.append("[D级-低意向]");
                    break;
                default:
                    title.append("[E级-待观察]");
            }
        }

        if (comment.getExtractedPhone() != null) {
            title.append("[留电话]");
        }
        if (comment.getExtractedWechat() != null) {
            title.append("[留微信]");
        }

        title.append(comment.getAuthorName());

        if (analysis != null && analysis.getAiSummary() != null) {
            String summary = analysis.getAiSummary();
            if (summary.length() > 20) {
                summary = summary.substring(0, 20) + "...";
            }
            title.append("-").append(summary);
        }

        return title.toString();
    }

    private boolean shouldGenerateLead(SocialComment comment, LeadGenerationCriteria criteria) {
        if (comment.getAiIntentScore() == null) {
            return false;
        }

        if (criteria.getMinScore() != null && comment.getAiIntentScore() < criteria.getMinScore()) {
            return false;
        }

        if (criteria.getRequiredLevels() != null && !criteria.getRequiredLevels().isEmpty()) {
            if (comment.getAiIntentLevel() == null || 
                !criteria.getRequiredLevels().contains(comment.getAiIntentLevel())) {
                return false;
            }
        }

        if (criteria.isRequireContactInfo()) {
            if (Boolean.TRUE.equals(comment.getHasPhoneContact()) ||
                Boolean.TRUE.equals(comment.getHasWechatContact())) {
                return true;
            }
            return false;
        }

        if (criteria.getMinFollowerCount() != null) {
            if (comment.getUserFollowerCount() == null ||
                comment.getUserFollowerCount() < criteria.getMinFollowerCount()) {
                return false;
            }
        }

        return true;
    }

    private void autoAssignLead(Lead lead, LeadGenerationCriteria criteria) {
        try {
            if ("ROUND_ROBIN".equals(criteria.getAssignmentStrategy())) {
                leadService.autoAssignLead(lead.getId());
            } else if ("REGIONAL".equals(criteria.getAssignmentStrategy())) {
                leadService.autoAssignLead(lead.getId());
            }
        } catch (Exception e) {
            log.warn("自动分配商机失败: leadId={}", lead.getId(), e);
        }
    }

    private void generateInitialFollowUpTask(Lead lead, SocialComment comment,
                                              AiIntentAnalysisV2Service.IntentAnalysisResult analysis) {
        log.info("为商机生成初始跟进任务: leadId={}", lead.getId());
    }

    private String getPlatformName(String platformCode) {
        switch (platformCode.toUpperCase()) {
            case "DOUYIN": return "抖音";
            case "XIAOHONGSHU": return "小红书";
            case "KUAISHOU": return "快手";
            case "WEIBO": return "微博";
            case "BILIBILI": return "B站";
            default: return platformCode;
        }
    }

    private String buildCommentUrl(SocialComment comment) {
        switch (comment.getPlatformCode().toUpperCase()) {
            case "DOUYIN":
                return "https://www.douyin.com/video/" + comment.getContentId();
            case "XIAOHONGSHU":
                return "https://www.xiaohongshu.com/explore/" + comment.getContentId();
            case "KUAISHOU":
                return "https://www.kuaishou.com/short-video/" + comment.getContentId();
            case "WEIBO":
                return "https://weibo.com/" + comment.getContentId();
            default:
                return comment.getContentId();
        }
    }

    private void updateCrawlTaskStats(Long taskId, Integer totalFound, Integer highIntent, 
                                       Integer leads, Integer messages) {
        CrawlTask task = crawlTaskMapper.selectById(taskId);
        if (task != null) {
            if (totalFound != null) task.setTotalCommentsFound(totalFound);
            if (highIntent != null) task.setHighIntentCount(highIntent);
            if (leads != null) task.setLeadsGenerated(
                    (task.getLeadsGenerated() != null ? task.getLeadsGenerated() : 0) + leads
            );
            if (messages != null) task.setMessagesSent(messages);
            crawlTaskMapper.updateById(task);
        }
    }

    public static class PenetrationResult {
        private int totalHighIntentComments;
        private int generatedCount;
        private int skippedCount;
        private Integer totalLeads;
        private Double conversionRate;
        private List<Lead> generatedLeads;
        private List<Long> skippedComments;
        private Map<Long, String> errors;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean success;

        public PenetrationResult() {
            this.generatedLeads = new ArrayList<>();
            this.skippedComments = new ArrayList<>();
            this.errors = new LinkedHashMap<>();
        }

        public int getTotalHighIntentComments() { return totalHighIntentComments; }
        public void setTotalHighIntentComments(int totalHighIntentComments) { this.totalHighIntentComments = totalHighIntentComments; }
        public int getGeneratedCount() { return generatedCount; }
        public void setGeneratedCount(int generatedCount) { this.generatedCount = generatedCount; }
        public int getSkippedCount() { return skippedCount; }
        public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
        public Integer getTotalLeads() { return totalLeads; }
        public void setTotalLeads(Integer totalLeads) { this.totalLeads = totalLeads; }
        public Double getConversionRate() { return conversionRate; }
        public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }
        public List<Lead> getGeneratedLeads() { return generatedLeads; }
        public void setGeneratedLeads(List<Lead> generatedLeads) { this.generatedLeads = generatedLeads; }
        public List<Long> getSkippedComments() { return skippedComments; }
        public void setSkippedComments(List<Long> skippedComments) { this.skippedComments = skippedComments; }
        public Map<Long, String> getErrors() { return errors; }
        public void setErrors(Map<Long, String> errors) { this.errors = errors; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public void addError(Long commentId, String errorMessage) {
            this.errors.put(commentId, errorMessage);
        }
    }

    public static class LeadGenerationCriteria {
        private Integer minScore;
        private Integer maxLeads;
        private List<String> requiredLevels;
        private boolean requireContactInfo;
        private Integer minFollowerCount;
        private boolean autoAssign;
        private String assignmentStrategy;
        private boolean generateFollowUpTask;
        private Long tenantId;
        private Long createdBy;

        public Integer getMinScore() { return minScore; }
        public void setMinScore(Integer minScore) { this.minScore = minScore; }
        public void setMinIntentScore(Integer minScore) { this.minScore = minScore; }
        public Integer getMaxLeads() { return maxLeads; }
        public void setMaxLeads(Integer maxLeads) { this.maxLeads = maxLeads; }
        public List<String> getRequiredLevels() { return requiredLevels; }
        public void setRequiredLevels(List<String> requiredLevels) { this.requiredLevels = requiredLevels; }
        public boolean isRequireContactInfo() { return requireContactInfo; }
        public void setRequireContactInfo(boolean requireContactInfo) { this.requireContactInfo = requireContactInfo; }
        public void setRequireContact(boolean requireContactInfo) { this.requireContactInfo = requireContactInfo; }
        public Integer getMinFollowerCount() { return minFollowerCount; }
        public void setMinFollowerCount(Integer minFollowerCount) { this.minFollowerCount = minFollowerCount; }
        public boolean isAutoAssign() { return autoAssign; }
        public void setAutoAssign(boolean autoAssign) { this.autoAssign = autoAssign; }
        public String getAssignmentStrategy() { return assignmentStrategy; }
        public void setAssignmentStrategy(String assignmentStrategy) { this.assignmentStrategy = assignmentStrategy; }
        public boolean isGenerateFollowUpTask() { return generateFollowUpTask; }
        public void setGenerateFollowUpTask(boolean generateFollowUpTask) { this.generateFollowUpTask = generateFollowUpTask; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    }
}
