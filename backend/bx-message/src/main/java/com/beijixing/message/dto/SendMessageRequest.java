package com.beijixing.message.dto;

import com.beijixing.message.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送消息请求DTO
 *
 * @author 苏波（EMP-BE-001）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /**
     * 会话ID（私聊时可选，群聊时必填）
     */
    private String sessionId;

    /**
     * 接收者用户ID（私聊时必填）
     */
    private Long receiverId;

    /**
     * 接收者名称
     */
    private String receiverName;

    /**
     * 消息类型
     */
    @NotNull(message = "消息类型不能为空")
    private MessageType messageType;

    /**
     * 消息内容（文本消息或媒体URL）
     */
    @NotBlank(message = "消息内容不能为空")
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
     * 关联的AI商机ID
     */
    private String leadId;

    /**
     * 回复的消息ID
     */
    private String replyId;

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
}
