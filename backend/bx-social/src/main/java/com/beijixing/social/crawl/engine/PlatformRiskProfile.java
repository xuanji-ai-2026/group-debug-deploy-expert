package com.beijixing.social.crawl.engine;

import java.time.LocalDateTime;

public class PlatformRiskProfile {

    private String platformCode;
    private String platformName;
    private LocalDateTime lastUpdated;
    private LocalDateTime lastRuleChangeDetected;
    private int currentRiskLevel;
    private double requestSuccessRate;
    private long avgResponseTimeMs;
    private int activeRulesCount;
    private String signatureAlgorithmVersion;
    private String cookieExpiryPolicy;
    private int recommendedMaxRequestsPerMinute;
    private int recommendedMinDelayMs;
    private int banThreshold;
    private boolean requiresProxyRotation;
    private boolean requiresAccountRotation;
    private String detectionMechanism;
    private String knownLimitations;
    private String mitigationStrategies;

    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    public LocalDateTime getLastRuleChangeDetected() { return lastRuleChangeDetected; }
    public void setLastRuleChangeDetected(LocalDateTime lastRuleChangeDetected) { this.lastRuleChangeDetected = lastRuleChangeDetected; }
    public int getCurrentRiskLevel() { return currentRiskLevel; }
    public void setCurrentRiskLevel(int currentRiskLevel) { this.currentRiskLevel = currentRiskLevel; }
    public double getRequestSuccessRate() { return requestSuccessRate; }
    public void setRequestSuccessRate(double requestSuccessRate) { this.requestSuccessRate = requestSuccessRate; }
    public long getAvgResponseTimeMs() { return avgResponseTimeMs; }
    public void setAvgResponseTimeMs(long avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }
    public int getActiveRulesCount() { return activeRulesCount; }
    public void setActiveRulesCount(int activeRulesCount) { this.activeRulesCount = activeRulesCount; }
    public String getSignatureAlgorithmVersion() { return signatureAlgorithmVersion; }
    public void setSignatureAlgorithmVersion(String signatureAlgorithmVersion) { this.signatureAlgorithmVersion = signatureAlgorithmVersion; }
    public String getCookieExpiryPolicy() { return cookieExpiryPolicy; }
    public void setCookieExpiryPolicy(String cookieExpiryPolicy) { this.cookieExpiryPolicy = cookieExpiryPolicy; }
    public int getRecommendedMaxRequestsPerMinute() { return recommendedMaxRequestsPerMinute; }
    public void setRecommendedMaxRequestsPerMinute(int recommendedMaxRequestsPerMinute) { this.recommendedMaxRequestsPerMinute = recommendedMaxRequestsPerMinute; }
    public int getRecommendedMinDelayMs() { return recommendedMinDelayMs; }
    public void setRecommendedMinDelayMs(int recommendedMinDelayMs) { this.recommendedMinDelayMs = recommendedMinDelayMs; }
    public int getBanThreshold() { return banThreshold; }
    public void setBanThreshold(int banThreshold) { this.banThreshold = banThreshold; }
    public boolean isRequiresProxyRotation() { return requiresProxyRotation; }
    public void setRequiresProxyRotation(boolean requiresProxyRotation) { this.requiresProxyRotation = requiresProxyRotation; }
    public boolean isRequiresAccountRotation() { return requiresAccountRotation; }
    public void setRequiresAccountRotation(boolean requiresAccountRotation) { this.requiresAccountRotation = requiresAccountRotation; }
    public String getDetectionMechanism() { return detectionMechanism; }
    public void setDetectionMechanism(String detectionMechanism) { this.detectionMechanism = detectionMechanism; }
    public String getKnownLimitations() { return knownLimitations; }
    public void setKnownLimitations(String knownLimitations) { this.knownLimitations = knownLimitations; }
    public String getMitigationStrategies() { return mitigationStrategies; }
    public void setMitigationStrategies(String mitigationStrategies) { this.mitigationStrategies = mitigationStrategies; }

    public static PlatformRiskProfile createDefault(String platformCode, String platformName) {
        PlatformRiskProfile profile = new PlatformRiskProfile();
        profile.setPlatformCode(platformCode);
        profile.setPlatformName(platformName);
        profile.setLastUpdated(LocalDateTime.now());
        profile.setCurrentRiskLevel(3);
        profile.setRequestSuccessRate(95.0);
        profile.setAvgResponseTimeMs(500);
        profile.setActiveRulesCount(5);
        profile.setRecommendedMaxRequestsPerMinute(30);
        profile.setRecommendedMinDelayMs(2000);
        profile.setBanThreshold(10);
        profile.setRequiresProxyRotation(true);
        profile.setRequiresAccountRotation(true);
        return profile;
    }
}
