package com.beijixing.content.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 审核类型枚举 - CO-007: 内容审核
 * @author 胡云 (EMP-CONTENT-001)
 */
@Getter
public enum AuditType {

    AI(1, "AI审核"),
    MANUAL(2, "人工审核");

    private final Integer code;
    private final String name;

    AuditType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .map(AuditType::getName)
                .orElse("未知");
    }
}
