//
//  LeadListView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  商机列表视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 商机列表视图
/// 
/// 展示商机列表，支持筛选、搜索、刷新等功能。
struct LeadListView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = LeadListViewModel()
    
    /// 搜索文本
    @State private var searchText = ""
    
    /// 是否显示筛选
    @State private var showFilter = false
    
    /// 是否显示创建商机
    @State private var showCreate = false
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                // 主内容
                if viewModel.isLoading && viewModel.leads.isEmpty {
                    LoadingView(message: "加载商机...")
                } else if viewModel.leads.isEmpty {
                    EmptyStateView(
                        icon: "star",
                        title: "暂无商机",
                        message: viewModel.hasActiveFilter ? "没有符合筛选条件的商机" : "开始添加您的第一个商机",
                        actionTitle: viewModel.hasActiveFilter ? "清除筛选" : "添加商机"
                    ) {
                        if viewModel.hasActiveFilter {
                            Task { await viewModel.clearFilter() }
                        } else {
                            showCreate = true
                        }
                    }
                } else {
                    leadList
                }
            }
            .navigationTitle("商机")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                // 搜索按钮
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showFilter = true }) {
                        Image(systemName: viewModel.hasActiveFilter ? 
                            "line.3.horizontal.decrease.circle.fill" : 
                            "line.3.horizontal.decrease.circle")
                    }
                }
                
                // 添加按钮
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showCreate = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .searchable(text: $searchText, prompt: "搜索商机")
            .onChange(of: searchText) { _, newValue in
                Task {
                    await viewModel.searchLeads(keyword: newValue)
                }
            }
            .refreshable {
                await viewModel.refreshLeads()
            }
            .sheet(isPresented: $showFilter) {
                LeadFilterSheet(viewModel: viewModel)
            }
            .sheet(isPresented: $showCreate) {
                CreateLeadView()
            }
            .sheet(isPresented: $viewModel.showLeadDetail) {
                if let lead = viewModel.selectedLead {
                    LeadDetailView(lead: lead)
                }
            }
            .alert("提示", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "操作失败")
            }
        }
        .onAppear {
            if viewModel.leads.isEmpty {
                Task { await viewModel.loadLeads() }
            }
        }
    }
    
    // MARK: - 组件 - 商机列表
    
    /// 商机列表
    private var leadList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                // 统计信息
                if let stats = viewModel.statistics {
                    statisticsBar(stats)
                }
                
                // 列表
                ForEach(viewModel.leads) { lead in
                    LeadCard(lead: lead)
                        .onTapGesture {
                            viewModel.selectLead(lead)
                        }
                        .onAppear {
                            // 加载更多
                            if lead.id == viewModel.leads.last?.id {
                                Task { await viewModel.loadMoreLeads() }
                            }
                        }
                        .contextMenu {
                            leadContextMenu(lead)
                        }
                }
                
                // 加载更多指示器
                if viewModel.isLoadingMore {
                    ProgressView()
                        .padding()
                }
            }
            .padding()
        }
    }
    
    // MARK: - 组件 - 统计条
    
    /// 统计条
    private func statisticsBar(_ stats: LeadStatistics) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                StatBadge(
                    title: "全部",
                    count: stats.totalCount,
                    color: .blue
                )
                
                StatBadge(
                    title: "高意向",
                    count: stats.highIntentionCount,
                    color: .red
                )
                
                StatBadge(
                    title: "中意向",
                    count: stats.mediumIntentionCount,
                    color: .orange
                )
                
                StatBadge(
                    title: "低意向",
                    count: stats.lowIntentionCount,
                    color: .gray
                )
            }
        }
    }
    
    // MARK: - 组件 - 右键菜单
    
    /// 商机右键菜单
    private func leadContextMenu(_ lead: Lead) -> some View {
        Group {
            Button(action: { viewModel.selectLead(lead) }) {
                Label("查看详情", systemImage: "eye")
            }
            
            Divider()
            
            // 状态操作
            Menu("更新状态") {
                ForEach(FollowUpStatus.allCases, id: \.self) { status in
                    Button(status.name) {
                        Task { await viewModel.updateLeadStatus(lead, status: status) }
                    }
                }
            }
            
            // 意向操作
            Menu("更新意向") {
                ForEach(IntentionLevel.allCases, id: \.self) { level in
                    Button(level.name) {
                        Task { await viewModel.updateLeadIntention(lead, level: level) }
                    }
                }
            }
            
            Divider()
            
            Button(role: .destructive, action: {
                Task { await viewModel.deleteLead(lead) }
            }) {
                Label("删除", systemImage: "trash")
            }
        }
    }
}

