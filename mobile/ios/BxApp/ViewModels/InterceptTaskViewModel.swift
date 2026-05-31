//
//  InterceptTaskViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  截客任务视图模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI

/// 截客任务视图模型
/// 
/// 负责管理截客任务列表和任务详情的逻辑，包括任务的加载、创建、状态更新等。
@MainActor
class InterceptTaskViewModel: ObservableObject {
    
    // MARK: - Published属性 - 列表
    
    /// 截客任务列表
    @Published var tasks: [InterceptTask] = []
    
    /// 筛选条件
    @Published var filter: InterceptTaskFilter = InterceptTaskFilter()
    
    /// 是否加载中
    @Published var isLoading: Bool = false
    
    /// 是否正在刷新
    @Published var isRefreshing: Bool = false
    
    /// 是否正在加载更多
    @Published var isLoadingMore: Bool = false
    
    /// 错误信息
    @Published var errorMessage: String?
    
    /// 是否显示错误
    @Published var showError: Bool = false
    
    // MARK: - Published属性 - 详情
    
    /// 当前选中的任务
    @Published var selectedTask: InterceptTask?
    
    /// 任务日志列表
    @Published var taskLogs: [InterceptLog] = []
    
    /// 是否正在加载详情
    @Published var isLoadingDetail: Bool = false
    
    /// 是否显示详情
    @Published var showTaskDetail: Bool = false
    
    // MARK: - Published属性 - 创建任务
    
    /// 是否显示创建任务弹窗
    @Published var showCreateSheet: Bool = false
    
    /// 新任务表单数据
    @Published var newTaskForm = NewInterceptTaskForm()
    
    /// 是否正在创建任务
    @Published var isCreating: Bool = false
    
    /// 创建成功提示
    @Published var showCreateSuccess: Bool = false
    
    // MARK: - Published属性 - 统计
    
    /// 任务统计
    @Published var statistics: InterceptTaskStatistics?
    
    /// 平台筛选
    @Published var selectedPlatform: SourcePlatform?
    
    // MARK: - Computed属性
    
    /// 是否有更多数据
    var hasMore: Bool {
        filter.page < calculateTotalPages()
    }
    
    /// 任务总数
    var totalCount: Int {
        statistics?.totalCount ?? tasks.count
    }
    
    /// 待执行任务数
    var pendingCount: Int {
        tasks.filter { $0.status == .pending }.count
    }
    
    /// 执行中任务数
    var runningCount: Int {
        tasks.filter { $0.status == .running }.count
    }
    
    /// 已完成任务数
    var completedCount: Int {
        tasks.filter { $0.status == .completed }.count
    }
    
    /// 今日截客数
    var todayInterceptCount: Int {
        statistics?.todayInterceptedCount ?? tasks.reduce(0) { $0 + $1.interceptedCount }
    }
    
    /// 总截客数
    var totalInterceptCount: Int {
        statistics?.totalInterceptedCount ?? tasks.reduce(0) { $0 + $1.interceptedCount }
    }
    
    /// 按平台分组的任务
    var tasksByPlatform: [SourcePlatform: [InterceptTask]] {
        Dictionary(grouping: tasks) { $0.platform }
    }
    
    /// 支持的平台列表
    var supportedPlatforms: [SourcePlatform] {
        [.douyin, .xiaohongshu, .wechatChannels, .kuaishou]
    }
    
    // MARK: - 私有属性
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    /// 实时更新定时器
    private var realtimeTimer: Timer?
    
    // MARK: - 初始化
    
    init() {
        setupBindings()
    }
    
    deinit {
        realtimeTimer?.invalidate()
    }
    
    // MARK: - 公开方法 - 列表
    
