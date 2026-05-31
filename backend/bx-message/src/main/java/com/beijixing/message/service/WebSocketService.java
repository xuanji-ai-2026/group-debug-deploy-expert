package com.beijixing.message.service;

import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket服务接口
 *
 * @author 苏波（EMP-BE-001）
 */
public interface WebSocketService {

    /**
     * 处理收到的WebSocket消息
     *
     * @param userId 发送者用户ID
     * @param payload 消息内容（JSON字符串）
     * @param session WebSocket会话
     */
    void handleMessage(Long userId, String payload, WebSocketSession session);

    /**
     * 连接建立时调用
     *
     * @param userId 用户ID
     * @param session WebSocket会话
     */
    void onConnect(Long userId, WebSocketSession session);

    /**
     * 连接断开时调用
     *
     * @param userId 用户ID
     */
    void onDisconnect(Long userId);

    /**
     * 推送消息给指定用户（通过WebSocket）
     *
     * @param userId 目标用户ID
     * @param message 消息VO
     */
    void pushToUser(Long userId, Object message);

    /**
     * 推送消息给多个用户
     *
     * @param userIds 目标用户ID列表
     * @param message 消息VO
     */
    void pushToUsers(java.util.List<Long> userIds, Object message);

    /**
     * 广播消息给所有在线用户
     *
     * @param message 消息VO
     */
    void broadcast(Object message);

    /**
     * 发送心跳Ping
     *
     * @param userId 目标用户ID
     */
    void sendPing(Long userId);
}
