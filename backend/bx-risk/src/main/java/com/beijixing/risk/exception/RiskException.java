package com.beijixing.risk.exception;

import lombok.Getter;

/**
 * 风控业务异常
 *
 * @author 林超 (EMP-SEC-001)
 */
@Getter
public class RiskException extends RuntimeException {

    private final String code;
    private final String message;

    public RiskException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public RiskException(String message) {
        this("RISK_ERROR", message);
    }

    public static RiskException ruleNotFound(Long ruleId) {
        return new RiskException("RULE_NOT_FOUND", "规则不存在: " + ruleId);
    }

    public static RiskException strategyNotFound(String strategyCode) {
        return new RiskException("STRATEGY_NOT_FOUND", "策略不存在: " + strategyCode);
    }

    public static RiskException decisionTimeout() {
        return new RiskException("DECISION_TIMEOUT", "决策超时，请稍后重试");
    }

    public static RiskException checkFailed(String reason) {
        return new RiskException("CHECK_FAILED", "风控检查失败: " + reason);
    }
}
