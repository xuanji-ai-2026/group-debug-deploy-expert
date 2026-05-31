package com.beijixing.social.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oauth")
public class OAuthConfig {

    private PlatformConfig douyin = new PlatformConfig();
    private PlatformConfig xiaohongshu = new PlatformConfig();
    private PlatformConfig weixin = new PlatformConfig();
    private PlatformConfig kuaishou = new PlatformConfig();

    public static class PlatformConfig {
        private String appKey;
        private String appSecret;
        private String appId;
        private String authorizeUrl;
        private String tokenUrl;
        private String userInfoUrl;
        private String refreshUrl;

        public String getAppKey() { return appKey; }
        public void setAppKey(String appKey) { this.appKey = appKey; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAuthorizeUrl() { return authorizeUrl; }
        public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }
        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
        public String getUserInfoUrl() { return userInfoUrl; }
        public void setUserInfoUrl(String userInfoUrl) { this.userInfoUrl = userInfoUrl; }
        public String getRefreshUrl() { return refreshUrl; }
        public void setRefreshUrl(String refreshUrl) { this.refreshUrl = refreshUrl; }
    }

    public PlatformConfig getDouyin() { return douyin; }
    public void setDouyin(PlatformConfig douyin) { this.douyin = douyin; }
    public PlatformConfig getXiaohongshu() { return xiaohongshu; }
    public void setXiaohongshu(PlatformConfig xiaohongshu) { this.xiaohongshu = xiaohongshu; }
    public PlatformConfig getWeixin() { return weixin; }
    public void setWeixin(PlatformConfig weixin) { this.weixin = weixin; }
    public PlatformConfig getKuaishou() { return kuaishou; }
    public void setKuaishou(PlatformConfig kuaishou) { this.kuaishou = kuaishou; }

    /**
     * 获取平台配置
     */
    public PlatformConfig getPlatformConfig(String platformCode) {
        return switch (platformCode.toUpperCase()) {
            case "DOUYIN" -> douyin;
            case "XIAOHONGSHU" -> xiaohongshu;
            case "WEIXIN", "VIDEO" -> weixin;
            default -> null;
        };
    }
}
