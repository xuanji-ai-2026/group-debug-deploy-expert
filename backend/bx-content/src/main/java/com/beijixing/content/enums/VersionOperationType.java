package com.beijixing.content.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 版本操作类型枚举 - CO-009: 内容版本管理
 * @author 胡云 (EMP-CONTENT-001)
 */
@Getter
public enum VersionOperationType {

    CREATE(1, "创建"),
    EDIT(2, "编辑"),
    PUBLISH(3, "发布"),
    WITHDRAW(4, "撤回");

    private final Integer code;
    private final String name;

    VersionOperationType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .map(VersionOperationType::getName)
                .orElse("未知");
    }
}
