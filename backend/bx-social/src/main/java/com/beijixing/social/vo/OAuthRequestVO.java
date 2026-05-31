package com.beijixing.social.vo;

import jakarta.validation.constraints.NotBlank;

public class OAuthRequestVO {
    @NotBlank(message = "平台代码不能为空")
    private String platformCode;
    private String redirectUri;
    private String state;
    private String scope;

    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
