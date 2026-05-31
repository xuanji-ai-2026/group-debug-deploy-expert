package com.beijixing.social.crawl.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("social_comment")
public class SocialComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("platform_code")
    private String platformCode;

    @TableField("content_id")
    private String contentId;

    @TableField("content_type")
    private String contentType;

    @TableField("author_id")
    private String authorId;

    @TableField("author_name")
    private String authorName;

    @TableField("author_avatar")
    private String authorAvatar;

    @TableField("comment_id")
    private String commentId;

    @TableField("parent_comment_id")
    private String parentCommentId;

    @TableField("comment_text")
    private String commentText;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("reply_count")
    private Integer replyCount;

    @TableField("publish_time")
    private LocalDateTime publishTime;

    @TableField("is_reply")
    private Boolean isReply;

    @TableField("reply_to_user_id")
    private String replyToUserId;

    @TableField("reply_to_user_name")
    private String replyToUserName;

    @TableField("user_follower_count")
    private Integer userFollowerCount;

    @TableField("user_following_count")
    private Integer userFollowingCount;

    @TableField("user_like_count")
    private Integer userLikeCount;

    @TableField("user_verified")
    private Boolean userVerified;

    @TableField("user_location")
    private String userLocation;

    @TableField("has_phone_contact")
    private Boolean hasPhoneContact;

    @TableField("has_wechat_contact")
    private Boolean hasWechatContact;

    @TableField("extracted_phone")
    private String extractedPhone;

    @TableField("extracted_wechat")
    private String extractedWechat;

    @TableField("ai_intent_score")
    private Integer aiIntentScore;

    @TableField("ai_intent_level")
    private String aiIntentLevel;

    @TableField("ai_intent_tags")
    private String aiIntentTags;

    @TableField("ai_analysis_result")
    private String aiAnalysisResult;

    @TableField("is_high_intent")
    private Boolean isHighIntent;

    @TableField("lead_generated")
    private Boolean leadGenerated;

    @TableField("generated_lead_id")
    private Long generatedLeadId;

    @TableField("message_sent")
    private Boolean messageSent;

    @TableField("message_template_id")
    private Long messageTemplateId;

    @TableField("crawl_source")
    private String crawlSource;

    @TableField("crawl_task_id")
    private Long crawlTaskId;

    @TableField("raw_data")
    private String rawData;

    @TableField("status")
    private Integer status;

    @TableField("error_msg")
    private String errorMsg;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setUserFollowerCount(Integer userFollowerCount) { this.userFollowerCount = userFollowerCount; }
    public Integer getUserFollowerCount() { return userFollowerCount; }
    public void setUserFollowingCount(Integer userFollowingCount) { this.userFollowingCount = userFollowingCount; }
    public Integer getUserFollowingCount() { return userFollowingCount; }
    public void setUserLikeCount(Integer userLikeCount) { this.userLikeCount = userLikeCount; }
    public Integer getUserLikeCount() { return userLikeCount; }
    public void setCommentId(String commentId) { this.commentId = commentId; }
    public String getCommentId() { return commentId; }
    public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }
    public String getParentCommentId() { return parentCommentId; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public String getCommentText() { return commentText; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getLikeCount() { return likeCount; }
    public void setCrawlTaskId(Long crawlTaskId) { this.crawlTaskId = crawlTaskId; }
    public Long getCrawlTaskId() { return crawlTaskId; }
    public Integer getAiIntentScore() { return aiIntentScore; }
    public void setAiIntentScore(Integer aiIntentScore) { this.aiIntentScore = aiIntentScore; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorName() { return authorName; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setCrawlSource(String crawlSource) { this.crawlSource = crawlSource; }
    public String getCrawlSource() { return crawlSource; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    public String getRawData() { return rawData; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorId() { return authorId; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getPlatformCode() { return platformCode; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public String getContentId() { return contentId; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentType() { return contentType; }
    public void setIsReply(Boolean isReply) { this.isReply = isReply; }
    public Boolean getIsReply() { return isReply; }
    public void setHasPhoneContact(Boolean hasPhoneContact) { this.hasPhoneContact = hasPhoneContact; }
    public Boolean getHasPhoneContact() { return hasPhoneContact; }
    public void setExtractedPhone(String extractedPhone) { this.extractedPhone = extractedPhone; }
    public String getExtractedPhone() { return extractedPhone; }
    public void setHasWechatContact(Boolean hasWechatContact) { this.hasWechatContact = hasWechatContact; }
    public Boolean getHasWechatContact() { return hasWechatContact; }
    public void setExtractedWechat(String extractedWechat) { this.extractedWechat = extractedWechat; }
    public String getExtractedWechat() { return extractedWechat; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getStatus() { return status; }
    public void setUserVerified(Boolean userVerified) { this.userVerified = userVerified; }
    public Boolean getUserVerified() { return userVerified; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }
    public Integer getReplyCount() { return replyCount; }
    public void setAiIntentLevel(String aiIntentLevel) { this.aiIntentLevel = aiIntentLevel; }
    public String getAiIntentLevel() { return aiIntentLevel; }
    public void setAiIntentTags(String aiIntentTags) { this.aiIntentTags = aiIntentTags; }
    public String getAiIntentTags() { return aiIntentTags; }
    public void setAiAnalysisResult(String aiAnalysisResult) { this.aiAnalysisResult = aiAnalysisResult; }
    public String getAiAnalysisResult() { return aiAnalysisResult; }
    public void setIsHighIntent(Boolean isHighIntent) { this.isHighIntent = isHighIntent; }
    public Boolean getIsHighIntent() { return isHighIntent; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isHasPhoneContact() { return hasPhoneContact != null ? hasPhoneContact : false; }
    public boolean isHasWechatContact() { return hasWechatContact != null ? hasWechatContact : false; }
    public Boolean getLeadGenerated() { return leadGenerated; }
    public void setLeadGenerated(Boolean leadGenerated) { this.leadGenerated = leadGenerated; }
    public Long getGeneratedLeadId() { return generatedLeadId; }
    public void setGeneratedLeadId(Long generatedLeadId) { this.generatedLeadId = generatedLeadId; }
    public void setReplyToUserId(String replyToUserId) { this.replyToUserId = replyToUserId; }
    public String getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserName(String replyToUserName) { this.replyToUserName = replyToUserName; }
    public String getReplyToUserName() { return replyToUserName; }
    public void setUserLocation(String userLocation) { this.userLocation = userLocation; }
    public String getUserLocation() { return userLocation; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Boolean getMessageSent() { return messageSent; }
    public void setMessageSent(Boolean messageSent) { this.messageSent = messageSent; }
    public Long getMessageTemplateId() { return messageTemplateId; }
    public void setMessageTemplateId(Long messageTemplateId) { this.messageTemplateId = messageTemplateId; }
}
