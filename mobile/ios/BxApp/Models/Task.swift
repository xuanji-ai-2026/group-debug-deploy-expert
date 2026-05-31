//
//  Task.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  任务数据模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

// MARK: - 任务模型

/// 任务信息模型
/// 
/// 存储任务的基本信息，包括任务类型、进度、状态等。
/// 实现了Codable协议，支持JSON序列化和本地存储。
struct Task: Codable, Identifiable, Equatable {
    
    // MARK: - 基本信息
    
    /// 任务ID - 唯一标识
    var id: String
    
    /// 任务编号 - 业务编号
    var taskNo: String
    
    /// 任务名称
    var name: String
    
    /// 任务描述
    var description: String?
    
    /// 任务类型
    var type: TaskType
    
    /// 任务状态
    var status: TaskStatus
    
    /// 任务优先级
    var priority: TaskPriority
    
    // MARK: - 执行信息
    
    /// 关联的商机ID
    var leadId: String?
    
    /// 关联的商机名称
    var leadName: String?
    
    /// 关联的社媒账号ID
    var accountId: String?
    
    /// 关联的社媒平台
    var platform: SourcePlatform?
    
    // MARK: - 进度信息
    
    /// 任务进度（0-100）
    var progress: Int
    
    /// 总步骤数
    var totalSteps: Int
    
    /// 已完成步骤数
    var completedSteps: Int
    
    /// 当前执行步骤
    var currentStep: Int
    
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
    
    /// 执行人ID
    var executorId: String
    
    /// 执行人名称
    var executorName: String
    
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
        type: TaskType,
        status: TaskStatus = .pending,
        priority: TaskPriority = .medium,
        leadId: String? = nil,
        leadName: String? = nil,
        accountId: String? = nil,
        platform: SourcePlatform? = nil,
        progress: Int = 0,
        totalSteps: Int = 0,
        completedSteps: Int = 0,
        currentStep: Int = 0,
        result: String? = nil,
        scheduledStartAt: Date? = nil,
        scheduledEndAt: Date? = nil,
        actualStartAt: Date? = nil,
        actualEndAt: Date? = nil,
        creatorId: String,
        creatorName: String = "",
        executorId: String,
        executorName: String = "",
        tenantId: String,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.taskNo = taskNo
        self.name = name
        self.description = description
        self.type = type
        self.status = status
        self.priority = priority
        self.leadId = leadId
        self.leadName = leadName
        self.accountId = accountId
        self.platform = platform
        self.progress = progress
        self.totalSteps = totalSteps
        self.completedSteps = completedSteps
        self.currentStep = currentStep
        self.result = result
        self.scheduledStartAt = scheduledStartAt
        self.scheduledEndAt = scheduledEndAt
        self.actualStartAt = actualStartAt
        self.actualEndAt = actualEndAt
        self.creatorId = creatorId
        self.creatorName = creatorName
        self.executorId = executorId
        self.executorName = executorName
        self.tenantId = tenantId
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

// MARK: - 任务类型

/// 任务类型枚举
enum TaskType: String, Codable, CaseIterable {
    /// 截客任务
    case intercept = "intercept"
    
    /// 主动获客
    case activeAcquire = "active_acquire"
    
    /// 内容发布
    case contentPublish = "content_publish"
    
    /// 私信触达
    case privateMessage = "private_message"
    
    /// 养号任务
    case accountMaintain = "account_maintain"
    
    /// 意向识别
    case intentionRecognition = "intention_recognition"
    
    /// AI生成
    case aiGenerate = "ai_generate"
    
    /// 自定义任务
    case custom = "custom"
    
    /// 类型名称
    var name: String {
        switch self {
        case .intercept: return "截客任务"
        case .activeAcquire: return "主动获客"
        case .contentPublish: return "内容发布"
        case .privateMessage: return "私信触达"
        case .accountMaintain: return "养号任务"
        case .intentionRecognition: return "意向识别"
        case .aiGenerate: return "AI生成"
        case .custom: return "自定义任务"
        }
    }
    
    /// 类型图标
    var iconName: String {
        switch self {
        case .intercept: return "target"
        case .activeAcquire: return "person.badge.plus"
        case .contentPublish: return "square.and.arrow.up"
        case .privateMessage: return "message"
        case .accountMaintain: return "heart"
        case .intentionRecognition: return "brain"
        case .aiGenerate: return "sparkles"
        case .custom: return "doc.text"
        }
    }
    
    /// 类型颜色
    var colorName: String {
        switch self {
        case .intercept: return "red"
        case .activeAcquire: return "green"
        case .contentPublish: return "blue"
        case .privateMessage: return "purple"
        case .accountMaintain: return "orange"
        case .intentionRecognition: return "pink"
        case .aiGenerate: return "indigo"
        case .custom: return "gray"
        }
    }
}

// MARK: - 任务状态

/// 任务状态枚举
enum TaskStatus: String, Codable, CaseIterable {
    /// 待执行
    case pending = "pending"
    
