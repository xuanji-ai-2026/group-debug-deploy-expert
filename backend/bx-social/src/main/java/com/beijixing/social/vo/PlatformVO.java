package com.beijixing.social.vo;

public class PlatformVO {
    private Long id;
    private String platformCode;
    private String platformName;
    private String iconUrl;
    private String authorizeUrl;
    private Integer enabled;
    private Integer accountCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public String getAuthorizeUrl() { return authorizeUrl; }
    public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Integer getAccountCount() { return accountCount; }
    public void setAccountCount(Integer accountCount) { this.accountCount = accountCount; }
}
