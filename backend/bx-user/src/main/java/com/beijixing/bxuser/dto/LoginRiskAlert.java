package com.beijixing.bxuser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRiskAlert {
    
    // 是否异地登录
    private Boolean isGeoAbnormal;
    
    // 距离上次登录位置距离（公里）
    private Double distanceKm;
    
    // 是否新设备
    private Boolean isNewDevice;
    
    // 风险提示
    private String riskMessage;
    
    // 建议操作
    private String suggestedAction;
}
