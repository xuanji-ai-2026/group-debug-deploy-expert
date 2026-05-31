package com.beijixing.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("nurturing_strategy")
public class NurturingStrategy {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String strategyName;
    private String platformCode;
    private Long accountId;
    private Integer dailyLikeCount;
    private Integer dailyFollowCount;
    private Integer dailyCommentCount;
    private Integer dailyViewCount;
    private Integer dailyPublishCount;
    private Integer dailyDmCount;
    private String executeTimeSlots;
    private String followTags;
    private Integer enabled;
    private Long userId;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Integer getDailyLikeCount() { return dailyLikeCount; }
    public void setDailyLikeCount(Integer dailyLikeCount) { this.dailyLikeCount = dailyLikeCount; }
    public Integer getDailyFollowCount() { return dailyFollowCount; }
    public void setDailyFollowCount(Integer dailyFollowCount) { this.dailyFollowCount = dailyFollowCount; }
    public Integer getDailyCommentCount() { return dailyCommentCount; }
    public void setDailyCommentCount(Integer dailyCommentCount) { this.dailyCommentCount = dailyCommentCount; }
    public Integer getDailyViewCount() { return dailyViewCount; }
    public void setDailyViewCount(Integer dailyViewCount) { this.dailyViewCount = dailyViewCount; }
    public Integer getDailyPublishCount() { return dailyPublishCount; }
    public void setDailyPublishCount(Integer dailyPublishCount) { this.dailyPublishCount = dailyPublishCount; }
    public Integer getDailyDmCount() { return dailyDmCount; }
    public void setDailyDmCount(Integer dailyDmCount) { this.dailyDmCount = dailyDmCount; }
    public String getExecuteTimeSlots() { return executeTimeSlots; }
    public void setExecuteTimeSlots(String executeTimeSlots) { this.executeTimeSlots = executeTimeSlots; }
    public String getFollowTags() { return followTags; }
    public void setFollowTags(String followTags) { this.followTags = followTags; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
