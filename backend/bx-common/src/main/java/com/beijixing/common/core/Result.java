package com.beijixing.common.core;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
@SuppressWarnings("nullness")
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    private String traceId;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Result<T> success() {
        return success((T) null);
    }
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
    
    public static <T> Result<T> error(String message) {
        return error(50000, message);
    }

    public static <T> Result<T> error(String code, String message) {
        Result<T> result = new Result<>();
        int codeVal = code != null ? code.hashCode() : 50000;
        result.setCode(codeVal);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(String code, String message) {
        return error(code, message);
    }
}
