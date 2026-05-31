//
//  User.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  用户数据模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

// MARK: - 用户模型

/// 用户信息模型
/// 
/// 存储用户的基本信息，包括用户ID、姓名、联系方式等。
/// 实现了Codable协议，支持JSON序列化和本地存储。
struct User: Codable, Identifiable, Equatable {
    
    // MARK: - 基本信息
    
    /// 用户ID - 唯一标识
    var id: String
    
    /// 用户名
    var username: String
    
    /// 昵称
    var nickname: String
    
    /// 手机号
    var phone: String
    
    /// 邮箱
    var email: String?
    
    /// 头像URL
    var avatarUrl: String?
    
    /// 用户类型
    var userType: UserType
    
    /// 用户状态
    var status: UserStatus
    
    // MARK: - 租户信息
    
    /// 租户ID
    var tenantId: String?
    
    /// 租户名称
    var tenantName: String?
    
    // MARK: - 账户信息
    
    /// 账户余额（积分）
    var balance: Int
    
    /// 账户等级
    var level: Int
    
    // MARK: - 时间戳
    
    /// 创建时间
    var createdAt: Date
    
    /// 更新时间
    var updatedAt: Date
    
    /// 最后登录时间
    var lastLoginAt: Date?
    
    // MARK: - 初始化
    
    init(
        id: String = UUID().uuidString,
        username: String,
        nickname: String = "",
        phone: String,
        email: String? = nil,
        avatarUrl: String? = nil,
        userType: UserType = .normal,
        status: UserStatus = .active,
        tenantId: String? = nil,
        tenantName: String? = nil,
        balance: Int = 0,
        level: Int = 1,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        lastLoginAt: Date? = nil
    ) {
        self.id = id
        self.username = username
        self.nickname = nickname
        self.phone = phone
        self.email = email
        self.avatarUrl = avatarUrl
        self.userType = userType
        self.status = status
        self.tenantId = tenantId
        self.tenantName = tenantName
        self.balance = balance
        self.level = level
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.lastLoginAt = lastLoginAt
    }
}

// MARK: - 用户类型

/// 用户类型枚举
enum UserType: String, Codable, CaseIterable {
    /// 普通用户
    case normal = "normal"
    
    /// VIP用户
    case vip = "vip"
    
    /// 企业用户
    case enterprise = "enterprise"
    
    /// 管理员
    case admin = "admin"
    
    /// 超级管理员
    case superAdmin = "super_admin"
    
    /// 用户类型描述
    var description: String {
        switch self {
        case .normal: return "普通用户"
        case .vip: return "VIP用户"
        case .enterprise: return "企业用户"
        case .admin: return "管理员"
        case .superAdmin: return "超级管理员"
        }
    }
}

// MARK: - 用户状态

/// 用户状态枚举
enum UserStatus: String, Codable, CaseIterable {
    /// 活跃
    case active = "active"
    
    /// 禁用
    case disabled = "disabled"
    
    /// 待验证
    case pending = "pending"
    
    /// 已注销
    case deleted = "deleted"
    
    /// 状态描述
    var description: String {
        switch self {
        case .active: return "正常"
        case .disabled: return "已禁用"
        case .pending: return "待验证"
        case .deleted: return "已注销"
        }
    }
    
    /// 是否可以登录
    var canLogin: Bool {
        self == .active
    }
}

// MARK: - 用户认证信息

/// 用户认证信息
/// 
/// 用于存储登录后的认证凭证。
struct AuthInfo: Codable {
    
    /// 访问Token
    var accessToken: String
    
    /// 刷新Token
    var refreshToken: String
    
    /// Token类型
    var tokenType: String
    
    /// Token过期时间
    var expiresAt: Date
    
    /// 用户ID
    var userId: String
    
    /// 创建时间
    var createdAt: Date
    
    init(
        accessToken: String,
        refreshToken: String,
        tokenType: String = "Bearer",
        expiresIn: Int = 7200,
        userId: String
    ) {
        self.accessToken = accessToken
        self.refreshToken = refreshToken
        self.tokenType = tokenType
        self.expiresAt = Date().addingTimeInterval(TimeInterval(expiresIn))
        self.userId = userId
        self.createdAt = Date()
    }
    
