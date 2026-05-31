package com.beijixing.social.crawl.engine.risk;

import com.beijixing.social.crawl.engine.CrawlTaskContext;
import com.beijixing.social.crawl.engine.RiskControlAction;
import com.alibaba.fastjson2.JSONObject;

public class SignatureValidationRule extends AbstractRiskControlRule {

    public SignatureValidationRule(String ruleId, String platformCode, String ruleName,
                                    String ruleType, int priority) {
        super(ruleId, platformCode, ruleName, ruleType, priority);
        this.setActionOnTrigger(RiskControlAction.ROTATE_ACCOUNT);
    }

    @Override
    public boolean evaluate(CrawlTaskContext context) {
        if (context.getLastResponse() == null) {
            return false;
        }

        JSONObject response = context.getLastResponse();
        
        int errorCode = response.getIntValue("error_code");
        if (errorCode == 403 || errorCode == 4001) {
            System.out.println("[SignatureValidationRule] 签名验证失败: code=" + errorCode);
            return true;
        }

        String errorMsg = response.getString("error_msg");
        if (errorMsg != null && (
            errorMsg.contains("签名") ||
            errorMsg.contains("signature") ||
            errorMsg.contains("x-s") ||
            errorMsg.contains("token"))) {
            System.out.println("[SignatureValidationRule] 签名相关错误: " + errorMsg);
            return true;
        }

        return false;
    }
}
