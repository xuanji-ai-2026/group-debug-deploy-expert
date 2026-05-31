//
//  AcquireTask.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  获客任务数据模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

// MARK: - 获客任务模型

/// 获客任务信息模型
/// 
/// 存储主动获客任务的详细信息，包括渠道配置、目标人群、执行计划等。
/// 实现了Codable协议，支持JSON序列化和本地存储。
struct AcquireTask: Codable, Identifiable, Equatable {
    
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
    
    /// 获客类型
    var acquireType: AcquireType
    
    // MARK: - 渠道配置
    
    /// 获客渠道列表
    var channels: [AcquireChannel]
    
    /// 主渠道平台
    var primaryPlatform: SourcePlatform
    
    // MARK: - 目标人群配置
    
    /// 目标人群设置
    var targetAudience: TargetAudience
    
    /// 目标地区
    var targetLocations: [String]
    
    /// 目标行业
    var targetIndustries: [String]
    
    /// 目标职位
    var targetPositions: [String]
    
    // MARK: - 内容配置
    
    /// 内容类型
    var contentType: ContentType
    
    /// 内容主题
    var contentTopics: [String]
    
    /// 内容模板ID
    var contentTemplateId: String?
    
    /// 是否使用AI生成内容
    var useAIGeneration: Bool
    
    /// AI生成提示词
    var aiPrompt: String?
    
    // MARK: - 执行配置
    
    /// 执行策略
    var executionStrategy: ExecutionStrategy
    
    /// 每日执行次数
    var dailyExecutionCount: Int
    
    /// 每次获客数量
    var acquireBatchSize: Int
    
    /// 执行间隔（分钟）
    var executionInterval: Int
    
    /// 执行时间段
    var executionTimeSlots: [TimeSlot]
    
    /// 执行日期（周几）
    var executionDays: [WeekDay]
    
    // MARK: - 触达配置
    
    /// 触达方式
    var reachOutMethod: ReachOutMethod
    
    /// 触达模板
    var reachOutTemplate: String?
    
    /// 是否自动跟进
    var autoFollowUp: Bool
    
    /// 跟进间隔（小时）
    var followUpInterval: Int
    
    /// 最大跟进次数
    var maxFollowUpCount: Int
    
    // MARK: - 执行结果
    
    /// 今日已执行次数
    var todayExecutionCount: Int
    
    /// 总触达人数
    var totalReachedCount: Int
    
    /// 回复人数
    var repliedCount: Int
    
    /// 转化人数
    var convertedCount: Int
    
    /// 获客成本（元）
    var acquisitionCost: Double?
    
    /// 获客列表
    var acquiredLeads: [Lead]?
    
    /// 执行日志
    var executionLogs: [AcquireLog]
    
    /// 任务结果
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
        acquireType: AcquireType = .content,
        channels: [AcquireChannel] = [],
        primaryPlatform: SourcePlatform = .douyin,
        targetAudience: TargetAudience = TargetAudience(),
        targetLocations: [String] = [],
        targetIndustries: [String] = [],
        targetPositions: [String] = [],
        contentType: ContentType = .article,
        contentTopics: [String] = [],
        contentTemplateId: String? = nil,
        useAIGeneration: Bool = false,
        aiPrompt: String? = nil,
        executionStrategy: ExecutionStrategy = .continuous,
        dailyExecutionCount: Int = 10,
        acquireBatchSize: Int = 20,
        executionInterval: Int = 60,
        executionTimeSlots: [TimeSlot] = [],
        executionDays: [WeekDay] = WeekDay.allCases,
        reachOutMethod: ReachOutMethod = .privateMessage,
        reachOutTemplate: String? = nil,
        autoFollowUp: Bool = false,
        followUpInterval: Int = 24,
        maxFollowUpCount: Int = 3,
        todayExecutionCount: Int = 0,
        totalReachedCount: Int = 0,
        repliedCount: Int = 0,
        convertedCount: Int = 0,
        acquisitionCost: Double? = nil,
        acquiredLeads: [Lead]? = nil,
        executionLogs: [AcquireLog] = [],
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
        self.acquireType = acquireType
        self.channels = channels
        self.primaryPlatform = primaryPlatform
        self.targetAudience = targetAudience
        self.targetLocations = targetLocations
        self.targetIndustries = targetIndustries
        self.targetPositions = targetPositions
        self.contentType = contentType
        self.contentTopics = contentTopics
        self.contentTemplateId = contentTemplateId
        self.useAIGeneration = useAIGeneration
        self.aiPrompt = aiPrompt
        self.executionStrategy = executionStrategy
        self.dailyExecutionCount = dailyExecutionCount
        self.acquireBatchSize = acquireBatchSize
        self.executionInterval = executionInterval
        self.executionTimeSlots = executionTimeSlots
        self.executionDays = executionDays
        self.reachOutMethod = reachOutMethod
        self.reachOutTemplate = reachOutTemplate
        self.autoFollowUp = autoFollowUp
        self.followUpInterval = followUpInterval
        self.maxFollowUpCount = maxFollowUpCount
        self.todayExecutionCount = todayExecutionCount
        self.totalReachedCount = totalReachedCount
        self.repliedCount = repliedCount
        self.convertedCount = convertedCount
        self.acquisitionCost = acquisitionCost
        self.acquiredLeads = acquiredLeads
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
    
