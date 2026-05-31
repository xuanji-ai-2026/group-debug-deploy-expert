package com.beijixing.bxuser.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerificationCodeRequest {
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    // 用途: REGISTER, LOGIN, RESET_PASSWORD
    @NotBlank(message = "用途不能为空")
    private String purpose;
}
