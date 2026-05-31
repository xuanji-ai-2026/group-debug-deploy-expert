//
//  Response.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  API响应模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

// MARK: - API响应基类

/// API统一响应模型
/// 
/// 所有API接口的统一响应格式，包含状态码、消息和数据。
/// 使用泛型T表示具体的数据类型。
struct APIResponse<T: Codable>: Codable {
    
    /// 状态码 - 0表示成功，其他表示错误
    var code: Int
    
    /// 消息
    var message: String
    
    /// 数据
    var data: T?
    
    /// 请求ID
    var requestId: String?
    
    /// 时间戳
    var timestamp: Date
    
    // MARK: - 辅助属性
    
    /// 是否成功
    var isSuccess: Bool {
        code == 0 || code == 200
    }
    
    /// 错误信息
    var errorMessage: String? {
        isSuccess ? nil : message
    }
    
    // MARK: - 初始化
    
    init(
        code: Int = 0,
        message: String = "",
        data: T? = nil,
        requestId: String? = nil,
        timestamp: Date = Date()
    ) {
        self.code = code
        self.message = message
        self.data = data
        self.requestId = requestId
        self.timestamp = timestamp
    }
    
    /// 创建成功响应
    static func success(_ data: T?, message: String = "操作成功") -> APIResponse<T> {
        APIResponse(code: 0, message: message, data: data)
    }
    
    /// 创建错误响应
    static func failure(_ code: Int, message: String) -> APIResponse<T> {
        APIResponse(code: code, message: message)
    }
}

// MARK: - 分页响应

/// 分页响应模型
/// 
/// 用于列表类接口的分页响应。
struct PageResponse<T: Codable>: Codable {
    
    /// 数据列表
    var list: [T]
    
    /// 总记录数
    var total: Int
    
    /// 当前页码
    var page: Int
    
    /// 每页记录数
    var pageSize: Int
    
    /// 总页数
    var totalPages: Int
    
    /// 是否有下一页
    var hasMore: Bool
    
    /// 排序信息
    var sortInfo: String?
    
    // MARK: - 初始化
    
    init(
        list: [T] = [],
        total: Int = 0,
        page: Int = 1,
        pageSize: Int = 20
    ) {
        self.list = list
        self.total = total
        self.page = page
        self.pageSize = pageSize
        self.totalPages = total > 0 ? (total + pageSize - 1) / pageSize : 0
        self.hasMore = page < self.totalPages
    }
    
    /// 从API响应创建
    static func from<T: Codable>(
        response: APIResponse<T>?,
        page: Int = 1,
        pageSize: Int = 20
    ) -> PageResponse<T>? where T: Collection {
        guard let data = response?.data else { return nil }
        let list = Array(data)
        return PageResponse(
            list: list,
            total: (response?.data as? [T])?.count ?? 0,
            page: page,
            pageSize: pageSize
        )
    }
}

// MARK: - 错误码定义

/// API错误码枚举
enum APIErrorCode: Int, Codable, CaseIterable {
    // 通用错误 (1000-1999)
    /// 成功
    case success = 0
    
    /// 未知错误
    case unknown = 1000
    
    /// 参数错误
    case invalidParams = 1001
    
    /// 缺少参数
    case missingParams = 1002
    
    /// 参数格式错误
    case invalidFormat = 1003
    
    /// 数据不存在
    case notFound = 1004
    
    /// 数据已存在
    case alreadyExists = 1005
    
    /// 操作不支持
    case notSupported = 1006
    
    /// 请求过于频繁
    case tooFrequent = 1007
    
    // 认证错误 (2000-2999)
    /// 未登录
    case notLoggedIn = 2000
    
    /// Token过期
    case tokenExpired = 2001
    
    /// Token无效
    case tokenInvalid = 2002
    
    /// 权限不足
    case unauthorized = 2003
    
    /// 账户被禁用
    case accountDisabled = 2004
    
    /// 登录失败
    case loginFailed = 2005
    
    /// 注册失败
    case registerFailed = 2006
    
    /// 验证码错误
    case verifyCodeError = 2007
    
    /// 验证码过期
    case verifyCodeExpired = 2008
    
    // 业务错误 (3000-3999)
    /// 余额不足
    case insufficientBalance = 3000
    
    /// 账户被封禁
    case accountBanned = 3001
    
    /// 超出限制
    case exceedLimit = 3002
    
    /// 资源不足
    case resourceInsufficient = 3003
    
    /// 业务处理失败
    case businessFailed = 3004
    
    // 服务错误 (5000-5999)
    /// 服务器错误
    case serverError = 5000
    
    /// 服务不可用
    case serviceUnavailable = 5001
    
    /// 数据库错误
    case databaseError = 5002
    
    /// 缓存服务错误
    case cacheError = 5003
    
    /// 第三方服务错误
    case thirdPartyError = 5004
    
    /// 网络错误
    case networkError = 5005
    
    /// 超时错误
    case timeoutError = 5006
    
    // MARK: - 错误信息
    
