//
//  UserRepository.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  用户数据仓库
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

/// 用户数据仓库
/// 
/// 负责用户相关数据的获取、缓存和持久化。
class UserRepository {
    
    // MARK: - 单例
    
    static let shared = UserRepository()
    
    // MARK: - 属性
    
    /// 内存缓存
    private var cache: [String: User] = [:]
    
    /// 缓存有效期（秒）
    private let cacheExpiration: TimeInterval = 300
    
    /// 缓存时间记录
    private var cacheTimestamps: [String: Date] = [:]
    
    // MARK: - 初始化
    
    private init() {}
    
    // MARK: - 公开方法
    
    /// 获取用户信息
    /// - Parameters:
    ///   - id: 用户ID
    ///   - forceRefresh: 是否强制刷新
    /// - Returns: 用户信息
    func getUser(id: String, forceRefresh: Bool = false) async throws -> User {
        // 检查缓存
        if !forceRefresh, let cached = getCachedUser(id: id) {
            return cached
        }
        
        // 从API获取
        let response: APIResponse<User> = try await APIService.shared.get("/users/\(id)")
        
        guard let user = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        // 更新缓存
        cacheUser(user)
        
        return user
    }
    
    /// 获取当前用户
    /// - Returns: 当前用户信息
    func getCurrentUser() async throws -> User {
        // 先尝试从本地获取
        if let localUser = getLocalUser() {
            // 异步刷新
            Task {
                try? await refreshUser(id: localUser.id)
            }
            return localUser
        }
        
        // 从API获取
        return try await fetchCurrentUser()
    }
    
    /// 刷新用户信息
    /// - Parameter id: 用户ID
    func refreshUser(id: String) async throws {
        _ = try await getUser(id: id, forceRefresh: true)
    }
    
    /// 更新用户信息
    /// - Parameter user: 用户信息
    /// - Returns: 更新后的用户信息
    func updateUser(_ user: User) async throws -> User {
        let params: [String: Any] = [
            "nickname": user.nickname,
            "email": user.email ?? "",
            "avatarUrl": user.avatarUrl ?? ""
        ]
        
        let response: APIResponse<User> = try await APIService.shared.put(
            "/users/\(user.id)",
            parameters: params
        )
        
        guard let updatedUser = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        // 更新缓存
        cacheUser(updatedUser)
        
        return updatedUser
    }
    
    /// 上传头像
    /// - Parameters:
    ///   - userId: 用户ID
    ///   - imageData: 头像图片数据
    /// - Returns: 头像URL
    func uploadAvatar(userId: String, imageData: Data) async throws -> String {
        let response: APIResponse<String> = try await APIService.shared.upload(
            "/users/\(userId)/avatar",
            fileData: imageData,
            fileName: "avatar.jpg",
            mimeType: "image/jpeg"
        )
        
        guard let avatarUrl = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return avatarUrl
    }
    
    /// 获取用户列表
    /// - Parameters:
    ///   - keyword: 搜索关键词
    ///   - page: 页码
    ///   - pageSize: 每页数量
    /// - Returns: 用户分页列表
    func getUserList(
        keyword: String? = nil,
        page: Int = 1,
        pageSize: Int = 20
    ) async throws -> PageResponse<User> {
        var params: [String: Any] = [
            "page": page,
            "pageSize": pageSize
        ]
        
        if let keyword = keyword {
            params["keyword"] = keyword
        }
        
        let response: APIResponse<PageResponse<User>> = try await APIService.shared.get(
            "/users/list",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return data
    }
    
    /// 获取用户统计数据
    /// - Returns: 用户统计数据
    func getUserStatistics() async throws -> UserStatistics {
        let response: APIResponse<UserStatistics> = try await APIService.shared.get(
            "/users/statistics"
        )
        
        guard let statistics = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return statistics
    }
    
    /// 清除用户缓存
    /// - Parameter id: 用户ID（nil表示清除所有）
    func clearCache(id: String? = nil) {
        if let id = id {
            cache.removeValue(forKey: id)
            cacheTimestamps.removeValue(forKey: id)
        } else {
            cache.removeAll()
            cacheTimestamps.removeAll()
        }
    }
    
    // MARK: - 私有方法
    
    /// 获取缓存的用户
    private func getCachedUser(id: String) -> User? {
        guard let user = cache[id],
              let timestamp = cacheTimestamps[id] else {
            return nil
        }
        
        // 检查是否过期
        if Date().timeIntervalSince(timestamp) > cacheExpiration {
            cache.removeValue(forKey: id)
            cacheTimestamps.removeValue(forKey: id)
            return nil
        }
        
        return user
    }
    
    /// 缓存用户
    private func cacheUser(_ user: User) {
        cache[user.id] = user
        cacheTimestamps[user.id] = Date()
    }
    
    /// 获取本地存储的用户
    private func getLocalUser() -> User? {
        if let data = UserDefaults.standard.data(forKey: "currentUser"),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            return user
        }
        return nil
    }
    
    /// 获取当前用户
    private func fetchCurrentUser() async throws -> User {
        let response: APIResponse<User> = try await APIService.shared.get("/users/info")
        
        guard let user = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        // 保存到本地
        if let data = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(data, forKey: "currentUser")
        }
        
        // 缓存
        cacheUser(user)
        
        return user
    }
}

// MARK: - 用户统计

/// 用户统计数据
struct UserStatistics: Codable {
    
    /// 总用户数
    var totalUsers: Int
    
    /// 今日新增
    var todayNewUsers: Int
    
    /// 本周新增
    var weeklyNewUsers: Int
    
    /// 本月新增
    var monthlyNewUsers: Int
    
    /// VIP用户数
    var vipUsers: Int
    
    /// 企业用户数
    var enterpriseUsers: Int
    
    /// 活跃用户数
    var activeUsers: Int
    
    /// 活跃率
    var activeRate: Double {
        guard totalUsers > 0 else { return 0 }
        return Double(activeUsers) / Double(totalUsers) * 100
    }
}
