//
//  AcquireTaskView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  获客任务主视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 获客任务主视图
/// 
/// 作为获客任务模块的入口页面，展示任务列表和概览。
struct AcquireTaskView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = AcquireTaskViewModel()
    
    /// 选中的标签页
    @State private var selectedTab: AcquireTab = .list
    
    /// 搜索文本
    @State private var searchText = ""
    
    // MARK: - 标签页枚举
    
    enum AcquireTab: String, CaseIterable {
        case list = "任务列表"
        case statistics = "效果分析"
        
        var icon: String {
            switch self {
            case .list: return "list.bullet"
            case .statistics: return "chart.pie"
            }
        }
    }
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // 统计概览卡片
                statisticsOverview
                
                // 获客类型筛选
                typeFilterSection
                
                // 标签页切换
                tabSelector
                
                // 内容区域
                contentSection
            }
            .navigationTitle("获客任务")
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
            .searchable(text: $searchText, prompt: "搜索任务名称")
            .onChange(of: searchText) { _, newValue in
                Task {
                    await viewModel.searchTasks(keyword: newValue)
                }
            }
            .sheet(isPresented: $viewModel.showCreateSheet) {
                CreateAcquireTaskView(viewModel: viewModel)
            }
            .sheet(isPresented: $viewModel.showTaskDetail) {
                if let task = viewModel.selectedTask {
                    AcquireTaskDetailView(
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
                    title: "今日触达",
                    value: "\(viewModel.todayReachedCount)",
                    icon: "paperplane.fill",
                    color: .orange
                )
                
                StatCard(
                    title: "转化率",
                    value: String(format: "%.1f%%", viewModel.totalConversionRate),
                    icon: "percent",
                    color: .purple
                )
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .background(Color(.systemBackground))
    }
    
    // MARK: - 组件 - 类型筛选
    
    /// 获客类型筛选区域
    private var typeFilterSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // 全部
                TypeFilterChip(
                    type: nil,
                    isSelected: true
                ) {
                    Task {
                        await viewModel.filterByType(nil)
                    }
                }
                
                // 各类型
                ForEach(AcquireType.allCases, id: \.self) { type in
                    TypeFilterChip(
                        type: type,
                        isSelected: false
                    ) {
                        Task {
                            await viewModel.filterByType(type)
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
            ForEach(AcquireTab.allCases, id: \.self) { tab in
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
            AcquireTaskListView(viewModel: viewModel)
        case .statistics:
            AcquireStatisticsView(viewModel: viewModel)
        }
    }
}

// MARK: - 类型筛选芯片

struct TypeFilterChip: View {
    let type: AcquireType?
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if let type = type {
                    Image(systemName: type.iconName)
                        .font(.caption)
                }
                
                Text(type?.name ?? "全部")
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
}

// MARK: - 统计视图（占位）

struct AcquireStatisticsView: View {
    @ObservedObject var viewModel: AcquireTaskViewModel
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // 转化率概览
                conversionOverviewSection
                
                // 渠道效果
                channelPerformanceSection
                
                // 趋势图表
                trendChartSection
            }
            .padding()
        }
    }
    
    private var conversionOverviewSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("转化漏斗")
                .font(.headline)
            
            VStack(spacing: 12) {
                funnelItem(
                    title: "触达用户",
                    count: viewModel.statistics?.totalReachedCount ?? 0,
                    percentage: 100,
                    color: .blue
                )
                
                funnelItem(
                    title: "用户回复",
                    count: viewModel.statistics?.totalRepliedCount ?? 0,
                    percentage: viewModel.statistics.map { Double($0.totalRepliedCount) / Double($0.totalReachedCount) * 100 } ?? 0,
                    color: .orange
                )
                
                funnelItem(
                    title: "成功转化",
                    count: viewModel.statistics?.totalConvertedCount ?? 0,
                    percentage: viewModel.totalConversionRate,
                    color: .green
                )
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    private func funnelItem(title: String, count: Int, percentage: Double, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title)
                    .font(.subheadline)
                
                Spacer()
                
                Text("\(count) 人")
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(String(format: "%.1f%%", percentage))
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .frame(width: 50, alignment: .trailing)
            }
            
            ProgressView(value: percentage, total: 100)
                .tint(color)
        }
    }
    
    private var channelPerformanceSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("渠道效果")
                .font(.headline)
            
            if let stats = viewModel.statistics?.platformStats {
                ForEach(stats) { stat in
                    HStack {
                        Text(stat.platform.name)
                            .font(.subheadline)
                        
                        Spacer()
                        
                        Text("\(stat.interceptedCount) 转化")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    ProgressView(value: Double(stat.interceptedCount), total: Double(viewModel.statistics?.totalConvertedCount ?? 1))
                        .tint(Color.purple)
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
                            .fill(Color.green.opacity(0.7))
                            .frame(height: CGFloat.random(in: 30...120))
                        
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
}

// MARK: - 预览

#if DEBUG
struct AcquireTaskView_Previews: PreviewProvider {
    static var previews: some View {
        AcquireTaskView()
    }
}
#endif
