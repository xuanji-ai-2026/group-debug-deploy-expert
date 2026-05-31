//
//  TaskRepository.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  任务数据仓库
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

/// 任务数据仓库
/// 
/// 负责任务相关数据的获取、缓存、CRUD操作。
class TaskRepository {
    
    // MARK: - 单例
    
    static let shared = TaskRepository()
    
    // MARK: - 属性
    
    /// 内存缓存
    private var cache: [String: Task] = [:]
    
    /// 列表缓存
    private var listCache: [String: [Task]] = [:]
    
    /// 缓存有效期（秒）
    private let cacheExpiration: TimeInterval = 120
    
    /// 缓存时间记录
    private var cacheTimestamps: [String: Date] = [:]
    
    // MARK: - 初始化
    
    private init() {}
    
    // MARK: - 公开方法 - 列表
    
    /// 获取任务列表
    /// - Parameter filter: 筛选条件
    /// - Returns: 任务分页列表
    func fetchTasks(filter: TaskFilter) async throws -> PageResponse<Task> {
        // 构建查询参数
        var params: [String: Any] = [
            "page": filter.page,
            "pageSize": filter.pageSize,
            "sortBy": filter.sortBy.rawValue,
            "sortOrder": filter.sortOrder.rawValue
        ]
        
        // 添加筛选条件
        if let types = filter.types, !types.isEmpty {
            params["types"] = types.map { $0.rawValue }.joined(separator: ",")
        }
        
        if let statuses = filter.statuses, !statuses.isEmpty {
            params["statuses"] = statuses.map { $0.rawValue }.joined(separator: ",")
        }
        
        if let priorities = filter.priorities, !priorities.isEmpty {
            params["priorities"] = priorities.map { $0.rawValue }.joined(separator: ",")
        }
        
        if let keyword = filter.keyword {
            params["keyword"] = keyword
        }
        
        // 发送请求
        let response: APIResponse<PageResponse<Task>> = try await APIService.shared.get(
            "/tasks",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return data
    }
    
    /// 搜索任务
    /// - Parameters:
    ///   - keyword: 关键词
    ///   - page: 页码
    /// - Returns: 任务分页列表
    func searchTasks(keyword: String, page: Int = 1) async throws -> PageResponse<Task> {
        let params: [String: Any] = [
            "keyword": keyword,
            "page": page,
            "pageSize": 20
        ]
        
        let response: APIResponse<PageResponse<Task>> = try await APIService.shared.get(
            "/tasks/search",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return data
    }
    
    /// 获取任务统计数据
    /// - Returns: 任务统计数据
    func fetchTaskStatistics() async throws -> TaskStatistics {
        let response: APIResponse<TaskStatistics> = try await APIService.shared.get(
            "/tasks/statistics"
        )
        
        guard let statistics = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return statistics
    }
    
    // MARK: - 公开方法 - 详情
    
    /// 获取任务详情
    /// - Parameter id: 任务ID
    /// - Returns: 任务详情
    func getTaskDetail(id: String) async throws -> TaskDetail {
        let response: APIResponse<TaskDetail> = try await APIService.shared.get("/tasks/\(id)/detail")
        
        guard let detail = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return detail
    }
    
    /// 获取任务步骤
    /// - Parameter id: 任务ID
    /// - Returns: 步骤列表
    func getTaskSteps(id: String) async throws -> [TaskStep] {
        let response: APIResponse<[TaskStep]> = try await APIService.shared.get("/tasks/\(id)/steps")
        
        guard let steps = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return steps
    }
    
    // MARK: - 公开方法 - CRUD
    
    /// 创建任务
    /// - Parameter task: 任务信息
    /// - Returns: 创建的任务
    func createTask(_ task: Task) async throws -> Task {
        let params: [String: Any] = [
            "name": task.name,
            "description": task.description ?? "",
            "type": task.type.rawValue,
            "priority": task.priority.rawValue,
            "leadId": task.leadId ?? "",
            "accountId": task.accountId ?? "",
            "scheduledStartAt": task.scheduledStartAt?.toISOString() ?? "",
            "scheduledEndAt": task.scheduledEndAt?.toISOString() ?? ""
        ]
        
        let response: APIResponse<Task> = try await APIService.shared.post(
            "/tasks",
            parameters: params
        )
        
        guard let newTask = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return newTask
    }
    
    /// 更新任务
    /// - Parameter task: 任务信息
    /// - Returns: 更新后的任务
    func updateTask(_ task: Task) async throws -> Task {
        let params: [String: Any] = [
            "name": task.name,
            "description": task.description ?? "",
            "priority": task.priority.rawValue
        ]
        
        let response: APIResponse<Task> = try await APIService.shared.put(
            "/tasks/\(task.id)",
            parameters: params
        )
        
        guard let updatedTask = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        // 更新缓存
        cache[task.id] = updatedTask
        
        return updatedTask
    }
    
    /// 删除任务
    /// - Parameter id: 任务ID
    func deleteTask(id: String) async throws {
        let response: APIResponse<String?> = try await APIService.shared.delete("/tasks/\(id)")
        
        if !response.isSuccess {
            throw APIException(code: response.code, message: response.message)
        }
        
        // 清除缓存
        cache.removeValue(forKey: id)
    }
    
    // MARK: - 公开方法 - 操作
    
    /// 暂停任务
    /// - Parameter id: 任务ID
    /// - Returns: 更新后的任务
    func pauseTask(id: String) async throws -> Task {
        let response: APIResponse<Task> = try await APIService.shared.post(
            "/tasks/\(id)/pause",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        cache[id] = task
        return task
    }
    
    /// 恢复任务
    /// - Parameter id: 任务ID
    /// - Returns: 更新后的任务
    func resumeTask(id: String) async throws -> Task {
        let response: APIResponse<Task> = try await APIService.shared.post(
            "/tasks/\(id)/resume",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        cache[id] = task
        return task
    }
    
    /// 取消任务
    /// - Parameter id: 任务ID
    /// - Returns: 更新后的任务
    func cancelTask(id: String) async throws -> Task {
        let response: APIResponse<Task> = try await APIService.shared.post(
            "/tasks/\(id)/cancel",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        cache[id] = task
        return task
    }
    
    /// 重试任务
    /// - Parameter id: 任务ID
    /// - Returns: 更新后的任务
    func retryTask(id: String) async throws -> Task {
        let response: APIResponse<Task> = try await APIService.shared.post(
            "/tasks/\(id)/retry",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        cache[id] = task
        return task
    }
    
    /// 同步任务
    func syncTasks() async throws {
        print("[TaskRepository] 离线数据同步(预留)")
    }
    
    /// 清除缓存
    func clearCache() {
        cache.removeAll()
        listCache.removeAll()
        cacheTimestamps.removeAll()
    }
}
