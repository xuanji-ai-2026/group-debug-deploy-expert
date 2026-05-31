package com.beijixing.tenant.exception;

import lombok.Getter;

/**
 * 租户业务异常类
 *
 * @author bx-tenant
 */
@Getter
public class TenantException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    public TenantException(String message) {
        super(message);
        this.code = 20000;
    }

    public TenantException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public TenantException(String message, Throwable cause) {
        super(message, cause);
        this.code = 20000;
    }
}
