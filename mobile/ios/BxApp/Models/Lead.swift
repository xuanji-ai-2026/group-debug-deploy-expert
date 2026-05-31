//
//  Lead.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  商机数据模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

// MARK: - 商机模型

/// 商机信息模型
/// 
/// 存储商机的基本信息，包括客户资料、意向等级、跟进状态等。
/// 实现了Codable协议，支持JSON序列化和本地存储。
struct Lead: Codable, Identifiable, Equatable {
    
    // MARK: - 基本信息
    
    /// 商机ID - 唯一标识
    var id: String
    
    /// 商机编号 - 业务编号
    var leadNo: String
    
    /// 客户名称
    var customerName: String
    
    /// 客户手机号
    var customerPhone: String
    
    /// 客户微信
    var customerWechat: String?
    
    /// 客户公司
    var customerCompany: String?
    
    /// 客户职位
    var customerPosition: String?
    
    /// 客户地址
    var customerAddress: String?
    
    // MARK: - 来源信息
    
    /// 来源平台
    var sourcePlatform: SourcePlatform
    
    /// 来源ID
    var sourceId: String?
    
    /// 来源URL
    var sourceUrl: String?
    
    /// 来源关键词
    var sourceKeyword: String?
    
    /// 截客内容
    var interceptedContent: String?
    
    // MARK: - 意向信息
    
    /// 意向等级
    var intentionLevel: IntentionLevel
    
    /// 意向标签
    var intentionTags: [String]
    
    /// 意向描述
    var intentionDescription: String?
    
    // MARK: - 跟进信息
    
    /// 跟进状态
    var followUpStatus: FollowUpStatus
    
    /// 跟进记录数
    var followUpCount: Int
    
    /// 最后跟进时间
    var lastFollowUpAt: Date?
    
    /// 下次跟进时间
    var nextFollowUpAt: Date?
    
    // MARK: - 归属信息
    
    /// 负责人ID
    var ownerId: String
    
    /// 负责人名称
    var ownerName: String
    
    /// 租户ID
    var tenantId: String
    
    // MARK: - 标签信息
    
    /// 标签列表
    var tags: [String]
    
    /// 备注
    var remark: String?
    
    // MARK: - 时间戳
    
    /// 创建时间
    var createdAt: Date
    
    /// 更新时间
    var updatedAt: Date
    
    // MARK: - 初始化
    
