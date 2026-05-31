//
//  InterceptTaskListView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  截客任务列表视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 截客任务列表视图
/// 
/// 展示截客任务列表，支持刷新、加载更多、状态筛选等功能。
struct InterceptTaskListView: View {
    
    // MARK: - 属性
    
    @ObservedObject var viewModel: InterceptTaskViewModel
    
    // MARK: - 界面
    
    var body: some View {
        ZStack {
            if viewModel.isLoading && viewModel.tasks.isEmpty {
                LoadingStateView(message: "加载任务中...")
            } else if viewModel.tasks.isEmpty {
                EmptyStateView(
                    icon: "target",
                    title: "暂无截客任务",
                    message: "创建您的第一个截客任务，开始自动获取潜在客户",
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
                InterceptTaskCard(task: task, viewModel: viewModel)
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

// MARK: - 截客任务卡片

struct InterceptTaskCard: View {
    let task: InterceptTask
    @ObservedObject var viewModel: InterceptTaskViewModel
    @State private var showActionSheet = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // 头部：平台图标 + 状态 + 操作
            headerSection
            
            // 任务名称和描述
            contentSection
            
            // 关键词展示
            keywordsSection
            
            // 进度和统计
            progressSection
            
            // 底部信息和操作按钮
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
            // 平台图标和名称
            HStack(spacing: 6) {
                platformIcon
                    .font(.title3)
                
                Text(task.platform.name)
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
    
    private var platformIcon: some View {
        Group {
            switch task.platform {
            case .douyin:
                Image(systemName: "music.note")
                    .foregroundColor(.pink)
            case .xiaohongshu:
                Image(systemName: "book.fill")
                    .foregroundColor(.red)
            case .wechatChannels:
                Image(systemName: "video.fill")
                    .foregroundColor(.green)
            case .kuaishou:
                Image(systemName: "bolt.fill")
                    .foregroundColor(.orange)
            default:
                Image(systemName: "globe")
                    .foregroundColor(.blue)
            }
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
    
    // MARK: - 关键词
    
    private var keywordsSection: some View {
        FlowLayout(spacing: 6) {
            ForEach(task.keywords.prefix(5), id: \.self) { keyword in
                KeywordTag(text: keyword)
            }
            
            if task.keywords.count > 5 {
                Text("+\(task.keywords.count - 5)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color(.systemGray5))
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
    
    // MARK: - 底部
    
    private var footerSection: some View {
        HStack {
            // 截客统计
            HStack(spacing: 12) {
                Label("\(task.interceptedCount)/\(task.maxInterceptCount)", systemImage: "person.badge.plus")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                if task.autoReachOut {
                    Label("自动触达", systemImage: "paperplane.fill")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
            }
            
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
                    
                    if task.interceptedCount > 0 {
                        Button("触达") {
                            Task { await viewModel.reachOutAll(task) }
                        }
                        .buttonStyle(SmallActionButtonStyle(color: .blue))
                    }
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
                if task.interceptedCount > 0 && !task.autoReachOut {
                    Button("一键触达") {
                        Task { await viewModel.reachOutAll(task) }
                    }
                    .buttonStyle(SmallActionButtonStyle(color: .blue))
                }
                
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
                
                if task.interceptedCount > 0 {
                    Button("一键触达") {
                        Task { await viewModel.reachOutAll(task) }
                    }
                }
                
            case .paused:
                Button("恢复任务") {
                    Task { await viewModel.resumeTask(task) }
                }
                
                Button("停止任务") {
                    Task { await viewModel.stopTask(task) }
                }
                
            default:
                if task.interceptedCount > 0 && !task.autoReachOut {
                    Button("一键触达") {
                        Task { await viewModel.reachOutAll(task) }
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

// MARK: - 关键词标签

struct KeywordTag: View {
    let text: String
    
    var body: some View {
        Text(text)
            .font(.caption)
            .foregroundColor(.blue)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Color.blue.opacity(0.1))
            .cornerRadius(4)
    }
}

// MARK: - 状态徽章

struct StatusBadge: View {
    let status: TaskStatus
    
    var body: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(statusColor)
                .frame(width: 6, height: 6)
            
            Text(status.name)
                .font(.caption)
                .fontWeight(.medium)
        }
        .foregroundColor(statusColor)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(statusColor.opacity(0.1))
        .cornerRadius(12)
    }
    
    private var statusColor: Color {
        switch status {
        case .pending: return .gray
        case .running: return .blue
        case .paused: return .orange
        case .completed: return .green
        case .failed: return .red
        case .cancelled: return .gray
        }
    }
}

// MARK: - 小型操作按钮样式

struct SmallActionButtonStyle: ButtonStyle {
    let color: Color
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.caption)
            .fontWeight(.medium)
            .foregroundColor(color)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(color.opacity(0.1))
            .cornerRadius(6)
            .scaleEffect(configuration.isPressed ? 0.95 : 1)
    }
}

// MARK: - 流式布局

struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(in: proposal.replacingUnspecifiedDimensions().width, subviews: subviews, spacing: spacing)
        return result.size
    }
    
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(in: bounds.width, subviews: subviews, spacing: spacing)
        for (index, subview) in subviews.enumerated() {
            subview.place(at: CGPoint(x: bounds.minX + result.positions[index].x, y: bounds.minY + result.positions[index].y), proposal: .unspecified)
        }
    }
    
    struct FlowResult {
        var size: CGSize = .zero
        var positions: [CGPoint] = []
        
        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var x: CGFloat = 0
            var y: CGFloat = 0
            var rowHeight: CGFloat = 0
            
            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)
                
                if x + size.width > maxWidth && x > 0 {
                    x = 0
                    y += rowHeight + spacing
                    rowHeight = 0
                }
                
                positions.append(CGPoint(x: x, y: y))
                rowHeight = max(rowHeight, size.height)
                x += size.width + spacing
                
                self.size.width = max(self.size.width, x)
            }
            
            self.size.height = y + rowHeight
        }
    }
}

// MARK: - 加载状态视图

struct LoadingStateView: View {
    let message: String
    
    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - 空状态视图

struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    let buttonTitle: String
    let action: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text(title)
                .font(.headline)
                .foregroundColor(.primary)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            Button(action: action) {
                Text(buttonTitle)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.blue)
                    .cornerRadius(8)
            }
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - 预览

#if DEBUG
struct InterceptTaskListView_Previews: PreviewProvider {
    static var previews: some View {
        InterceptTaskListView(viewModel: InterceptTaskViewModel.preview)
    }
}
#endif