    /// 回复率
    var replyRate: Double {
        guard totalReachedCount > 0 else { return 0 }
        return Double(repliedCount) / Double(totalReachedCount) * 100
    }
    
    /// 转化率
    var conversionRate: Double {
        guard totalReachedCount > 0 else { return 0 }
        return Double(convertedCount) / Double(totalReachedCount) * 100
    }
    
    /// 平均获客成本
    var avgAcquisitionCost: Double {
        guard convertedCount > 0, let cost = acquisitionCost else { return 0 }
        return cost / Double(convertedCount)
    }
    
    /// 任务进度百分比
    var progress: Int {
        guard dailyExecutionCount > 0 else { return 0 }
        return min(Int(Double(todayExecutionCount) / Double(dailyExecutionCount) * 100), 100)
    }
    
    /// 执行状态描述
    var executionStatusText: String {
        switch status {
        case .pending:
            return "待执行"
        case .running:
            return "今日已执行 \(todayExecutionCount)/\(dailyExecutionCount) 次"
        case .paused:
            return "已暂停"
        case .completed:
            return "已完成 - 共转化 \(convertedCount) 人"
        case .failed:
            return "执行失败"
        case .cancelled:
            return "已取消"
        }
    }
}

// MARK: - 获客类型

/// 获客类型枚举
enum AcquireType: String, Codable, CaseIterable {
    /// 内容获客
    case content = "content"
    
    /// 社群获客
    case community = "community"
    
    /// 活动获客
    case event = "event"
    
    /// 合作获客
    case partnership = "partnership"
    
    /// 付费获客
    case paid = "paid"
    
    /// 类型名称
    var name: String {
        switch self {
        case .content: return "内容获客"
        case .community: return "社群获客"
        case .event: return "活动获客"
        case .partnership: return "合作获客"
        case .paid: return "付费获客"
        }
    }
    
    /// 类型图标
    var iconName: String {
        switch self {
        case .content: return "doc.text"
        case .community: return "person.3"
        case .event: return "calendar"
        case .partnership: return "handshake"
        case .paid: return "dollarsign.circle"
        }
    }
    
    /// 类型描述
    var description: String {
        switch self {
        case .content: return "通过发布优质内容吸引客户"
        case .community: return "通过社群运营获取客户"
        case .event: return "通过线上线下活动获客"
        case .partnership: return "通过合作伙伴推荐获客"
        case .paid: return "通过付费推广获客"
        }
    }
}

// MARK: - 获客渠道

/// 获客渠道模型
struct AcquireChannel: Codable, Identifiable, Equatable {
    
    /// 渠道ID
    var id: String
    
    /// 渠道平台
    var platform: SourcePlatform
    
    /// 渠道账号ID
    var accountId: String?
    
    /// 渠道账号名称
    var accountName: String?
    
    /// 渠道配置参数
    var config: [String: String]
    
    /// 是否启用
    var isEnabled: Bool
    
    /// 渠道权重（1-10）
    var weight: Int
    
    init(
        id: String = UUID().uuidString,
        platform: SourcePlatform,
        accountId: String? = nil,
        accountName: String? = nil,
        config: [String: String] = [:],
        isEnabled: Bool = true,
        weight: Int = 5
    ) {
        self.id = id
        self.platform = platform
        self.accountId = accountId
        self.accountName = accountName
        self.config = config
        self.isEnabled = isEnabled
        self.weight = weight
    }
}

