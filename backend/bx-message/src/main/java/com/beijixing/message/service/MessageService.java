package com.beijixing.message.service;

import com.beijixing.message.dto.SendMessageRequest;
import com.beijixing.message.vo.MessageVO;
import org.springframework.data.domain.Page;

/**
 * 消息服务接口
 *
 * @author 苏波（EMP-BE-001）
 */
public interface MessageService {

    /**
     * 发送消息（支持单聊和群聊）
     *
     * @param request 发送消息请求
     * @param senderId 发送者ID
     * @param senderName 发送者名称
     * @return 消息VO
     */
    MessageVO sendMessage(SendMessageRequest request, Long senderId, String senderName);

    /**
     * 分页查询会话消息历史
     *
     * @param sessionId 会话ID
     * @param page 页码
     * @param size 每页数量
     * @return 消息分页结果
     */
    Page<MessageVO> getSessionMessages(String sessionId, int page, int size);

    /**
     * 获取与某用户的历史私聊消息
     *
     * @param userId 当前用户ID
     * @param otherUserId 对方用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 消息分页结果
     */
    Page<MessageVO> getPrivateMessages(Long userId, Long otherUserId, int page, int size);

    /**
     * 标记消息为已读
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    void markAsRead(String sessionId, Long userId);

    /**
     * 撤回消息
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean recallMessage(String messageId, Long userId);

    /**
     * 删除消息（软删除）
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteMessage(String messageId, Long userId);

    /**
     * 获取用户未读消息总数
     *
     * @param userId 用户ID
     * @return 未读消息数
     */
    long getUnreadCount(Long userId);

    /**
     * 获取会话未读消息数
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 未读消息数
     */
    long getSessionUnreadCount(String sessionId, Long userId);

    /**
     * 标记用户所有消息已读（移动端需要）
     *
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 推送消息到RabbitMQ队列
     *
     * @param message 消息VO
     */
    void pushToQueue(MessageVO message);

    /**
     * 消费RabbitMQ队列中的消息
     *
     * @param message 消息VO
     */
    void consumeMessage(MessageVO message);
}
