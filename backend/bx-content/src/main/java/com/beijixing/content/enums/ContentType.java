package com.beijixing.content.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 内容类型枚举
 * @author 胡云 (EMP-CONTENT-001)
 */
@Getter
public enum ContentType {

    ARTICLE(1, "文章"),
    IMAGE_TEXT(2, "图文"),
    VIDEO(3, "视频"),
    SHORT_CONTENT(4, "短内容");

    private final Integer code;
    private final String name;

    ContentType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .map(ContentType::getName)
                .orElse("未知");
    }
}
