package com.beijixing.social.crawl.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("crawl_task")
public class CrawlTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_name")
    private String taskName;

    @TableField("task_type")
    private String taskType;

    @TableField("platform_code")
    private String platformCode;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private String targetId;

    @TableField("target_url")
    private String targetUrl;

    @TableField("keywords")
    private String keywords;

    @TableField("keyword_logic")
    private String keywordLogic;

    @TableField("filter_conditions")
    private String filterConditions;

    @TableField("max_crawl_count")
    private Integer maxCrawlCount;

    @TableField("crawl_interval_seconds")
    private Integer crawlIntervalSeconds;

    @TableField("total_comments_found")
    private Integer totalCommentsFound;

    @TableField("high_intent_count")
    private Integer highIntentCount;

    @TableField("leads_generated")
    private Integer leadsGenerated;

    @TableField("messages_sent")
    private Integer messagesSent;

    @TableField("status")
    private Integer status;

    @TableField("progress_percent")
    private Integer progressPercent;

    @TableField("last_crawl_time")
    private LocalDateTime lastCrawlTime;

    @TableField("next_crawl_time")
    private LocalDateTime nextCrawlTime;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("created_by")
    private Long createdBy;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public void setStatus(Integer status) { this.status = status; }
    public Integer getStatus() { return status; }
    public void setTotalCommentsFound(Integer totalCommentsFound) { this.totalCommentsFound = totalCommentsFound; }
    public Integer getTotalCommentsFound() { return totalCommentsFound; }
    public void setHighIntentCount(Integer highIntentCount) { this.highIntentCount = highIntentCount; }
    public Integer getHighIntentCount() { return highIntentCount; }
    public void setLeadsGenerated(Integer leadsGenerated) { this.leadsGenerated = leadsGenerated; }
    public Integer getLeadsGenerated() { return leadsGenerated; }
    public void setMessagesSent(Integer messagesSent) { this.messagesSent = messagesSent; }
    public Integer getMessagesSent() { return messagesSent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public Integer getProgressPercent() { return progressPercent; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Integer getMaxCrawlCount() { return maxCrawlCount; }
    public void setMaxCrawlCount(Integer maxCrawlCount) { this.maxCrawlCount = maxCrawlCount; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public String getFilterConditions() { return filterConditions; }
    public void setFilterConditions(String filterConditions) { this.filterConditions = filterConditions; }
    public String getKeywordLogic() { return keywordLogic; }
    public void setKeywordLogic(String keywordLogic) { this.keywordLogic = keywordLogic; }
    public Integer getCrawlIntervalSeconds() { return crawlIntervalSeconds; }
    public void setCrawlIntervalSeconds(Integer crawlIntervalSeconds) { this.crawlIntervalSeconds = crawlIntervalSeconds; }
    public LocalDateTime getLastCrawlTime() { return lastCrawlTime; }
    public void setLastCrawlTime(LocalDateTime lastCrawlTime) { this.lastCrawlTime = lastCrawlTime; }
    public LocalDateTime getNextCrawlTime() { return nextCrawlTime; }
    public void setNextCrawlTime(LocalDateTime nextCrawlTime) { this.nextCrawlTime = nextCrawlTime; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
