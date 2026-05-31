package com.beijixing.bxuser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCodeResponse {
    
    private String phone;
    
    private Integer expireSeconds;
    
    // 开发环境返回验证码，生产环境不返回
    private String code;
    
    private String message;
}
