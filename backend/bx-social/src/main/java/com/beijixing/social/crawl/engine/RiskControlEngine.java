package com.beijixing.social.crawl.engine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface RiskControlEngine {

    List<RiskControlRule> getActiveRules(String platformCode);

    RiskControlEvaluationResult evaluateRequest(CrawlTaskContext context);

    void recordViolation(String platformCode, String ruleId, CrawlTaskContext context);

    void updateRulesFromMonitor(String platformCode, RuleUpdatePayload payload);

    boolean shouldExecuteTask(CrawlTaskContext context);

    long getRecommendedDelayMs(String platformCode);

    interface RuleUpdatePayload {
        String getPlatformCode();
        LocalDateTime getEffectiveDate();
        List<RiskControlRule> getUpdatedRules();
        String getChangeReason();

        class RuleItem {
            private String ruleId;
            private String ruleName;
            private String action;
            private boolean enabled;
            private Map<String, Object> parameters;

            public RuleItem(String ruleId, String ruleName, String action, boolean enabled, Map<String, Object> parameters) {
                this.ruleId = ruleId;
                this.ruleName = ruleName;
                this.action = action;
                this.enabled = enabled;
                this.parameters = parameters;
            }

            public String getRuleId() { return ruleId; }
            public String getRuleName() { return ruleName; }
            public String getAction() { return action; }
            public boolean isEnabled() { return enabled; }
            public Map<String, Object> getParameters() { return parameters; }
        }

        List<RuleItem> getChanges();
    }

    class RiskControlEvaluationResult {
        private boolean allowed;
        private RiskControlAction action;
        private String triggeredRuleId;
        private String message;
        private long recommendedDelayMs;

        public static RiskControlEvaluationResult allowed() {
            RiskControlEvaluationResult result = new RiskControlEvaluationResult();
            result.allowed = true;
            result.action = RiskControlAction.ALERT_ONLY;
            result.recommendedDelayMs = 0;
            return result;
        }

        public static RiskControlEvaluationResult blocked(RiskControlAction action, String ruleId, String message) {
            RiskControlEvaluationResult result = new RiskControlEvaluationResult();
            result.allowed = false;
            result.action = action;
            result.triggeredRuleId = ruleId;
            result.message = message;
            return result;
        }

        public boolean isAllowed() { return allowed; }
        public RiskControlAction getAction() { return action; }
        public String getTriggeredRuleId() { return triggeredRuleId; }
        public String getMessage() { return message; }
        public long getRecommendedDelayMs() { return recommendedDelayMs; }
        public void setRecommendedDelayMs(long delay) { this.recommendedDelayMs = delay; }
    }
}
