package com.beijixing.social.crawl.engine.risk;

import com.beijixing.social.crawl.engine.CrawlTaskContext;
import com.beijixing.social.crawl.engine.RiskControlAction;

public class BehaviorAnalysisRule extends AbstractRiskControlRule {

    public BehaviorAnalysisRule(String ruleId, String platformCode, String ruleName,
                                 String ruleType, int priority) {
        super(ruleId, platformCode, ruleName, ruleType, priority);
        this.setActionOnTrigger(RiskControlAction.REDUCE_RATE);
    }

    @Override
    public boolean evaluate(CrawlTaskContext context) {
        if (context.getRequestCount() < 5) {
            return false;
        }

        long avgInterval = calculateAverageRequestInterval(context);
        
        if (avgInterval < 500) {
            System.out.println("[BehaviorAnalysisRule] 请求间隔过短: avg=" + avgInterval + "ms");
            return true;
        }

        double failRate = (double) context.getFailCount() / context.getRequestCount();
        if (failRate > 0.3) {
            System.out.println("[BehaviorAnalysisRule] 失败率过高: rate=" + (failRate * 100) + "%");
            return true;
        }

        if (context.getTotalCount() > 100 && 
            context.getTimeSinceLastRequestMs() < 100) {
            System.out.println("[BehaviorAnalysisRule] 检测到异常快速连续请求");
            return true;
        }

        return false;
    }

    private long calculateAverageRequestInterval(CrawlTaskContext context) {
        if (context.getRequestCount() <= 1) {
            return Long.MAX_VALUE;
        }
        
        long totalDuration = java.time.Duration.between(
                context.getStartTime(),
                java.time.LocalDateTime.now()
        ).toMillis();
        
        return totalDuration / context.getRequestCount();
    }
}
