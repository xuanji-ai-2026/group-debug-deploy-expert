package com.beijixing.social.crawl.engine;

public interface RiskControlRule {

    String getRuleId();

    String getPlatformCode();

    String getRuleName();

    String getRuleType();

    int getPriority();

    boolean isActive();

    boolean evaluate(CrawlTaskContext context);

    RiskControlAction getActionOnTrigger();

    default String getDescription() {
        return "Risk control rule: " + getRuleName();
    }
}
