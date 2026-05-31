package com.beijixing.social.crawl.engine.monitor;

import com.beijixing.social.crawl.engine.PlatformRiskProfile;
import com.beijixing.social.crawl.engine.RiskControlEngine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface RiskRuleMonitorService {

    void startMonitoring(String platformCode);

    void stopMonitoring(String platformCode);

    boolean isMonitoringActive(String platformCode);

    PlatformRiskProfile getCurrentRiskProfile(String platformCode);

    Map<String, PlatformRiskProfile> getAllRiskProfiles();

    RuleChangeDetectionResult detectChanges(String platformCode);

    void applyRuleUpdates(String platformCode, RuleUpdatePackage updatePackage);

    List<MonitoringEvent> getRecentEvents(String platformCode, int limit);

    interface RuleUpdatePackage {
        String getPlatformCode();
        LocalDateTime getEffectiveDate();
        String getUpdateReason();
        List<RiskControlEngine.RuleUpdatePayload.RuleItem> getChanges();
    }

    class RuleChangeDetectionResult {
        private boolean hasChanges;
        private String changeType;
        private String description;
        private double confidence;
        private LocalDateTime detectedAt;
        private List<String> affectedRules;

        public static RuleChangeDetectionResult noChange() {
            RuleChangeDetectionResult result = new RuleChangeDetectionResult();
            result.hasChanges = false;
            result.detectedAt = LocalDateTime.now();
            return result;
        }

        public static RuleChangeDetectionResult changeDetected(String type, String desc, double confidence) {
            RuleChangeDetectionResult result = new RuleChangeDetectionResult();
            result.hasChanges = true;
            result.changeType = type;
            result.description = desc;
            result.confidence = confidence;
            result.detectedAt = LocalDateTime.now();
            return result;
        }

        public boolean hasChanges() { return hasChanges; }
        public String getChangeType() { return changeType; }
        public String getDescription() { return description; }
        public double getConfidence() { return confidence; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
        public List<String> getAffectedRules() { return affectedRules; }
        public void setAffectedRules(List<String> rules) { this.affectedRules = rules; }
    }

    class MonitoringEvent {
        private String eventId;
        private String platformCode;
        private String eventType;
        private String message;
        private LocalDateTime timestamp;
        private String severity;
        private Map<String, Object> metadata;

        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public String getPlatformCode() { return platformCode; }
        public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}
