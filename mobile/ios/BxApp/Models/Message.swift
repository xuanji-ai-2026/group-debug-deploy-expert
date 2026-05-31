//
//  Message.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  消息数据模型
//
//  Created by Lin Feng (EMP-IOS-003) on 2024-01-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

// MARK: - 消息模型

/// 消息模型
///
/// 存储用户消息的基本信息，包括消息ID、类型、内容、状态等。
/// 实现了Codable协议，支持JSON序列化和本地存储。
struct Message: Codable, Identifiable, Equatable {
    
    // MARK: - 基本信息
    
    /// 消息ID - 唯一标识
    var id: String
    
    /// 消息标题
    var title: String
    
    /// 消息内容
    var content: String
    
    /// 消息类型
    var type: MessageType
    
    /// 消息状态
    var status: MessageStatus
    
    /// 关联业务ID（如商机ID、任务ID等）
    var relatedId: String?
    
    /// 关联业务类型
    var relatedType: RelatedType?
    
    /// 发送者信息
    var sender: MessageSender?
    
    // MARK: - 时间戳
    
    /// 创建时间
    var createdAt: Date
    
    /// 阅读时间
    var readAt: Date?
    
    /// 过期时间（可选）
    var expireAt: Date?
    
    // MARK: - 扩展信息
    
    /// 额外数据（JSON字符串，用于存储扩展信息）
    var extraData: String?
    
    /// 操作按钮列表
    var actions: [MessageAction]?
    
    // MARK: - 初始化
    
    init(
        id: String = UUID().uuidString,
        title: String,
        content: String,
        type: MessageType = .system,
        status: MessageStatus = .unread,
        relatedId: String? = nil,
        relatedType: RelatedType? = nil,
        sender: MessageSender? = nil,
        createdAt: Date = Date(),
        readAt: Date? = nil,
        expireAt: Date? = nil,
        extraData: String? = nil,
        actions: [MessageAction]? = nil
    ) {
        self.id = id
        self.title = title
        self.content = content
        self.type = type
        self.status = status
        self.relatedId = relatedId
        self.relatedType = relatedType
        self.sender = sender
        self.createdAt = createdAt
        self.readAt = readAt
        self.expireAt = expireAt
        self.extraData = extraData
        self.actions = actions
    }
}

// MARK: - 消息类型

/// 消息类型枚举
enum MessageType: String, Codable, CaseIterable {
    /// 系统消息
    case system = "system"
    
    /// 商机消息
    case lead = "lead"
    
    /// 任务消息
    case task = "task"
    
    /// 账户消息
    case account = "account"
    
    /// 活动消息
    case activity = "activity"
    
    /// 消息类型描述
    var description: String {
        switch self {
        case .system: return "系统消息"
        case .lead: return "商机消息"
        case .task: return "任务消息"
        case .account: return "账户消息"
        case .activity: return "活动消息"
        }
    }
    
    /// 图标名称
    var iconName: String {
        switch self {
        case .system: return "bell.fill"
        case .lead: return "briefcase.fill"
        case .task: return "checklist"
        case .account: return "person.crop.circle.fill"
        case .activity: return "gift.fill"
        }
    }
    
    /// 颜色名称
    var colorName: String {
        switch self {
        case .system: return "systemBlue"
        case .lead: return "systemOrange"
        case .task: return "systemGreen"
        case .account: return "systemPurple"
        case .activity: return "systemRed"
        }
    }
}

// MARK: - 消息状态

/// 消息状态枚举
enum MessageStatus: String, Codable, CaseIterable {
    /// 未读
    case unread = "unread"
    
    /// 已读
    case read = "read"
    
    /// 已删除
    case deleted = "deleted"
    
    /// 状态描述
    var description: String {
        switch self {
        case .unread: return "未读"
        case .read: return "已读"
        case .deleted: return "已删除"
        }
    }
    
    /// 是否已读
    var isRead: Bool {
        self == .read
    }
}

// MARK: - 关联类型

/// 关联业务类型枚举
enum RelatedType: String, Codable, CaseIterable {
    /// 商机
    case lead = "lead"
    
    /// 任务
    case task = "task"
    
    /// 订单
    case order = "order"
    
    /// 账户
    case account = "account"
    
    /// 充值
    case recharge = "recharge"
    
    /// 描述
    var description: String {
        switch self {
        case .lead: return "商机"
        case .task: return "任务"
        case .order: return "订单"
        case .account: return "账户"
        case .recharge: return "充值"
        }
    }
}

// MARK: - 消息发送者

