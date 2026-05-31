# bx-message 消息服务

> 北极星AI商机获客系统 - 消息服务

## 功能特性

| 功能ID | 功能名称 | 说明 |
|--------|---------|------|
| MSG-001 | 即时消息推送 | WebSocket实现实时消息通信 |
| MSG-002 | 消息队列 | RabbitMQ解耦异步消息处理 |
| MSG-003 | 消息存储 | MongoDB存储聊天记录 |
| MSG-004 | 通知服务 | 系统通知推送 |

## 技术栈

- Spring Boot 3.2
- WebSocket（实时通信）
- RabbitMQ（异步消息队列）
- MongoDB 6.0（聊天记录存储）
- Redis 6.0（会话管理）

## 服务端口

```
8089
```

## 目录结构

```
bx-message/
├── src/main/java/com/beijixing/message/
│   ├── BxMessageApplication.java          # 启动类
│   ├── config/                            # 配置类
│   │   ├── RabbitMQConfig.java            # RabbitMQ配置
│   │   ├── WebSocketConfig.java           # WebSocket配置
│   │   └── MongoConfig.java               # MongoDB配置
│   ├── controller/                         # 控制器
│   │   ├── MessageController.java         # 消息接口
│   │   └── NotificationController.java    # 通知接口
│   ├── service/                            # 服务层
│   │   ├── MessageService.java
│   │   ├── NotificationService.java
│   │   ├── WebSocketService.java
│   │   └── impl/
│   │       ├── MessageServiceImpl.java
│   │       ├── NotificationServiceImpl.java
│   │       └── WebSocketServiceImpl.java
│   ├── websocket/                          # WebSocket处理器
│   │   └── MessageWebSocketHandler.java
│   ├── repository/                         # 数据访问层
│   │   ├── MessageRepository.java
│   │   └── NotificationRepository.java
│   ├── entity/                             # 实体类
│   │   ├── ChatMessage.java
│   │   ├── Notification.java
│   │   └── MessageSession.java
│   ├── vo/                                 # 视图对象
│   │   ├── MessageVO.java
│   │   └── NotificationVO.java
│   ├── dto/                                # 数据传输对象
│   │   ├── SendMessageRequest.java
│   │   └── SendNotificationRequest.java
│   └── enums/                              # 枚举类
│       ├── MessageType.java
│       └── NotificationType.java
├── src/main/resources/
│   └── application.yml                     # 应用配置
└── pom.xml
```

## API接口

### 消息接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/messages/send | 发送消息 |
| GET | /api/v1/messages/session/{sessionId} | 获取会话消息 |
| GET | /api/v1/messages/private/{userId} | 获取私聊消息 |
| PUT | /api/v1/messages/{sessionId}/read | 标记已读 |
| DELETE | /api/v1/messages/{messageId} | 撤回消息 |
| GET | /api/v1/messages/unread/count | 未读消息数 |

### 通知接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/notifications/send | 发送通知 |
| GET | /api/v1/notifications | 获取通知列表 |
| GET | /api/v1/notifications/unread | 获取未读通知 |
| PUT | /api/v1/notifications/{id}/read | 标记已读 |
| DELETE | /api/v1/notifications/{id} | 删除通知 |
| GET | /api/v1/notifications/unread/count | 未读通知数 |

### WebSocket端点

```
ws://host:8089/ws/message?userId=xxx
```

## 消息类型

| Code | 类型 | 说明 |
|------|------|------|
| 1 | TEXT | 文本消息 |
| 2 | IMAGE | 图片消息 |
| 3 | VOICE | 语音消息 |
| 4 | VIDEO | 视频消息 |
| 5 | FILE | 文件消息 |
| 6 | LINK | 链接消息 |
| 7 | AI_REPLY | AI回复消息 |
| 8 | SYSTEM | 系统消息 |

## 通知类型

| Code | 类型 | 说明 |
|------|------|------|
| 1 | SYSTEM | 系统通知 |
| 2 | LEAD_ALERT | 商机提醒 |
| 3 | RISK_WARNING | 风控预警 |
| 4 | PACKAGE_EXPIRE | 套餐到期提醒 |
| 5 | CONTENT_REVIEW | 内容审核通知 |
| 6 | SECURITY_ALERT | 账号安全通知 |
| 7 | CAMPAIGN | 营销活动通知 |
| 8 | POINTS_CHANGE | 积分变动通知 |

## 快速启动

```bash
cd bx-message
mvn clean package -DskipTests
java -jar target/bx-message-1.0.0-SNAPSHOT.jar
```

## 环境要求

- JDK 17+
- MongoDB 6.0+
- RabbitMQ 3.x
- Redis 6.0+
