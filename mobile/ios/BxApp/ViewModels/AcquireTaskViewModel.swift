//
//  AcquireTaskViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  获客任务视图模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI

/// 获客任务视图模型
/// 
/// 负责管理获客任务列表和任务详情的逻辑，包括任务的加载、创建、状态更新等。
@MainActor
class AcquireTaskViewModel: ObservableObject {
    
    // MARK: - Published属性 - 列表
    
    /// 获客任务列表
    @Published var tasks: [AcquireTask] = []
    
    /// 筛选条件
    @Published var filter: AcquireTaskFilter = AcquireTaskFilter()
    
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
    @Published var selectedTask: AcquireTask?
    
    /// 任务日志列表
    @Published var taskLogs: [AcquireLog] = []
    
    /// 是否正在加载详情
    @Published var isLoadingDetail: Bool = false
    
    /// 是否显示详情
    @Published var showTaskDetail: Bool = false
    
    // MARK: - Published属性 - 创建任务
    
    /// 是否显示创建任务弹窗
    @Published var showCreateSheet: Bool = false
    
    /// 新任务表单数据
    @Published var newTaskForm = NewAcquireTaskForm()
    
    /// 是否正在创建任务
    @Published var isCreating: Bool = false
    
    /// 创建成功提示
    @Published var showCreateSuccess: Bool = false
    
    // MARK: - Published属性 - 统计
    
    /// 任务统计
    @Published var statistics: AcquireTaskStatistics?
    
    /// 当前步骤（创建向导）
    @Published var createStep: Int = 0
    
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
    
    /// 今日触达数
    var todayReachedCount: Int {
        statistics?.todayReachedCount ?? 0
    }
    
    /// 今日转化数
    var todayConvertedCount: Int {
        statistics?.todayConvertedCount ?? 0
    }
    
    /// 总转化率
    var totalConversionRate: Double {
        statistics?.avgConversionRate ?? 0
    }
    
    /// 按类型分组的任务
    var tasksByType: [AcquireType: [AcquireTask]] {
        Dictionary(grouping: tasks) { $0.acquireType }
    }
    
    /// 创建步骤总数
    let totalCreateSteps = 4
    
