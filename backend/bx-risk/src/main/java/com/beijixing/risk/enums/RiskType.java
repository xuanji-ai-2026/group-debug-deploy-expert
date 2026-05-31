package com.beijixing.risk.enums;

/**
 * 风险类型枚举
 *
 * @author 林超 (EMP-SEC-001)
 */
public enum RiskType {

    /**
     * 账号风险
     */
    ACCOUNT("账号风险", "ACCOUNT"),

    /**
     * 操作风险
     */
    OPERATION("操作风险", "OPERATION"),

    /**
     * 内容风险
     */
    CONTENT("内容风险", "CONTENT"),

    /**
     * 反爬虫风险
     */
    CRAWLER("反爬虫", "CRAWLER"),

    /**
     * 反验证风险
     */
    CAPTCHA("反验证", "CAPTCHA"),

    /**
     * IP风险
     */
    IP("IP风险", "IP"),

    /**
     * 设备指纹风险
     */
    DEVICE("设备指纹风险", "DEVICE");

    private final String description;
    private final String code;

    RiskType(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }
}
