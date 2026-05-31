package com.beijixing.bxuser.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    
    private String loginType;
    
    @NotBlank(message = "手机号不能为空")
    private String phone;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private String deviceFingerprint;
    
    private String userAgent;
    
    private String ipAddress;
    
    private Double latitude;
    
    private Double longitude;
    
    // 2FA验证码
    private String twoFactorCode;
}
