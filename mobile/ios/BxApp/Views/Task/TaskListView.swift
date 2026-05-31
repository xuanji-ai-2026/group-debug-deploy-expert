//
//  TaskListView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  任务列表视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 任务列表视图
/// 
/// 展示任务列表，支持筛选、搜索、状态过滤等功能。
struct TaskListView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = TaskViewModel()
    
    /// 搜索文本
    @State private var searchText = ""
    
    /// 选中的状态过滤
    @State private var selectedStatus: TaskStatus?
    
    /// 是否显示详情
    @State private var showDetail = false
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.isLoading && viewModel.tasks.isEmpty {
                    LoadingView(message: "加载任务...")
                } else if viewModel.tasks.isEmpty {
                    EmptyStateView(
                        icon: "checklist",
                        title: "暂无任务",
                        message: selectedStatus != nil ? "没有符合条件的状态的任务" : "开始创建您的第一个任务",
                        actionTitle: selectedStatus != nil ? "清除筛选" : "创建任务"
                    ) {
                        if selectedStatus != nil {
                            selectedStatus = nil
                            Task { await viewModel.loadTasks() }
                        } else {
                            print("[TaskListView] 显示创建任务(预留)")
                        }
                    }
                } else {
                    taskList
                }
            }
            .navigationTitle("任务")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(action: {}) {
                            Label("创建任务", systemImage: "plus")
                        }
                        
                        Button(action: {}) {
                            Label("刷新", systemImage: "arrow.clockwise")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .searchable(text: $searchText, prompt: "搜索任务")
            .onChange(of: searchText) { _, newValue in
                Task {
                    await viewModel.searchTasks(keyword: newValue)
                }
            }
            .refreshable {
                await viewModel.refreshTasks()
            }
            .sheet(isPresented: $showDetail) {
                if let task = viewModel.selectedTask {
                    TaskProgressView(task: task)
                }
            }
            .alert("提示", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "操作失败")
            }
        }
        .onAppear {
            if viewModel.tasks.isEmpty {
                Task { await viewModel.loadTasks() }
            }
        }
    }
    
    // MARK: - 组件 - 任务列表
    
    /// 任务列表
    private var taskList: some View {
        ScrollView {
            VStack(spacing: 12) {
                // 统计概览
                statisticsSection
                
                // 状态过滤
                statusFilterSection
                
                // 任务列表
                ForEach(viewModel.tasks) { task in
                    TaskCard(task: task)
                        .onTapGesture {
                            viewModel.selectedTask = task
                            showDetail = true
                        }
                        .contextMenu {
                            taskContextMenu(task)
                        }
                }
                
                // 加载更多
                if viewModel.isLoadingMore {
                    ProgressView()
                        .padding()
                }
            }
            .padding()
        }
    }
    
    // MARK: - 组件 - 统计概览
    
    /// 统计概览
    private var statisticsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("任务概览")
                .font(.headline)
            
            HStack(spacing: 12) {
                TaskStatCard(
                    title: "待执行",
                    count: viewModel.pendingCount,
                    color: .gray,
                    icon: "clock"
                )
                
                TaskStatCard(
                    title: "执行中",
                    count: viewModel.runningCount,
                    color: .blue,
                    icon: "play.fill"
                )
                
                TaskStatCard(
                    title: "已完成",
                    count: viewModel.completedCount,
                    color: .green,
                    icon: "checkmark.circle.fill"
                )
                
                TaskStatCard(
                    title: "失败",
                    count: viewModel.failedCount,
                    color: .red,
                    icon: "xmark.circle.fill"
                )
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 组件 - 状态过滤
    
    /// 状态过滤
    private var statusFilterSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // 全部
                StatusFilterChip(
                    title: "全部",
                    isSelected: selectedStatus == nil
                ) {
                    selectedStatus = nil
                    Task { await viewModel.clearFilter() }
                }
                
                // 各状态
                ForEach(TaskStatus.allCases, id: \.self) { status in
                    StatusFilterChip(
                        title: status.name,
                        color: status.colorName.color,
                        isSelected: selectedStatus == status
                    ) {
                        selectedStatus = status
                        var filter = TaskFilter()
                        filter.statuses = [status]
                        Task { await viewModel.applyFilter(filter) }
                    }
                }
            }
        }
    }
    
    // MARK: - 组件 - 右键菜单
    
    /// 任务右键菜单
    private func taskContextMenu(_ task: Task) -> some View {
        Group {
            Button(action: {
                viewModel.selectedTask = task
                showDetail = true
            }) {
                Label("查看详情", systemImage: "eye")
            }
            
            if task.status.canPause {
                Button(action: {
                    Task { await viewModel.pauseTask(task) }
                }) {
                    Label("暂停", systemImage: "pause")
                }
            }
            
            if task.status.canExecute {
                Button(action: {
                    Task { await viewModel.resumeTask(task) }
                }) {
                    Label("执行", systemImage: "play")
                }
            }
            
            if task.status.canRetry {
                Button(action: {
                    Task { await viewModel.retryTask(task) }
                }) {
                    Label("重试", systemImage: "arrow.clockwise")
                }
            }
            
            Divider()
            
            Button(role: .destructive, action: {
                Task { await viewModel.deleteTask(task) }
            }) {
                Label("删除", systemImage: "trash")
            }
        }
    }
}