    /// 是否可以进入下一步
    var canProceedToNextStep: Bool {
        switch createStep {
        case 0:
            return !newTaskForm.name.isEmpty && newTaskForm.channels.count > 0
        case 1:
            return !newTaskForm.targetAudience.interestTags.isEmpty
        case 2:
            return newTaskForm.contentType != nil
        case 3:
            return newTaskForm.dailyExecutionCount > 0
        default:
            return true
        }
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
    
    /// 加载获客任务列表
    func loadTasks() async {
        if filter.page > 1 {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        
        errorMessage = nil
        
        do {
            let response = try await TaskRepository.shared.fetchAcquireTasks(filter: filter)
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
    
    /// 按类型筛选
    func filterByType(_ type: AcquireType?) async {
        filter.acquireTypes = type == nil ? nil : [type!]
        filter.page = 1
        
        await loadTasks()
    }
    
    /// 按平台筛选
    func filterByPlatform(_ platform: SourcePlatform?) async {
        filter.platforms = platform == nil ? nil : [platform!]
        filter.page = 1
        
        await loadTasks()
    }
    
    /// 清空筛选
    func clearFilter() async {
        filter.clear()
        await loadTasks()
    }
    
    // MARK: - 公开方法 - 创建任务
    
    /// 打开创建任务向导
    func openCreateSheet() {
        newTaskForm = NewAcquireTaskForm()
        createStep = 0
        showCreateSheet = true
    }
    
    /// 关闭创建任务表单
    func closeCreateSheet() {
        showCreateSheet = false
        newTaskForm = NewAcquireTaskForm()
        createStep = 0
    }
    
    /// 进入下一步
    func nextStep() {
        guard createStep < totalCreateSteps - 1 else { return }
        createStep += 1
    }
    
    /// 返回上一步
    func previousStep() {
        guard createStep > 0 else { return }
        createStep -= 1
    }
    
    /// 创建获客任务
    func createTask() async -> Bool {
        guard newTaskForm.isValid else {
            errorMessage = "请填写完整的任务信息"
            showError = true
            return false
        }
        
        isCreating = true
        
        do {
            let task = newTaskForm.toAcquireTask()
            let newTask = try await TaskRepository.shared.createAcquireTask(task)
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
    
    /// 添加获客渠道
    func addChannel(_ channel: AcquireChannel) {
        guard !newTaskForm.channels.contains(where: { $0.platform == channel.platform }) else { return }
        newTaskForm.channels.append(channel)
    }
    
    /// 移除获客渠道
    func removeChannel(_ platform: SourcePlatform) {
        newTaskForm.channels.removeAll { $0.platform == platform }
    }
    
    /// 添加兴趣标签
    func addInterestTag(_ tag: String) {
        let trimmed = tag.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              !newTaskForm.targetAudience.interestTags.contains(trimmed) else { return }
        newTaskForm.targetAudience.interestTags.append(trimmed)
    }
    
    /// 移除兴趣标签
    func removeInterestTag(_ tag: String) {
        newTaskForm.targetAudience.interestTags.removeAll { $0 == tag }
    }
    
    /// 添加内容主题
    func addContentTopic(_ topic: String) {
        let trimmed = topic.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              !newTaskForm.contentTopics.contains(trimmed) else { return }
        newTaskForm.contentTopics.append(trimmed)
    }
    
    /// 移除内容主题
    func removeContentTopic(_ topic: String) {
        newTaskForm.contentTopics.removeAll { $0 == topic }
    }
    
    /// 添加执行日期
    func toggleExecutionDay(_ day: WeekDay) {
        if newTaskForm.executionDays.contains(day) {
            newTaskForm.executionDays.removeAll { $0 == day }
        } else {
            newTaskForm.executionDays.append(day)
        }
    }
    
    // MARK: - 公开方法 - 任务操作
    
    /// 删除任务
    func deleteTask(_ task: AcquireTask) async -> Bool {
        do {
            try await TaskRepository.shared.deleteAcquireTask(id: task.id)
            tasks.removeAll { $0.id == task.id }
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 启动任务
    func startTask(_ task: AcquireTask) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.startAcquireTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 暂停任务
    func pauseTask(_ task: AcquireTask) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.pauseAcquireTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 恢复任务
    func resumeTask(_ task: AcquireTask) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.resumeAcquireTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 停止任务
    func stopTask(_ task: AcquireTask) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.stopAcquireTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    // MARK: - 公开方法 - 详情
    
    /// 加载任务详情
    func loadTaskDetail(_ task: AcquireTask) async {
        selectedTask = task
        isLoadingDetail = true
        showTaskDetail = true
        
        do {
            let detail = try await TaskRepository.shared.getAcquireTaskDetail(id: task.id)
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
    private func updateTaskInList(_ task: AcquireTask) {
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
        
        // 每5秒更新一次
        realtimeTimer = Timer.scheduledTimer(withTimeInterval: 5, repeats: true) { [weak self] _ in
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
            let detail = try await TaskRepository.shared.getAcquireTaskDetail(id: taskId)
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

// MARK: - 新建获客任务表单

/// 新建获客任务表单数据
struct NewAcquireTaskForm {
    
    /// 任务名称
    var name: String = ""
    
    /// 任务描述
    var description: String = ""
    
    /// 获客类型
    var acquireType: AcquireType = .content
    
    /// 获客渠道
    var channels: [AcquireChannel] = []
    
    /// 主平台
    var primaryPlatform: SourcePlatform = .douyin
    
    /// 目标人群
    var targetAudience: TargetAudience = TargetAudience()
    
    /// 目标地区
    var targetLocations: [String] = []
    
    /// 目标行业
    var targetIndustries: [String] = []
    
    /// 内容类型
    var contentType: ContentType?
    
    /// 内容主题
    var contentTopics: [String] = []
    
    /// 是否使用AI生成
    var useAIGeneration: Bool = false
    
    /// AI提示词
    var aiPrompt: String = ""
    
    /// 执行策略
    var executionStrategy: ExecutionStrategy = .smart
    
    /// 每日执行次数
    var dailyExecutionCount: Int = 10
    
    /// 每次获客数量
    var acquireBatchSize: Int = 50
    
    /// 执行时间段
    var executionTimeSlots: [TimeSlot] = TimeSlot.default
    
    /// 执行日期
    var executionDays: [WeekDay] = WeekDay.allCases
    
    /// 触达方式
    var reachOutMethod: ReachOutMethod = .privateMessage
    
    /// 是否自动跟进
    var autoFollowUp: Bool = false
    
    /// 优先级
    var priority: TaskPriority = .medium
    
    /// 表单是否有效
    var isValid: Bool {
        !name.isEmpty && !channels.isEmpty && contentType != nil
    }
    
    /// 转换为获客任务
    func toAcquireTask() -> AcquireTask {
        AcquireTask(
            name: name,
            description: description.isEmpty ? nil : description,
            acquireType: acquireType,
            channels: channels,
            primaryPlatform: primaryPlatform,
            targetAudience: targetAudience,
            targetLocations: targetLocations,
            targetIndustries: targetIndustries,
            contentType: contentType ?? .article,
            contentTopics: contentTopics,
            useAIGeneration: useAIGeneration,
            aiPrompt: aiPrompt.isEmpty ? nil : aiPrompt,
            executionStrategy: executionStrategy,
            dailyExecutionCount: dailyExecutionCount,
            acquireBatchSize: acquireBatchSize,
            executionTimeSlots: executionTimeSlots,
            executionDays: executionDays,
            reachOutMethod: reachOutMethod,
            autoFollowUp: autoFollowUp,
            creatorId: UserDefaults.standard.string(forKey: "currentUserId") ?? "",
            tenantId: UserDefaults.standard.string(forKey: "currentTenantId") ?? ""
        )
    }
}

// MARK: - API 响应扩展

extension TaskRepository {
    
    /// 获取获客任务列表
    func fetchAcquireTasks(filter: AcquireTaskFilter) async throws -> AcquireTaskListResponse {
        var params: [String: Any] = [
            "page": filter.page,
            "pageSize": filter.pageSize,
            "sortBy": filter.sortBy.rawValue,
            "sortOrder": filter.sortOrder.rawValue
        ]
        
        if let statuses = filter.statuses {
            params["statuses"] = statuses.map { $0.rawValue }.joined(separator: ",")
        }
        
        if let types = filter.acquireTypes {
            params["types"] = types.map { $0.rawValue }.joined(separator: ",")
        }
        
        if let platforms = filter.platforms {
            params["platforms"] = platforms.map { $0.rawValue }.joined(separator: ",")
        }
        
        if let keyword = filter.keyword {
            params["keyword"] = keyword
        }
        
        let response: APIResponse<AcquireTaskListResponse> = try await APIService.shared.get(
            "/acquire-tasks",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return data
    }
    
    /// 获取获客任务详情
    func getAcquireTaskDetail(id: String) async throws -> AcquireTask {
        let response: APIResponse<AcquireTask> = try await APIService.shared.get("/acquire-tasks/\(id)")
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
    
    /// 创建获客任务
    func createAcquireTask(_ task: AcquireTask) async throws -> AcquireTask {
        let params: [String: Any] = [
            "name": task.name,
            "description": task.description ?? "",
            "acquireType": task.acquireType.rawValue,
            "channels": task.channels.map { [
                "platform": $0.platform.rawValue,
                "accountId": $0.accountId ?? "",
                "weight": $0.weight
            ]},
            "primaryPlatform": task.primaryPlatform.rawValue,
            "targetAudience": [
                "gender": task.targetAudience.gender.rawValue,
                "activityLevel": task.targetAudience.activityLevel.rawValue
            ],
            "contentType": task.contentType.rawValue,
            "contentTopics": task.contentTopics,
            "useAIGeneration": task.useAIGeneration,
            "executionStrategy": task.executionStrategy.rawValue,
            "dailyExecutionCount": task.dailyExecutionCount,
            "acquireBatchSize": task.acquireBatchSize,
            "executionDays": task.executionDays.map { $0.rawValue },
            "reachOutMethod": task.reachOutMethod.rawValue,
            "autoFollowUp": task.autoFollowUp,
            "priority": task.priority.rawValue
        ]
        
        let response: APIResponse<AcquireTask> = try await APIService.shared.post(
            "/acquire-tasks",
            parameters: params
        )
        
        guard let newTask = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return newTask
    }
    
    /// 删除获客任务
    func deleteAcquireTask(id: String) async throws {
        let response: APIResponse<String?> = try await APIService.shared.delete("/acquire-tasks/\(id)")
        
        if !response.isSuccess {
            throw APIException(code: response.code, message: response.message)
        }
    }
    
    /// 启动获客任务
    func startAcquireTask(id: String) async throws -> AcquireTask {
        let response: APIResponse<AcquireTask> = try await APIService.shared.post(
            "/acquire-tasks/\(id)/start",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
    
    /// 暂停获客任务
    func pauseAcquireTask(id: String) async throws -> AcquireTask {
        let response: APIResponse<AcquireTask> = try await APIService.shared.post(
            "/acquire-tasks/\(id)/pause",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
    
    /// 恢复获客任务
    func resumeAcquireTask(id: String) async throws -> AcquireTask {
        let response: APIResponse<AcquireTask> = try await APIService.shared.post(
            "/acquire-tasks/\(id)/resume",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
    
    /// 停止获客任务
    func stopAcquireTask(id: String) async throws -> AcquireTask {
        let response: APIResponse<AcquireTask> = try await APIService.shared.post(
            "/acquire-tasks/\(id)/stop",
            parameters: nil
        )
        
        guard let task = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        return task
    }
}

// MARK: - 获客任务列表响应

struct AcquireTaskListResponse: Codable {
    var list: [AcquireTask]
    var total: Int
    var page: Int
    var pageSize: Int
    var statistics: AcquireTaskStatistics?
}

// MARK: - 预览

#if DEBUG
extension AcquireTaskViewModel {
    static let preview = AcquireTaskViewModel()
}
#endif
