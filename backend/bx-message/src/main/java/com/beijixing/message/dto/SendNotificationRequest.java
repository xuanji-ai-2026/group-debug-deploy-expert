package com.beijixing.message.dto;

import com.beijixing.message.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 发送通知请求DTO
 *
 * @author 苏波（EMP-BE-001）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    /**
     * 通知标题
     */
    @NotBlank(message = "通知标题不能为空")
    private String title;

    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    private String content;

    /**
     * 通知类型
     */
    @NotNull(message = "通知类型不能为空")
    private NotificationType notificationType;

    /**
     * 目标用户ID
     */
    private Long userId;

    /**
     * 目标用户名
     */
    private String userName;

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /**
     * 关联的业务ID
     */
    private String businessId;

    /**
     * 关联的业务类型
     */
    private String businessType;

    /**
     * 扩展数据
     */
    private Map<String, Object> extraData;

    /**
     * 优先级：1-低，2-中，3-高，4-紧急
     */
    private Integer priority;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
}
