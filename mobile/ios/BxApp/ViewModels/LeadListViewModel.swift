//
//  LeadListViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  商机列表视图模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI

/// 商机列表视图模型
/// 
/// 负责管理商机列表的数据获取、筛选、分页等逻辑。
@MainActor
class LeadListViewModel: ObservableObject {
    
    // MARK: - Published属性
    
    /// 商机列表
    @Published var leads: [Lead] = []
    
    /// 筛选条件
    @Published var filter: LeadFilter = LeadFilter()
    
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
    
    /// 是否显示筛选面板
    @Published var showFilterPanel: Bool = false
    
    /// 是否显示商机详情
    @Published var showLeadDetail: Bool = false
    
    /// 当前选中的商机
    @Published var selectedLead: Lead?
    
    /// 统计信息
    @Published var statistics: LeadStatistics?
    
    // MARK: - Computed属性
    
    /// 是否有更多数据
    var hasMore: Bool {
        filter.page < calculateTotalPages()
    }
    
    /// 商机总数
    var totalCount: Int {
        statistics?.totalCount ?? leads.count
    }
    
    /// 筛选条件是否为空
    var hasActiveFilter: Bool {
        !filter.isEmpty
    }
    
    // MARK: - 私有属性
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    init() {
        setupBindings()
    }
    
    // MARK: - 公开方法
    
    /// 加载商机列表
    func loadLeads() async {
        // 如果是加载更多
        if filter.page > 1 {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        
        errorMessage = nil
        
        do {
            let response = try await LeadRepository.shared.fetchLeads(filter: filter)
            leads = response.list
            statistics = LeadStatistics(
                totalCount: response.total,
                highIntentionCount: response.list.filter { $0.intentionLevel == .high }.count,
                mediumIntentionCount: response.list.filter { $0.intentionLevel == .medium }.count,
                lowIntentionCount: response.list.filter { $0.intentionLevel == .low }.count
            )
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
        isLoadingMore = false
    }
    
    /// 刷新商机列表
    func refreshLeads() async {
        isRefreshing = true
        filter.page = 1
        
        await loadLeads()
        
        isRefreshing = false
    }
    
    /// 加载更多
    func loadMoreLeads() async {
        guard hasMore && !isLoadingMore else { return }
        
        filter.page += 1
        await loadLeads()
    }
    
    /// 搜索商机
    func searchLeads(keyword: String) async {
        filter.keyword = keyword.isEmpty ? nil : keyword
        filter.page = 1
        
        await loadLeads()
    }
    
    /// 应用筛选
    func applyFilter(_ newFilter: LeadFilter) async {
        filter = newFilter
        filter.page = 1
        
        await loadLeads()
    }
    
    /// 清空筛选
    func clearFilter() async {
        filter.clear()
        await loadLeads()
    }
    
    /// 删除商机
    func deleteLead(_ lead: Lead) async -> Bool {
        do {
            try await LeadRepository.shared.deleteLead(id: lead.id)
            
            // 从列表中移除
            leads.removeAll { $0.id == lead.id }
            
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 更新商机状态
    func updateLeadStatus(_ lead: Lead, status: FollowUpStatus) async -> Bool {
        do {
            let updatedLead = try await LeadRepository.shared.updateLeadStatus(
                id: lead.id,
                status: status
            )
            
            // 更新列表中的商机
            if let index = leads.firstIndex(where: { $0.id == lead.id }) {
                leads[index] = updatedLead
            }
            
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 更新商机意向等级
    func updateLeadIntention(_ lead: Lead, level: IntentionLevel) async -> Bool {
        do {
            let updatedLead = try await LeadRepository.shared.updateLeadIntention(
                id: lead.id,
                level: level
            )
            
            // 更新列表中的商机
            if let index = leads.firstIndex(where: { $0.id == lead.id }) {
                leads[index] = updatedLead
            }
            
            return true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            return false
        }
    }
    
    /// 选择商机查看详情
    func selectLead(_ lead: Lead) {
        selectedLead = lead
        showLeadDetail = true
    }
    
    /// 关闭商机详情
    func closeLeadDetail() {
        showLeadDetail = false
        selectedLead = nil
    }
    
    /// 排序方式切换
    func toggleSortOrder() {
        filter.sortOrder = filter.sortOrder == .ascending ? .descending : .ascending
    }
    
    /// 排序字段切换
    func switchSortField(_ field: LeadSortBy) {
        filter.sortBy = field
        filter.page = 1
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
                    await self?.loadLeads()
                }
            }
            .store(in: &cancellables)
        
        // 订阅商机更新通知
        NotificationCenter.default.publisher(for: .leadUpdated)
            .sink { [weak self] _ in
                Task { @MainActor in
                    await self?.refreshLeads()
                }
            }
            .store(in: &cancellables)
    }
    
    /// 计算总页数
    private func calculateTotalPages() -> Int {
        guard let stats = statistics, stats.totalCount > 0 else { return 1 }
        return (stats.totalCount + filter.pageSize - 1) / filter.pageSize
    }
}

// MARK: - 商机统计

/// 商机统计信息
struct LeadStatistics: Codable {
    
    /// 总数
    var totalCount: Int
    
    /// 高意向数量
    var highIntentionCount: Int
    
    /// 中意向数量
    var mediumIntentionCount: Int
    
    /// 低意向数量
    var lowIntentionCount: Int
    
    /// 今日新增
    var todayNewCount: Int
    
    /// 本周新增
    var weeklyNewCount: Int
    
    /// 本月新增
    var monthlyNewCount: Int
    
    // MARK: - Computed属性
    
    /// 无意向数量
    var noneIntentionCount: Int {
        totalCount - highIntentionCount - mediumIntentionCount - lowIntentionCount
    }
    
    /// 高意向占比
    var highIntentionRate: Double {
        guard totalCount > 0 else { return 0 }
        return Double(highIntentionCount) / Double(totalCount) * 100
    }
}

// MARK: - 预览

#if DEBUG
extension LeadListViewModel {
    static let preview = LeadListViewModel()
}
#endif
