package com.beijixing.message.vo;

import com.beijixing.message.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知视图对象（返回给前端）
 *
 * @author 苏波（EMP-BE-001）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    /**
     * 通知ID
     */
    private String id;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型
     */
    private NotificationType notificationType;

    /**
     * 通知类型描述
     */
    private String notificationTypeDesc;

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
     * 优先级描述
     */
    private String priorityDesc;

    /**
     * 通知状态：0-未读，1-已读，2-已处理
     */
    private Integer status;

    /**
     * 是否已读
     */
    private Boolean read;

    /**
     * 创建时间
     */

    /**
     * 用户ID
     */
    private Long userId;
    private LocalDateTime createdTime;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
}
