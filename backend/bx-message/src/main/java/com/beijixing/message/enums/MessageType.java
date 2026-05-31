package com.beijixing.message.enums;

/**
 * 消息类型枚举
 *
 * @author 苏波（EMP-BE-001）
 */
public enum MessageType {

    /**
     * 文本消息
     */
    TEXT(1, "文本消息"),

    /**
     * 图片消息
     */
    IMAGE(2, "图片消息"),

    /**
     * 语音消息
     */
    VOICE(3, "语音消息"),

    /**
     * 视频消息
     */
    VIDEO(4, "视频消息"),

    /**
     * 文件消息
     */
    FILE(5, "文件消息"),

    /**
     * 链接消息
     */
    LINK(6, "链接消息"),

    /**
     * AI回复消息
     */
    AI_REPLY(7, "AI回复消息"),

    /**
     * 系统消息
     */
    SYSTEM(8, "系统消息");

    private final Integer code;
    private final String description;

    MessageType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static MessageType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
