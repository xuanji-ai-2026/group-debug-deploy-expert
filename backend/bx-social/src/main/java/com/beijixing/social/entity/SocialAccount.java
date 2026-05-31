package com.beijixing.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("social_account")
public class SocialAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号ID(平台方) */
    private String accountId;

    /** 平台代码 */
    private String platformCode;

    /** 账号昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 账号主页URL */
    private String profileUrl;

    /** 粉丝数 */
    private Long fansCount;

    /** 关注数 */
    private Long followCount;

    /** 获赞数 */
    private Long likeCount;

    /** 账号等级 */
    private Integer accountLevel;

    /** 账号认证状态: 0-未认证 1-已认证 */
    private Integer verified;

    /** 账号状态: 0-未激活 1-正常 2-异常 3-封禁 */
    private Integer status;

    /** OAuth Access Token */
    private String accessToken;

    /** Token过期时间 */
    private LocalDateTime tokenExpireTime;

    /** Refresh Token */
    private String refreshToken;

    /** Refresh Token过期时间 */
    private LocalDateTime refreshExpireTime;

    /** 分组ID */
    private Long groupId;

    /** 用户ID(北极星系统) */
    private Long userId;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveTime;

    /** 养号状态: 0-未开始 1-执行中 2-已完成 */
    private Integer nurturingStatus;

    /** 异常描述 */
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
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
    public Integer getAccountLevel() { return accountLevel; }
    public void setAccountLevel(Integer accountLevel) { this.accountLevel = accountLevel; }
    public Integer getVerified() { return verified; }
    public void setVerified(Integer verified) { this.verified = verified; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public LocalDateTime getTokenExpireTime() { return tokenExpireTime; }
    public void setTokenExpireTime(LocalDateTime tokenExpireTime) { this.tokenExpireTime = tokenExpireTime; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public LocalDateTime getRefreshExpireTime() { return refreshExpireTime; }
    public void setRefreshExpireTime(LocalDateTime refreshExpireTime) { this.refreshExpireTime = refreshExpireTime; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }
    public Integer getNurturingStatus() { return nurturingStatus; }
    public void setNurturingStatus(Integer nurturingStatus) { this.nurturingStatus = nurturingStatus; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
