//
//  InterceptTask.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  截客任务数据模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

// MARK: - 截客任务模型

/// 截客任务信息模型
/// 
/// 存储截客任务的详细信息，包括平台配置、关键词设置、执行结果等。
/// 实现了Codable协议，支持JSON序列化和本地存储。
struct InterceptTask: Codable, Identifiable, Equatable {
    
    // MARK: - 基本信息
    
    /// 任务ID - 唯一标识
    var id: String
    
    /// 任务编号 - 业务编号
    var taskNo: String
    
    /// 任务名称
    var name: String
    
    /// 任务描述
    var description: String?
    
    /// 任务状态
    var status: TaskStatus
    
    /// 任务优先级
    var priority: TaskPriority
    
    // MARK: - 平台配置
    
    /// 目标平台
    var platform: SourcePlatform
    
    /// 关联的社媒账号ID
    var accountId: String?
    
    /// 关联的社媒账号名称
    var accountName: String?
    
    // MARK: - 关键词配置
    
    /// 关键词列表
    var keywords: [String]
    
    /// 关键词匹配模式
    var keywordMatchMode: KeywordMatchMode
    
    /// 排除关键词列表
    var excludeKeywords: [String]
    
    /// 搜索时间范围（小时）
    var searchTimeRange: Int
    
    /// 搜索排序方式
    var sortBy: SearchSortBy
    
    // MARK: - 筛选配置
    
    /// 最小点赞数
    var minLikes: Int?
    
    /// 最小评论数
    var minComments: Int?
    
    /// 发布时间限制（小时）
    var maxPublishHours: Int?
    
    /// 用户粉丝数限制
    var minFollowers: Int?
    
    /// 只搜索认证用户
    var onlyVerified: Bool
    
    // MARK: - 执行配置
    
    /// 最大搜索次数
    var maxSearchCount: Int
    
    /// 当前搜索次数
    var currentSearchCount: Int
    
    /// 最大截客数量
    var maxInterceptCount: Int
    
    /// 已截客数量
    var interceptedCount: Int
    
    /// 执行间隔（秒）
    var executionInterval: Int
    
    // MARK: - 触达配置
    
    /// 是否自动触达
    var autoReachOut: Bool
    
    /// 触达方式
    var reachOutMethods: [ReachOutMethod]
    
    /// 触达模板ID
    var messageTemplateId: String?
    
    // MARK: - 执行结果
    
    /// 已截取的商机ID列表
    var interceptedLeadIds: [String]
    
    /// 已截取的商机列表
    var interceptedLeads: [Lead]?
    
    /// 最后执行时间
    var lastExecutedAt: Date?
    
    /// 下次执行时间
    var nextExecuteAt: Date?
    
    /// 任务执行日志
    var executionLogs: [InterceptLog]
    
    /// 任务结果摘要
    var result: String?
    
    // MARK: - 时间信息
    
    /// 计划开始时间
    var scheduledStartAt: Date?
    
    /// 计划结束时间
    var scheduledEndAt: Date?
    
    /// 实际开始时间
    var actualStartAt: Date?
    
    /// 实际结束时间
    var actualEndAt: Date?
    
    // MARK: - 归属信息
    
    /// 创建人ID
    var creatorId: String
    
    /// 创建人名称
    var creatorName: String
    
    /// 租户ID
    var tenantId: String
    
    // MARK: - 时间戳
    
    /// 创建时间
    var createdAt: Date
    
    /// 更新时间
    var updatedAt: Date
    
    // MARK: - 初始化
    
