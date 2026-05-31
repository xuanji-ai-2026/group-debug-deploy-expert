package com.beijixing.bxuser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    
    private String accessToken;
    
    private String refreshToken;
    
    private Long accessTokenExpiresIn;
    
    private Long refreshTokenExpiresIn;
    
    private String tokenType;
    
    // 是否为首次登录
    private Boolean isFirstLogin;
    
    // 是否需要2FA
    private Boolean require2fa;
    
    // 2FA临时Token
    private String twoFactorTempToken;
}
