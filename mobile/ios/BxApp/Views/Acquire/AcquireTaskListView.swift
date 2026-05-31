//
//  AcquireTaskListView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  获客任务列表视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 获客任务列表视图
/// 
/// 展示获客任务列表，支持刷新、加载更多、状态筛选等功能。
struct AcquireTaskListView: View {
    
    // MARK: - 属性
    
    @ObservedObject var viewModel: AcquireTaskViewModel
    
    // MARK: - 界面
    
    var body: some View {
        ZStack {
            if viewModel.isLoading && viewModel.tasks.isEmpty {
                LoadingStateView(message: "加载任务中...")
            } else if viewModel.tasks.isEmpty {
                EmptyStateView(
                    icon: "person.badge.plus",
                    title: "暂无获客任务",
                    message: "创建您的第一个获客任务，主动出击获取客户",
                    buttonTitle: "创建任务"
                ) {
                    viewModel.openCreateSheet()
                }
            } else {
                taskList
            }
        }
    }
    
    // MARK: - 组件 - 任务列表
    
    /// 任务列表
    private var taskList: some View {
        List {
            ForEach(viewModel.tasks) { task in
                AcquireTaskCard(task: task, viewModel: viewModel)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                    .onTapGesture {
                        Task {
                            await viewModel.loadTaskDetail(task)
                        }
                    }
            }
            
            // 加载更多
            if viewModel.hasMore {
                loadMoreSection
            }
        }
        .listStyle(PlainListStyle())
        .refreshable {
            await viewModel.refreshTasks()
        }
    }
    
    // MARK: - 组件 - 加载更多
    
    /// 加载更多区域
    private var loadMoreSection: some View {
        HStack {
            Spacer()
            
            if viewModel.isLoadingMore {
                ProgressView()
                    .scaleEffect(0.8)
            } else {
                Button("加载更多") {
                    Task {
                        await viewModel.loadMoreTasks()
                    }
                }
                .font(.subheadline)
                .foregroundColor(.blue)
            }
            
            Spacer()
        }
        .padding()
        .listRowSeparator(.hidden)
    }
}

// MARK: - 获客任务卡片

struct AcquireTaskCard: View {
    let task: AcquireTask
    @ObservedObject var viewModel: AcquireTaskViewModel
    @State private var showActionSheet = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // 头部：类型 + 状态 + 操作
            headerSection
            
            // 任务名称和描述
            contentSection
            
            // 渠道展示
            channelsSection
            
            // 进度和统计
            progressSection
            
            // 转化数据
            conversionStatsSection
            