    init(
        id: String = UUID().uuidString,
        taskNo: String = "",
        name: String,
        description: String? = nil,
        status: TaskStatus = .pending,
        priority: TaskPriority = .medium,
        platform: SourcePlatform,
        accountId: String? = nil,
        accountName: String? = nil,
        keywords: [String] = [],
        keywordMatchMode: KeywordMatchMode = .any,
        excludeKeywords: [String] = [],
        searchTimeRange: Int = 24,
        sortBy: SearchSortBy = .latest,
        minLikes: Int? = nil,
        minComments: Int? = nil,
        maxPublishHours: Int? = nil,
        minFollowers: Int? = nil,
        onlyVerified: Bool = false,
        maxSearchCount: Int = 100,
        currentSearchCount: Int = 0,
        maxInterceptCount: Int = 50,
        interceptedCount: Int = 0,
        executionInterval: Int = 30,
        autoReachOut: Bool = false,
        reachOutMethods: [ReachOutMethod] = [],
        messageTemplateId: String? = nil,
        interceptedLeadIds: [String] = [],
        interceptedLeads: [Lead]? = nil,
        lastExecutedAt: Date? = nil,
        nextExecuteAt: Date? = nil,
        executionLogs: [InterceptLog] = [],
        result: String? = nil,
        scheduledStartAt: Date? = nil,
        scheduledEndAt: Date? = nil,
        actualStartAt: Date? = nil,
        actualEndAt: Date? = nil,
        creatorId: String,
        creatorName: String = "",
        tenantId: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.taskNo = taskNo
        self.name = name
        self.description = description
        self.status = status
        self.priority = priority
        self.platform = platform
        self.accountId = accountId
        self.accountName = accountName
        self.keywords = keywords
        self.keywordMatchMode = keywordMatchMode
        self.excludeKeywords = excludeKeywords
        self.searchTimeRange = searchTimeRange
        self.sortBy = sortBy
        self.minLikes = minLikes
        self.minComments = minComments
        self.maxPublishHours = maxPublishHours
        self.minFollowers = minFollowers
        self.onlyVerified = onlyVerified
        self.maxSearchCount = maxSearchCount
        self.currentSearchCount = currentSearchCount
        self.maxInterceptCount = maxInterceptCount
        self.interceptedCount = interceptedCount
        self.executionInterval = executionInterval
        self.autoReachOut = autoReachOut
        self.reachOutMethods = reachOutMethods
        self.messageTemplateId = messageTemplateId
        self.interceptedLeadIds = interceptedLeadIds
        self.interceptedLeads = interceptedLeads
        self.lastExecutedAt = lastExecutedAt
        self.nextExecuteAt = nextExecuteAt
        self.executionLogs = executionLogs
        self.result = result
        self.scheduledStartAt = scheduledStartAt
        self.scheduledEndAt = scheduledEndAt
        self.actualStartAt = actualStartAt
        self.actualEndAt = actualEndAt
        self.creatorId = creatorId
        self.creatorName = creatorName
        self.tenantId = tenantId
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
    
    // MARK: - 计算属性
    
    /// 任务进度百分比
    var progress: Int {
        guard maxSearchCount > 0 else { return 0 }
        return min(Int(Double(currentSearchCount) / Double(maxSearchCount) * 100), 100)
    }
    
    /// 是否已达到截客上限
    var isInterceptLimitReached: Bool {
        interceptedCount >= maxInterceptCount
    }
    
    /// 关键词字符串（用于显示）
    var keywordsDisplay: String {
        keywords.joined(separator: ", ")
    }
    
    /// 执行状态描述
    var executionStatusText: String {
        switch status {
        case .pending:
            return "待执行"
        case .running:
            return "已搜索 \(currentSearchCount)/\(maxSearchCount) 次，截客 \(interceptedCount) 人"
        case .paused:
            return "已暂停 - 已截客 \(interceptedCount) 人"
        case .completed:
            return "已完成 - 共截客 \(interceptedCount) 人"
        case .failed:
            return "执行失败"
        case .cancelled:
            return "已取消"
        }
    }
}

// MARK: - 关键词匹配模式

/// 关键词匹配模式枚举
enum KeywordMatchMode: String, Codable, CaseIterable {
    /// 匹配任意关键词
    case any = "any"
    
    /// 匹配所有关键词
    case all = "all"
    
    /// 精确匹配
    case exact = "exact"
    
    /// 模式名称
    var name: String {
        switch self {
        case .any: return "任意匹配"
        case .all: return "全部匹配"
        case .exact: return "精确匹配"
        }
    }
    
    /// 模式描述
    var description: String {
        switch self {
        case .any: return "包含任一关键词即可"
        case .all: return "必须包含所有关键词"
        case .exact: return "精确匹配整个短语"
        }
    }
}

// MARK: - 搜索排序方式

/// 搜索排序方式枚举
enum SearchSortBy: String, Codable, CaseIterable {
    /// 最新发布
    case latest = "latest"
    
