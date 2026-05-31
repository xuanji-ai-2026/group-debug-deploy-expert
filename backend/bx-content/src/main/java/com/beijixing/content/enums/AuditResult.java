package com.beijixing.content.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 审核结果枚举 - CO-007: 内容审核
 * @author 胡云 (EMP-CONTENT-001)
 */
@Getter
public enum AuditResult {

    PENDING(0, "待审核"),
    PASSED(1, "通过"),
    REJECTED(2, "不通过"),
    NEED_MODIFY(3, "需修改");

    private final Integer code;
    private final String name;

    AuditResult(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .map(AuditResult::getName)
                .orElse("未知");
    }
}
