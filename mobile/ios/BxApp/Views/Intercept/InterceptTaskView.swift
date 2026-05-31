//
//  InterceptTaskView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  截客任务主视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 截客任务主视图
/// 
/// 作为截客任务模块的入口页面，展示任务列表和概览。
struct InterceptTaskView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = InterceptTaskViewModel()
    
    /// 选中的标签页
    @State private var selectedTab: InterceptTab = .list
    
    /// 搜索文本
    @State private var searchText = ""
    
    // MARK: - 标签页枚举
    
    enum InterceptTab: String, CaseIterable {
        case list = "任务列表"
        case statistics = "数据统计"
        
        var icon: String {
            switch self {
            case .list: return "list.bullet"
            case .statistics: return "chart.bar"
            }
        }
    }
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // 统计概览卡片
                statisticsOverview
                
                // 平台筛选
                platformFilterSection
                
                // 标签页切换
                tabSelector
                
                // 内容区域
                contentSection
            }
            .navigationTitle("截客任务")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        viewModel.openCreateSheet()
                    }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                    }
                }
            }
            .searchable(text: $searchText, prompt: "搜索任务名称、关键词")
            .onChange(of: searchText) { _, newValue in
                Task {
                    await viewModel.searchTasks(keyword: newValue)
                }
            }
            .sheet(isPresented: $viewModel.showCreateSheet) {
                CreateInterceptTaskView(viewModel: viewModel)
            }
            .sheet(isPresented: $viewModel.showTaskDetail) {
                if let task = viewModel.selectedTask {
                    TaskDetailView(
                        task: task,
                        viewModel: viewModel
                    )
                }
            }
            .alert("提示", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "操作失败")
            }
            .onAppear {
                if viewModel.tasks.isEmpty {
                    Task { await viewModel.loadTasks() }
                }
            }
        }
    }
    
    // MARK: - 组件 - 统计概览
    
    /// 统计概览卡片
    private var statisticsOverview: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                StatCard(
                    title: "总任务",
                    value: "\(viewModel.totalCount)",
                    icon: "number",
                    color: .blue
                )
                
                StatCard(
                    title: "执行中",
                    value: "\(viewModel.runningCount)",
                    icon: "play.fill",
                    color: .green
                )
                
                StatCard(
                    title: "今日截客",
                    value: "\(viewModel.todayInterceptCount)",
                    icon: "person.badge.plus",
                    color: .orange
                )
                
                StatCard(
                    title: "累计截客",
                    value: "\(viewModel.totalInterceptCount)",
                    icon: "person.3",
                    color: .purple
                )
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .background(Color(.systemBackground))
    }
    
    // MARK: - 组件 - 平台筛选
    
    /// 平台筛选区域
    private var platformFilterSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // 全部
                PlatformFilterChip(
                    platform: nil,
                    isSelected: viewModel.selectedPlatform == nil
                ) {
                    Task {
                        await viewModel.filterByPlatform(nil)
                    }
                }
                
                // 各平台
                ForEach(viewModel.supportedPlatforms, id: \.self) { platform in
                    PlatformFilterChip(
                        platform: platform,
                        isSelected: viewModel.selectedPlatform == platform
                    ) {
                        Task {
                            await viewModel.filterByPlatform(platform)
                        }
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .background(Color(.secondarySystemBackground))
    }
    
    // MARK: - 组件 - 标签页切换
    
    /// 标签页选择器
    private var tabSelector: some View {
        Picker("", selection: $selectedTab) {
            ForEach(InterceptTab.allCases, id: \.self) { tab in
                Label(tab.rawValue, systemImage: tab.icon)
                    .tag(tab)
            }
        }
        .pickerStyle(SegmentedPickerStyle())
        .padding()
    }
    
    // MARK: - 组件 - 内容区域
    
    /// 内容区域
    @ViewBuilder
    private var contentSection: some View {
        switch selectedTab {
        case .list:
            InterceptTaskListView(viewModel: viewModel)
        case .statistics:
            InterceptStatisticsView(viewModel: viewModel)
        }
    }
}

// MARK: - 统计卡片

struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Spacer()
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(width: 100)
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 平台筛选芯片

struct PlatformFilterChip: View {
    let platform: SourcePlatform?
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if let platform = platform {
                    Image(systemName: platformIcon(for: platform))
                        .font(.caption)
                }
                
                Text(platform?.name ?? "全部")
                    .font(.subheadline)
            }
            .foregroundColor(isSelected ? .white : .primary)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(isSelected ? Color.blue : Color(.systemBackground))
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? Color.blue : Color.gray.opacity(0.3), lineWidth: 1)
            )
        }
    }
    
    private func platformIcon(for platform: SourcePlatform) -> String {
        switch platform {
        case .douyin:
            return "music.note"
        case .xiaohongshu:
            return "book.fill"
        case .wechatChannels:
            return "video.fill"
        case .kuaishou:
            return "bolt.fill"
        default:
            return "globe"
        }
    }
}

// MARK: - 统计视图（占位）

struct InterceptStatisticsView: View {
    @ObservedObject var viewModel: InterceptTaskViewModel
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // 平台分布
                platformDistributionSection
                
                // 趋势图表
                trendChartSection
                
                // 关键词效果
                keywordPerformanceSection
            }
            .padding()
        }
    }
    
    private var platformDistributionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("平台分布")
                .font(.headline)
            
            if let stats = viewModel.statistics?.platformStats {
                ForEach(stats) { stat in
                    HStack {
                        Text(stat.platform.name)
                            .font(.subheadline)
                        
                        Spacer()
                        
                        Text("\(stat.interceptedCount) 人")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    
                    ProgressView(value: Double(stat.interceptedCount), total: Double(viewModel.totalInterceptCount))
                        .tint(Color.blue)
                }
            } else {
                Text("暂无数据")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private var trendChartSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("近7天趋势")
                .font(.headline)
            
            // 简化版趋势展示
            HStack(spacing: 8) {
                ForEach(0..<7) { index in
                    VStack {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.blue.opacity(0.7))
                            .frame(height: CGFloat.random(in: 50...150))
                        
                        Text("\(index + 1)")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private var keywordPerformanceSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("关键词效果")
                .font(.headline)
            
            if let stats = viewModel.statistics?.keywordStats {
                ForEach(stats.prefix(5)) { stat in
                    HStack {
                        Text(stat.keyword)
                            .font(.subheadline)
                        
                        Spacer()
                        
                        VStack(alignment: .trailing, spacing: 2) {
                            Text("\(stat.interceptedCount) 人")
                                .font(.caption)
                            Text(String(format: "转化率 %.1f%%", stat.conversionRate))
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            } else {
                Text("暂无数据")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 预览

#if DEBUG
struct InterceptTaskView_Previews: PreviewProvider {
    static var previews: some View {
        InterceptTaskView()
    }
}
#endif
