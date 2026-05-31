package com.beijixing.content.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 发布状态枚举
 * @author 胡云 (EMP-CONTENT-001)
 */
@Getter
public enum PublishStatus {

    UNPUBLISHED(0, "未发布"),
    PUBLISHED(1, "发布成功"),
    FAILED(2, "发布失败"),
    PUBLISHING(3, "发布中");

    private final Integer code;
    private final String name;

    PublishStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .map(PublishStatus::getName)
                .orElse("未知");
    }
}
