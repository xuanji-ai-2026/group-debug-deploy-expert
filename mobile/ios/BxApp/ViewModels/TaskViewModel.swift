//
//  TaskViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  任务视图模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI

/// 任务视图模型
/// 
/// 负责管理任务列表和任务详情的逻辑，包括任务的加载、状态更新、进度跟踪等。
@MainActor
class TaskViewModel: ObservableObject {
    
    // MARK: - Published属性 - 列表
    
    /// 任务列表
    @Published var tasks: [Task] = []
    
    /// 筛选条件
    @Published var filter: TaskFilter = TaskFilter()
    
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
    @Published var selectedTask: Task?
    
    /// 任务详情
    @Published var taskDetail: TaskDetail?
    
    /// 任务步骤列表
    @Published var taskSteps: [TaskStep] = []
    
    /// 任务日志
    @Published var taskLogs: [TaskStepLog] = []
    
    /// 是否正在加载详情
    @Published var isLoadingDetail: Bool = false
    
    /// 是否显示详情
    @Published var showTaskDetail: Bool = false
    
    /// 是否显示进度弹窗
    @Published var showProgressView: Bool = false
    
    // MARK: - Published属性 - 统计
    
    /// 任务统计
    @Published var statistics: TaskStatistics?
    
    // MARK: - Computed属性 - 列表
    
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
    
    /// 完成任务数
    var completedCount: Int {
        tasks.filter { $0.status == .completed }.count
    }
    
    /// 失败任务数
    var failedCount: Int {
        tasks.filter { $0.status == .failed }.count
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
    
    /// 加载任务列表
    func loadTasks() async {
        if filter.page > 1 {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        
        errorMessage = nil
        
        do {
            let response = try await TaskRepository.shared.fetchTasks(filter: filter)
            tasks = response.list
            statistics = calculateStatistics(response.list)
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
    
    /// 应用筛选
    func applyFilter(_ newFilter: TaskFilter) async {
        filter = newFilter
        filter.page = 1
        
        await loadTasks()
    }
    
    /// 清空筛选
    func clearFilter() async {
        filter.clear()
        await loadTasks()
    }
    
    /// 创建任务
    func createTask(_ task: Task) async -> Bool {
        do {
            let newTask = try await TaskRepository.shared.createTask(task)
            tasks.insert(newTask, at: 0)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 删除任务
    func deleteTask(_ task: Task) async -> Bool {
        do {
            try await TaskRepository.shared.deleteTask(id: task.id)
            tasks.removeAll { $0.id == task.id }
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    // MARK: - 公开方法 - 详情
    
    /// 加载任务详情
    func loadTaskDetail(_ task: Task) async {
        selectedTask = task
        isLoadingDetail = true
        showTaskDetail = true
        
        do {
            // 并行加载详情和步骤
            async let detailResult = TaskRepository.shared.getTaskDetail(id: task.id)
            async let stepsResult = TaskRepository.shared.getTaskSteps(id: task.id)
            
            let (detail, steps) = try await (detailResult, stepsResult)
            taskDetail = detail
            taskSteps = steps
            
            // 合并所有日志
            taskLogs = steps.flatMap { $0.logs }.sorted { $0.createdAt < $1.createdAt }
            
            // 启动实时更新
            startRealtimeUpdates(for: task.id)
            
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
        taskDetail = nil
        taskSteps = []
        taskLogs = []
        stopRealtimeUpdates()
    }
    
    /// 暂停任务
    func pauseTask(_ task: Task) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.pauseTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 恢复任务
    func resumeTask(_ task: Task) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.resumeTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 取消任务
    func cancelTask(_ task: Task) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.cancelTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 重试任务
    func retryTask(_ task: Task) async -> Bool {
        do {
            let updatedTask = try await TaskRepository.shared.retryTask(id: task.id)
            updateTaskInList(updatedTask)
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 打开进度弹窗
    func openProgressView(_ task: Task) {
        selectedTask = task
        showProgressView = true
    }
    
    /// 关闭进度弹窗
    func closeProgressView() {
        showProgressView = false
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
        
        // 订阅任务更新通知
        NotificationCenter.default.publisher(for: .taskUpdated)
            .sink { [weak self] _ in
                Task { @MainActor in
                    await self?.refreshTasks()
                }
            }
            .store(in: &cancellables)
    }
    
    /// 计算统计信息
    private func calculateStatistics(_ tasks: [Task]) -> TaskStatistics {
        TaskStatistics(
            totalCount: tasks.count,
            pendingCount: tasks.filter { $0.status == .pending }.count,
            runningCount: tasks.filter { $0.status == .running }.count,
            completedCount: tasks.filter { $0.status == .completed }.count,
            failedCount: tasks.filter { $0.status == .failed }.count,
            pausedCount: tasks.filter { $0.status == .paused }.count,
            cancelledCount: tasks.filter { $0.status == .cancelled }.count
        )
    }
    
    /// 计算总页数
    private func calculateTotalPages() -> Int {
        guard let stats = statistics, stats.totalCount > 0 else { return 1 }
        return (stats.totalCount + filter.pageSize - 1) / filter.pageSize
    }
    
    /// 更新列表中的任务
    private func updateTaskInList(_ task: Task) {
        if let index = tasks.firstIndex(where: { $0.id == task.id }) {
            tasks[index] = task
        }
        selectedTask = task
        taskDetail?.task = task
    }
    
    /// 启动实时更新
    private func startRealtimeUpdates(for taskId: String) {
        stopRealtimeUpdates()
        
        // 每5秒更新一次进度
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
            let steps = try await TaskRepository.shared.getTaskSteps(id: taskId)
            taskSteps = steps
            
            // 更新进度
            if let runningStep = steps.first(where: { $0.status == .running }) {
                if var detail = taskDetail {
                    detail.task.currentStep = runningStep.stepIndex
                    detail.task.completedSteps = steps.filter { $0.status == .completed }.count
                    detail.task.progress = calculateProgress(from: steps)
                    taskDetail = detail
                }
            }
            
        } catch {
            // 静默失败，不显示错误
        }
    }
    
    /// 计算进度
    private func calculateProgress(from steps: [TaskStep]) -> Int {
        guard !steps.isEmpty else { return 0 }
        let completed = steps.filter { $0.status == .completed }.count
        return Int(Double(completed) / Double(steps.count) * 100)
    }
}

// MARK: - 任务统计

/// 任务统计信息
struct TaskStatistics: Codable {
    
    /// 总数
    var totalCount: Int
    
    /// 待执行数
    var pendingCount: Int
    
    /// 执行中数
    var runningCount: Int
    
    /// 已完成数
    var completedCount: Int
    
    /// 失败数
    var failedCount: Int
    
    /// 已暂停数
    var pausedCount: Int
    
    /// 已取消数
    var cancelledCount: Int
    
    // MARK: - Computed属性
    
    /// 完成率
    var completionRate: Double {
        guard totalCount > 0 else { return 0 }
        return Double(completedCount) / Double(totalCount) * 100
    }
    
    /// 成功率
    var successRate: Double {
        let finished = completedCount + failedCount
        guard finished > 0 else { return 0 }
        return Double(completedCount) / Double(finished) * 100
    }
}

/// 任务详情
struct TaskDetail: Codable {
    var task: Task
    var steps: [TaskStep]
    var logs: [TaskStepLog]
    var relatedLeads: [Lead]?
}

// MARK: - 预览

#if DEBUG
extension TaskViewModel {
    static let preview = TaskViewModel()
}
#endif