/// 消息发送者信息
struct MessageSender: Codable, Equatable {
    /// 发送者ID
    var id: String
    
    /// 发送者名称
    var name: String
    
    /// 发送者头像
    var avatarUrl: String?
    
    /// 发送者类型
    var type: SenderType
}

/// 发送者类型枚举
enum SenderType: String, Codable, CaseIterable {
    /// 系统
    case system = "system"
    
    /// 用户
    case user = "user"
    
    /// 机器人
    case bot = "bot"
    
    /// 描述
    var description: String {
        switch self {
        case .system: return "系统"
        case .user: return "用户"
        case .bot: return "AI助手"
        }
    }
}

// MARK: - 消息操作

/// 消息操作按钮
struct MessageAction: Codable, Equatable {
    /// 操作ID
    var id: String
    
    /// 操作标题
    var title: String
    
    /// 操作类型
    var type: ActionType
    
    /// 关联链接或参数
    var payload: String?
    
    /// 操作样式
    var style: ActionStyle
}

/// 操作类型枚举
enum ActionType: String, Codable, CaseIterable {
    /// 打开详情
    case openDetail = "open_detail"
    
    /// 打开链接
    case openLink = "open_link"
    
    /// 执行操作
    case execute = "execute"
    
    /// 确认
    case confirm = "confirm"
    
    /// 取消
    case cancel = "cancel"
}

/// 操作样式枚举
enum ActionStyle: String, Codable, CaseIterable {
    /// 默认样式
    case `default` = "default"
    
    /// 主要样式
    case primary = "primary"
    
    /// 危险样式
    case danger = "danger"
    
    /// 链接样式
    case link = "link"
}

// MARK: - 消息筛选

/// 消息筛选条件
struct MessageFilter: Codable, Equatable {
    /// 消息类型
    var types: [MessageType]?
    
    /// 消息状态
    var status: MessageStatus?
    
    /// 搜索关键词
    var keyword: String?
    
    /// 开始时间
    var startDate: Date?
    
    /// 结束时间
    var endDate: Date?
    
    /// 页码
    var page: Int = 1
    
    /// 每页数量
    var pageSize: Int = 20
    
    /// 清空筛选
    mutating func clear() {
        types = nil
        status = nil
        keyword = nil
        startDate = nil
        endDate = nil
        page = 1
    }
}

// MARK: - 消息统计

/// 消息统计信息
struct MessageStatistics: Codable {
    /// 总数
    var totalCount: Int
    
    /// 未读数
    var unreadCount: Int
    
    /// 系统消息数
    var systemCount: Int
    
    /// 商机消息数
    var leadCount: Int
    
    /// 任务消息数
    var taskCount: Int
    
    /// 账户消息数
    var accountCount: Int
    
    /// 活动消息数
    var activityCount: Int
    
    /// 未读消息总数
    var totalUnreadCount: Int {
        return unreadCount
    }
}

// MARK: - WebSocket消息

/// WebSocket消息结构
struct WebSocketMessage: Codable {
    /// 消息类型
    var type: WSMessageType
    
    /// 消息数据
    var data: Message?
    
    /// 时间戳
    var timestamp: Date
    
    /// 设备ID
    var deviceId: String?
}

/// WebSocket消息类型枚举
enum WSMessageType: String, Codable {
    /// 新消息
    case newMessage = "new_message"
    
    /// 消息已读
    case messageRead = "message_read"
    
    /// 消息删除
    case messageDeleted = "message_deleted"
    
    /// 连接确认
    case connected = "connected"
    
    /// 心跳
    case ping = "ping"
    
    /// 心跳响应
    case pong = "pong"
    
    /// 错误
    case error = "error"
}

// MARK: - 预览数据（仅用于SwiftUI Preview，生产环境不使用）

#if DEBUG
extension Message {
    static let preview = Message(
        id: "msg_preview_001",
        title: "新商机提醒",
        content: "您有一个新的潜在客户，请及时跟进处理。",
        type: .lead,
        status: .unread,
        relatedId: "lead_preview_001",
        relatedType: .lead,
        sender: MessageSender(
            id: "system",
            name: "系统消息",
            avatarUrl: nil,
            type: .system
        ),
        createdAt: Date(),
        actions: [
            MessageAction(
                id: "view",
                title: "查看详情",
                type: .openDetail,
                payload: "lead_preview_001",
                style: .primary
            )
        ]
    )
    
    ⚠️ 注意：已移除 previewList 假数据，请使用 MessageViewModel.loadMessages() 从服务器获取真实数据
}
#endif
