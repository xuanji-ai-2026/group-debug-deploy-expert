package com.beijixing.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.beijixing.message.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类（统一技术栈 - MariaDB + MyBatis-Plus）
 * 存储用户之间的即时聊天消息
 *
 * @author 苏波（EMP-BE-001）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage {

    /**
     * 消息ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 发送者用户ID
     */
    private Long senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 接收者用户ID
     */
    private Long receiverId;

    /**
     * 接收者名称
     */
    private String receiverName;

    /**
     * 租户ID（多租户隔离）
     */
    private Long tenantId;

    /**
     * 消息类型
     */
    private MessageType messageType;

    /**
     * 消息内容（文本消息内容，或媒体资源的URL）
     */
    private String content;

    /**
     * 媒体资源URL（图片、语音、视频、文件时使用）
     */
    private String mediaUrl;

    /**
     * 媒体资源大小（字节）
     */
    private Long mediaSize;

    /**
     * 媒体资源名称
     */
    private String mediaName;

    /**
     * 消息状态：0-发送中，1-已发送，2-已读，3-已撤回
     */
    private Integer status;

    /**
     * 是否已删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 关联的AI商机ID（如果有）
     */
    private String leadId;

    /**
     * 回复的消息ID
     */
    private String replyId;

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