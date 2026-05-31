package com.beijixing.common.exception;

import com.beijixing.common.enums.ErrorCode;
import lombok.Getter;

@Getter
public class SystemException extends RuntimeException {

    private final String code;
    private final String message;

    public SystemException(String message) {
        super(message);
        this.code =ErrorCode.SYSTEM_ERROR.getCode();
        this.message = message;
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
        this.code =ErrorCode.SYSTEM_ERROR.getCode();
        this.message = message;
    }

    public SystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
}