    /// 最多点赞
    case mostLiked = "most_liked"
    
    /// 最多评论
    case mostCommented = "most_commented"
    
    /// 综合排序
    case comprehensive = "comprehensive"
    
    /// 排序名称
    var name: String {
        switch self {
        case .latest: return "最新发布"
        case .mostLiked: return "最多点赞"
        case .mostCommented: return "最多评论"
        case .comprehensive: return "综合排序"
        }
    }
}

// MARK: - 触达方式

/// 触达方式枚举
enum ReachOutMethod: String, Codable, CaseIterable {
    /// 私信
    case privateMessage = "private_message"
    
    /// 评论
    case comment = "comment"
    
    /// 关注
    case follow = "follow"
    
    /// 点赞
    case like = "like"
    
    /// 方式名称
    var name: String {
        switch self {
        case .privateMessage: return "发送私信"
        case .comment: return "评论互动"
        case .follow: return "关注用户"
        case .like: return "点赞内容"
        }
    }
    
    /// 方式图标
    var iconName: String {
        switch self {
        case .privateMessage: return "message.fill"
        case .comment: return "bubble.left.fill"
        case .follow: return "person.badge.plus"
        case .like: return "heart.fill"
        }
    }
}

// MARK: - 截客日志

/// 截客任务执行日志
struct InterceptLog: Codable, Identifiable, Equatable {
    
    /// 日志ID
    var id: String
    
    /// 任务ID
    var taskId: String
    
    /// 日志级别
    var level: LogLevel
    
    /// 日志内容
    var message: String
    
    /// 关联的商机ID
    var leadId: String?
    
    /// 关联的用户ID
    var userId: String?
    
    /// 关联的用户名
    var username: String?
    
    /// 关联的内容URL
    var contentUrl: String?
    
    /// 创建时间
    var createdAt: Date
    
    init(
        id: String = UUID().uuidString,
        taskId: String,
        level: LogLevel = .info,
        message: String,
        leadId: String? = nil,
        userId: String? = nil,
        username: String? = nil,
        contentUrl: String? = nil,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.taskId = taskId
        self.level = level
        self.message = message
        self.leadId = leadId
        self.userId = userId
        self.username = username
        self.contentUrl = contentUrl
        self.createdAt = createdAt
    }
}

// MARK: - 截客任务筛选条件

/// 截客任务筛选条件
struct InterceptTaskFilter: Codable, Equatable {
    
    /// 任务状态
    var statuses: [TaskStatus]?
    
    /// 目标平台
    var platforms: [SourcePlatform]?
    
    /// 关键词搜索
    var keyword: String?
    
    /// 按创建时间筛选
    var createdAt: DateRange?
    
    /// 排序字段
    var sortBy: TaskSortBy = .createdAt
    
    /// 排序方向
    var sortOrder: SortOrder = .descending
    
    /// 页码
    var page: Int = 1
    
    /// 每页数量
    var pageSize: Int = 20
    
    /// 是否为空筛选
    var isEmpty: Bool {
        statuses == nil &&
        platforms == nil &&
        keyword == nil &&
        createdAt == nil
    }
    
    /// 清空筛选条件
    mutating func clear() {
        statuses = nil
        platforms = nil
        keyword = nil
        createdAt = nil
        page = 1
    }
}

// MARK: - 截客任务统计

/// 截客任务统计信息
struct InterceptTaskStatistics: Codable {
    
    /// 总任务数
    var totalCount: Int
    
    /// 待执行数
    var pendingCount: Int
    
    /// 执行中数
    var runningCount: Int
    
    /// 已完成数
    var completedCount: Int
    
    /// 已暂停数
    var pausedCount: Int
    
    /// 失败数
    var failedCount: Int
    
    /// 今日截客数
    var todayInterceptedCount: Int
    
    /// 本周截客数
    var weeklyInterceptedCount: Int
    
    /// 本月截客数
    var monthlyInterceptedCount: Int
    
    /// 总截客数
    var totalInterceptedCount: Int
    
    /// 按平台统计
    var platformStats: [PlatformStat]
    