// MARK: - 任务统计卡片

struct TaskStatCard: View {
    let title: String
    let count: Int
    let color: Color
    let icon: String
    
    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .foregroundColor(color)
            
            Text("\(count)")
                .font(.title3)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(color.opacity(0.1))
        .cornerRadius(8)
    }
}

// MARK: - 状态过滤芯片

struct StatusFilterChip: View {
    let title: String
    var color: Color = .blue
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .foregroundColor(isSelected ? .white : color)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? color : color.opacity(0.1))
                .cornerRadius(16)
        }
    }
}

// MARK: - 任务卡片

struct TaskCard: View {
    let task: Task
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // 头部
            HStack {
                // 任务类型
                HStack(spacing: 4) {
                    Image(systemName: task.type.iconName)
                        .foregroundColor(task.type.colorName.color)
                    Text(task.type.name)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // 状态
                TaskStatusBadge(status: task.status)
            }
            
            // 任务名称
            Text(task.name)
                .font(.headline)
                .lineLimit(2)
            
            // 描述
            if let description = task.description {
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
            
            // 进度条
            if task.totalSteps > 0 {
                TaskProgressBar(
                    progress: task.progress,
                    completedSteps: task.completedSteps,
                    totalSteps: task.totalSteps
                )
            }
            
            // 底部信息
            HStack {
                // 优先级
                HStack(spacing: 2) {
                    Image(systemName: task.priority.iconName)
                        .font(.caption2)
                    Text(task.priority.name)
                        .font(.caption2)
                }
                .foregroundColor(task.priority.colorName.color)
                
                Spacer()
                
                // 时间
                if let endTime = task.scheduledEndAt ?? task.actualEndAt {
                    Text(endTime.formatted(date: .abbreviated, time: .omitted))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 任务状态徽章

struct TaskStatusBadge: View {
    let status: TaskStatus
    
    var body: some View {
        Text(status.name)
            .font(.caption)
            .fontWeight(.medium)
            .foregroundColor(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(status.colorName.color)
            .cornerRadius(4)
    }
}

// MARK: - 任务进度条

struct TaskProgressBar: View {
    let progress: Int
    let completedSteps: Int
    let totalSteps: Int
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // 进度条
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    // 背景
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(.systemGray5))
                        .frame(height: 8)
                    
                    // 进度
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color.blue)
                        .frame(width: geometry.size.width * CGFloat(progress) / 100, height: 8)
                }
            }
            .frame(height: 8)
            
            // 步骤文字
            Text("\(completedSteps)/\(totalSteps) 步骤完成")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - 预览

#if DEBUG
struct TaskListView_Previews: PreviewProvider {
    static var previews: some View {
        TaskListView()
    }
}
#endif
