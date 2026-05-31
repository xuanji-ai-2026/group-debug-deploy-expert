package com.beijixing.content.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 内容状态枚举
 * @author 胡云 (EMP-CONTENT-001)
 */
@Getter
public enum ContentStatus {

    DRAFT(0, "草稿"),
    AUDITING(1, "审核中"),
    PUBLISHED(2, "已发布"),
    WITHDRAWN(3, "已撤回"),
    DELETED(4, "已删除");

    private final Integer code;
    private final String name;

    ContentStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .map(ContentStatus::getName)
                .orElse("未知");
    }
}
