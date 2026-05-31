//
//  MainViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  主页面视图模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI

/// 主页面视图模型
/// 
/// 负责管理主页面的状态，包括底部导航、用户信息、统计数据等。
@MainActor
class MainViewModel: ObservableObject {
    
    // MARK: - Published属性
    
    /// 当前选中的标签页索引
    @Published var selectedTab: Tab = .home
    
    /// 用户信息
    @Published var currentUser: User?
    
    /// 统计数据
    @Published var statistics: DashboardStatistics?
    
    /// 是否加载中
    @Published var isLoading: Bool = false
    
    /// 错误信息
    @Published var errorMessage: String?
    
    // MARK: - 标签页枚举
    
    /// 底部标签页
    enum Tab: Int, CaseIterable {
        /// 首页
        case home = 0
        
        /// 商机
        case leads = 1
        
        /// 任务
        case tasks = 2
        
        /// 我的
        case profile = 3
        
        /// 标签页名称
        var title: String {
            switch self {
            case .home: return "首页"
            case .leads: return "商机"
            case .tasks: return "任务"
            case .profile: return "我的"
            }
        }
        
        /// 标签页图标
        var iconName: String {
            switch self {
            case .home: return "house"
            case .leads: return "star"
            case .tasks: return "checklist"
            case .profile: return "person"
            }
        }
        
        /// 选中图标
        var selectedIconName: String {
            switch self {
            case .home: return "house.fill"
            case .leads: return "star.fill"
            case .tasks: return "checklist"
            case .profile: return "person.fill"
            }
        }
    }
    
    // MARK: - 私有属性
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    init() {
        // 加载用户信息
        loadCurrentUser()
        
        // 订阅通知
        setupNotifications()
    }
    
    // MARK: - 公开方法
    
    /// 刷新数据
    func refresh() async {
        // 并行加载数据
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.loadCurrentUser() }
            group.addTask { await self.loadStatistics() }
        }
    }
    
    /// 加载用户信息
    func loadCurrentUser() async {
        // 从本地加载
        if let data = UserDefaults.standard.data(forKey: "currentUser"),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            currentUser = user
        }
    }
    
    /// 加载统计数据
    func loadStatistics() async {
        isLoading = true
        
        do {
            let stats = try await APIService.shared.fetchDashboardStatistics()
            statistics = stats
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    /// 切换标签页
    func switchTab(_ tab: Tab) {
        selectedTab = tab
    }
    
    /// 登出
    func logout() async -> Bool {
        isLoading = true
        
        do {
            // 调用登出接口
            try await AuthService.shared.logout()
            
            // 清除本地数据
            clearLocalData()
            
            isLoading = false
            return true
            
        } catch {
            // 即使接口失败，也清除本地数据
            clearLocalData()
            isLoading = false
            return true
        }
    }
    
    /// 检查更新
    func checkForUpdates() async -> Bool {
        print("[MainViewModel] 版本检查(预留)")
        return false
    }
    
    // MARK: - 私有方法
    
    /// 设置通知订阅
    private func setupNotifications() {
        // 认证状态变更
        NotificationCenter.default.publisher(for: .authStateChanged)
            .sink { [weak self] _ in
                Task { @MainActor in
                    await self?.loadCurrentUser()
                }
            }
            .store(in: &cancellables)
        
        // 商机更新
        NotificationCenter.default.publisher(for: .leadUpdated)
            .sink { [weak self] _ in
                Task { @MainActor in
                    await self?.loadStatistics()
                }
            }
            .store(in: &cancellables)
        
        // 任务更新
        NotificationCenter.default.publisher(for: .taskUpdated)
            .sink { [weak self] _ in
                Task { @MainActor in
                    await self?.loadStatistics()
                }
            }
            .store(in: &cancellables)
    }
    
    /// 清除本地数据
    private func clearLocalData() {
        // 清除认证信息
        KeychainManager.shared.deleteToken()
        KeychainManager.shared.deleteRefreshToken()
        KeychainManager.shared.deleteTokenExpiration()
        
        // 清除用户信息
        UserDefaults.standard.removeObject(forKey: "currentUser")
        
        // 重置状态
        currentUser = nil
        statistics = nil
    }
}

// MARK: - 统计数据

/// 仪表盘统计数据
struct DashboardStatistics: Codable {
    
    /// 今日新增商机
    var todayNewLeads: Int
    
    /// 商机总数
    var totalLeads: Int
    
    /// 本周成交
    var weeklyClosedDeals: Int
    
    /// 成交总额
    var totalClosedAmount: Double
    
    /// 进行中任务
    var runningTasks: Int
    
    /// 待执行任务
    var pendingTasks: Int
    
    /// 完成任务
    var completedTasks: Int
    
    /// 失败任务
    var failedTasks: Int
    
    /// 账户余额
    var accountBalance: Int
    
    /// 本月消耗
    var monthlyConsumption: Int
    
    // MARK: - Computed属性
    
    /// 任务完成率
    var taskCompletionRate: Double {
        let total = runningTasks + pendingTasks + completedTasks + failedTasks
        guard total > 0 else { return 0 }
        return Double(completedTasks) / Double(total) * 100
    }
    
    /// 商机转化率
    var leadConversionRate: Double {
        guard totalLeads > 0 else { return 0 }
        return Double(weeklyClosedDeals) / Double(totalLeads) * 100
    }
}

// MARK: - 预览

#if DEBUG
extension DashboardStatistics {
    static let preview = DashboardStatistics(
        todayNewLeads: 15,
        totalLeads: 256,
        weeklyClosedDeals: 8,
        totalClosedAmount: 128000,
        runningTasks: 5,
        pendingTasks: 12,
        completedTasks: 45,
        failedTasks: 2,
        accountBalance: 5000,
        monthlyConsumption: 1200
    )
}
#endif