// MARK: - 统计徽章

struct StatBadge: View {
    let title: String
    let count: Int
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text("\(count)")
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(color)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(color.opacity(0.1))
        .cornerRadius(8)
    }
}

// MARK: - 商机筛选面板

struct LeadFilterSheet: View {
    @ObservedObject var viewModel: LeadListViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var selectedPlatforms: Set<SourcePlatform> = []
    @State private var selectedLevels: Set<IntentionLevel> = []
    @State private var selectedStatuses: Set<FollowUpStatus> = []
    
    var body: some View {
        NavigationView {
            Form {
                // 来源平台
                Section("来源平台") {
                    ForEach(SourcePlatform.allCases, id: \.self) { platform in
                        Toggle(platform.name, isOn: Binding(
                            get: { selectedPlatforms.contains(platform) },
                            set: { isSelected in
                                if isSelected {
                                    selectedPlatforms.insert(platform)
                                } else {
                                    selectedPlatforms.remove(platform)
                                }
                            }
                        ))
                    }
                }
                
                // 意向等级
                Section("意向等级") {
                    ForEach(IntentionLevel.allCases, id: \.self) { level in
                        Toggle(level.name, isOn: Binding(
                            get: { selectedLevels.contains(level) },
                            set: { isSelected in
                                if isSelected {
                                    selectedLevels.insert(level)
                                } else {
                                    selectedLevels.remove(level)
                                }
                            }
                        ))
                    }
                }
                
                // 跟进状态
                Section("跟进状态") {
                    ForEach(FollowUpStatus.allCases, id: \.self) { status in
                        Toggle(status.name, isOn: Binding(
                            get: { selectedStatuses.contains(status) },
                            set: { isSelected in
                                if isSelected {
                                    selectedStatuses.insert(status)
                                } else {
                                    selectedStatuses.remove(status)
                                }
                            }
                        ))
                    }
                }
                
                // 操作按钮
                Section {
                    Button("清除筛选") {
                        clearFilter()
                    }
                    .foregroundColor(.red)
                }
            }
            .navigationTitle("筛选")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") { dismiss() }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("应用") {
                        applyFilter()
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
        .onAppear {
            loadCurrentFilter()
        }
    }
    
    private func loadCurrentFilter() {
        selectedPlatforms = Set(viewModel.filter.sourcePlatforms ?? [])
        selectedLevels = Set(viewModel.filter.intentionLevels ?? [])
        selectedStatuses = Set(viewModel.filter.followUpStatuses ?? [])
    }
    
    private func clearFilter() {
        selectedPlatforms.removeAll()
        selectedLevels.removeAll()
        selectedStatuses.removeAll()
    }
    
    private func applyFilter() {
        var newFilter = viewModel.filter
        newFilter.sourcePlatforms = selectedPlatforms.isEmpty ? nil : Array(selectedPlatforms)
        newFilter.intentionLevels = selectedLevels.isEmpty ? nil : Array(selectedLevels)
        newFilter.followUpStatuses = selectedStatuses.isEmpty ? nil : Array(selectedStatuses)
        newFilter.page = 1
        
        Task { await viewModel.applyFilter(newFilter) }
    }
}

// MARK: - 创建商机视图

struct CreateLeadView: View {
    @Environment(\.dismiss) private var dismiss
    
    @State private var customerName = ""
    @State private var customerPhone = ""
    @State private var customerCompany = ""
    @State private var remark = ""
    
    var body: some View {
        NavigationView {
            Form {
                Section("客户信息") {
                    TextField("客户姓名", text: $customerName)
                    TextField("手机号", text: $customerPhone)
                        .keyboardType(.phonePad)
                    TextField("公司名称", text: $customerCompany)
                }
                
                Section("备注") {
                    TextEditor(text: $remark)
                        .frame(minHeight: 100)
                }
            }
            .navigationTitle("添加商机")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") { dismiss() }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("保存") {
                        // 保存商机
                        dismiss()
                    }
                    .disabled(customerName.isEmpty || customerPhone.isEmpty)
                }
            }
        }
    }
}

// MARK: - 预览

#if DEBUG
struct LeadListView_Previews: PreviewProvider {
    static var previews: some View {
        LeadListView()
    }
}
#endif
