//
//  TaskDetailView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  任务详情视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 任务详情视图（通用）
/// 
/// 展示任务的详细信息、执行日志、数据统计和操作按钮。
struct TaskDetailView: View {
    
    // MARK: - 属性
    
    let task: InterceptTask
    @ObservedObject var viewModel: InterceptTaskViewModel
    @Environment(\.dismiss) private var dismiss
    
    /// 选中的标签页
    @State private var selectedTab: DetailTab = .overview
    
    // MARK: - 标签页枚举
    
    enum DetailTab: String, CaseIterable {
        case overview = "概览"
        case logs = "日志"
        case leads = "商机"
        
        var icon: String {
            switch self {
            case .overview: return "chart.pie"
            case .logs: return "doc.text"
            case .leads: return "person.2"
            }
        }
    }
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // 任务头部信息
                taskHeader
                
                // 标签页切换
                tabSelector
                
                // 内容区域
                contentSection
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
    
    // MARK: - 组件 - 任务头部
    
    private var taskHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            // 平台与状态
            HStack {
                HStack(spacing: 6) {
                    platformIcon
                        .font(.title3)
                    
                    Text(task.platform.name)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                StatusBadge(status: task.status)
            }
            
            // 任务名称
            Text(task.name)
                .font(.title2)
                .fontWeight(.bold)
            
            // 描述
            if let description = task.description, !description.isEmpty {
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
            
            Divider()
            
            // 进度信息
            VStack(spacing: 8) {
                HStack {
                    Text(task.executionStatusText)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Text("\(task.progress)%")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(progressColor)
                }
                
                // 进度条
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color(.systemGray5))
                            .frame(height: 8)
                        
                        RoundedRectangle(cornerRadius: 4)
                            .fill(progressColor)
                            .frame(width: geometry.size.width * CGFloat(task.progress) / 100, height: 8)
                    }
                }
                .frame(height: 8)
            }
            
            // 操作按钮
            actionButtons
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .padding()
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
    
    private var progressColor: Color {
        switch task.status {
        case .running: return .blue
        case .completed: return .green
        case .failed: return .red
        case .paused: return .orange
        default: return .gray
        }
    }
    
    // MARK: - 组件 - 操作按钮
    
    private var actionButtons: some View {
        HStack(spacing: 12) {
            switch task.status {
            case .pending:
                ActionButton(
                    title: "开始任务",
                    icon: "play.fill",
                    color: .green
                ) {
                    Task { await viewModel.startTask(task) }
                }
                
            case .running:
                HStack(spacing: 12) {
                    ActionButton(
                        title: "暂停",
                        icon: "pause.fill",
                        color: .orange
                    ) {
                        Task { await viewModel.pauseTask(task) }
                    }
                    
                    ActionButton(
                        title: "停止",
                        icon: "stop.fill",
                        color: .red
                    ) {
                        Task { await viewModel.stopTask(task) }
                    }
                    
                    if task.interceptedCount > 0 {
                        ActionButton(
                            title: "一键触达",
                            icon: "paperplane.fill",
                            color: .blue
                        ) {
                            Task { await viewModel.reachOutAll(task) }
                        }
                    }
                }
                
            case .paused:
                HStack(spacing: 12) {
                    ActionButton(
                        title: "恢复",
                        icon: "play.fill",
                        color: .green
                    ) {
                        Task { await viewModel.resumeTask(task) }
                    }
                    
                    ActionButton(
                        title: "停止",
                        icon: "stop.fill",
                        color: .red
                    ) {
                        Task { await viewModel.stopTask(task) }
                    }
                }
                
            case .completed, .failed, .cancelled:
                if task.interceptedCount > 0 && !task.autoReachOut {
                    ActionButton(
                        title: "一键触达",
                        icon: "paperplane.fill",
                        color: .blue
                    ) {
                        Task { await viewModel.reachOutAll(task) }
                    }
                }
                
                if task.status == .failed {
                    ActionButton(
                        title: "重试",
                        icon: "arrow.clockwise",
                        color: .green
                    ) {
                        Task { await viewModel.startTask(task) }
                    }
                }
            }
        }
        .padding(.top, 8)
    }
    
    // MARK: - 组件 - 标签页选择器
    
    private var tabSelector: some View {
        Picker("", selection: $selectedTab) {
            ForEach(DetailTab.allCases, id: \.self) { tab in
                Label(tab.rawValue, systemImage: tab.icon)
                    .tag(tab)
            }
        }
        .pickerStyle(SegmentedPickerStyle())
        .padding(.horizontal)
    }
    
    // MARK: - 组件 - 内容区域
    
    @ViewBuilder
    private var contentSection: some View {
        switch selectedTab {
        case .overview:
            overviewTab
        case .logs:
            logsTab
        case .leads:
            leadsTab
        }
    }
    
    // MARK: - 概览标签页
    
    private var overviewTab: some View {
        ScrollView {
            VStack(spacing: 16) {
                // 关键指标卡片
                metricsCard
                
                // 关键词卡片
                keywordsCard
                
                // 执行配置卡片
                configCard
                
                // 触达配置卡片
                reachOutConfigCard
            }
            .padding()
        }
    }
    
    private var metricsCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("关键指标")
                .font(.headline)
            
            HStack(spacing: 0) {
                MetricItem(
                    title: "搜索次数",
                    value: "\(task.currentSearchCount)/\(task.maxSearchCount)",
                    icon: "magnifyingglass",
                    color: .blue
                )
                
                Divider()
                
                MetricItem(
                    title: "截客数量",
                    value: "\(task.interceptedCount)/\(task.maxInterceptCount)",
                    icon: "person.badge.plus",
                    color: .green
                )
            }
            
            if let result = task.result, !result.isEmpty {
                Divider()
                
                HStack {
                    Image(systemName: "checkmark.seal.fill")
                        .foregroundColor(.green)
                    
                    Text(result)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private var keywordsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("关键词配置")
                    .font(.headline)
                
                Spacer()
                
                Text(task.keywordMatchMode.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(Color(.systemGray5))
                    .cornerRadius(4)
            }
            
            FlowLayout(spacing: 8) {
                ForEach(task.keywords, id: \.self) { keyword in
                    Text(keyword)
                        .font(.subheadline)
                        .foregroundColor(.blue)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color.blue.opacity(0.1))
                        .cornerRadius(16)
                }
            }
            
            if !task.excludeKeywords.isEmpty {
                Divider()
                
                Text("排除关键词")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                FlowLayout(spacing: 8) {
                    ForEach(task.excludeKeywords, id: \.self) { keyword in
                        Text(keyword)
                            .font(.subheadline)
                            .foregroundColor(.orange)
                            .strikethrough()
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Color.orange.opacity(0.1))
                            .cornerRadius(16)
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private var configCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("执行配置")
                .font(.headline)
            
            VStack(alignment: .leading, spacing: 8) {
                ConfigItem(label: "搜索范围", value: "\(task.searchTimeRange)小时内")
                ConfigItem(label: "执行间隔", value: "\(task.executionInterval)秒")
                ConfigItem(label: "排序方式", value: task.sortBy.name)
                
                if let minLikes = task.minLikes {
                    ConfigItem(label: "最小点赞", value: "\(minLikes)")
                }
                
                if let minComments = task.minComments {
                    ConfigItem(label: "最小评论", value: "\(minComments)")
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private var reachOutConfigCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("触达配置")
                    .font(.headline)
                
                Spacer()
                
                if task.autoReachOut {
                    Label("已启用", systemImage: "checkmark.circle.fill")
                        .font(.caption)
                        .foregroundColor(.green)
                } else {
                    Label("未启用", systemImage: "xmark.circle.fill")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
            }
            
            if task.autoReachOut && !task.reachOutMethods.isEmpty {
                FlowLayout(spacing: 8) {
                    ForEach(task.reachOutMethods, id: \.self) { method in
                        HStack(spacing: 4) {
                            Image(systemName: method.iconName)
                                .font(.caption)
                            Text(method.name)
                                .font(.caption)
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color.blue)
                        .cornerRadius(16)
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 日志标签页
    
    private var logsTab: some View {
        ScrollView {
            VStack(spacing: 12) {
                if viewModel.taskLogs.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "doc.text")
                            .font(.system(size: 50))
                            .foregroundColor(.secondary)
                        
                        Text("暂无日志")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, minHeight: 200)
                } else {
                    ForEach(viewModel.taskLogs) { log in
                        LogEntryCard(log: log)
                    }
                }
            }
            .padding()
        }
    }
    
    // MARK: - 商机标签页
    
    private var leadsTab: some View {
        ScrollView {
            VStack(spacing: 12) {
                if task.interceptedLeadIds.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "person.2.slash")
                            .font(.system(size: 50))
                            .foregroundColor(.secondary)
                        
                        Text("暂无商机")
                            .font(.headline)
                            .foregroundColor(.secondary)
                        
                        Text("任务执行后将自动获取商机")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, minHeight: 200)
                } else {
                    // 商机统计
                    HStack {
                        Text("共 \(task.interceptedLeadIds.count) 位商机")
                            .font(.headline)
                        
                        Spacer()
                        
                        if !task.autoReachOut {
                            Button("全部触达") {
                                Task { await viewModel.reachOutAll(task) }
                            }
                            .font(.subheadline)
                        }
                    }
                    .padding(.horizontal)
                    
                    // 商机列表预览
                    if let leads = task.interceptedLeads {
                        ForEach(leads.prefix(5)) { lead in
                            LeadPreviewCard(lead: lead)
                        }
                        
                        if leads.count > 5 {
                            Button("查看全部 \(leads.count) 位商机") {
                                print("[TaskDetailView] 跳转到商机列表")
                            }
                            .font(.subheadline)
                            .foregroundColor(.blue)
                            .padding()
                        }
                    } else {
                        ForEach(task.interceptedLeadIds.prefix(5), id: \.self) { leadId in
                            LeadIdRow(leadId: leadId)
                        }
                    }
                }
            }
            .padding(.vertical)
        }
    }
}

// MARK: - 操作按钮组件

struct ActionButton: View {
    let title: String
    let icon: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                Text(title)
            }
            .font(.subheadline)
            .fontWeight(.medium)
            .foregroundColor(.white)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(color)
            .cornerRadius(8)
        }
    }
}