// MARK: - 目标人群

/// 目标人群设置
struct TargetAudience: Codable, Equatable {
    
    /// 性别筛选
    var gender: GenderFilter
    
    /// 年龄范围
    var ageRange: AgeRange?
    
    /// 最小粉丝数
    var minFollowers: Int?
    
    /// 最大粉丝数
    var maxFollowers: Int?
    
    /// 活跃度要求
    var activityLevel: ActivityLevel
    
    /// 认证状态
    var verificationStatus: VerificationStatus
    
    /// 地域范围
    var locationScope: LocationScope
    
    /// 兴趣标签
    var interestTags: [String]
    
    init(
        gender: GenderFilter = .all,
        ageRange: AgeRange? = nil,
        minFollowers: Int? = nil,
        maxFollowers: Int? = nil,
        activityLevel: ActivityLevel = .medium,
        verificationStatus: VerificationStatus = .all,
        locationScope: LocationScope = .all,
        interestTags: [String] = []
    ) {
        self.gender = gender
        self.ageRange = ageRange
        self.minFollowers = minFollowers
        self.maxFollowers = maxFollowers
        self.activityLevel = activityLevel
        self.verificationStatus = verificationStatus
        self.locationScope = locationScope
        self.interestTags = interestTags
    }
}

/// 性别筛选
enum GenderFilter: String, Codable, CaseIterable {
    case all = "all"
    case male = "male"
    case female = "female"
    
    var name: String {
        switch self {
        case .all: return "全部"
        case .male: return "男性"
        case .female: return "女性"
        }
    }
}

/// 年龄范围
struct AgeRange: Codable, Equatable {
    var min: Int
    var max: Int
    
    static let `default` = AgeRange(min: 18, max: 65)
}

/// 活跃度级别
enum ActivityLevel: String, Codable, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case veryHigh = "very_high"
    
    var name: String {
        switch self {
        case .low: return "低活跃"
        case .medium: return "中等活跃"
        case .high: return "高活跃"
        case .veryHigh: return "非常活跃"
        }
    }
}

/// 认证状态
enum VerificationStatus: String, Codable, CaseIterable {
    case all = "all"
    case verified = "verified"
    case unverified = "unverified"
    
    var name: String {
        switch self {
        case .all: return "全部"
        case .verified: return "已认证"
        case .unverified: return "未认证"
        }
    }
}

/// 地域范围
enum LocationScope: String, Codable, CaseIterable {
    case all = "all"
    case local = "local"
    case national = "national"
    
    var name: String {
        switch self {
        case .all: return "不限"
        case .local: return "同城"
        case .national: return "全国"
        }
    }
}

// MARK: - 内容类型

/// 内容类型枚举
enum ContentType: String, Codable, CaseIterable {
    case article = "article"
    case video = "video"
    case image = "image"
    case live = "live"
    case audio = "audio"
    
    var name: String {
        switch self {
        case .article: return "图文"
        case .video: return "视频"
        case .image: return "图片"
        case .live: return "直播"
        case .audio: return "音频"
        }
    }
    
    var iconName: String {
        switch self {
        case .article: return "doc.text"
        case .video: return "video.fill"
        case .image: return "photo.fill"
        case .live: return "video.badge.waveform.fill"
        case .audio: return "waveform"
        }
    }
}

// MARK: - 执行策略

/// 执行策略枚举
enum ExecutionStrategy: String, Codable, CaseIterable {
    case continuous = "continuous"
    case scheduled = "scheduled"
    case smart = "smart"
    
    var name: String {
        switch self {
        case .continuous: return "持续执行"
        case .scheduled: return "定时执行"
        case .smart: return "智能执行"
        }
    }
    
    var description: String {
        switch self {
        case .continuous: return "任务创建后立即开始，按间隔持续执行"
        case .scheduled: return "在指定时间段内执行"
        case .smart: return "AI智能选择最佳时间执行"
        }
    }
}

// MARK: - 时间段

/// 执行时间段
struct TimeSlot: Codable, Identifiable, Equatable {
    var id: String = UUID().uuidString
    var startHour: Int
    var startMinute: Int
    var endHour: Int
    var endMinute: Int
    
