package com.beijixing.message.config;

import com.beijixing.message.websocket.MessageWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 注册WebSocket处理路径和处理器
 *
 * @author 苏波（EMP-BE-001）
 */
@Configuration
@EnableWebSocket
@SuppressWarnings("nullness")
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MessageWebSocketHandler messageWebSocketHandler;

    /**
     * WebSocket端点路径（从配置读取）
     */
    private static final String WEBSOCKET_ENDPOINT = "/ws/message";

    @Override
    @SuppressWarnings("nullness")
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, WEBSOCKET_ENDPOINT)
                .setAllowedOrigins("*"); // 生产环境应限制为具体域名
    }
}