    /// Token是否过期
    var isExpired: Bool {
        Date() >= expiresAt
    }
    
    /// 是否即将过期（剩余时间少于5分钟）
    var isExpiringSoon: Bool {
        Date().addingTimeInterval(300) >= expiresAt
    }
}

// MARK: - 登录请求

/// 登录请求参数
struct LoginRequest: Codable {
    
    /// 登录方式
    var loginType: LoginType
    
    /// 手机号（手机登录时）
    var phone: String?
    
    /// 验证码（手机登录时）
    var verificationCode: String?
    
    /// 密码（密码登录时）
    var password: String?
    
    /// 设备ID
    var deviceId: String
    
    /// 设备类型
    var deviceType: String = "ios"
    
    /// 设备名称
    var deviceName: String
    
    /// App版本
    var appVersion: String
    
    init(
        loginType: LoginType,
        phone: String? = nil,
        verificationCode: String? = nil,
        password: String? = nil
    ) {
        self.loginType = loginType
        self.phone = phone
        self.verificationCode = verificationCode
        self.password = password
        self.deviceId = DeviceManager.shared.getDeviceIdentifier()
        self.deviceName = UIDevice.current.name
        self.appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }
}

// MARK: - 登录类型

/// 登录类型枚举
enum LoginType: String, Codable, CaseIterable {
    /// 手机验证码登录
    case phoneCode = "phone_code"
    
    /// 密码登录
    case password = "password"
    
    /// 微信授权登录
    case wechat = "wechat"
    
    /// Apple登录
    case apple = "apple"
    
    /// 登录类型描述
    var description: String {
        switch self {
        case .phoneCode: return "验证码登录"
        case .password: return "密码登录"
        case .wechat: return "微信登录"
        case .apple: return "Apple登录"
        }
    }
}

// MARK: - 验证码请求

/// 验证码请求参数
struct VerificationCodeRequest: Codable {
    
    /// 手机号
    var phone: String
    
    /// 验证码类型
    var type: VerificationCodeType
    
    /// 设备ID
    var deviceId: String
}

/// 验证码类型
enum VerificationCodeType: String, Codable, CaseIterable {
    /// 登录
    case login = "login"
    
    /// 注册
    case register = "register"
    
    /// 修改密码
    case changePassword = "change_password"
    
    /// 绑定手机
    case bindPhone = "bind_phone"
    
    /// 描述
    var description: String {
        switch self {
        case .login: return "登录"
        case .register: return "注册"
        case .changePassword: return "修改密码"
        case .bindPhone: return "绑定手机"
        }
    }
}

// MARK: - 设备信息

/// 设备信息模型
struct DeviceInfo: Codable {
    
    /// 设备ID
    var deviceId: String
    
    /// 设备类型
    var deviceType: String
    
    /// 设备名称
    var deviceName: String
    
    /// 操作系统版本
    var osVersion: String
    
    /// App版本
    var appVersion: String
    
    /// 构建号
    var buildNumber: String
    
    /// 屏幕尺寸
    var screenSize: String
    
    /// 网络类型
    var networkType: String
    
    init() {
        let device = UIDevice.current
        
        self.deviceId = DeviceManager.shared.getDeviceIdentifier()
        self.deviceType = "ios"
        self.deviceName = device.name
        self.osVersion = device.systemVersion
        self.appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        self.buildNumber = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        self.screenSize = "\(UIScreen.main.bounds.width)x\(UIScreen.main.bounds.height)"
        self.networkType = NetworkManager.shared.currentNetworkType
    }
}

// MARK: - 预览

#if DEBUG
extension User {
    static let preview = User(
        id: "user_001",
        username: "zhangsan",
        nickname: "张三",
        phone: "13800138000",
        email: "zhangsan@example.com",
        avatarUrl: "https://example.com/avatar.jpg",
        userType: .vip,
        status: .active,
        tenantId: "tenant_001",
        tenantName: "北极星科技有限公司",
        balance: 5000,
        level: 3,
        createdAt: Date(),
        updatedAt: Date(),
        lastLoginAt: Date()
    )
    
    static let previewList = [
        User.preview,
        User(
            id: "user_002",
            username: "lisi",
            nickname: "李四",
            phone: "13800138001",
            userType: .normal,
            status: .active,
            balance: 1000
        )
    ]
}
#endif