    var displayText: String {
        String(format: "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
    }
    
    static let `default` = [
        TimeSlot(startHour: 9, startMinute: 0, endHour: 12, endMinute: 0),
        TimeSlot(startHour: 14, startMinute: 0, endHour: 18, endMinute: 0)
    ]
}

// MARK: - 星期

/// 星期枚举
enum WeekDay: String, Codable, CaseIterable, Identifiable {
    case monday = "monday"
    case tuesday = "tuesday"
    case wednesday = "wednesday"
    case thursday = "thursday"
    case friday = "friday"
    case saturday = "saturday"
    case sunday = "sunday"
    
    var id: String { rawValue }
    
    var name: String {
        switch self {
        case .monday: return "周一"
        case .tuesday: return "周二"
        case .wednesday: return "周三"
        case .thursday: return "周四"
        case .friday: return "周五"
        case .saturday: return "周六"
        case .sunday: return "周日"
        }
    }
    
    var shortName: String {
        switch self {
        case .monday: return "一"
        case .tuesday: return "二"
        case .wednesday: return "三"
        case .thursday: return "四"
        case .friday: return "五"
        case .saturday: return "六"
        case .sunday: return "日"
        }
    }
}

// MARK: - 获客日志

/// 获客任务执行日志
struct AcquireLog: Codable, Identifiable, Equatable {
    
    /// 日志ID
    var id: String
    
    /// 任务ID
    var taskId: String
    
    /// 日志级别
    var level: LogLevel
    
    /// 日志内容
    var message: String
    
    /// 渠道平台
    var platform: SourcePlatform?
    
    /// 关联的商机ID
    var leadId: String?
    
    /// 触达用户数
    var reachedCount: Int?
    
    /// 回复用户数
    var repliedCount: Int?
    
    /// 创建时间
    var createdAt: Date
    
    init(
        id: String = UUID().uuidString,
        taskId: String,
        level: LogLevel = .info,
        message: String,
        platform: SourcePlatform? = nil,
        leadId: String? = nil,
        reachedCount: Int? = nil,
        repliedCount: Int? = nil,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.taskId = taskId
        self.level = level
        self.message = message
        self.platform = platform
        self.leadId = leadId
        self.reachedCount = reachedCount
        self.repliedCount = repliedCount
        self.createdAt = createdAt
    }
}

// MARK: - 获客任务筛选条件

/// 获客任务筛选条件
struct AcquireTaskFilter: Codable, Equatable {
    
    /// 任务状态
    var statuses: [TaskStatus]?
    
    /// 获客类型
    var acquireTypes: [AcquireType]?
    
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
        acquireTypes == nil &&
        platforms == nil &&
        keyword == nil &&
        createdAt == nil
    }
    
    /// 清空筛选条件
    mutating func clear() {
        statuses = nil
        acquireTypes = nil
        platforms = nil
        keyword = nil
        createdAt = nil
        page = 1
    }
}

// MARK: - 获客任务统计

/// 获客任务统计信息
struct AcquireTaskStatistics: Codable {
    
    /// 总任务数
    var totalCount: Int
    
    /// 各状态数量
    var pendingCount: Int
    var runningCount: Int
    var completedCount: Int
    var pausedCount: Int
    var failedCount: Int
    
    /// 今日数据
    var todayReachedCount: Int
    var todayRepliedCount: Int
    var todayConvertedCount: Int
    
    /// 本周数据
    var weeklyReachedCount: Int
    var weeklyRepliedCount: Int
    var weeklyConvertedCount: Int
    
    /// 本月数据
    var monthlyReachedCount: Int
    var monthlyRepliedCount: Int
    var monthlyConvertedCount: Int
    
    /// 总数据
    var totalReachedCount: Int
    var totalRepliedCount: Int
    var totalConvertedCount: Int
    
    /// 平均回复率
    var avgReplyRate: Double
    
    /// 平均转化率
    var avgConversionRate: Double
    
    /// 平均获客成本
    var avgAcquisitionCost: Double
    
    /// 按平台统计
    var platformStats: [PlatformStat]
    
    /// 按类型统计
    var typeStats: [AcquireTypeStat]
    
    /// 趋势数据（最近30天）
    var trendData: [DailyStat]
}

/// 获客类型统计
struct AcquireTypeStat: Codable, Identifiable {
    var id: String { type.rawValue }
    var type: AcquireType
    var taskCount: Int
    var convertedCount: Int
    var conversionRate: Double
}

