//
//  Account.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  账号数据模型
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

// MARK: - 账号模型

/// 第三方平台账号模型
/// 
/// 存储用户绑定的第三方平台账号信息，如抖音、小红书等。
struct Account: Codable, Identifiable, Equatable {
    
    // MARK: - 基本信息
    
    /// 账号ID - 唯一标识
    var id: String
    
    /// 用户ID - 所属用户
    var userId: String
    
    /// 平台类型
    var platform: PlatformType
    
    /// 平台账号ID
    var platformUserId: String
    
    /// 平台昵称
    var platformNickname: String
    
    /// 平台头像URL
    var platformAvatar: String?
    
    /// 绑定手机号
    var bindPhone: String?
    
    // MARK: - 状态信息
    
    /// 账号状态
    var status: AccountStatus
    
    /// 健康度评分 (0-100)
    var healthScore: Int
    
    /// 授权状态
    var authStatus: AuthStatus
    
    /// 授权过期时间
    var authExpireAt: Date?
    
    // MARK: - 数据统计
    
    /// 粉丝数
    var followersCount: Int
    
    /// 作品数
    var worksCount: Int
    
    /// 获赞数
    var likesCount: Int
    
    /// 最后同步时间
    var lastSyncAt: Date?
    
    // MARK: - 时间戳
    
    /// 绑定时间
    var bindAt: Date
    
    /// 更新时间
    var updatedAt: Date
    
    // MARK: - 初始化
    
    init(
        id: String = UUID().uuidString,
        userId: String,
        platform: PlatformType,
        platformUserId: String,
        platformNickname: String,
        platformAvatar: String? = nil,
        bindPhone: String? = nil,
        status: AccountStatus = .active,
        healthScore: Int = 100,
        authStatus: AuthStatus = .authorized,
        authExpireAt: Date? = nil,
        followersCount: Int = 0,
        worksCount: Int = 0,
        likesCount: Int = 0,
        lastSyncAt: Date? = nil,
        bindAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.userId = userId
        self.platform = platform
        self.platformUserId = platformUserId
        self.platformNickname = platformNickname
        self.platformAvatar = platformAvatar
        self.bindPhone = bindPhone
        self.status = status
        self.healthScore = healthScore
        self.authStatus = authStatus
        self.authExpireAt = authExpireAt
        self.followersCount = followersCount
        self.worksCount = worksCount
        self.likesCount = likesCount
        self.lastSyncAt = lastSyncAt
        self.bindAt = bindAt
        self.updatedAt = updatedAt
    }
}

// MARK: - 平台类型

/// 平台类型枚举
enum PlatformType: String, Codable, CaseIterable {
    /// 抖音
    case douyin = "douyin"
    
    /// 小红书
    case xiaohongshu = "xiaohongshu"
    
    /// 快手
    case kuaishou = "kuaishou"
    
    /// B站
    case bilibili = "bilibili"
    
    /// 微博
    case weibo = "weibo"
    
    /// 视频号
    case channels = "channels"
    
    /// 平台名称
    var name: String {
        switch self {
        case .douyin: return "抖音"
        case .xiaohongshu: return "小红书"
        case .kuaishou: return "快手"
        case .bilibili: return "B站"
        case .weibo: return "微博"
        case .channels: return "视频号"
        }
    }
    
    /// 平台图标名称
    var iconName: String {
        switch self {
        case .douyin: return "music.note"
        case .xiaohongshu: return "book.fill"
        case .kuaishou: return "video.fill"
        case .bilibili: return "tv.fill"
        case .weibo: return "message.circle.fill"
        case .channels: return "play.rectangle.fill"
        }
    }
    
    /// 平台品牌色
    var brandColor: String {
        switch self {
        case .douyin: return "#000000"
        case .xiaohongshu: return "#FF2442"
        case .kuaishou: return "#FF6600"
        case .bilibili: return "#00A1D6"
        case .weibo: return "#E6162D"
        case .channels: return "#07C160"
        }
    }
}

// MARK: - 账号状态

/// 账号状态枚举
enum AccountStatus: String, Codable, CaseIterable {
    /// 正常
    case active = "active"
    
    /// 禁用
    case disabled = "disabled"
    
    /// 异常
    case abnormal = "abnormal"
    
    /// 已解绑
    case unbound = "unbound"
    
    /// 状态描述
    var description: String {
        switch self {
        case .active: return "正常"
        case .disabled: return "已禁用"
        case .abnormal: return "异常"
        case .unbound: return "已解绑"
        }
    }
    
