package com.beijixing.message.controller;

import com.beijixing.message.dto.SendMessageRequest;
import com.beijixing.message.service.MessageService;
import com.beijixing.message.vo.MessageVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息控制器
 * 提供即时消息的HTTP API接口
 *
 * @author 苏波（EMP-BE-001）
 */
@Slf4j
@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * MSG-001: 发送消息
     * POST /api/v1/messages/send
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        log.info("发送消息请求：senderId={}, receiverId={}, type={}", userId, request.getReceiverId(), request.getMessageType());

        MessageVO message = messageService.sendMessage(request, userId, userName);

        return success(message);
    }

    /**
     * MSG-003: 获取会话消息历史
     * GET /api/v1/messages/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("获取会话消息：sessionId={}, page={}, size={}", sessionId, page, size);

        Page<MessageVO> messages = messageService.getSessionMessages(sessionId, page, size);

        return success(messages);
    }

    /**
     * 获取与指定用户的私聊消息
     * GET /api/v1/messages/private/{userId}
     */
    @GetMapping("/private/{userId}")
    public ResponseEntity<Map<String, Object>> getPrivateMessages(
            @PathVariable Long userId,
            @RequestParam Long currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("获取私聊消息：currentUserId={}, otherUserId={}", currentUserId, userId);

        Page<MessageVO> messages = messageService.getPrivateMessages(currentUserId, userId, page, size);

        return success(messages);
    }

    /**
     * 标记消息已读
     * PUT /api/v1/messages/{sessionId}/read
     */
    @PutMapping("/{sessionId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable String sessionId,
            @RequestParam Long userId) {

        log.info("标记消息已读：sessionId={}, userId={}", sessionId, userId);

        messageService.markAsRead(sessionId, userId);

        return success(null);
    }

    /**
     * 撤回消息
     * DELETE /api/v1/messages/{messageId}
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Map<String, Object>> recallMessage(
            @PathVariable String messageId,
            @RequestParam Long userId) {

        log.info("撤回消息：messageId={}, userId={}", messageId, userId);

        boolean success = messageService.recallMessage(messageId, userId);

        if (success) {
            return success("消息已撤回");
        } else {
            return error(40001, "撤回失败，消息不存在或无权撤回");
        }
    }

    /**
     * 删除消息
     * DELETE /api/v1/messages/{messageId}
     */
    @DeleteMapping("/{messageId}/delete")
    public ResponseEntity<Map<String, Object>> deleteMessage(
            @PathVariable String messageId,
            @RequestParam Long userId) {

        log.info("删除消息：messageId={}, userId={}", messageId, userId);

        boolean success = messageService.deleteMessage(messageId, userId);

        if (success) {
            return success("消息已删除");
        } else {
            return error(40001, "删除失败");
        }
    }

    /**
     * 获取未读消息数量
     * GET /api/v1/messages/unread/count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@RequestParam Long userId) {
        long count = messageService.getUnreadCount(userId);
        return success(count);
    }

    /**
     * 标记所有消息已读（移动端需要）
     * PUT /api/v1/messages/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@RequestParam Long userId) {
        log.info("标记所有消息已读：userId={}", userId);
        messageService.markAllAsRead(userId);
        return success("所有消息已标记已读");
    }

    /**
     * 获取会话未读数量
     * GET /api/v1/messages/session/{sessionId}/unread
     */
    @GetMapping("/session/{sessionId}/unread")
    public ResponseEntity<Map<String, Object>> getSessionUnreadCount(
            @PathVariable String sessionId,
            @RequestParam Long userId) {
        long count = messageService.getSessionUnreadCount(sessionId, userId);
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