// MARK: - 指标项

struct MetricItem: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }
}

// MARK: - 配置项

struct ConfigItem: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.subheadline)
                .foregroundColor(.primary)
        }
    }
}

// MARK: - 日志条目卡片

struct LogEntryCard: View {
    let log: InterceptLog
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // 时间线
            VStack(spacing: 0) {
                Circle()
                    .fill(logLevelColor)
                    .frame(width: 10, height: 10)
                
                Rectangle()
                    .fill(Color(.systemGray5))
                    .frame(width: 2)
            }
            
            // 内容
            VStack(alignment: .leading, spacing: 4) {
                Text(log.message)
                    .font(.subheadline)
                    .foregroundColor(.primary)
                
                if let username = log.username {
                    Text("@\(username)")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                
                Text(log.createdAt.formatted(date: .omitted, time: .standard))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(8)
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

// MARK: - 商机预览卡片

struct LeadPreviewCard: View {
    let lead: Lead
    
    var body: some View {
        HStack(spacing: 12) {
            // 头像占位
            Circle()
                .fill(Color.blue.opacity(0.2))
                .frame(width: 48, height: 48)
                .overlay(
                    Text(String(lead.customerName.prefix(1)))
                        .font(.title3)
                        .fontWeight(.medium)
                        .foregroundColor(.blue)
                )
            
            VStack(alignment: .leading, spacing: 4) {
                Text(lead.customerName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                if let company = lead.customerCompany {
                    Text(company)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                HStack(spacing: 8) {
                    IntentionBadge(level: lead.intentionLevel)
                    
                    if let keyword = lead.sourceKeyword {
                        Text(keyword)
                            .font(.caption2)
                            .foregroundColor(.blue)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(4)
                    }
                }
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .foregroundColor(.secondary)
                .font(.caption)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 意向等级徽章

struct IntentionBadge: View {
    let level: IntentionLevel
    
    var body: some View {
        Text(level.name)
            .font(.caption2)
            .fontWeight(.medium)
            .foregroundColor(.white)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(intentionColor)
            .cornerRadius(4)
    }
    
    private var intentionColor: Color {
        switch level {
        case .high: return .red
        case .medium: return .orange
        case .low: return .blue
        case .none: return .gray
        }
    }
}

// MARK: - 商机ID行

struct LeadIdRow: View {
    let leadId: String
    
    var body: some View {
        HStack {
            Circle()
                .fill(Color.gray.opacity(0.2))
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: "person.fill")
                        .foregroundColor(.gray)
                )
            
            Text("商机 \(leadId.prefix(8))...")
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Spacer()
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 预览

#if DEBUG
struct TaskDetailView_Previews: PreviewProvider {
    static var previews: some View {
        TaskDetailView(
            task: InterceptTask.preview,
            viewModel: InterceptTaskViewModel.preview
        )
    }
}
#endif