            // 底部操作按钮
            footerSection
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
        .contextMenu {
            taskContextMenu
        }
        .confirmationDialog("选择操作", isPresented: $showActionSheet, titleVisibility: .visible) {
            actionSheetButtons
        }
    }
    
    // MARK: - 头部
    
    private var headerSection: some View {
        HStack {
            // 获客类型
            HStack(spacing: 6) {
                Image(systemName: task.acquireType.iconName)
                    .font(.title3)
                    .foregroundColor(acquireTypeColor)
                
                Text(task.acquireType.name)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // 状态标签
            StatusBadge(status: task.status)
            
            // 更多操作按钮
            Button(action: { showActionSheet = true }) {
                Image(systemName: "ellipsis")
                    .foregroundColor(.secondary)
                    .padding(4)
            }
        }
    }
    
    private var acquireTypeColor: Color {
        switch task.acquireType {
        case .content: return .blue
        case .community: return .green
        case .event: return .orange
        case .partnership: return .purple
        case .paid: return .red
        }
    }
    
    // MARK: - 内容
    
    private var contentSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(task.name)
                .font(.headline)
                .lineLimit(1)
            
            if let description = task.description, !description.isEmpty {
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
        }
    }
    
    // MARK: - 渠道展示
    
    private var channelsSection: some View {
        FlowLayout(spacing: 6) {
            ForEach(task.channels.prefix(3)) { channel in
                ChannelTag(channel: channel)
            }
            
            if task.channels.count > 3 {
                Text("+\(task.channels.count - 3)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color(.systemGray5))
                    .cornerRadius(4)
            }
            
            if task.useAIGeneration {
                Label("AI生成", systemImage: "sparkles")
                    .font(.caption)
                    .foregroundColor(.purple)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color.purple.opacity(0.1))
                    .cornerRadius(4)
            }
        }
    }
    
    // MARK: - 进度
    
    private var progressSection: some View {
        VStack(spacing: 6) {
            // 进度条
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    // 背景
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color(.systemGray5))
                        .frame(height: 6)
                    
                    // 进度
                    RoundedRectangle(cornerRadius: 3)
                        .fill(progressColor)
                        .frame(width: geometry.size.width * CGFloat(task.progress) / 100, height: 6)
                }
            }
            .frame(height: 6)
            
            // 进度文字
            HStack {
                Text(task.executionStatusText)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                Text("\(task.progress)%")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(progressColor)
            }
        }
    }
    
    private var progressColor: Color {
        switch task.status {
        case .running:
            return .blue
        case .completed:
            return .green
        case .failed:
            return .red
        case .paused:
            return .orange
        default:
            return .gray
        }
    }
    
    // MARK: - 转化数据
    
    private var conversionStatsSection: some View {
        HStack(spacing: 16) {
            // 触达
            VStack(spacing: 2) {
                Text("\(task.totalReachedCount)")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                
                Text("触达")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Divider()
                .frame(height: 24)
            
            // 回复
            VStack(spacing: 2) {
                Text("\(task.repliedCount)")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.orange)
                
                Text("回复")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Divider()
                .frame(height: 24)
            
            // 转化
            VStack(spacing: 2) {
                Text("\(task.convertedCount)")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.green)
                
                Text("转化")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Divider()
                .frame(height: 24)
            
            // 回复率
            VStack(spacing: 2) {
                Text(String(format: "%.1f%%", task.replyRate))
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.blue)
                
                Text("回复率")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.vertical, 4)
    }
    
    // MARK: - 底部
    
    private var footerSection: some View {
        HStack {
            Spacer()
            
            // 操作按钮
            actionButtons
        }
    }
    
    @ViewBuilder
    private var actionButtons: some View {
        HStack(spacing: 8) {
            switch task.status {
            case .pending:
                Button("开始") {
                    Task { await viewModel.startTask(task) }
                }
                .buttonStyle(SmallActionButtonStyle(color: .green))
                
            case .running:
                HStack(spacing: 8) {
                    Button("暂停") {
                        Task { await viewModel.pauseTask(task) }
                    }
                    .buttonStyle(SmallActionButtonStyle(color: .orange))
                    
                    Button("停止") {
                        Task { await viewModel.stopTask(task) }
                    }
                    .buttonStyle(SmallActionButtonStyle(color: .red))
                }
                
            case .paused:
                HStack(spacing: 8) {
                    Button("恢复") {
                        Task { await viewModel.resumeTask(task) }
                    }
                    .buttonStyle(SmallActionButtonStyle(color: .green))
                    
                    Button("停止") {
                        Task { await viewModel.stopTask(task) }
                    }
                    .buttonStyle(SmallActionButtonStyle(color: .red))
                }
                
            case .completed, .failed, .cancelled:
                if task.status == .failed {
                    Button("重试") {
                        Task { await viewModel.startTask(task) }
                    }
                    .buttonStyle(SmallActionButtonStyle(color: .green))
                }
            }
        }
    }
    
    // MARK: - 右键菜单
    
    private var taskContextMenu: some View {
        Group {
            Button(action: {
                Task { await viewModel.loadTaskDetail(task) }
            }) {
                Label("查看详情", systemImage: "eye")
            }
            
            Divider()
            
            switch task.status {
            case .pending:
                Button(action: {
                    Task { await viewModel.startTask(task) }
                }) {
                    Label("开始任务", systemImage: "play")
                }
                
            case .running:
                Button(action: {
                    Task { await viewModel.pauseTask(task) }
                }) {
                    Label("暂停任务", systemImage: "pause")
                }
                
                Button(action: {
                    Task { await viewModel.stopTask(task) }
                }) {
                    Label("停止任务", systemImage: "stop")
                }
                
            case .paused:
                Button(action: {
                    Task { await viewModel.resumeTask(task) }
                }) {
                    Label("恢复任务", systemImage: "play")
                }
                
                Button(action: {
                    Task { await viewModel.stopTask(task) }
                }) {
                    Label("停止任务", systemImage: "stop")
                }
                
            default:
                EmptyView()
            }
            
            Divider()
            
            Button(role: .destructive, action: {
                Task { await viewModel.deleteTask(task) }
            }) {
                Label("删除任务", systemImage: "trash")
            }
        }
    }
    
    // MARK: - 操作表按钮
    
    private var actionSheetButtons: some View {
        Group {
            Button("查看详情") {
                Task { await viewModel.loadTaskDetail(task) }
            }
            
            switch task.status {
            case .pending:
                Button("开始任务") {
                    Task { await viewModel.startTask(task) }
                }
                
            case .running:
                Button("暂停任务") {
                    Task { await viewModel.pauseTask(task) }
                }
                
                Button("停止任务") {
                    Task { await viewModel.stopTask(task) }
                }
                
            case .paused:
                Button("恢复任务") {
                    Task { await viewModel.resumeTask(task) }
                }
                
                Button("停止任务") {
                    Task { await viewModel.stopTask(task) }
                }
                
            default:
                if task.status == .failed {
                    Button("重试") {
                        Task { await viewModel.startTask(task) }
                    }
                }
            }
            
            Button("取消", role: .cancel) {}
            
            Button("删除任务", role: .destructive) {
                Task { await viewModel.deleteTask(task) }
            }
        }
    }
}

// MARK: - 渠道标签

