package com.beijixing.social.crawl.engine.risk;

import com.beijixing.social.crawl.engine.CrawlTaskContext;
import com.beijixing.social.crawl.engine.RiskControlAction;

public class BanDetectionRule extends AbstractRiskControlRule {

    public BanDetectionRule(String ruleId, String platformCode, String ruleName,
                             String ruleType, int priority) {
        super(ruleId, platformCode, ruleName, ruleType, priority);
        this.setActionOnTrigger(RiskControlAction.SWITCH_PROXY);
    }

    @Override
    public boolean evaluate(CrawlTaskContext context) {
        if (context.getLastResponse() == null) {
            return false;
        }

        String responseStr = context.getLastResponse().toJSONString();
        
        if (isBanResponse(responseStr)) {
            System.out.println("[BanDetectionRule] 检测到封禁响应: platform=" + getPlatformCode());
            return true;
        }

        if (context.getFailCount() >= 3 && 
            context.getSuccessCount() > 0 && 
            (double)context.getFailCount() / context.getRequestCount() > 0.5) {
            System.out.println("[BanDetectionRule] 失败率过高: fails=" + context.getFailCount());
            return true;
        }

        return false;
    }

    private boolean isBanResponse(String response) {
        String lowerResponse = response.toLowerCase();
        
        return lowerResponse.contains("ban") ||
               lowerResponse.contains("forbidden") ||
               lowerResponse.contains("403") ||
               lowerResponse.contains("账号被封") ||
               lowerResponse.contains("access denied") ||
               lowerResponse.contains("请求过于频繁");
    }
}
