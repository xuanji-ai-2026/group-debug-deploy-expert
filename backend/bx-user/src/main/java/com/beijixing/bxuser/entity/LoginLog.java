package com.beijixing.bxuser.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("login_logs")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("login_type")
    private String loginType;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("user_agent")
    private String userAgent;

    @TableField("device_fingerprint")
    private String deviceFingerprint;

    @TableField("device_type")
    private String deviceType;

    @TableField("os")
    private String os;

    @TableField("browser")
    private String browser;

    @TableField("location")
    private String location;

    @TableField("latitude")
    private Double latitude;

    @TableField("longitude")
    private Double longitude;

    @TableField("login_result")
    private String loginResult;

    @TableField("fail_reason")
    private String failReason;

    @TableField("is_geo_abnormal")
    private Boolean isGeoAbnormal;

    @TableField("is_device_abnormal")
    private Boolean isDeviceAbnormal;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
