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
@TableName("sys_refresh_token")
public class RefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("token")
    private String token;

    @TableField("device_fingerprint")
    private String deviceFingerprint;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("is_revoked")
    private Boolean isRevoked;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
