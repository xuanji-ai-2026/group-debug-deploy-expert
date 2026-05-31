package com.beijixing.social.crawl.engine.risk;

import com.beijixing.social.crawl.engine.RiskControlAction;
import com.beijixing.social.crawl.engine.RiskControlRule;

public abstract class AbstractRiskControlRule implements RiskControlRule {

    protected String ruleId;
    protected String platformCode;
    protected String ruleName;
    protected String ruleType;
    protected int priority;
    protected boolean active;
    protected RiskControlAction actionOnTrigger;

    protected AbstractRiskControlRule(String ruleId, String platformCode, String ruleName,
                                       String ruleType, int priority) {
        this.ruleId = ruleId;
        this.platformCode = platformCode;
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.priority = priority;
        this.active = true;
        this.actionOnTrigger = RiskControlAction.DELAY_AND_RETRY;
    }

    @Override
    public String getRuleId() { return ruleId; }

    @Override
    public String getPlatformCode() { return platformCode; }

    @Override
    public String getRuleName() { return ruleName; }

    @Override
    public String getRuleType() { return ruleType; }

    @Override
    public int getPriority() { return priority; }

    @Override
    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    @Override
    public RiskControlAction getActionOnTrigger() { return actionOnTrigger; }

    public void setActionOnTrigger(RiskControlAction action) { this.actionOnTrigger = action; }
}
