package com.beijixing.message.vo;

import com.beijixing.message.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息视图对象（返回给前端）
 *
 * @author 苏波（EMP-BE-001）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    /**
     * 消息ID
     */
    private String id;

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
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 接收者用户ID
     */
    private Long receiverId;

    /**
     * 接收者名称
     */
    private String receiverName;

    /**
     * 消息类型
     */
    private MessageType messageType;

    /**
     * 消息类型描述
     */
    private String messageTypeDesc;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 媒体资源URL
     */
    private String mediaUrl;

    /**
     * 媒体资源大小
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
     * 回复的消息ID
     */
    private String replyId;

    /**
     * 回复的消息内容
     */
    private String replyContent;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 是否为当前用户发送的消息
     */
    private Boolean isSelf;
}
