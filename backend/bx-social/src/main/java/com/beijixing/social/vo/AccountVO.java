package com.beijixing.social.vo;

import java.time.LocalDateTime;

public class AccountVO {
    private Long id;
    private String accountId;
    private String platformCode;
    private String platformName;
    private String nickname;
    private String avatarUrl;
    private String profileUrl;
    private Long fansCount;
    private Long followCount;
    private Long likeCount;
    private Integer verified;
    private Integer status;
    private String statusName;
    private Long groupId;
    private String groupName;
    private LocalDateTime lastActiveTime;
    private Integer nurturingStatus;
    private LocalDateTime tokenExpireTime;
    private Integer daysUntilExpire;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public Long getFansCount() { return fansCount; }
    public void setFansCount(Long fansCount) { this.fansCount = fansCount; }
    public Long getFollowCount() { return followCount; }
    public void setFollowCount(Long followCount) { this.followCount = followCount; }
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    public Integer getVerified() { return verified; }
    public void setVerified(Integer verified) { this.verified = verified; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public LocalDateTime getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }
    public Integer getNurturingStatus() { return nurturingStatus; }
    public void setNurturingStatus(Integer nurturingStatus) { this.nurturingStatus = nurturingStatus; }
    public LocalDateTime getTokenExpireTime() { return tokenExpireTime; }
    public void setTokenExpireTime(LocalDateTime tokenExpireTime) { this.tokenExpireTime = tokenExpireTime; }
    public Integer getDaysUntilExpire() { return daysUntilExpire; }
    public void setDaysUntilExpire(Integer daysUntilExpire) { this.daysUntilExpire = daysUntilExpire; }
}
