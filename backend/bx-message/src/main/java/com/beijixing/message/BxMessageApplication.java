package com.beijixing.message;

import org.springframework.context.annotation.Configuration;

/**
 * 北极星AI商机获客系统 - 消息服务配置类
 *
 * 服务功能：
 * - MSG-001: 即时消息推送（WebSocket实时通信）
 * - MSG-002: 消息存储（MariaDB + MyBatis-Plus聊天记录持久化）
 * - MSG-003: 通知服务（系统通知推送）
 *
 * 技术栈：Spring Boot 3 + MariaDB + MyBatis-Plus + Redis + WebSocket
 *
 * 注意：已从@SpringBootApplication改为@Configuration
 * 原因：单体架构下由beijixing-app统一启动，此模块不再独立运行
 * 最后更新: 2026-05-20 (极简单体版)
 *
 * @author 苏波（EMP-BE-001）
 */
@Configuration
public class BxMessageApplication {

    // main方法已移除 - 单体架构下由BeijixingAiApplication统一启动
    // public static void main(String[] args) {
    //     SpringApplication.run(BxMessageApplication.class, args);
    // }
}
