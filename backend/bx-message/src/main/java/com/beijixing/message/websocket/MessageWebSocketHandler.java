package com.beijixing.message.websocket;

import com.beijixing.message.service.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息处理器
 * 处理客户端连接、消息收发、心跳等
 *
 * @author 苏波（EMP-BE-001）
 */
@Slf4j
@Component
@SuppressWarnings("nullness")
public class MessageWebSocketHandler extends TextWebSocketHandler {

    /**
     * 在线会话Map：key为userId，value为WebSocketSession
     */
    private final Map<Long, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 客户端连接成功
     */
    @Override
    @SuppressWarnings("nullness")
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            // 从请求参数中获取userId
            Map<String, Object> attributes = session.getAttributes();
            Object userIdObj = attributes.get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                onlineSessions.put(userId, session);
                log.info("WebSocket连接建立成功：userId={}, sessionId={}", userId, session.getId());
                webSocketService.onConnect(userId, session);
            } else {
                log.warn("WebSocket连接缺少userId参数：sessionId={}", session.getId());
                // 发送错误消息并关闭连接
                sendErrorMessage(session, "缺少userId参数");
                session.close(CloseStatus.BAD_DATA);
            }
        } catch (Exception e) {
            log.error("处理WebSocket连接建立异常：sessionId={}", session.getId(), e);
        }
    }

    /**
     * 收到客户端文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.debug("收到WebSocket消息：sessionId={}, payload={}", session.getId(), payload);

            // 获取发送者userId
            Object userIdObj = session.getAttributes().get("userId");
            if (userIdObj == null) {
                log.warn("消息缺少userId，跳过处理");
                return;
            }
            Long userId = Long.valueOf(userIdObj.toString());

            // 解析消息并处理
            webSocketService.handleMessage(userId, payload, session);

        } catch (Exception e) {
            log.error("处理WebSocket消息异常：sessionId={}", session.getId(), e);
            sendErrorMessage(session, "消息处理失败：" + e.getMessage());
        }
    }

    /**
     * 客户端连接关闭
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            Map<String, Object> attributes = session.getAttributes();
            Object userIdObj = attributes.get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                onlineSessions.remove(userId);
                log.info("WebSocket连接关闭：userId={}, sessionId={}, status={}", userId, session.getId(), status);
                webSocketService.onDisconnect(userId);
            }
        } catch (Exception e) {
            log.error("处理WebSocket连接关闭异常", e);
        }
    }

    /**
     * 传输错误
     */
    @Override
    @SuppressWarnings("nullness")
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        try {
            Map<String, Object> attributes = session.getAttributes();
            Object userIdObj = attributes.get("userId");
            Long userId = userIdObj != null ? Long.valueOf(userIdObj.toString()) : null;
            log.error("WebSocket传输错误：userId={}, sessionId={}", userId, session.getId(), exception);
            if (userId != null) {
                onlineSessions.remove(userId);
                webSocketService.onDisconnect(userId);
            }
        } catch (Exception e) {
            log.error("处理WebSocket传输错误异常", e);
        }
    }

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = onlineSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
                log.debug("WebSocket消息发送成功：userId={}, message={}", userId, message);
            } catch (IOException e) {
                log.error("WebSocket消息发送失败：userId={}", userId, e);
            }
        } else {
            log.warn("用户WebSocket不在线：userId={}", userId);
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcast(String message) {
        for (Map.Entry<Long, WebSocketSession> entry : onlineSessions.entrySet()) {
            try {
                WebSocketSession session = entry.getValue();
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("广播消息失败：userId={}", entry.getKey(), e);
            }
        }
    }

    /**
     * 获取当前在线用户数
     */
    public int getOnlineCount() {
        return onlineSessions.size();
    }

    /**
     * 判断用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = onlineSessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 发送错误消息给客户端
     */
    private void sendErrorMessage(WebSocketSession session, String errorMsg) {
        try {
            String msg = objectMapper.writeValueAsString(
                    java.util.Map.of("type", "error", "message", errorMsg)
            );
            session.sendMessage(new TextMessage(msg));
        } catch (IOException e) {
            log.error("发送错误消息失败", e);
        }
    }
}