    /// 状态颜色
    var color: String {
        switch self {
        case .active: return "#34C759"
        case .disabled: return "#8E8E93"
        case .abnormal: return "#FF3B30"
        case .unbound: return "#FF9500"
        }
    }
}

// MARK: - 授权状态

/// 授权状态枚举
enum AuthStatus: String, Codable, CaseIterable {
    /// 已授权
    case authorized = "authorized"
    
    /// 授权过期
    case expired = "expired"
    
    /// 授权失效
    case invalid = "invalid"
    
    /// 待授权
    case pending = "pending"
    
    /// 状态描述
    var description: String {
        switch self {
        case .authorized: return "已授权"
        case .expired: return "已过期"
        case .invalid: return "已失效"
        case .pending: return "待授权"
        }
    }
    
    /// 是否需要重新授权
    var needsReauth: Bool {
        self == .expired || self == .invalid
    }
}

// MARK: - 健康度等级

/// 账号健康度等级
enum HealthLevel: String, CaseIterable {
    /// 优秀
    case excellent = "excellent"
    
    /// 良好
    case good = "good"
    
    /// 一般
    case normal = "normal"
    
    /// 较差
    case poor = "poor"
    
    /// 危险
    case danger = "danger"
    
    /// 根据分数获取等级
    static func from(score: Int) -> HealthLevel {
        switch score {
        case 90...100: return .excellent
        case 75..<90: return .good
        case 60..<75: return .normal
        case 40..<60: return .poor
        default: return .danger
        }
    }
    
    /// 等级描述
    var description: String {
        switch self {
        case .excellent: return "优秀"
        case .good: return "良好"
        case .normal: return "一般"
        case .poor: return "较差"
        case .danger: return "危险"
        }
    }
    
    /// 等级颜色
    var color: String {
        switch self {
        case .excellent: return "#34C759"
        case .good: return "#30D158"
        case .normal: return "#FFCC00"
        case .poor: return "#FF9500"
        case .danger: return "#FF3B30"
        }
    }
}

// MARK: - 账号绑定请求

/// 账号绑定请求
struct BindAccountRequest: Codable {
    /// 平台类型
    var platform: PlatformType
    
    /// 授权码
    var authCode: String
    
    /// 状态码（防CSRF）
    var state: String
    
    /// 重定向URI
    var redirectUri: String
}

// MARK: - 账号同步响应

/// 账号同步响应
struct AccountSyncResponse: Codable {
    /// 账号ID
    var accountId: String
    
    /// 是否成功
    var success: Bool
    
    /// 同步时间
    var syncAt: Date
    
    /// 同步数据
    var data: AccountSyncData?
}

/// 账号同步数据
struct AccountSyncData: Codable {
    /// 粉丝数
    var followersCount: Int
    
    /// 作品数
    var worksCount: Int
    
    /// 获赞数
    var likesCount: Int
}

// MARK: - 预览

#if DEBUG
extension Account {
    static let preview = Account(
        id: "acc_001",
        userId: "user_001",
        platform: .douyin,
        platformUserId: "douyin_123456",
        platformNickname: "北极星AI官方",
        platformAvatar: "https://example.com/avatar.jpg",
        bindPhone: "13800138000",
        status: .active,
        healthScore: 95,
        authStatus: .authorized,
        authExpireAt: Date().addingTimeInterval(7 * 24 * 3600),
        followersCount: 125000,
        worksCount: 89,
        likesCount: 560000,
        lastSyncAt: Date(),
        bindAt: Date().addingTimeInterval(-30 * 24 * 3600),
        updatedAt: Date()
    )
    
    static let previewList = [
        Account.preview,
        Account(
            id: "acc_002",
            userId: "user_001",
            platform: .xiaohongshu,
            platformUserId: "xhs_789012",
            platformNickname: "北极星AI",
            platformAvatar: nil,
            status: .active,
            healthScore: 82,
            authStatus: .authorized,
            followersCount: 45000,
            worksCount: 156,
            likesCount: 230000,
            lastSyncAt: Date(),
            bindAt: Date().addingTimeInterval(-60 * 24 * 3600),
            updatedAt: Date()
        ),
        Account(
            id: "acc_003",
            userId: "user_001",
            platform: .kuaishou,
            platformUserId: "ks_345678",
            platformNickname: "北极星AI快手",
            status: .abnormal,
            healthScore: 45,
            authStatus: .expired,
            followersCount: 8000,
            worksCount: 23,
            likesCount: 12000,
            bindAt: Date().addingTimeInterval(-90 * 24 * 3600),
            updatedAt: Date()
        )
    ]
}
#endif
