package com.beijixing.risk.enums;

/**
 * 风险等级枚举
 *
 * @author 林超 (EMP-SEC-001)
 */
public enum RiskLevel {

    /**
     * 低风险（80-100分）
     */
    LOW("低风险", 1),

    /**
     * 中风险（60-79分）
     */
    MEDIUM("中风险", 2),

    /**
     * 高风险（40-59分）
     */
    HIGH("高风险", 3),

    /**
     * 严重风险（0-39分）
     */
    CRITICAL("严重风险", 4);

    private final String description;
    private final int level;

    RiskLevel(String description, int level) {
        this.description = description;
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 根据分数字符串获取风险等级
     */
    public static RiskLevel fromScore(int score) {
        if (score >= 80) {
            return LOW;
        } else if (score >= 60) {
            return MEDIUM;
        } else if (score >= 40) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }

    /**
     * 根据等级值获取风险等级
     */
    public static RiskLevel fromLevel(int level) {
        for (RiskLevel rl : values()) {
            if (rl.level == level) {
                return rl;
            }
        }
        return LOW;
    }
}