    /// 执行中
    case running = "running"
    
    /// 已暂停
    case paused = "paused"
    
    /// 已完成
    case completed = "completed"
    
    /// 已失败
    case failed = "failed"
    
    /// 已取消
    case cancelled = "cancelled"
    
    /// 状态名称
    var name: String {
        switch self {
        case .pending: return "待执行"
        case .running: return "执行中"
        case .paused: return "已暂停"
        case .completed: return "已完成"
        case .failed: return "已失败"
        case .cancelled: return "已取消"
        }
    }
    
    /// 状态颜色
    var colorName: String {
        switch self {
        case .pending: return "gray"
        case .running: return "blue"
        case .paused: return "orange"
        case .completed: return "green"
        case .failed: return "red"
        case .cancelled: return "gray"
        }
    }
    
    /// 是否可以执行
    var canExecute: Bool {
        self == .pending || self == .paused
    }
    
    /// 是否可以暂停
    var canPause: Bool {
        self == .running
    }
    
    /// 是否可以取消
    var canCancel: Bool {
        switch self {
        case .pending, .running, .paused: return true
        default: return false
        }
    }
    
    /// 是否可以重试
    var canRetry: Bool {
        self == .failed || self == .cancelled
    }
}

// MARK: - 任务优先级

/// 任务优先级枚举
enum TaskPriority: String, Codable, CaseIterable {
    /// 低优先级
    case low = "low"
    
    /// 中优先级
    case medium = "medium"
    
    /// 高优先级
    case high = "high"
    
    /// 紧急
    case urgent = "urgent"
    
    /// 优先级数值
    var level: Int {
        switch self {
        case .low: return 0
        case .medium: return 1
        case .high: return 2
        case .urgent: return 3
        }
    }
    
    /// 优先级名称
    var name: String {
        switch self {
        case .low: return "低"
        case .medium: return "中"
        case .high: return "高"
        case .urgent: return "紧急"
        }
    }
    
    /// 优先级图标
    var iconName: String {
        switch self {
        case .low: return "arrow.down"
        case .medium: return "minus"
        case .high: return "arrow.up"
        case .urgent: return "exclamationmark.2"
        }
    }
    
    /// 优先级颜色
    var colorName: String {
        switch self {
        case .low: return "gray"
        case .medium: return "blue"
        case .high: return "orange"
        case .urgent: return "red"
        }
    }
}

// MARK: - 任务步骤

/// 任务步骤模型
struct TaskStep: Codable, Identifiable, Equatable {
    
    /// 步骤ID
    var id: String
    
    /// 步骤序号
    var stepIndex: Int
    
    /// 步骤名称
    var name: String
    
    /// 步骤描述
    var description: String?
    
    /// 步骤状态
    var status: TaskStepStatus
    
    /// 开始时间
    var startedAt: Date?
    
    /// 结束时间
    var completedAt: Date?
    
    /// 步骤结果
    var result: String?
    
    /// 步骤日志
    var logs: [TaskStepLog]
    
    init(
        id: String = UUID().uuidString,
        stepIndex: Int,
        name: String,
        description: String? = nil,
        status: TaskStepStatus = .pending,
        startedAt: Date? = nil,
        completedAt: Date? = nil,
        result: String? = nil,
        logs: [TaskStepLog] = []
    ) {
        self.id = id
        self.stepIndex = stepIndex
        self.name = name
        self.description = description
        self.status = status
        self.startedAt = startedAt
        self.completedAt = completedAt
        self.result = result
        self.logs = logs
    }
}

/// 任务步骤状态
enum TaskStepStatus: String, Codable {
    /// 待执行
    case pending = "pending"
    
    /// 执行中
    case running = "running"
    
    /// 已完成
    case completed = "completed"
    
    /// 已失败
    case failed = "failed"
    
    /// 已跳过
    case skipped = "skipped"
}

/// 任务步骤日志
struct TaskStepLog: Codable, Identifiable {
    
    /// 日志ID
    var id: String
    
    /// 步骤ID
    var stepId: String
    
    /// 日志级别
    var level: LogLevel
    
    /// 日志内容
    var message: String
    
    /// 创建时间
    var createdAt: Date
    
    init(
        id: String = UUID().uuidString,
        stepId: String,
        level: LogLevel,
        message: String,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.stepId = stepId
        self.level = level
        self.message = message
        self.createdAt = createdAt
    }
}

/// 日志级别
enum LogLevel: String, Codable {
    /// 信息
    case info = "info"
    
    /// 警告
    case warning = "warning"
    
    /// 错误
    case error = "error"
    
    /// 调试
    case debug = "debug"
    
    /// 图标名称
    var iconName: String {
        switch self {
        case .info: return "info.circle"
        case .warning: return "exclamationmark.triangle"
        case .error: return "xmark.circle"
        case .debug: return "ant"
        }
    }
    
