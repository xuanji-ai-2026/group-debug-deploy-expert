package com.beijixing.message.enums;

/**
 * 通知类型枚举
 *
 * @author 苏波（EMP-BE-001）
 */
public enum NotificationType {

    /**
     * 系统通知
     */
    SYSTEM(1, "系统通知"),

    /**
     * 商机提醒
     */
    LEAD_ALERT(2, "商机提醒"),

    /**
     * 风控预警
     */
    RISK_WARNING(3, "风控预警"),

    /**
     * 套餐到期提醒
     */
    PACKAGE_EXPIRE(4, "套餐到期提醒"),

    /**
     * 内容审核通知
     */
    CONTENT_REVIEW(5, "内容审核通知"),

    /**
     * 账号安全通知
     */
    SECURITY_ALERT(6, "账号安全通知"),

    /**
     * 营销活动通知
     */
    CAMPAIGN(7, "营销活动通知"),

    /**
     * 积分变动通知
     */
    POINTS_CHANGE(8, "积分变动通知");

    private final Integer code;
    private final String description;

    NotificationType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static NotificationType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (NotificationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
