package com.beijixing.bxuser.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("phone")
    private String phone;

    @TableField("password")
    private String password;

    @TableField("nickname")
    private String nickName;

    @TableField("real_name")
    private String realName;

    @TableField("email")
    private String email;

    @TableField("avatar")
    private String avatar;

    @TableField("role_type")
    private String roleType;

    @TableField("status")
    private Integer status;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
