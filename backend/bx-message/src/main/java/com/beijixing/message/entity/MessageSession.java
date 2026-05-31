package com.beijixing.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息会话实体类（统一技术栈 - MariaDB + MyBatis-Plus）
 * 存储用户之间的聊天会话信息
 *
 * @author 苏波（EMP-BE-001）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message_session")
public class MessageSession {

    /**
     * 会话ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话类型：1-单聊，2-群聊
     */
    private Integer sessionType;

    /**
     * 会话名称（群聊名称或单聊对方的昵称）
     */
    private String sessionName;

    /**
     * 会话头像
     */
    private String avatar;

    /**
     * 参与者用户ID列表（JSON格式存储）
     */
    private String participants;

    /**
     * 租户ID（多租户隔离）
     */
    private Long tenantId;

    /**
     * 会话所有者/创建者ID
     */
    private Long ownerId;

    /**
     * 最后一条消息ID
     */
    private String lastMessageId;

    /**
     * 最后一条消息内容摘要
     */
    private String lastMessageSummary;

    /**
     * 最后消息发送者
     */
    private Long lastSenderId;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数量（按用户维度存储）
     */
    private Integer unreadCount;

    /**
     * 会话状态：0-正常，1-已归档，2-已删除
     */
    private Integer status;

    /**
     * 是否置顶：0-否，1-是
     */
    private Integer pinned;

    /**
     * 是否免打扰：0-否，1-是
     */
    private Integer mute;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}