/// 每日统计
struct DailyStat: Codable, Identifiable {
    var id: String { date }
    var date: String
    var reachedCount: Int
    var repliedCount: Int
    var convertedCount: Int
}

// MARK: - 预览

#if DEBUG
extension AcquireTask {
    static let preview = AcquireTask(
        id: "acquire_001",
        taskNo: "AT202401001",
        name: "抖音内容获客计划",
        description: "通过发布AI获客相关内容，吸引意向客户",
        status: .running,
        priority: .high,
        acquireType: .content,
        channels: [
            AcquireChannel(platform: .douyin, accountName: "北极星AI官方", weight: 10),
            AcquireChannel(platform: .xiaohongshu, accountName: "北极星获客", weight: 8)
        ],
        primaryPlatform: .douyin,
        targetAudience: TargetAudience(
            gender: .all,
            ageRange: AgeRange(min: 25, max: 45),
            minFollowers: 100,
            activityLevel: .medium,
            interestTags: ["AI", "获客", "营销"]
        ),
        targetLocations: ["北京", "上海", "深圳"],
        targetIndustries: ["互联网", "企业服务"],
        contentType: .video,
        contentTopics: ["AI获客技巧", "私域流量运营"],
        useAIGeneration: true,
        aiPrompt: "生成关于AI获客的专业内容",
        executionStrategy: .smart,
        dailyExecutionCount: 5,
        acquireBatchSize: 50,
        executionDays: [.monday, .tuesday, .wednesday, .thursday, .friday],
        reachOutMethod: .privateMessage,
        autoFollowUp: true,
        todayExecutionCount: 3,
        totalReachedCount: 156,
        repliedCount: 42,
        convertedCount: 15,
        executionLogs: [
            AcquireLog(taskId: "acquire_001", message: "开始执行获客任务"),
            AcquireLog(taskId: "acquire_001", message: "发布内容到抖音平台"),
            AcquireLog(taskId: "acquire_001", message: "触达50位目标用户")
        ],
        actualStartAt: Date(),
        creatorId: "user_001",
        creatorName: "李明",
        tenantId: "tenant_001"
    )
    
    static let previewList: [AcquireTask] = [
        preview,
        AcquireTask(
            id: "acquire_002",
            taskNo: "AT202401002",
            name: "微信社群运营",
            description: "通过社群活动获取潜在客户",
            status: .completed,
            priority: .medium,
            acquireType: .community,
            primaryPlatform: .wechatPublic,
            totalReachedCount: 300,
            repliedCount: 89,
            convertedCount: 32,
            result: "活动圆满成功，共转化32位客户",
            actualStartAt: Date().addingTimeInterval(-172800),
            actualEndAt: Date().addingTimeInterval(-86400),
            creatorId: "user_001",
            tenantId: "tenant_001"
        ),
        AcquireTask(
            id: "acquire_003",
            taskNo: "AT202401003",
            name: "小红书种草计划",
            description: "通过小红书内容种草吸引客户",
            status: .pending,
            priority: .medium,
            acquireType: .content,
            primaryPlatform: .xiaohongshu,
            contentType: .image,
            creatorId: "user_001",
            tenantId: "tenant_001"
        )
    ]
}

extension AcquireLog {
    static let previewList: [AcquireLog] = [
        AcquireLog(
            taskId: "acquire_001",
            level: .info,
            message: "开始执行获客任务",
            createdAt: Date().addingTimeInterval(-3600)
        ),
        AcquireLog(
            taskId: "acquire_001",
            level: .info,
            message: "AI生成内容完成",
            createdAt: Date().addingTimeInterval(-3500)
        ),
        AcquireLog(
            taskId: "acquire_001",
            level: .info,
            message: "发布内容到抖音平台",
            platform: .douyin,
            createdAt: Date().addingTimeInterval(-3400)
        ),
        AcquireLog(
            taskId: "acquire_001",
            level: .info,
            message: "触达50位目标用户",
            reachedCount: 50,
            createdAt: Date().addingTimeInterval(-3300)
        ),
        AcquireLog(
            taskId: "acquire_001",
            level: .info,
            message: "收到12条用户回复",
            repliedCount: 12,
            createdAt: Date().addingTimeInterval(-3200)
        ),
        AcquireLog(
            taskId: "acquire_001",
            level: .info,
            message: "转化3位意向客户",
            createdAt: Date().addingTimeInterval(-3100)
        )
    ]
}
#endif
