package com.beijixing.content.exception;

/**
 * 内容模块业务异常
 * @author 胡云 (EMP-CONTENT-001)
 */
public class ContentException extends RuntimeException {

    private Integer code;

    public ContentException(String message) {
        super(message);
        this.code = 400;
    }

    public ContentException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public ContentException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
    }

    public Integer getCode() {
        return code;
    }
}
