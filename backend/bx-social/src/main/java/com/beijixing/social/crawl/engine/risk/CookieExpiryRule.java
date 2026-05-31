package com.beijixing.social.crawl.engine.risk;

import com.beijixing.social.crawl.engine.CrawlTaskContext;
import com.beijixing.social.crawl.engine.RiskControlAction;

public class CookieExpiryRule extends AbstractRiskControlRule {

    public CookieExpiryRule(String ruleId, String platformCode, String ruleName,
                             String ruleType, int priority) {
        super(ruleId, platformCode, ruleName, ruleType, priority);
        this.setActionOnTrigger(RiskControlAction.ROTATE_ACCOUNT);
    }

    @Override
    public boolean evaluate(CrawlTaskContext context) {
        if (context.getCookie() == null || context.getCookie().isEmpty()) {
            System.out.println("[CookieExpiryRule] Cookie为空");
            return true;
        }

        long timeSinceStart = java.time.Duration.between(
                context.getStartTime(), 
                java.time.LocalDateTime.now()
        ).toMinutes();

        if (timeSinceStart > 10) {
            System.out.println("[CookieExpiryRule] Cookie可能已过期: 运行时间=" + timeSinceStart + "分钟");
            return true;
        }

        if (context.getRequestCount() > 50) {
            System.out.println("[CookieExpiryRule] 请求数过多，可能触发Cookie过期: count=" + context.getRequestCount());
            return true;
        }

        return false;
    }
}
