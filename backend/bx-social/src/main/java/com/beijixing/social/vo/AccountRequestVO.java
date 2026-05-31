package com.beijixing.social.vo;

import jakarta.validation.constraints.NotBlank;

public class AccountRequestVO {
    private Long id;

    @NotBlank(message = "平台代码不能为空")
    private String platformCode;

    private String nickname;
    private String avatarUrl;
    private Long groupId;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