    /// 错误描述
    var message: String {
        switch self {
        case .success: return "操作成功"
        case .unknown: return "未知错误"
        case .invalidParams: return "参数错误"
        case .missingParams: return "缺少必要参数"
        case .invalidFormat: return "参数格式错误"
        case .notFound: return "数据不存在"
        case .alreadyExists: return "数据已存在"
        case .notSupported: return "操作不支持"
        case .tooFrequent: return "请求过于频繁，请稍后再试"
        case .notLoggedIn: return "请先登录"
        case .tokenExpired: return "登录已过期，请重新登录"
        case .tokenInvalid: return "登录信息无效，请重新登录"
        case .unauthorized: return "您没有权限执行此操作"
        case .accountDisabled: return "账户已被禁用"
        case .loginFailed: return "登录失败，请检查账号密码"
        case .registerFailed: return "注册失败，请稍后重试"
        case .verifyCodeError: return "验证码错误"
        case .verifyCodeExpired: return "验证码已过期"
        case .insufficientBalance: return "账户余额不足"
        case .accountBanned: return "账户已被封禁"
        case .exceedLimit: return "超出使用限制"
        case .resourceInsufficient: return "资源不足"
        case .businessFailed: return "操作失败，请稍后重试"
        case .serverError: return "服务器错误，请稍后重试"
        case .serviceUnavailable: return "服务暂不可用"
        case .databaseError: return "数据库错误"
        case .cacheError: return "缓存服务错误"
        case .thirdPartyError: return "第三方服务错误"
        case .networkError: return "网络连接失败"
        case .timeoutError: return "请求超时"
        }
    }
    
    /// 是否需要重新登录
    var needRelogin: Bool {
        switch self {
        case .notLoggedIn, .tokenExpired, .tokenInvalid, .accountDisabled:
            return true
        default:
            return false
        }
    }
}

// MARK: - 网络错误

/// 网络错误类型
enum NetworkError: Error, LocalizedError {
    /// 无网络连接
    case noConnection
    
    /// 请求超时
    case timeout
    
    /// 服务器错误
    case serverError(Int)
    
    /// 网络不可达
    case unreachable
    
    /// SSL证书错误
    case sslError
    
    /// 解析错误
    case parseError
    
    /// 未知错误
    case unknown(Error)
    
    /// 错误描述
    var errorDescription: String? {
        switch self {
        case .noConnection:
            return "网络连接失败，请检查网络设置"
        case .timeout:
            return "请求超时，请稍后重试"
        case .serverError(let code):
            return "服务器错误(\(code))，请稍后重试"
        case .unreachable:
            return "网络不可达"
        case .sslError:
            return "安全连接失败"
        case .parseError:
            return "数据解析失败"
        case .unknown(let error):
            return error.localizedDescription
        }
    }
    
    /// 是否可以重试
    var canRetry: Bool {
        switch self {
        case .timeout, .serverError, .unreachable, .unknown:
            return true
        case .noConnection, .sslError, .parseError:
            return false
        }
    }
}

// MARK: - API异常

/// API异常
struct APIException: Error, LocalizedError {
    
    /// 错误码
    var code: Int
    
    /// 错误消息
    var message: String
    
    /// 请求ID
    var requestId: String?
    
    /// 原始数据
    var rawData: Data?
    
    /// 错误描述
    var errorDescription: String? {
        message
    }
    
    /// 错误码枚举
    var errorCode: APIErrorCode {
        APIErrorCode(rawValue: code) ?? .unknown
    }
    
    init(
        code: Int,
        message: String,
        requestId: String? = nil,
        rawData: Data? = nil
    ) {
        self.code = code
        self.message = message
        self.requestId = requestId
        self.rawData = rawData
    }
    
    /// 从APIResponse创建
    static func from<T: Codable>(response: APIResponse<T>) -> APIException? {
        guard !response.isSuccess else { return nil }
        return APIException(
            code: response.code,
            message: response.message,
            requestId: response.requestId
        )
    }
}

// MARK: - 请求配置

/// 请求配置
struct RequestConfig {
    
    /// 请求超时时间（秒）
    var timeout: TimeInterval = 30
    
    /// 重试次数
    var retryCount: Int = 3
    
    /// 重试间隔（秒）
    var retryInterval: TimeInterval = 1
    
    /// 是否显示loading
    var showLoading: Bool = true
    
    /// 是否显示错误提示
    var showError: Bool = true
    
    /// 是否缓存响应
    var cacheResponse: Bool = false
    
    /// 缓存有效期（秒）
    var cacheExpiration: TimeInterval = 300
    
    /// 请求头
    var headers: [String: String]?
    
    /// 默认请求配置
    static let `default` = RequestConfig()
    
    /// 上传请求配置
    static let upload = RequestConfig(
        timeout: 120,
        showLoading: true
    )
    
    /// 下载请求配置
    static let download = RequestConfig(
        timeout: 300,
        showLoading: true
    )
}

// MARK: - 预览

#if DEBUG
extension APIResponse {
    static let preview = APIResponse<String>(
        code: 0,
        message: "操作成功",
        data: "test data",
        requestId: "req_001"
    )
    
    static let errorPreview = APIResponse<String>(
        code: 1001,
        message: "参数错误"
    )
}

extension PageResponse {
    static let preview = PageResponse<String>(
        list: ["item1", "item2", "item3"],
        total: 100,
        page: 1,
        pageSize: 20
    )
}
#endif
