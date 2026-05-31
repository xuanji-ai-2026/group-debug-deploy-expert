//
//  AuthService.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  认证服务 - 用户登录、登出、Token管理
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

/// 认证服务
/// 
/// 负责用户认证相关的业务逻辑，包括登录、登出、Token管理等。
class AuthService {
    
    // MARK: - 单例
    
    static let shared = AuthService()
    
    // MARK: - 属性
    
    /// 当前认证信息
    private(set) var currentAuthInfo: AuthInfo?
    
    /// 当前用户
    private(set) var currentUser: User?
    
    // MARK: - 初始化
    
    private init() {
        loadStoredAuth()
    }
    
    // MARK: - 登录方法
    
    /// 手机验证码登录
    /// - Parameters:
    ///   - phone: 手机号
    ///   - code: 验证码
    /// - Returns: 登录响应（用户信息和认证信息）
    func loginWithCode(phone: String, code: String) async throws -> LoginResponse {
        let params: [String: Any] = [
            "phone": phone,
            "code": code,
            "deviceId": DeviceManager.shared.getDeviceIdentifier(),
            "deviceType": "ios",
            "deviceName": UIDevice.current.name
        ]
        
        let response: APIResponse<LoginResponseData> = try await APIService.shared.post(
            "/auth/login",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        // 保存认证信息
        saveAuth(data.authInfo)
        
        return LoginResponse(user: data.user, authInfo: data.authInfo)
    }
    
    /// 密码登录
    /// - Parameters:
    ///   - phone: 手机号
    ///   - password: 密码（明文，后端使用BCrypt加密）
    /// - Returns: 登录响应
    func loginWithPassword(phone: String, password: String) async throws -> LoginResponse {
        let params: [String: Any] = [
            "phone": phone,
            "password": password,
            "deviceId": DeviceManager.shared.getDeviceIdentifier(),
            "deviceType": "ios",
            "deviceName": UIDevice.current.name
        ]
        
        let response: APIResponse<LoginResponseData> = try await APIService.shared.post(
            "/auth/login",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        saveAuth(data.authInfo)
        
        return LoginResponse(user: data.user, authInfo: data.authInfo)
    }
    
    /// 微信登录
    /// - Parameter code: 微信授权码
    /// - Returns: 登录响应
    func loginWithWechat(code: String) async throws -> LoginResponse {
        let params: [String: Any] = [
            "code": code,
            "deviceId": DeviceManager.shared.getDeviceIdentifier(),
            "deviceType": "ios"
        ]
        
        let response: APIResponse<LoginResponseData> = try await APIService.shared.post(
            "/auth/wechat",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        saveAuth(data.authInfo)
        
        return LoginResponse(user: data.user, authInfo: data.authInfo)
    }
    
    /// Apple登录
    /// - Parameters:
    ///   - identityToken: Apple Identity Token
    ///   - authorizationCode: Apple Authorization Code
    /// - Returns: 登录响应
    func loginWithApple(identityToken: String, authorizationCode: String) async throws -> LoginResponse {
        let params: [String: Any] = [
            "identityToken": identityToken,
            "authorizationCode": authorizationCode,
            "deviceId": DeviceManager.shared.getDeviceIdentifier(),
            "deviceType": "ios"
        ]
        
        let response: APIResponse<LoginResponseData> = try await APIService.shared.post(
            "/auth/apple",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        saveAuth(data.authInfo)
        
        return LoginResponse(user: data.user, authInfo: data.authInfo)
    }
    
    // MARK: - 验证码方法
    
    /// 发送验证码
    /// - Parameters:
    ///   - phone: 手机号
    ///   - type: 验证码类型
    func sendVerificationCode(phone: String, type: VerificationCodeType) async throws {
        let params: [String: Any] = [
            "phone": phone,
            "type": type.rawValue,
            "deviceId": DeviceManager.shared.getDeviceIdentifier()
        ]
        
        let response: APIResponse<String?> = try await APIService.shared.post(
            "/auth/sms/send",
            parameters: params
        )
        
        if !response.isSuccess {
            throw APIException(code: response.code, message: response.message)
        }
    }
    
    // MARK: - Token方法
    
    /// 刷新Token
    func refreshToken() async throws {
        guard let refreshToken = KeychainManager.shared.getRefreshToken() else {
            throw APIException(code: APIErrorCode.tokenInvalid.rawValue, message: "请重新登录")
        }
        
        let params: [String: Any] = [
            "refreshToken": refreshToken
        ]
        
        let response: APIResponse<AuthInfo> = try await APIService.shared.post(
            "/auth/refresh",
            parameters: params
        )
        
        guard let authInfo = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        saveAuth(authInfo)
    }
    
    /// 验证Token
    /// - Parameter token: 要验证的Token
    /// - Returns: 是否有效
    func validateToken(_ token: String) -> Bool {
        // 验证本地Token是否过期
        if let expiresAt = KeychainManager.shared.getTokenExpiration() {
            return Date() < expiresAt
        }
        return false
    }
    
    /// 验证Token（异步）
    func validateToken(_ token: String) async -> Result<Bool, Error> {
        // 先检查本地过期时间
        if let expiresAt = KeychainManager.shared.getTokenExpiration() {
            // 如果即将过期，先尝试刷新
            if Date().addingTimeInterval(300) >= expiresAt {
                do {
                    try await refreshToken()
                    return .success(true)
                } catch {
                    return .success(false)
                }
            }
            return .success(Date() < expiresAt)
        }
        return .success(false)
    }
    
    // MARK: - 登出方法
    
    /// 登出
    func logout() async throws {
        let response: APIResponse<String?> = try await APIService.shared.post(
            "/auth/logout",
            parameters: nil
        )
        
        // 清除本地认证信息（不管接口是否成功）
        clearAuth()
    }
    
    // MARK: - 用户信息方法
    
    /// 获取当前用户信息
    func fetchCurrentUser() async throws -> User {
        let response: APIResponse<User> = try await APIService.shared.get("/users/info")
        
        guard let user = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        currentUser = user
        return user
    }
    
    /// 更新用户信息
    func updateUser(_ user: User) async throws -> User {
        let params: [String: Any] = [
            "nickname": user.nickname,
            "email": user.email ?? "",
            "avatarUrl": user.avatarUrl ?? ""
        ]
        
        let response: APIResponse<User> = try await APIService.shared.put(
            "/users/profile",
            parameters: params
        )
        
        guard let updatedUser = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        currentUser = updatedUser
        return updatedUser
    }
    
    // MARK: - 私有方法
    
    /// 保存认证信息
    private func saveAuth(_ authInfo: AuthInfo) {
        currentAuthInfo = authInfo
        
        // 保存到Keychain
        KeychainManager.shared.saveToken(authInfo.accessToken)
        KeychainManager.shared.saveRefreshToken(authInfo.refreshToken)
        KeychainManager.shared.saveTokenExpiration(authInfo.expiresAt)
        
        // 保存到本地
        UserDefaults.standard.set(authInfo.userId, forKey: "currentUserId")
    }
    
    /// 清除认证信息
    private func clearAuth() {
        currentAuthInfo = nil
        currentUser = nil
        
        // 清除Keychain
        KeychainManager.shared.deleteToken()
        KeychainManager.shared.deleteRefreshToken()
        KeychainManager.shared.deleteTokenExpiration()
        
        // 清除本地数据
        UserDefaults.standard.removeObject(forKey: "currentUser")
        UserDefaults.standard.removeObject(forKey: "currentUserId")
    }
    
    /// 加载存储的认证信息
    private func loadStoredAuth() {
        // 从Keychain加载
        if let token = KeychainManager.shared.getToken(),
           let refreshToken = KeychainManager.shared.getRefreshToken(),
           let expiresAt = KeychainManager.shared.getTokenExpiration() {
            let userId = UserDefaults.standard.string(forKey: "currentUserId") ?? ""
            currentAuthInfo = AuthInfo(
                accessToken: token,
                refreshToken: refreshToken,
                expiresIn: Int(expiresAt.timeIntervalSinceNow),
                userId: userId
            )
        }
        
        // 加载用户信息
        if let data = UserDefaults.standard.data(forKey: "currentUser"),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            currentUser = user
        }
    }
}

// MARK: - 登录响应数据

/// 登录响应数据
struct LoginResponseData: Codable {
    var user: User
    var authInfo: AuthInfo
}

// MARK: - 加密工具

/// 加密工具类
class EncryptUtils {
    
    /// MD5加密
    static func md5(_ string: String) -> String {
        guard let data = string.data(using: .utf8) else { return "" }
        
        var digest = [UInt8](repeating: 0, count: 16)
        data.withUnsafeBytes { buffer in
            _ = CC_MD5(buffer.baseAddress, CC_LONG(data.count), &digest)
        }
        
        return digest.map { String(format: "%02hhx", $0) }.joined()
    }
}

// MARK: - CommonCrypto导入
import CommonCrypto
