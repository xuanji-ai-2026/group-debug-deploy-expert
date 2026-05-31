package com.beijixing.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.beijixing.message.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知实体类（统一技术栈 - MariaDB + MyBatis-Plus）
 *
 * @author 苏波（EMP-BE-001）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification")
public class Notification {

    /** 通知ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型 */
    private NotificationType type;

    /** 租户ID（多租户隔离） */
    private Long tenantId;

    /** 关联的业务ID（如订单ID、消息ID等） */
    private String relatedId;

    /** 是否已读：0-未读，1-已读 */
    private Integer status;

    /** 是否已删除：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
