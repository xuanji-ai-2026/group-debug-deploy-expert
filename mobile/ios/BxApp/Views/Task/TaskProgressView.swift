//
//  TaskProgressView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  任务进度视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 任务进度视图
/// 
/// 展示任务的详细进度、步骤执行情况和日志。
struct TaskProgressView: View {
    
    // MARK: - 属性
    
    /// 任务
    let task: Task
    
    /// 环境
    @Environment(\.dismiss) private var dismiss
    
    /// 任务步骤
    @State private var steps: [TaskStep] = []
    
    /// 任务日志
    @State private var logs: [TaskStepLog] = []
    
    /// 是否加载中
    @State private var isLoading = true
    
    /// 选中的标签页
    @State private var selectedTab = 0
    
    /// 任务操作视图模型
    @StateObject private var taskViewModel = TaskViewModel()
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // 任务概览
                    taskOverviewCard
                    
                    // 进度概览
                    progressOverviewCard
                    
                    // 标签页
                    tabSelector
                    
                    // 标签页内容
                    tabContent
                }
                .padding()
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("任务详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("关闭") { dismiss() }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        if task.status.canPause {
                            Button(action: { pauseTask() }) {
                                Label("暂停任务", systemImage: "pause")
                            }
                        }
                        
                        if task.status.canExecute {
                            Button(action: { resumeTask() }) {
                                Label("继续执行", systemImage: "play")
                            }
                        }
                        
                        if task.status.canRetry {
                            Button(action: { retryTask() }) {
                                Label("重试任务", systemImage: "arrow.clockwise")
                            }
                        }
                        
                        if task.status.canCancel {
                            Button(role: .destructive, action: { cancelTask() }) {
                                Label("取消任务", systemImage: "xmark")
                            }
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .onAppear {
            Task { await loadTaskDetail() }
        }
    }
    
    // MARK: - 组件 - 任务概览卡片
    
    /// 任务概览卡片
    private var taskOverviewCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            // 头部
            HStack {
                // 任务类型
                HStack(spacing: 6) {
                    Image(systemName: task.type.iconName)
                        .foregroundColor(task.type.colorName.color)
                    Text(task.type.name)
                        .font(.subheadline)
                }
                
                Spacer()
                
                // 状态
                TaskStatusBadge(status: task.status)
            }
            
            // 任务名称
            Text(task.name)
                .font(.title3)
                .fontWeight(.bold)
            
            // 描述
            if let description = task.description {
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Divider()
            
            // 详细信息
            VStack(spacing: 8) {
                // 任务编号
                if !task.taskNo.isEmpty {
                    DetailRow(label: "任务编号", value: task.taskNo)
                }
                
                // 关联商机
                if let leadName = task.leadName {
                    DetailRow(label: "关联商机", value: leadName)
                }
                
                // 优先级
                DetailRow(label: "优先级", value: task.priority.name, color: task.priority.colorName.color)
                
                // 创建人
                DetailRow(label: "创建人", value: task.creatorName)
                
                // 执行人
                DetailRow(label: "执行人", value: task.executorName)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 组件 - 进度概览卡片
    
    /// 进度概览卡片
    private var progressOverviewCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("执行进度")
                .font(.headline)
            
            // 进度条
            VStack(spacing: 8) {
                // 百分比
                HStack {
                    Text("\(task.progress)%")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                    
                    Spacer()
                    
                    Text("\(task.completedSteps)/\(task.totalSteps) 步骤")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                // 进度条
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 6)
                            .fill(Color(.systemGray5))
                            .frame(height: 12)
                        
                        RoundedRectangle(cornerRadius: 6)
                            .fill(
                                LinearGradient(
                                    colors: [.blue, .blue.opacity(0.7)],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: geometry.size.width * CGFloat(task.progress) / 100, height: 12)
                    }
                }
                .frame(height: 12)
            }
            
            // 步骤状态统计
            HStack(spacing: 16) {
                StepStatItem(
                    title: "已完成",
                    count: steps.filter { $0.status == .completed }.count,
                    color: .green
                )
                
                StepStatItem(
                    title: "进行中",
                    count: steps.filter { $0.status == .running }.count,
                    color: .blue
                )
                
                StepStatItem(
                    title: "待执行",
                    count: steps.filter { $0.status == .pending }.count,
                    color: .gray
                )
                
                StepStatItem(
                    title: "失败",
                    count: steps.filter { $0.status == .failed }.count,
                    color: .red
                )
            }
            .padding(.top, 8)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 组件 - 标签页选择器
    
    /// 标签页选择器
    private var tabSelector: some View {
        HStack(spacing: 0) {
            ProgressTabButton(title: "步骤", isSelected: selectedTab == 0) {
                selectedTab = 0
            }
            
            ProgressTabButton(title: "日志", isSelected: selectedTab == 1) {
                selectedTab = 1
            }
            
            ProgressTabButton(title: "详情", isSelected: selectedTab == 2) {
                selectedTab = 2
            }
        }
        .padding(4)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(8)
    }
    
    // MARK: - 组件 - 标签页内容
    
    /// 标签页内容
    @ViewBuilder
    private var tabContent: some View {
        if isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, minHeight: 200)
        } else {
            switch selectedTab {
            case 0:
                stepsContent
            case 1:
                logsContent
            case 2:
                detailContent
            default:
                EmptyView()
            }
        }
    }
    
    // MARK: - 组件 - 步骤内容
    
    /// 步骤内容
    private var stepsContent: some View {
        VStack(spacing: 12) {
            ForEach(steps) { step in
                StepCard(step: step)
            }
        }
    }
    
    // MARK: - 组件 - 日志内容
    
    /// 日志内容
    private var logsContent: some View {
        VStack(spacing: 8) {
            if logs.isEmpty {
                Text("暂无日志")
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, minHeight: 100)
            } else {
                ForEach(logs) { log in
                    LogItem(log: log)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 组件 - 详情内容
    
    /// 详情内容
    private var detailContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            // 时间信息
            Group {
                if let startTime = task.actualStartAt {
                    DetailRow(label: "开始时间", value: startTime.formatted())
                }
                
                if let endTime = task.actualEndAt {
                    DetailRow(label: "结束时间", value: endTime.formatted())
                }
                
                if let scheduleStart = task.scheduledStartAt {
                    DetailRow(label: "计划开始", value: scheduleStart.formatted())
                }
                
                if let scheduleEnd = task.scheduledEndAt {
                    DetailRow(label: "计划结束", value: scheduleEnd.formatted())
                }
            }
            
            // 结果信息
            if let result = task.result {
                Divider()
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("执行结果")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    Text(result)
                        .font(.body)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 方法
    
    /// 加载任务详情
    private func loadTaskDetail() async {
        isLoading = true
        
        do {
            steps = try await TaskRepository.shared.getTaskSteps(id: task.id)
            logs = steps.flatMap { $0.logs }.sorted { $0.createdAt < $1.createdAt }
        } catch {
            // 处理错误
        }
        
        isLoading = false
    }
    
    /// 暂停任务
    private func pauseTask() {
        Task {
            let success = await taskViewModel.pauseTask(task)
            if success {
                dismiss()
            }
        }
    }
    
    /// 继续任务
    private func resumeTask() {
        Task {
            let success = await taskViewModel.resumeTask(task)
            if success {
                await loadTaskDetail()
            }
        }
    }
    
    /// 重试任务
    private func retryTask() {
        Task {
            let success = await taskViewModel.retryTask(task)
            if success {
                await loadTaskDetail()
            }
        }
    }
    
    /// 取消任务
    private func cancelTask() {
        Task {
            let success = await taskViewModel.cancelTask(task)
            if success {
                dismiss()
            }
        }
    }
}

// MARK: - 详情行

struct DetailRow: View {
    let label: String
    let value: String
    var color: Color = .primary
    
    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .foregroundColor(color)
        }
        .font(.subheadline)
    }
}

// MARK: - 步骤统计项

struct StepStatItem: View {
    let title: String
    let count: Int
    let color: Color
    
    var body: some View {
        VStack(spacing: 2) {
            Text("\(count)")
                .font(.headline)
                .foregroundColor(color)
            
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 标签页按钮

struct ProgressTabButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .fontWeight(isSelected ? .semibold : .regular)
                .foregroundColor(isSelected ? .white : .secondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(isSelected ? Color.blue : Color.clear)
                .cornerRadius(6)
        }
    }
}

// MARK: - 步骤卡片

struct StepCard: View {
    let step: TaskStep
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // 步骤指示器
            VStack(spacing: 4) {
                // 状态图标
                stepStatusIcon
                
                // 连接线
                Rectangle()
                    .fill(Color(.systemGray4))
                    .frame(width: 2)
                    .frame(maxHeight: .infinity)
            }
            .frame(width: 24)
            
            // 内容
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(step.name)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    Spacer()
                    
                    stepStatusBadge
                }
                
                if let description = step.description {
                    Text(description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                // 时间信息
                if let startTime = step.startedAt {
                    Text("开始: \(startTime.formatted())")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                if let endTime = step.completedAt {
                    Text("完成: \(endTime.formatted())")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                // 结果
                if let result = step.result {
                    Text(result)
                        .font(.caption)
                        .foregroundColor(.blue)
                        .padding(.top, 4)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    /// 步骤状态图标
    @ViewBuilder
    private var stepStatusIcon: some View {
        switch step.status {
        case .completed:
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(.green)
        case .running:
            Image(systemName: "arrow.triangle.2.circlepath")
                .foregroundColor(.blue)
                .rotationEffect(.degrees(isAnimating ? 360 : 0))
                .animation(.linear(duration: 1).repeatForever(autoreverses: false), value: isAnimating)
                .onAppear { isAnimating = true }
        case .failed:
            Image(systemName: "xmark.circle.fill")
                .foregroundColor(.red)
        case .skipped:
            Image(systemName: "forward.fill")
                .foregroundColor(.gray)
        case .pending:
            Image(systemName: "circle")
                .foregroundColor(.gray)
        }
    }
    
    @State private var isAnimating = false
    
    /// 步骤状态徽章
    private var stepStatusBadge: some View {
        Text(statusText)
            .font(.caption2)
            .foregroundColor(statusColor)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(statusColor.opacity(0.1))
            .cornerRadius(4)
    }
    
    private var statusText: String {
        switch step.status {
        case .completed: return "已完成"
        case .running: return "进行中"
        case .failed: return "失败"
        case .skipped: return "已跳过"
        case .pending: return "待执行"
        }
    }
    
    private var statusColor: Color {
        switch step.status {
        case .completed: return .green
        case .running: return .blue
        case .failed: return .red
        case .skipped: return .gray
        case .pending: return .secondary
        }
    }
}

// MARK: - 日志项

struct LogItem: View {
    let log: TaskStepLog
    
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: log.level.iconName)
                .font(.caption)
                .foregroundColor(log.level.colorName.color)
                .frame(width: 16)
            
            Text(log.message)
                .font(.caption)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(log.createdAt.formatted(date: .omitted, time: .shortened))
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - 预览

#if DEBUG
struct TaskProgressView_Previews: PreviewProvider {
    static var previews: some View {
        TaskProgressView(task: .preview)
    }
}
#endif