    /// 按关键词统计
    var keywordStats: [KeywordStat]
}

/// 平台统计
struct PlatformStat: Codable, Identifiable {
    var id: String { platform.rawValue }
    var platform: SourcePlatform
    var taskCount: Int
    var interceptedCount: Int
}

/// 关键词统计
struct KeywordStat: Codable, Identifiable {
    var id: String { keyword }
    var keyword: String
    var interceptedCount: Int
    var conversionRate: Double
}

// MARK: - 预览

#if DEBUG
extension InterceptTask {
    static let preview = InterceptTask(
        id: "intercept_001",
        taskNo: "IT202401001",
        name: "抖音AI获客截客",
        description: "在抖音平台搜索AI获客相关关键词，截取意向客户",
        status: .running,
        priority: .high,
        platform: .douyin,
        accountName: "北极星官方账号",
        keywords: ["AI获客", "智能获客", "获客系统"],
        keywordMatchMode: .any,
        excludeKeywords: ["招聘", "求职"],
        searchTimeRange: 24,
        sortBy: .latest,
        minLikes: 10,
        maxInterceptCount: 50,
        interceptedCount: 23,
        currentSearchCount: 45,
        autoReachOut: true,
        reachOutMethods: [.privateMessage, .follow],
        interceptedLeadIds: ["lead_001", "lead_002"],
        executionLogs: [
            InterceptLog(taskId: "intercept_001", level: .info, message: "开始执行截客任务"),
            InterceptLog(taskId: "intercept_001", level: .info, message: "搜索关键词: AI获客"),
            InterceptLog(taskId: "intercept_001", level: .info, message: "成功截取潜在客户: 张三")
        ],
        actualStartAt: Date(),
        creatorId: "user_001",
        creatorName: "李明",
        tenantId: "tenant_001"
    )
    
    static let previewList: [InterceptTask] = [
        preview,
        InterceptTask(
            id: "intercept_002",
            taskNo: "IT202401002",
            name: "小红书营销截客",
            description: "在小红书平台搜索营销相关话题",
            status: .completed,
            priority: .medium,
            platform: .xiaohongshu,
            accountName: "北极星小红书",
            keywords: ["私域流量", "获客技巧"],
            keywordMatchMode: .any,
            maxInterceptCount: 30,
            interceptedCount: 28,
            currentSearchCount: 100,
            result: "成功截取28位意向客户，转化率15%",
            actualStartAt: Date().addingTimeInterval(-86400),
            actualEndAt: Date().addingTimeInterval(-43200),
            creatorId: "user_001",
            tenantId: "tenant_001"
        ),
        InterceptTask(
            id: "intercept_003",
            taskNo: "IT202401003",
            name: "微信视频号截客",
            description: "在视频号搜索AI相关话题",
            status: .pending,
            priority: .low,
            platform: .wechatChannels,
            keywords: ["人工智能", "AI工具"],
            maxInterceptCount: 20,
            creatorId: "user_001",
            tenantId: "tenant_001"
        )
    ]
}

extension InterceptLog {
    static let previewList: [InterceptLog] = [
        InterceptLog(
            taskId: "intercept_001",
            level: .info,
            message: "开始执行截客任务",
            createdAt: Date().addingTimeInterval(-3600)
        ),
        InterceptLog(
            taskId: "intercept_001",
            level: .info,
            message: "搜索关键词: AI获客",
            createdAt: Date().addingTimeInterval(-3500)
        ),
        InterceptLog(
            taskId: "intercept_001",
            level: .info,
            message: "找到相关内容 15 条",
            createdAt: Date().addingTimeInterval(-3400)
        ),
        InterceptLog(
            taskId: "intercept_001",
            level: .warning,
            message: "用户 @zhangsan 已存在，跳过",
            username: "zhangsan",
            createdAt: Date().addingTimeInterval(-3300)
        ),
        InterceptLog(
            taskId: "intercept_001",
            level: .info,
            message: "成功截取潜在客户: 李四",
            username: "李四",
            leadId: "lead_001",
            createdAt: Date().addingTimeInterval(-3200)
        ),
        InterceptLog(
            taskId: "intercept_001",
            level: .info,
            message: "发送私信触达",
            username: "李四",
            createdAt: Date().addingTimeInterval(-3100)
        )
    ]
}
#endif
