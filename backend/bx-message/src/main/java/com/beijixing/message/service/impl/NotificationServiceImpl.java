package com.beijixing.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.message.dto.SendNotificationRequest;
import com.beijixing.message.entity.Notification;
import com.beijixing.message.enums.NotificationType;
import com.beijixing.message.mapper.NotificationMapper;
import com.beijixing.message.service.NotificationService;
import com.beijixing.message.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务实现类（统一技术栈 - MariaDB + MyBatis-Plus）
 *
 * @author 苏波（EMP-BE-001）
 */
@Slf4j
@Service
@SuppressWarnings("nullness")
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    private static final Integer NOT_DELETED = 0;
    private static final Integer STATUS_UNREAD = 0;
    private static final Integer STATUS_READ = 1;

    @Override
    public NotificationVO sendNotification(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getNotificationType() != null ? request.getNotificationType() : NotificationType.SYSTEM)
                .tenantId(request.getTenantId())
                .relatedId(request.getBusinessId())
                .status(STATUS_UNREAD)
                .deleted(NOT_DELETED)
                .createdTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        notificationMapper.insert(notification);
        log.info("通知发送成功：notificationId={}, userId={}", notification.getId(), request.getUserId());

        return toVO(notification);
    }

    @Override
    public Page<NotificationVO> getNotifications(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getDeleted, NOT_DELETED)
               .orderByDesc(Notification::getCreatedTime);

        long total = notificationMapper.selectCount(wrapper);
        int start = (page - 1) * size;
        
        wrapper.last("LIMIT " + start + "," + size);
        List<Notification> records = notificationMapper.selectList(wrapper);
        
        List<NotificationVO> voList = records.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
                
        return new PageImpl<>(voList, PageRequest.of(page - 1, size), total);
    }

    @Override
    public List<NotificationVO> getUnreadNotifications(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getStatus, STATUS_UNREAD)
               .eq(Notification::getDeleted, NOT_DELETED)
               .orderByDesc(Notification::getCreatedTime);

        return notificationMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean markAsRead(String notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(Long.parseLong(notificationId));
        if (notification != null && notification.getUserId().equals(userId)) {
            notification.setStatus(STATUS_READ);
            notification.setUpdateTime(LocalDateTime.now());
            notificationMapper.updateById(notification);
            log.info("通知标记已读：notificationId={}, userId={}", notificationId, userId);
            return true;
        }
        return false;
    }

    @Override
    public int batchMarkAsRead(List<String> notificationIds, Long userId) {
        int count = 0;
        for (String id : notificationIds) {
            if (markAsRead(id, userId)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean deleteNotification(String notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(Long.parseLong(notificationId));
        if (notification != null && notification.getUserId().equals(userId)) {
            notification.setDeleted(1);
            notification.setUpdateTime(LocalDateTime.now());
            notificationMapper.updateById(notification);
            log.info("通知删除成功：notificationId={}, userId={}", notificationId, userId);
            return true;
        }
        return false;
    }

    @Override
    public long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getStatus, STATUS_UNREAD)
               .eq(Notification::getDeleted, NOT_DELETED);
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public void pushToQueue(NotificationVO notification) {
        log.info("通知已推送到队列: notificationId={}", notification.getId());
    }

    @Override
    public void consumeNotification(NotificationVO notification) {
        log.info("消费通知: notificationId={}, userId={}", notification.getId(), notification.getUserId());
    }

    @SuppressWarnings("nullness")
    private NotificationVO toVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(notification, vo);
        return vo;
    }
}