    /// 颜色名称
    var colorName: String {
        switch self {
        case .info: return "blue"
        case .warning: return "orange"
        case .error: return "red"
        case .debug: return "gray"
        }
    }
}

// MARK: - 任务筛选条件

/// 任务筛选条件
struct TaskFilter: Codable, Equatable {
    
    /// 任务类型
    var types: [TaskType]?
    
    /// 任务状态
    var statuses: [TaskStatus]?
    
    /// 任务优先级
    var priorities: [TaskPriority]?
    
    /// 关键词搜索
    var keyword: String?
    
    /// 按创建时间筛选
    var createdAt: DateRange?
    
    /// 按计划时间筛选
    var scheduledAt: DateRange?
    
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
        types == nil &&
        statuses == nil &&
        priorities == nil &&
        keyword == nil &&
        createdAt == nil &&
        scheduledAt == nil
    }
    
    /// 清空筛选条件
    mutating func clear() {
        types = nil
        statuses = nil
        priorities = nil
        keyword = nil
        createdAt = nil
        scheduledAt = nil
        page = 1
    }
}

/// 排序字段
enum TaskSortBy: String, Codable, CaseIterable {
    /// 创建时间
    case createdAt = "created_at"
    
    /// 更新时间
    case updatedAt = "updated_at"
    
    /// 计划时间
    case scheduledEndAt = "scheduled_end_at"
    
    /// 优先级
    case priority = "priority"
    
    /// 进度
    case progress = "progress"
}

// MARK: - 预览

#if DEBUG
extension Task {
    static let preview = Task(
        id: "task_001",
        taskNo: "T202401001",
        name: "关键词截客任务",
        description: "在抖音平台搜索关键词'AI获客'，截取意向客户",
        type: .intercept,
        status: .running,
        priority: .high,
        leadId: "lead_001",
        leadName: "张三",
        platform: .douyin,
        progress: 65,
        totalSteps: 4,
        completedSteps: 2,
        currentStep: 3,
        scheduledStartAt: Date(),
        scheduledEndAt: Date().addingTimeInterval(3600),
        actualStartAt: Date(),
        creatorId: "user_001",
        creatorName: "李明",
        executorId: "user_001",
        executorName: "李明",
        tenantId: "tenant_001"
    )
    
    static let previewList: [Task] = [
        preview,
        Task(
            id: "task_002",
            taskNo: "T202401002",
            name: "内容发布任务",
            description: "在微信公众号发布AI产品介绍文章",
            type: .contentPublish,
            status: .pending,
            priority: .medium,
            platform: .wechatPublic,
            progress: 0,
            totalSteps: 3,
            creatorId: "user_001",
            executorId: "user_001",
            tenantId: "tenant_001"
        ),
        Task(
            id: "task_003",
            taskNo: "T202401003",
            name: "私信触达任务",
            description: "向高意向客户发送合作私信",
            type: .privateMessage,
            status: .completed,
            priority: .high,
            progress: 100,
            totalSteps: 5,
            completedSteps: 5,
            actualStartAt: Date().addingTimeInterval(-7200),
            actualEndAt: Date(),
            result: "成功触达10位客户，回复率60%",
            creatorId: "user_001",
            executorId: "user_001",
            tenantId: "tenant_001"
        )
    ]
}

extension TaskStep {
    static let previewList: [TaskStep] = [
        TaskStep(
            stepIndex: 1,
            name: "搜索关键词",
            description: "在平台搜索配置的关键字",
            status: .completed,
            startedAt: Date().addingTimeInterval(-1800),
            completedAt: Date().addingTimeInterval(-1500),
            result: "找到120条相关结果",
            logs: [
                TaskStepLog(stepId: "step_001", level: .info, message: "开始搜索关键词: AI获客"),
                TaskStepLog(stepId: "step_001", level: .info, message: "找到匹配内容120条"),
                TaskStepLog(stepId: "step_001", level: .info, message: "筛选高意向内容45条")
            ]
        ),
        TaskStep(
            stepIndex: 2,
            name: "内容分析",
            description: "AI分析内容意向度",
            status: .completed,
            startedAt: Date().addingTimeInterval(-1500),
            completedAt: Date().addingTimeInterval(-600),
            result: "识别高意向用户8人",
            logs: [
                TaskStepLog(stepId: "step_002", level: .info, message: "开始分析内容意向度"),
                TaskStepLog(stepId: "step_002", level: .info, message: "识别高意向用户8人")
            ]
        ),
        TaskStep(
            stepIndex: 3,
            name: "生成商机",
            description: "自动生成商机记录",
            status: .running,
            startedAt: Date().addingTimeInterval(-600),
            logs: [
                TaskStepLog(stepId: "step_003", level: .info, message: "开始生成商机记录"),
                TaskStepLog(stepId: "step_003", level: .debug, message: "处理用户 1/8")
            ]
        ),
        TaskStep(
            stepIndex: 4,
            name: "通知用户",
            description: "发送任务完成通知",
            status: .pending
        )
    ]
}
#endif
