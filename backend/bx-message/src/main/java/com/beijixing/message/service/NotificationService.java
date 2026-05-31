package com.beijixing.message.service;

import com.beijixing.message.dto.SendNotificationRequest;
import com.beijixing.message.vo.NotificationVO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 通知服务接口
 *
 * @author 苏波（EMP-BE-001）
 */
public interface NotificationService {

    /**
     * 发送通知
     *
     * @param request 发送通知请求
     * @return 通知VO
     */
    NotificationVO sendNotification(SendNotificationRequest request);

    /**
     * 分页查询用户通知列表
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 通知分页结果
     */
    Page<NotificationVO> getNotifications(Long userId, int page, int size);

    /**
     * 获取用户未读通知列表
     *
     * @param userId 用户ID
     * @return 未读通知列表
     */
    List<NotificationVO> getUnreadNotifications(Long userId);

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAsRead(String notificationId, Long userId);

    /**
     * 批量标记通知为已读
     *
     * @param notificationIds 通知ID列表
     * @param userId 用户ID
     * @return 成功数量
     */
    int batchMarkAsRead(List<String> notificationIds, Long userId);

    /**
     * 删除通知
     *
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteNotification(String notificationId, Long userId);

    /**
     * 获取用户未读通知数量
     *
     * @param userId 用户ID
     * @return 未读通知数
     */
    long getUnreadCount(Long userId);

    /**
     * 推送通知到RabbitMQ队列
     *
     * @param notification 通知VO
     */
    void pushToQueue(NotificationVO notification);

    /**
     * 消费RabbitMQ队列中的通知
     *
     * @param notification 通知VO
     */
    void consumeNotification(NotificationVO notification);
}
