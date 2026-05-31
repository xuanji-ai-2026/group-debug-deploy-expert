package com.beijixing.message.controller;

import com.beijixing.message.dto.SendNotificationRequest;
import com.beijixing.message.service.NotificationService;
import com.beijixing.message.vo.NotificationVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 * 提供系统通知的HTTP API接口
 *
 * @author 苏波（EMP-BE-001）
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * MSG-004: 发送通知
     * POST /api/v1/notifications/send
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        log.info("发送通知请求：userId={}, type={}, title={}",
                request.getUserId(), request.getNotificationType(), request.getTitle());

        NotificationVO notification = notificationService.sendNotification(request);

        return success(notification);
    }

    /**
     * MSG-004: 获取用户通知列表（分页）
     * GET /api/v1/notifications
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("获取通知列表：userId={}, page={}, size={}", userId, page, size);

        Page<NotificationVO> notifications = notificationService.getNotifications(userId, page, size);

        return success(notifications);
    }

    /**
     * 获取用户未读通知列表
     * GET /api/v1/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications(@RequestParam Long userId) {
        List<NotificationVO> notifications = notificationService.getUnreadNotifications(userId);
        return success(notifications);
    }

    /**
     * 标记通知为已读
     * PUT /api/v1/notifications/{notificationId}/read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable String notificationId,
            @RequestParam Long userId) {

        log.info("标记通知已读：notificationId={}, userId={}", notificationId, userId);

        boolean success = notificationService.markAsRead(notificationId, userId);

        if (success) {
            return success("通知已标记为已读");
        } else {
            return error(40001, "标记失败，通知不存在或无权操作");
        }
    }

    /**
     * 批量标记通知为已读
     * PUT /api/v1/notifications/read/batch
     */
    @PutMapping("/read/batch")
    public ResponseEntity<Map<String, Object>> batchMarkAsRead(
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<String> notificationIds = (List<String>) request.get("notificationIds");
        Long userId = Long.valueOf(request.get("userId").toString());

        log.info("批量标记通知已读：userId={}, count={}", userId, notificationIds.size());

        int count = notificationService.batchMarkAsRead(notificationIds, userId);

        return success(count);
    }

    /**
     * 删除通知
     * DELETE /api/v1/notifications/{notificationId}
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable String notificationId,
            @RequestParam Long userId) {

        log.info("删除通知：notificationId={}, userId={}", notificationId, userId);

        boolean success = notificationService.deleteNotification(notificationId, userId);

        if (success) {
            return success("通知已删除");
        } else {
            return error(40001, "删除失败");
        }
    }

    /**
     * 获取未读通知数量
     * GET /api/v1/notifications/unread/count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@RequestParam Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return success(count);
    }

    // ==================== 通用响应方法 ====================

    private ResponseEntity<Map<String, Object>> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", data);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> error(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }
}