struct ChannelTag: View {
    let channel: AcquireChannel
    
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: platformIcon(for: channel.platform))
                .font(.caption2)
            
            if let accountName = channel.accountName {
                Text(accountName)
                    .font(.caption)
            } else {
                Text(channel.platform.name)
                    .font(.caption)
            }
        }
        .foregroundColor(.white)
        .padding(.horizontal, 8)
        .padding(.vertical, 3)
        .background(platformColor(for: channel.platform))
        .cornerRadius(4)
    }
    
    private func platformIcon(for platform: SourcePlatform) -> String {
        switch platform {
        case .douyin: return "music.note"
        case .xiaohongshu: return "book.fill"
        case .wechatChannels: return "video.fill"
        case .kuaishou: return "bolt.fill"
        case .wechatPublic: return "message.fill"
        default: return "globe"
        }
    }
    
    private func platformColor(for platform: SourcePlatform) -> Color {
        switch platform {
        case .douyin: return .pink
        case .xiaohongshu: return .red
        case .wechatChannels: return .green
        case .kuaishou: return .orange
        case .wechatPublic: return .green.opacity(0.8)
        default: return .blue
        }
    }
}

// MARK: - 获客任务详情视图

struct AcquireTaskDetailView: View {
    let task: AcquireTask
    @ObservedObject var viewModel: AcquireTaskViewModel
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // 基本信息卡片
                    basicInfoCard
                    
                    // 转化数据卡片
                    conversionDataCard
                    
                    // 渠道配置卡片
                    channelsConfigCard
                    
                    // 目标人群卡片
                    targetAudienceCard
                    
                    // 执行日志
                    executionLogsSection
                }
                .padding()
            }
            .navigationTitle("任务详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("关闭") {
                        dismiss()
                    }
                }
            }
        }
    }
    
    private var basicInfoCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(task.name)
                        .font(.headline)
                    
                    if let description = task.description {
                        Text(description)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                StatusBadge(status: task.status)
            }
            
            Divider()
            
            HStack {
                Label(task.acquireType.name, systemImage: task.acquireType.iconName)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                Text("创建于 \(task.createdAt.formatted(date: .abbreviated, time: .omitted))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private var conversionDataCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("转化数据")
                .font(.headline)
            
            HStack(spacing: 0) {
                ConversionStatItem(
                    title: "触达",
                    value: task.totalReachedCount,
                    color: .blue
                )
                
                ConversionStatItem(
                    title: "回复",
                    value: task.repliedCount,
                    color: .orange
                )
                
                ConversionStatItem(
                    title: "转化",
                    value: task.convertedCount,
                    color: .green
                )
                
                ConversionStatItem(
                    title: "回复率",
                    value: String(format: "%.1f%%", task.replyRate),
                    color: .purple,
                    isText: true
                )
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private var channelsConfigCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("获客渠道")
                .font(.headline)
            
            ForEach(task.channels) { channel in
                HStack {
                    ChannelTag(channel: channel)
                    
                    Spacer()
                    
                    if channel.isEnabled {
                        Text("已启用")
                            .font(.caption)
                            .foregroundColor(.green)
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private var targetAudienceCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("目标人群")
                .font(.headline)
            
            VStack(alignment: .leading, spacing: 8) {
                Label("性别: \(task.targetAudience.gender.name)", systemImage: "person.fill")
                    .font(.subheadline)
                
                Label("活跃度: \(task.targetAudience.activityLevel.name)", systemImage: "bolt.fill")
                    .font(.subheadline)
                
                if !task.targetAudience.interestTags.isEmpty {
                    FlowLayout(spacing: 6) {
                        ForEach(task.targetAudience.interestTags, id: \.self) { tag in
                            Text(tag)
                                .font(.caption)
                                .foregroundColor(.blue)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.blue.opacity(0.1))
                                .cornerRadius(4)
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private var executionLogsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("执行日志")
                .font(.headline)
            
            if viewModel.taskLogs.isEmpty {
                Text("暂无日志")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            } else {
                ForEach(viewModel.taskLogs.prefix(10)) { log in
                    LogItemView(log: log)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 转化统计项

struct ConversionStatItem: View {
    let title: String
    let value: Any
    let color: Color
    var isText: Bool = false
    
    var body: some View {
        VStack(spacing: 4) {
            if isText {
                Text(value as? String ?? "")
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(color)
            } else {
                Text("\(value as? Int ?? 0)")
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(color)
            }
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 日志项视图

struct LogItemView: View {
    let log: AcquireLog
    
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: log.level.iconName)
                .foregroundColor(logLevelColor)
                .font(.caption)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(log.message)
                    .font(.caption)
                    .foregroundColor(.primary)
                
                Text(log.createdAt.formatted(date: .omitted, time: .standard))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.vertical, 4)
    }
    
    private var logLevelColor: Color {
        switch log.level {
        case .info: return .blue
        case .warning: return .orange
        case .error: return .red
        case .debug: return .gray
        }
    }
}

// MARK: - 预览

#if DEBUG
struct AcquireTaskListView_Previews: PreviewProvider {
    static var previews: some View {
        AcquireTaskListView(viewModel: AcquireTaskViewModel.preview)
    }
}
#endif