    /// 加载截客任务列表
    func loadTasks() async {
        if filter.page > 1 {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        
        errorMessage = nil
        
        do {
            let response = try await TaskRepository.shared.fetchInterceptTasks(filter: filter)
            if filter.page == 1 {
                tasks = response.list
            } else {
                tasks.append(contentsOf: response.list)
            }
            statistics = response.statistics
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
        isLoadingMore = false
    }
    
    /// 刷新任务列表
    func refreshTasks() async {
        isRefreshing = true
        filter.page = 1
        
        await loadTasks()
        
        isRefreshing = false
    }
    
    /// 加载更多任务
    func loadMoreTasks() async {
        guard hasMore && !isLoadingMore else { return }
        
        filter.page += 1
        await loadTasks()
    }
    
    /// 搜索任务
    func searchTasks(keyword: String) async {
        filter.keyword = keyword.isEmpty ? nil : keyword
        filter.page = 1
        
        await loadTasks()
    }
    
    /// 按平台筛选
    func filterByPlatform(_ platform: SourcePlatform?) async {
        selectedPlatform = platform
        filter.platforms = platform == nil ? nil : [platform!]
        filter.page = 1
        
        await loadTasks()
    }
    
    /// 按状态筛选
    func filterByStatus(_ status: TaskStatus?) async {
        filter.statuses = status == nil ? nil : [status!]
        filter.page = 1
        
        await loadTasks()
    }
    
    /// 清空筛选
    func clearFilter() async {
        filter.clear()
        selectedPlatform = nil
        await loadTasks()
    }
    
    // MARK: - 公开方法 - 创建任务
    
    /// 打开创建任务表单
    func openCreateSheet() {
        newTaskForm = NewInterceptTaskForm()
        showCreateSheet = true
    }
    
    /// 关闭创建任务表单
    func closeCreateSheet() {
        showCreateSheet = false
        newTaskForm = NewInterceptTaskForm()
    }
    
    /// 创建截客任务
    func createTask() async -> Bool {
        guard newTaskForm.isValid else {
            errorMessage = "请填写完整的任务信息"
            showError = true
            return false
        }
        
        isCreating = true
        
        do {
            let task = newTaskForm.toInterceptTask()
            let newTask = try await TaskRepository.shared.createInterceptTask(task)
            tasks.insert(newTask, at: 0)
            showCreateSuccess = true
            closeCreateSheet()
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 添加关键词
    func addKeyword(_ keyword: String) {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              !newTaskForm.keywords.contains(trimmed) else { return }
        newTaskForm.keywords.append(trimmed)
    }
    
    /// 移除关键词
    func removeKeyword(_ keyword: String) {
        newTaskForm.keywords.removeAll { $0 == keyword }
    }
    
    /// 添加排除关键词
    func addExcludeKeyword(_ keyword: String) {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              !newTaskForm.excludeKeywords.contains(trimmed) else { return }
        newTaskForm.excludeKeywords.append(trimmed)
    }
    
    /// 移除排除关键词
    func removeExcludeKeyword(_ keyword: String) {
        newTaskForm.excludeKeywords.removeAll { $0 == keyword }
    }
    
    // MARK: - 公开方法 - 任务操作
    
    /// 删除任务
    func deleteTask(_ task: InterceptTask) async -> Bool {
        do {
            try await TaskRepository.shared.deleteInterceptTask(id: task.id)
            tasks.removeAll { $0.id == task.id }
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 启动任务
    func startTask(_ task: InterceptTask) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.startInterceptTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 暂停任务
    func pauseTask(_ task: InterceptTask) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.pauseInterceptTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 恢复任务
    func resumeTask(_ task: InterceptTask) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.resumeInterceptTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 停止任务
    func stopTask(_ task: InterceptTask) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.stopInterceptTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 一键触达
    func reachOutAll(_ task: InterceptTask) async -> Bool {
        do {
            let result = try await TaskRepository.shared.reachOutInterceptedLeads(taskId: task.id)
            // 重新加载任务详情
            if let updatedTask = try? await TaskRepository.shared.getInterceptTaskDetail(id: task.id) {
                updateTaskInList(updatedTask)
            }
            return result
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    // MARK: - 公开方法 - 详情
    
    /// 加载任务详情
    func loadTaskDetail(_ task: InterceptTask) async {
        selectedTask = task
        isLoadingDetail = true
        showTaskDetail = true
        
        do {
            let detail = try await TaskRepository.shared.getInterceptTaskDetail(id: task.id)
            selectedTask = detail
            taskLogs = detail.executionLogs
            
            // 启动实时更新
            if task.status == .running {
                startRealtimeUpdates(for: task.id)
            }
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoadingDetail = false
    }
    
    /// 关闭任务详情
    func closeTaskDetail() {
        showTaskDetail = false
        selectedTask = nil
        taskLogs = []
        stopRealtimeUpdates()
    }
    
    // MARK: - 私有方法
    
    /// 设置数据绑定
    private func setupBindings() {
        // 监听筛选条件变化
        $filter
            .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
            .dropFirst()
            .sink { [weak self] _ in
                Task { @MainActor in
                    await self?.loadTasks()
                }
            }
            .store(in: &cancellables)
    }
    
    /// 计算总页数
    private func calculateTotalPages() -> Int {
        guard let stats = statistics, stats.totalCount > 0 else { return 1 }
        return (stats.totalCount + filter.pageSize - 1) / filter.pageSize
    }
    
    /// 更新列表中的任务
    private func updateTaskInList(_ task: InterceptTask) {
        if let index = tasks.firstIndex(where: { $0.id == task.id }) {
            tasks[index] = task
        }
        if selectedTask?.id == task.id {
            selectedTask = task
        }
    }
    
    /// 启动实时更新
    private func startRealtimeUpdates(for taskId: String) {
        stopRealtimeUpdates()
        
        // 每3秒更新一次
        realtimeTimer = Timer.scheduledTimer(withTimeInterval: 3, repeats: true) { [weak self] _ in
            Task { @MainActor in
                await self?.refreshTaskProgress(taskId)
            }
        }
    }
    
    /// 停止实时更新
    private func stopRealtimeUpdates() {
        realtimeTimer?.invalidate()
        realtimeTimer = nil
    }
    
    /// 刷新任务进度
    private func refreshTaskProgress(_ taskId: String) async {
        do {
            let detail = try await TaskRepository.shared.getInterceptTaskDetail(id: taskId)
            updateTaskInList(detail)
            taskLogs = detail.executionLogs
            
            // 如果任务已结束，停止实时更新
            if detail.status != .running {
                stopRealtimeUpdates()
            }
        } catch {
            // 静默失败
        }
    }
}

// MARK: - 新建截客任务表单

/// 新建截客任务表单数据
struct NewInterceptTaskForm {
    
    /// 任务名称
    var name: String = ""
    
    /// 任务描述
    var description: String = ""
    
    /// 目标平台
    var platform: SourcePlatform = .douyin
    
    /// 关键词列表
    var keywords: [String] = []
    
    /// 关键词匹配模式
    var keywordMatchMode: KeywordMatchMode = .any
    
    /// 排除关键词列表
    var excludeKeywords: [String] = []
    
    /// 最大截客数量
    var maxInterceptCount: Int = 50
    
    /// 最大搜索次数
    var maxSearchCount: Int = 100
    
    /// 搜索时间范围（小时）
    var searchTimeRange: Int = 24
    
    /// 是否自动触达
    var autoReachOut: Bool = false
    
    /// 触达方式
    var reachOutMethods: [ReachOutMethod] = []
    
    /// 执行间隔（秒）
    var executionInterval: Int = 30
    
    /// 优先级
    var priority: TaskPriority = .medium
    
    /// 最小点赞数
    var minLikes: Int?
    
    /// 最小评论数
    var minComments: Int?
    
    /// 表单是否有效
    var isValid: Bool {
        !name.isEmpty && !keywords.isEmpty
    }
    
    /// 转换为截客任务
    func toInterceptTask() -> InterceptTask {
        InterceptTask(
            name: name,
            description: description.isEmpty ? nil : description,
            platform: platform,
            keywords: keywords,
            keywordMatchMode: keywordMatchMode,
            excludeKeywords: excludeKeywords,
            searchTimeRange: searchTimeRange,
            maxSearchCount: maxSearchCount,
            maxInterceptCount: maxInterceptCount,
            executionInterval: executionInterval,
            autoReachOut: autoReachOut,
            reachOutMethods: reachOutMethods,
            minLikes: minLikes,
            minComments: minComments,
            creatorId: UserDefaults.standard.string(forKey: "currentUserId") ?? "",
            tenantId: UserDefaults.standard.string(forKey: "currentTenantId") ?? ""
        )
    }
}

// MARK: - API 响应扩展

extension TaskRepository {
    
    /// 获取截客任务列表
    func fetchInterceptTasks(filter: InterceptTaskFilter) async throws -> InterceptTaskListResponse {
        var params: [String: Any] = [
            "page": filter.page,
            "pageSize": filter.pageSize,
            "sortBy": filter.sortBy.rawValue,
            "sortOrder": filter.sortOrder.rawValue
        ]
        
        if let statuses = filter.statuses {
            params["statuses"] = statuses.map { $0.rawValue }.joined(separator: ",")
        }
        
        if let platforms = filter.platforms {
            params["platforms"] = platforms.map { $0.rawValue }.joined(separator: ",")
        }
        
        if let keyword = filter.keyword {
            params["keyword"] = keyword
        }
        
        let response: APIResponse<InterceptTaskListResponse> = try await APIService.shared.get(
            "/intercept-tasks",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return data
    }
    
    /// 获取截客任务详情
    func getInterceptTaskDetail(id: String) async throws -> InterceptTask {
        let response: APIResponse<InterceptTask> = try await APIService.shared.get("/intercept-tasks/\(id)")
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
    
    /// 创建截客任务
    func createInterceptTask(_ task: InterceptTask) async throws -> InterceptTask {
        let params: [String: Any] = [
            "name": task.name,
            "description": task.description ?? "",
            "platform": task.platform.rawValue,
            "keywords": task.keywords,
            "keywordMatchMode": task.keywordMatchMode.rawValue,
            "excludeKeywords": task.excludeKeywords,
            "searchTimeRange": task.searchTimeRange,
            "maxSearchCount": task.maxSearchCount,
            "maxInterceptCount": task.maxInterceptCount,
            "executionInterval": task.executionInterval,
            "autoReachOut": task.autoReachOut,
            "reachOutMethods": task.reachOutMethods.map { $0.rawValue },
            "priority": task.priority.rawValue
        ]
        
        let response: APIResponse<InterceptTask> = try await APIService.shared.post(
            "/intercept-tasks",
            parameters: params
        )
        
        guard let newTask = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return newTask
    }
    
    /// 删除截客任务
    func deleteInterceptTask(id: String) async throws {
        let response: APIResponse<String?> = try await APIService.shared.delete("/intercept-tasks/\(id)")
        
        if !response.isSuccess {
            throw APIException(code: response.code, message: response.message)
        }
    }
    
    /// 启动截客任务
    func startInterceptTask(id: String) async throws -> InterceptTask {
        let response: APIResponse<InterceptTask> = try await APIService.shared.post(
            "/intercept-tasks/\(id)/start",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
    
    /// 暂停截客任务
    func pauseInterceptTask(id: String) async throws -> InterceptTask {
        let response: APIResponse<InterceptTask> = try await APIService.shared.post(
            "/intercept-tasks/\(id)/pause",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
    
    /// 恢复截客任务
    func resumeInterceptTask(id: String) async throws -> InterceptTask {
        let response: APIResponse<InterceptTask> = try await APIService.shared.post(
            "/intercept-tasks/\(id)/resume",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
    
    /// 停止截客任务
    func stopInterceptTask(id: String) async throws -> InterceptTask {
        let response: APIResponse<InterceptTask> = try await APIService.shared.post(
            "/intercept-tasks/\(id)/stop",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
    
    /// 一键触达截取的商机
    func reachOutInterceptedLeads(taskId: String) async throws -> Bool {
        let response: APIResponse<Bool> = try await APIService.shared.post(
            "/intercept-tasks/\(taskId)/reach-out",
            parameters: nil
        )
        
        guard let result = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return result
    }
}

// MARK: - 截客任务列表响应

struct InterceptTaskListResponse: Codable {
    var list: [InterceptTask]
    var total: Int
    var page: Int
    var pageSize: Int
    var statistics: InterceptTaskStatistics?
}

// MARK: - 预览

#if DEBUG
extension InterceptTaskViewModel {
    static let preview = InterceptTaskViewModel()
}
#endif
