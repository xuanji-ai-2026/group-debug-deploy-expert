package com.beijixing.content.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 发布平台枚举 - CO-008: 多平台一键发布
 * @author 胡云 (EMP-CONTENT-001)
 */
@Getter
public enum PublishPlatform {

    WECHAT(1, "微信公众号"),
    WEIBO(2, "微博"),
    DOUYIN(3, "抖音"),
    XIAOHONGSHU(4, "小红书"),
    BILIBILI(5, "B站"),
    WEBSITE(6, "官网");

    private final Integer code;
    private final String name;

    PublishPlatform(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .map(PublishPlatform::getName)
                .orElse("未知");
    }
}
