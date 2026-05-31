package com.beijixing.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS("000000", "操作成功"),

    SYSTEM_ERROR("500000", "系统内部错误"),
    PARAM_ERROR("400000", "参数错误"),
    BUSINESS_ERROR("400001", "业务处理失败"),
    UNAUTHORIZED("401000", "未认证"),
    FORBIDDEN("403000", "无权限"),
    NOT_FOUND("404000", "资源不存在"),
    METHOD_NOT_ALLOWED("405000", "请求方法不允许"),
    TOO_MANY_REQUESTS("429000", "请求过于频繁"),
    VALIDATION_ERROR("422000", "数据校验失败"),

    USER_001("401001", "用户名或密码错误"),
    USER_002("401002", "账号已被禁用"),
    USER_003("401003", "Token已过期，请重新登录"),
    USER_004("401004", "Token无效"),
    USER_005("400002", "用户名已存在"),
    USER_006("404001", "用户不存在"),
    USER_007("400003", "原密码错误"),
    USER_008("400004", "两次输入的密码不一致"),
    USER_009("400005", "手机号格式不正确"),
    USER_010("400006", "邮箱格式不正确"),

    CONTENT_001("400010", "内容不能为空"),
    CONTENT_002("400011", "内容长度超出限制"),
    CONTENT_003("400012", "内容包含敏感词"),
    CONTENT_004("404010", "内容不存在"),
    CONTENT_005("400013", "内容状态异常"),

    FILE_001("400020", "文件上传失败"),
    FILE_002("400021", "文件类型不支持"),
    FILE_003("400022", "文件大小超出限制"),
    FILE_004("404020", "文件不存在"),
    FILE_005("400023", "文件下载失败"),

    AI_001("400030", "AI服务调用失败"),
    AI_002("400031", "AI模型不可用"),
    AI_003("400032", "请求超时，请稍后重试"),
    AI_004("400033", "配额不足"),
    AI_005("400034", "内容审核未通过"),

    BILLING_001("400040", "余额不足"),
    BILLING_002("400041", "充值失败"),
    BILLING_003("400042", "订单不存在"),
    BILLING_004("400043", "订单状态异常"),
    BILLING_005("404040", "账单不存在");

    private final String code;
    private final String message;

    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }
}
