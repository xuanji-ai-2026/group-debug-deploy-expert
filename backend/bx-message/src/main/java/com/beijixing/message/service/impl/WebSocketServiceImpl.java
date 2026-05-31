package com.beijixing.message.service.impl;

import com.beijixing.message.service.WebSocketService;
import com.beijixing.message.vo.MessageVO;
import com.beijixing.message.vo.NotificationVO;
import com.beijixing.message.websocket.MessageWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings("nullness")
public class WebSocketServiceImpl implements WebSocketService {

    @Autowired
    @Lazy
    private MessageWebSocketHandler webSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String MSG_TYPE_PING = "ping";
    private static final String MSG_TYPE_PONG = "pong";
    private static final String MSG_TYPE_MESSAGE = "message";
    private static final String MSG_TYPE_NOTIFICATION = "notification";
    private static final String MSG_TYPE_ACK = "ack";

    @Override
    public void handleMessage(Long userId, String payload, WebSocketSession session) {
        try {
            Map<String, Object> msgData = objectMapper.readValue(payload, Map.class);
            String type = (String) msgData.get("type");

            if (type == null) {
                log.warn("消息类型为空：userId={}", userId);
                return;
            }

            switch (type) {
                case MSG_TYPE_PING:
                    handlePing(userId);
                    break;
                case MSG_TYPE_MESSAGE:
                    handleDirectMessage(userId, msgData);
                    break;
                case MSG_TYPE_NOTIFICATION:
                    handleNotificationRequest(userId, msgData);
                    break;
                default:
                    log.warn("未知的消息类型：type={}, userId={}", type, userId);
            }
        } catch (Exception e) {
            log.error("WebSocket消息处理异常：userId={}", userId, e);
        }
    }

    @Override
    public void onConnect(Long userId, WebSocketSession session) {
        log.info("用户WebSocket连接建立：userId={}", userId);
    }

    @Override
    public void onDisconnect(Long userId) {
        log.info("用户WebSocket连接断开：userId={}", userId);
    }

    @Override
    public void pushToUser(Long userId, Object message) {
        try {
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("type", MSG_TYPE_MESSAGE);
            pushData.put("data", message);
            String json = objectMapper.writeValueAsString(pushData);
            webSocketHandler.sendToUser(userId, json);
            log.debug("WebSocket推送消息成功：userId={}, messageId={}", userId, ((MessageVO) message).getId());
        } catch (Exception e) {
            log.error("WebSocket推送消息失败：userId={}", userId, e);
        }
    }

    @Override
    public void pushToUsers(List<Long> userIds, Object message) {
        for (Long userId : userIds) {
            pushToUser(userId, message);
        }
    }

    @Override
    public void broadcast(Object message) {
        try {
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("type", MSG_TYPE_MESSAGE);
            pushData.put("data", message);
            String json = objectMapper.writeValueAsString(pushData);
            webSocketHandler.broadcast(json);
            log.debug("WebSocket广播消息成功：messageId={}", ((MessageVO) message).getId());
        } catch (Exception e) {
            log.error("WebSocket广播消息失败：messageId={}", ((MessageVO) message).getId(), e);
        }
    }

    @Override
    public void sendPing(Long userId) {
        try {
            Map<String, Object> pongData = new HashMap<>();
            pongData.put("type", MSG_TYPE_PONG);
            pongData.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(pongData);
            webSocketHandler.sendToUser(userId, json);
        } catch (Exception e) {
            log.error("发送心跳失败：userId={}", userId, e);
        }
    }

    private void handlePing(Long userId) {
        log.debug("收到心跳：userId={}", userId);
        sendPing(userId);
    }

    private void handleDirectMessage(Long userId, Map<String, Object> msgData) {
        log.debug("收到WebSocket直发消息：userId={}", userId);
        sendAck(userId, msgData);
    }

    private void handleNotificationRequest(Long userId, Map<String, Object> msgData) {
        log.debug("收到通知请求：userId={}", userId);
    }

    private void sendAck(Long userId, Map<String, Object> originalMsg) {
        try {
            Map<String, Object> ackData = new HashMap<>();
            ackData.put("type", MSG_TYPE_ACK);
            ackData.put("clientMsgId", originalMsg.get("clientMsgId"));
            ackData.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(ackData);
            webSocketHandler.sendToUser(userId, json);
        } catch (Exception e) {
            log.error("发送ACK失败：userId={}", userId, e);
        }
    }

    public void pushToUser(Long userId, NotificationVO notification) {
        try {
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("type", MSG_TYPE_NOTIFICATION);
            pushData.put("data", notification);
            String json = objectMapper.writeValueAsString(pushData);
            webSocketHandler.sendToUser(userId, json);
            log.debug("WebSocket推送通知成功：userId={}, notificationId={}", userId, notification.getId());
        } catch (Exception e) {
            log.error("WebSocket推送通知失败：userId={}", userId, e);
        }
    }
}