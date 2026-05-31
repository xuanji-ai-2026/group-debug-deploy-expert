package com.beijixing.risk.enums;

/**
 * 风控动作枚举 - 定义风控决策的执行动作
 *
 * @author 林超 (EMP-SEC-001)
 */
public enum RiskAction {

    /**
     * 放行 - 通过检查
     */
    PASS("放行", 1),

    /**
     * 警告 - 记录日志但放行
     */
    WARN("警告", 2),

    /**
     * 拦截 - 直接拒绝请求
     */
    BLOCK("拦截", 3),

    /**
     * 人工审核 - 需要人工介入
     */
    REVIEW("人工审核", 4),

    /**
     * 限流 - 降低操作频率
     */
    RATE_LIMIT("限流", 5),

    /**
     * 封禁 - 禁止账号/设备/IP
     */
    BAN("封禁", 6),

    /**
     * 优化建议 - 内容需要优化
     */
    OPTIMIZE("优化建议", 7),

    /**
     * 重试 - 稍后重试
     */
    RETRY("重试", 8);

    private final String description;
    private final int priority;

    RiskAction(String description, int priority) {
        this.description = description;
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 判断是否需要阻断操作
     */
    public boolean isBlocking() {
        return this == BLOCK || this == BAN;
    }

    /**
     * 判断是否需要记录
     */
    public boolean needsLogging() {
        return this != PASS;
    }
}
