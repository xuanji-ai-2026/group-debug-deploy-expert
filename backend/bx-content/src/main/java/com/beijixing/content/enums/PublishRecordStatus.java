package com.beijixing.content.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 发布记录状态枚举 - CO-004: 发布状态查询
 * @author 胡云 (EMP-CONTENT-001)
 */
@Getter
public enum PublishRecordStatus {

    PENDING(0, "待发布"),
    PUBLISHING(1, "发布中"),
    SUCCESS(2, "发布成功"),
    FAILED(3, "发布失败");

    private final Integer code;
    private final String name;

    PublishRecordStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .map(PublishRecordStatus::getName)
                .orElse("未知");
    }
}
