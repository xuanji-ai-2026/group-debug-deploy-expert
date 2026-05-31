package com.beijixing.tenant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 套餐类型枚举
 *
 * @author bx-tenant
 */
@Getter
@AllArgsConstructor
public enum PackageType {

    /**
     * 基础版套餐
     */
    BASIC("basic", "基础版"),

    /**
     * 高级版套餐
     */
    ADVANCED("advanced", "高级版"),

    /**
     * 年度套餐
     */
    ANNUAL("annual", "年度套餐"),

    /**
     * 终身买断套餐
     */
    LIFETIME("lifetime", "终身买断");

    /**
     * 套餐编码
     */
    private final String code;

    /**
     * 套餐名称
     */
    private final String description;

    /**
     * 根据编码获取枚举
     *
     * @param code 套餐编码
     * @return PackageType
     */
    public static PackageType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PackageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