    init(
        id: String = UUID().uuidString,
        leadNo: String = "",
        customerName: String,
        customerPhone: String,
        customerWechat: String? = nil,
        customerCompany: String? = nil,
        customerPosition: String? = nil,
        customerAddress: String? = nil,
        sourcePlatform: SourcePlatform = .unknown,
        sourceId: String? = nil,
        sourceUrl: String? = nil,
        sourceKeyword: String? = nil,
        interceptedContent: String? = nil,
        intentionLevel: IntentionLevel = .medium,
        intentionTags: [String] = [],
        intentionDescription: String? = nil,
        followUpStatus: FollowUpStatus = .notStarted,
        followUpCount: Int = 0,
        lastFollowUpAt: Date? = nil,
        nextFollowUpAt: Date? = nil,
        ownerId: String,
        ownerName: String = "",
        tenantId: String,
        tags: [String] = [],
        remark: String? = nil,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.leadNo = leadNo
        self.customerName = customerName
        self.customerPhone = customerPhone
        self.customerWechat = customerWechat
        self.customerCompany = customerCompany
        self.customerPosition = customerPosition
        self.customerAddress = customerAddress
        self.sourcePlatform = sourcePlatform
        self.sourceId = sourceId
        self.sourceUrl = sourceUrl
        self.sourceKeyword = sourceKeyword
        self.interceptedContent = interceptedContent
        self.intentionLevel = intentionLevel
        self.intentionTags = intentionTags
        self.intentionDescription = intentionDescription
        self.followUpStatus = followUpStatus
        self.followUpCount = followUpCount
        self.lastFollowUpAt = lastFollowUpAt
        self.nextFollowUpAt = nextFollowUpAt
        self.ownerId = ownerId
        self.ownerName = ownerName
        self.tenantId = tenantId
        self.tags = tags
        self.remark = remark
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

// MARK: - 来源平台

/// 来源平台枚举
enum SourcePlatform: String, Codable, CaseIterable {
    /// 微信公众号
    case wechatPublic = "wechat_public"
    
    /// 微信视频号
    case wechatChannels = "wechat_channels"
    
    /// 微博
    case weibo = "weibo"
    
    /// 抖音
    case douyin = "douyin"
    
    /// 快手
    case kuaishou = "kuaishou"
    
    /// 小红书
    case xiaohongshu = "xiaohongshu"
    
    /// 知乎
    case zhihu = "zhihu"
    
    /// 百度
    case baidu = "baidu"
    
    /// 阿里巴巴
    case alibaba = "alibaba"
    
    /// 1688
    case alibaba1688 = "alibaba_1688"
    
    /// BOSS直聘
    case bossZhipin = "boss_zhipin"
    
    /// 未知
    case unknown = "unknown"
    
    /// 平台名称
    var name: String {
        switch self {
        case .wechatPublic: return "微信公众号"
        case .wechatChannels: return "微信视频号"
        case .weibo: return "微博"
        case .douyin: return "抖音"
        case .kuaishou: return "快手"
        case .xiaohongshu: return "小红书"
        case .zhihu: return "知乎"
        case .baidu: return "百度"
        case .alibaba: return "阿里巴巴"
        case .alibaba1688: return "1688"
        case .bossZhipin: return "BOSS直聘"
        case .unknown: return "未知来源"
        }
    }
    
    /// 平台图标名称
    var iconName: String {
        switch self {
        case .wechatPublic, .wechatChannels: return "wechat"
        case .weibo: return "weibo"
        case .douyin: return "douyin"
        case .kuaishou: return "kuaishou"
        case .xiaohongshu: return "xiaohongshu"
        case .zhihu: return "zhihu"
        case .baidu: return "baidu"
        case .alibaba, .alibaba1688: return "alibaba"
        case .bossZhipin: return "boss"
        case .unknown: return "questionmark.circle"
        }
    }
}

// MARK: - 意向等级

/// 意向等级枚举
enum IntentionLevel: String, Codable, CaseIterable {
    /// 高意向
    case high = "high"
    
    /// 中意向
    case medium = "medium"
    
    /// 低意向
    case low = "low"
    
    /// 无意向
    case none = "none"
    
    /// 等级数值（用于排序）
    var level: Int {
        switch self {
        case .high: return 3
        case .medium: return 2
        case .low: return 1
        case .none: return 0
        }
    }
    
    /// 等级名称
    var name: String {
        switch self {
        case .high: return "高意向"
        case .medium: return "中意向"
        case .low: return "低意向"
        case .none: return "无意向"
        }
    }
    
    /// 等级颜色
    var colorName: String {
        switch self {
        case .high: return "red"
        case .medium: return "orange"
        case .low: return "blue"
        case .none: return "gray"
        }
    }
}

// MARK: - 跟进状态

/// 跟进状态枚举
enum FollowUpStatus: String, Codable, CaseIterable {
    /// 未开始
    case notStarted = "not_started"
    
    /// 跟进中
    case inProgress = "in_progress"
    
    /// 已成交
    case closed = "closed"
    
    /// 已流失
    case lost = "lost"
    
    /// 暂不跟进
    case paused = "paused"
    
    /// 状态名称
    var name: String {
        switch self {
        case .notStarted: return "未开始"
        case .inProgress: return "跟进中"
        case .closed: return "已成交"
        case .lost: return "已流失"
        case .paused: return "暂不跟进"
        }
    }
    
    /// 状态颜色
    var colorName: String {
        switch self {
        case .notStarted: return "gray"
        case .inProgress: return "blue"
        case .closed: return "green"
        case .lost: return "red"
        case .paused: return "orange"
        }
    }
    
    /// 是否可编辑
    var isEditable: Bool {
        switch self {
        case .closed, .lost: return false
        default: return true
        }
    }
}

// MARK: - 商机跟进记录

/// 商机跟进记录模型
struct FollowUpRecord: Codable, Identifiable {
    
    /// 记录ID
    var id: String
    
    /// 商机ID
    var leadId: String
    
    /// 跟进类型
    var type: FollowUpType
    
    /// 跟进内容
    var content: String
    
    /// 跟进人ID
    var userId: String
    
    /// 跟进人名称
    var userName: String
    
    /// 创建时间
    var createdAt: Date
    
    /// 附件URL列表
    var attachments: [String]
    
    init(
        id: String = UUID().uuidString,
        leadId: String,
        type: FollowUpType,
        content: String,
        userId: String,
        userName: String,
        createdAt: Date = Date(),
        attachments: [String] = []
    ) {
        self.id = id
        self.leadId = leadId
        self.type = type
        self.content = content
        self.userId = userId
        self.userName = userName
        self.createdAt = createdAt
        self.attachments = attachments
    }
}

/// 跟进类型
enum FollowUpType: String, Codable, CaseIterable {
    /// 电话沟通
    case phone = "phone"
    
    /// 微信沟通
    case wechat = "wechat"
    
    /// 面谈
    case meeting = "meeting"
    
    /// 发送资料
    case sendMaterial = "send_material"
    
    /// 报价
    case quote = "quote"
    
    /// 其他
    case other = "other"
    
    /// 类型名称
    var name: String {
        switch self {
        case .phone: return "电话沟通"
        case .wechat: return "微信沟通"
        case .meeting: return "面谈"
        case .sendMaterial: return "发送资料"
        case .quote: return "报价"
        case .other: return "其他"
        }
    }
    
    /// 类型图标
    var iconName: String {
        switch self {
        case .phone: return "phone.fill"
        case .wechat: return "message.fill"
        case .meeting: return "person.2.fill"
        case .sendMaterial: return "paperplane.fill"
        case .quote: return "dollarsign.circle.fill"
        case .other: return "ellipsis.circle.fill"
        }
    }
}

// MARK: - 商机筛选条件

/// 商机筛选条件
struct LeadFilter: Codable, Equatable {
    
    /// 来源平台
    var sourcePlatforms: [SourcePlatform]?
    
    /// 意向等级
    var intentionLevels: [IntentionLevel]?
    
    /// 跟进状态
    var followUpStatuses: [FollowUpStatus]?
    
    /// 关键词搜索
    var keyword: String?
    
    /// 开始日期
    var startDate: Date?
    
    /// 结束日期
    var endDate: Date?
    
    /// 按创建时间筛选
    var createdAt: DateRange?
    
    /// 按更新时间筛选
    var updatedAt: DateRange?
    
    /// 排序字段
    var sortBy: LeadSortBy = .createdAt
    
    /// 排序方向
    var sortOrder: SortOrder = .descending
    
    /// 页码
    var page: Int = 1
    
    /// 每页数量
    var pageSize: Int = 20
    
    /// 是否为空筛选
    var isEmpty: Bool {
        sourcePlatforms == nil &&
        intentionLevels == nil &&
        followUpStatuses == nil &&
        keyword == nil &&
        startDate == nil &&
        endDate == nil
    }
    
    /// 清空筛选条件
    mutating func clear() {
        sourcePlatforms = nil
        intentionLevels = nil
        followUpStatuses = nil
        keyword = nil
        startDate = nil
        endDate = nil
        page = 1
    }
}

/// 排序字段
enum LeadSortBy: String, Codable, CaseIterable {
    /// 创建时间
    case createdAt = "created_at"
    
    /// 更新时间
    case updatedAt = "updated_at"
    
    /// 意向等级
    case intentionLevel = "intention_level"
    
    /// 客户名称
    case customerName = "customer_name"
}

/// 排序方向
enum SortOrder: String, Codable {
    /// 升序
    case ascending = "asc"
    
    /// 降序
    case descending = "desc"
}

/// 日期范围
struct DateRange: Codable {
    var start: Date
    var end: Date
}

// MARK: - 预览

#if DEBUG
extension Lead {
    static let preview = Lead(
        id: "lead_001",
        leadNo: "L202401001",
        customerName: "张三",
        customerPhone: "13800138000",
        customerWechat: "zhangsan888",
        customerCompany: "北极星科技有限公司",
        customerPosition: "总经理",
        customerAddress: "北京市朝阳区",
        sourcePlatform: .wechatPublic,
        sourceKeyword: "AI商机系统",
        interceptedContent: "这个AI商机获客系统看起来不错，怎么合作？",
        intentionLevel: .high,
        intentionTags: ["AI获客", "企业服务", "有预算"],
        followUpStatus: .inProgress,
        followUpCount: 3,
        ownerId: "user_001",
        ownerName: "李明",
        tenantId: "tenant_001",
        tags: ["重点客户", "年前成交"],
        remark: "客户对AI功能很感兴趣，重点跟进"
    )
    
    static let previewList: [Lead] = [
        preview,
        Lead(
            id: "lead_002",
            leadNo: "L202401002",
            customerName: "李四",
            customerPhone: "13900139000",
            sourcePlatform: .xiaohongshu,
            intentionLevel: .medium,
            followUpStatus: .notStarted,
            ownerId: "user_001",
            tenantId: "tenant_001"
        ),
        Lead(
            id: "lead_003",
            leadNo: "L202401003",
            customerName: "王五",
            customerPhone: "13700137000",
            sourcePlatform: .douyin,
            intentionLevel: .low,
            followUpStatus: .closed,
            ownerId: "user_001",
            tenantId: "tenant_001"
        )
    ]
}

extension FollowUpRecord {
    static let preview = FollowUpRecord(
        id: "record_001",
        leadId: "lead_001",
        type: .phone,
        content: "客户对产品很感兴趣，询问了价格和合作方式，已发送产品资料",
        userId: "user_001",
        userName: "李明"
    )
}
#endif
