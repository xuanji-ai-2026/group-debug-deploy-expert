package com.beijixing.tenant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 租户状态枚举
 *
 * @author bx-tenant
 */
@Getter
@AllArgsConstructor
public enum TenantStatus {

    /**
     * 待审核 - 租户刚注册，等待管理员审核
     */
    PENDING(0, "待审核"),

    /**
     * 正常 - 审核通过，租户可正常使用系统
     */
    NORMAL(1, "正常"),

    /**
     * 禁用 - 租户被管理员禁用，无法使用系统
     */
    DISABLED(2, "禁用"),

    /**
     * 注销 - 租户主动申请注销，数据保留30天后清理
     */
    CANCELLED(3, "注销");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return TenantStatus
     */
    public static TenantStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TenantStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
