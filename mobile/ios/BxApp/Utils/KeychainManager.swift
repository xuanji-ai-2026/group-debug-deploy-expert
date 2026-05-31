//
//  KeychainManager.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  钥匙串管理器 - 安全存储敏感数据
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Security

/// 钥匙串管理器
/// 
/// 提供安全存储Token、用户凭证等敏感信息的接口。
class KeychainManager {
    
    // MARK: - 单例
    
    static let shared = KeychainManager()
    
    // MARK: - 常量
    
    /// 钥匙串服务标识
    private let service = "com.beijixing.app"
    
    /// Token键
    private enum Key {
        static let accessToken = "access_token"
        static let refreshToken = "refresh_token"
        static let tokenExpiration = "token_expiration"
        static let deviceId = "device_id"
    }
    
    // MARK: - 初始化
    
    private init() {}
    
    // MARK: - Token管理
    
    /// 保存Token
    /// - Parameter token: 访问令牌
    func saveToken(_ token: String) {
        save(key: Key.accessToken, value: token)
    }
    
    /// 获取Token
    /// - Returns: 访问令牌
    func getToken() -> String? {
        return get(key: Key.accessToken)
    }
    
    /// 删除Token
    func deleteToken() {
        delete(key: Key.accessToken)
    }
    
    /// 保存刷新Token
    /// - Parameter token: 刷新令牌
    func saveRefreshToken(_ token: String) {
        save(key: Key.refreshToken, value: token)
    }
    
    /// 获取刷新Token
    /// - Returns: 刷新令牌
    func getRefreshToken() -> String? {
        return get(key: Key.refreshToken)
    }
    
    /// 删除刷新Token
    func deleteRefreshToken() {
        delete(key: Key.refreshToken)
    }
    
    /// 保存Token过期时间
    /// - Parameter date: 过期时间
    func saveTokenExpiration(_ date: Date) {
        let timestamp = String(date.timeIntervalSince1970)
        save(key: Key.tokenExpiration, value: timestamp)
    }
    
    /// 获取Token过期时间
    /// - Returns: 过期时间
    func getTokenExpiration() -> Date? {
        guard let timestampStr = get(key: Key.tokenExpiration),
              let timestamp = Double(timestampStr) else {
            return nil
        }
        return Date(timeIntervalSince1970: timestamp)
    }
    
    /// 删除Token过期时间
    func deleteTokenExpiration() {
        delete(key: Key.tokenExpiration)
    }
    
    // MARK: - 设备ID管理
    
    /// 保存设备ID
    /// - Parameter deviceId: 设备ID
    func saveDeviceId(_ deviceId: String) {
        save(key: Key.deviceId, value: deviceId)
    }
    
    /// 获取设备ID
    /// - Returns: 设备ID
    func getDeviceId() -> String? {
        return get(key: Key.deviceId)
    }
    
    // MARK: - 通用方法
    
    /// 保存数据
    /// - Parameters:
    ///   - key: 键
    ///   - value: 值
    private func save(key: String, value: String) {
        guard let data = value.data(using: .utf8) else { return }
        
        // 先删除旧的
        delete(key: key)
        
        // 创建查询
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]
        
        // 添加
        SecItemAdd(query as CFDictionary, nil)
    }
    
    /// 获取数据
    /// - Parameter key: 键
    /// - Returns: 值
    private func get(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let data = result as? Data,
              let value = String(data: data, encoding: .utf8) else {
            return nil
        }
        
        return value
    }
    
    /// 删除数据
    /// - Parameter key: 键
    private func delete(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        
        SecItemDelete(query as CFDictionary)
    }
    
    /// 同步保存
    func synchronize() {
        // Keychain操作会自动同步，无需额外处理
    }
    
    /// 清除所有数据
    func clearAll() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service
        ]
        
        SecItemDelete(query as CFDictionary)
    }
}

// MARK: - 扩展NetworkType

extension NetworkManager {
    enum NetworkType: String {
        case wifi = "WiFi"
        case cellular = "Cellular"
        case ethernet = "Ethernet"
        case unknown = "Unknown"
    }
}
