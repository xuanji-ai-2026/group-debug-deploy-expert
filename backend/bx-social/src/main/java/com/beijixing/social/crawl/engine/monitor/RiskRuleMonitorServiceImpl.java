package com.beijixing.social.crawl.engine.monitor;

import com.beijixing.social.crawl.engine.PlatformRiskProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskRuleMonitorServiceImpl implements RiskRuleMonitorService {

    private final StringRedisTemplate redisTemplate;

    private final Map<String, Boolean> monitoringStatus = new ConcurrentHashMap<>();
    private final Map<String, PlatformRiskProfile> riskProfiles = new ConcurrentHashMap<>();

    @Override
    public void startMonitoring(String platformCode) {
        monitoringStatus.put(platformCode.toUpperCase(), true);
        if (!riskProfiles.containsKey(platformCode.toUpperCase())) {
            riskProfiles.put(platformCode.toUpperCase(), 
                    PlatformRiskProfile.createDefault(platformCode, getPlatformName(platformCode)));
        }
        log.info("启动风控规则监控: platform={}", platformCode);
    }

    @Override
    public void stopMonitoring(String platformCode) {
        monitoringStatus.put(platformCode.toUpperCase(), false);
        log.info("停止风控规则监控: platform={}", platformCode);
    }

    @Override
    public boolean isMonitoringActive(String platformCode) {
        return monitoringStatus.getOrDefault(platformCode.toUpperCase(), false);
    }

    @Override
    public PlatformRiskProfile getCurrentRiskProfile(String platformCode) {
        return riskProfiles.getOrDefault(platformCode.toUpperCase(), null);
    }

    @Override
    public Map<String, PlatformRiskProfile> getAllRiskProfiles() {
        return Map.copyOf(riskProfiles);
    }

    @Scheduled(fixedRate = 300000)
    public void performMonitoringCycle() {
        log.debug("执行风控规则监控周期检查");
        
        for (Map.Entry<String, Boolean> entry : monitoringStatus.entrySet()) {
            if (entry.getValue()) {
                String platformCode = entry.getKey();
                try {
                    RuleChangeDetectionResult result = detectChanges(platformCode);
                    if (result.hasChanges()) {
                        handleDetectedChanges(platformCode, result);
                    }
                } catch (Exception e) {
                    log.error("平台{}监控检测失败: {}", platformCode, e.getMessage());
                }
            }
        }
    }

    @Override
    public RuleChangeDetectionResult detectChanges(String platformCode) {
        PlatformRiskProfile currentProfile = getCurrentRiskProfile(platformCode);
        if (currentProfile == null) {
            return RuleChangeDetectionResult.noChange();
        }

        String signatureVersion = checkSignatureAlgorithmVersion(platformCode);
        String cookiePolicy = checkCookieExpiryPolicy(platformCode);
        double successRate = calculateRecentSuccessRate(platformCode);

        List<String> changes = new java.util.ArrayList<>();
        StringBuilder description = new StringBuilder();

        if (!signatureVersion.equals(currentProfile.getSignatureAlgorithmVersion())) {
            changes.add("SIGNATURE_ALGORITHM_CHANGE");
            description.append("签名算法版本变化: ").append(signatureVersion).append("; ");
        }

        if (!cookiePolicy.equals(currentProfile.getCookieExpiryPolicy())) {
            changes.add("COOKIE_POLICY_CHANGE");
            description.append("Cookie过期策略变化: ").append(cookiePolicy).append("; ");
        }

        if (Math.abs(successRate - currentProfile.getRequestSuccessRate()) > 10) {
            changes.add("SUCCESS_RATE_DROP");
            description.append(String.format("请求成功率异常: %.1f%% -> %.1f%%", 
                    currentProfile.getRequestSuccessRate(), successRate));
        }

        if (changes.isEmpty()) {
            return RuleChangeDetectionResult.noChange();
        }

        RuleChangeDetectionResult result = RuleChangeDetectionResult.changeDetected(
                "MULTIPLE_CHANGES", 
                description.toString(), 
                85.0
        );
        result.setAffectedRules(changes);

        MonitoringEvent event = new MonitoringEvent();
        event.setEventId(java.util.UUID.randomUUID().toString());
        event.setPlatformCode(platformCode);
        event.setEventType("RULE_CHANGE_DETECTED");
        event.setMessage(description.toString());
        event.setTimestamp(LocalDateTime.now());
        event.setSeverity("WARNING");

        recordMonitoringEvent(event);

        return result;
    }

    @Override
    public void applyRuleUpdates(String platformCode, RuleUpdatePackage updatePackage) {
        log.info("应用风控规则更新: platform={}, reason={}", 
                platformCode, updatePackage.getUpdateReason());

        PlatformRiskProfile profile = getCurrentRiskProfile(platformCode);
        if (profile != null) {
            profile.setLastUpdated(updatePackage.getEffectiveDate());
            profile.setLastRuleChangeDetected(LocalDateTime.now());
            
            MonitoringEvent event = new MonitoringEvent();
            event.setEventId(java.util.UUID.randomUUID().toString());
            event.setPlatformCode(platformCode);
            event.setEventType("RULE_UPDATE_APPLIED");
            event.setMessage("已应用" + updatePackage.getChanges().size() + "条规则更新: " + 
                           updatePackage.getUpdateReason());
            event.setTimestamp(LocalDateTime.now());
            event.setSeverity("INFO");
            
            recordMonitoringEvent(event);
        }
    }

    @Override
    public List<MonitoringEvent> getRecentEvents(String platformCode, int limit) {
        String key = "monitor:events:" + platformCode;
        List<String> eventJsons = redisTemplate.opsForList().range(key, 0, limit - 1);
        
        if (eventJsons == null || eventJsons.isEmpty()) {
            return List.of();
        }

        return eventJsons.stream()
                .map(json -> {
                    try {
                        return com.alibaba.fastjson2.JSON.parseObject(json, MonitoringEvent.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(e -> e != null)
                .toList();
    }

    private void handleDetectedChanges(String platformCode, RuleChangeDetectionResult result) {
        log.warn("检测到风控规则变化: platform={}, changes={}", platformCode, result.getDescription());

        PlatformRiskProfile profile = getCurrentRiskProfile(platformCode);
        if (profile != null) {
            profile.setCurrentRiskLevel(Math.min(10, profile.getCurrentRiskLevel() + 1));
            profile.setLastRuleChangeDetected(result.getDetectedAt());
        }

        log.info("发送风控规则变更告警: platform={}, hasChanges={}, affectedRules={}", 
                platformCode, result.hasChanges(), result.getAffectedRules());
    }

    private void recordMonitoringEvent(MonitoringEvent event) {
        String key = "monitor:events:" + event.getPlatformCode();
        redisTemplate.opsForList().leftPush(key, com.alibaba.fastjson2.JSON.toJSONString(event));
        redisTemplate.opsForList().trim(key, 0, 99);
    }

    private String checkSignatureAlgorithmVersion(String platformCode) {
        String key = "platform:signature_version:" + platformCode;
        return redisTemplate.opsForValue().get(key) != null ? 
               redisTemplate.opsForValue().get(key) : "v1.0";
    }

    private String checkCookieExpiryPolicy(String platformCode) {
        String key = "platform:cookie_policy:" + platformCode;
        return redisTemplate.opsForValue().get(key) != null ?
               redisTemplate.opsForValue().get(key) : "10min";
    }

    private double calculateRecentSuccessRate(String platformCode) {
        String key = "crawl:stats:success_rate:" + platformCode;
        String rateStr = redisTemplate.opsForValue().get(key);
        if (rateStr != null) {
            try {
                return Double.parseDouble(rateStr);
            } catch (NumberFormatException e) {
                return 95.0;
            }
        }
        return 95.0;
    }

    private String getPlatformName(String platformCode) {
        switch (platformCode.toUpperCase()) {
            case "DOUYIN": return "抖音";
            case "XIAOHONGSHU": return "小红书";
            case "KUAISHOU": return "快手";
            case "WEIBO": return "微博";
            case "BILIBILI": return "B站";
            default: return platformCode;
        }
    }
}
