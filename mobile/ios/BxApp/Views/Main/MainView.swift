//
//  MainView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  主页面视图 - 底部标签导航
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 主页面视图
/// 
/// 应用的主页面，包含底部标签导航和各功能模块入口。
struct MainView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = MainViewModel()
    
    /// 选中的标签页
    @State private var selectedTab: MainViewModel.Tab = .home
    
    // MARK: - 界面
    
    var body: some View {
        TabView(selection: $selectedTab) {
            // 首页
            HomeView()
                .tabItem {
                    Label(
                        MainViewModel.Tab.home.title,
                        systemImage: selectedTab == .home ? 
                            MainViewModel.Tab.home.selectedIconName : 
                            MainViewModel.Tab.home.iconName
                    )
                }
                .tag(MainViewModel.Tab.home)
            
            // 商机
            LeadListView()
                .tabItem {
                    Label(
                        MainViewModel.Tab.leads.title,
                        systemImage: selectedTab == .leads ? 
                            MainViewModel.Tab.leads.selectedIconName : 
                            MainViewModel.Tab.leads.iconName
                    )
                }
                .tag(MainViewModel.Tab.leads)
            
            // 任务
            TaskListView()
                .tabItem {
                    Label(
                        MainViewModel.Tab.tasks.title,
                        systemImage: selectedTab == .tasks ? 
                            MainViewModel.Tab.tasks.selectedIconName : 
                            MainViewModel.Tab.tasks.iconName
                    )
                }
                .tag(MainViewModel.Tab.tasks)
            
            // 我的
            ProfileView()
                .tabItem {
                    Label(
                        MainViewModel.Tab.profile.title,
                        systemImage: selectedTab == .profile ? 
                            MainViewModel.Tab.profile.selectedIconName : 
                            MainViewModel.Tab.profile.iconName
                    )
                }
                .tag(MainViewModel.Tab.profile)
        }
        .tint(.blue)
        .onReceive(NotificationCenter.default.publisher(for: .authStateChanged)) { _ in
            // 处理深度链接导航
        }
    }
}

// MARK: - 首页视图

/// 首页视图
struct HomeView: View {
    
    /// 用户信息
    @EnvironmentObject var authState: AuthState
    
    /// 统计数据
    @State private var statistics: DashboardStatistics?
    
    /// 是否加载中
    @State private var isLoading = true
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // 欢迎区域
                    welcomeSection
                    
                    // 快捷操作
                    quickActionsSection
                    
                    // 统计卡片
                    statisticsSection
                    
                    // 最新商机
                    recentLeadsSection
                }
                .padding()
            }
            .navigationTitle("首页")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await loadData()
            }
        }
        .onAppear {
            Task {
                await loadData()
            }
        }
    }
    
    /// 欢迎区域
    private var welcomeSection: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("欢迎回来")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                Text(authState.currentUser?.nickname ?? "用户")
                    .font(.title2)
                    .fontWeight(.bold)
            }
            
            Spacer()
            
            // 头像
            if let avatarUrl = authState.currentUser?.avatarUrl,
               let url = URL(string: avatarUrl) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Image(systemName: "person.circle.fill")
                        .font(.system(size: 44))
                        .foregroundColor(.secondary)
                }
                .frame(width: 50, height: 50)
                .clipShape(Circle())
            } else {
                Image(systemName: "person.circle.fill")
                    .font(.system(size: 44))
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    /// 快捷操作
    private var quickActionsSection: some View {
        HStack(spacing: 16) {
            QuickActionButton(
                icon: "star.fill",
                title: "商机",
                color: .orange
            ) {
                // 跳转商机
            }
            
            QuickActionButton(
                icon: "checklist",
                title: "任务",
                color: .blue
            ) {
                // 跳转任务
            }
            
            QuickActionButton(
                icon: "chart.bar.fill",
                title: "统计",
                color: .green
            ) {
                // 跳转统计
            }
            
            QuickActionButton(
                icon: "person.fill",
                title: "我的",
                color: .purple
            ) {
                // 跳转我的
            }
        }
    }
    
    /// 统计卡片
    private var statisticsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("今日概览")
                .font(.headline)
            
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 12) {
                StatisticsCard(
                    title: "新增商机",
                    value: "\(statistics?.todayNewLeads ?? 0)",
                    icon: "star.fill",
                    color: .orange
                )
                
                StatisticsCard(
                    title: "进行中任务",
                    value: "\(statistics?.runningTasks ?? 0)",
                    icon: "play.fill",
                    color: .blue
                )
                
                StatisticsCard(
                    title: "完成任务",
                    value: "\(statistics?.completedTasks ?? 0)",
                    icon: "checkmark.circle.fill",
                    color: .green
                )
                
                StatisticsCard(
                    title: "账户余额",
                    value: "\(statistics?.accountBalance ?? 0)积分",
                    icon: "dollarsign.circle.fill",
                    color: .purple
                )
            }
        }
    }
    
    /// 最新商机
    private var recentLeadsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("最新商机")
                    .font(.headline)
                
                Spacer()
                
                NavigationLink(destination: LeadListView()) {
                    Text("查看全部")
                        .font(.subheadline)
                        .foregroundColor(.blue)
                }
            }
            
            print("[MainView] 显示最新商机列表(预留)")
            Text("暂无最新商机")
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
        }
    }
    
    /// 加载数据
    private func loadData() async {
        isLoading = true
        
        do {
            statistics = try await APIService.shared.fetchDashboardStatistics()
        } catch {
            // 处理错误
        }
        
        isLoading = false
    }
}

// MARK: - 快捷操作按钮

struct QuickActionButton: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 24))
                    .foregroundColor(color)
                    .frame(width: 50, height: 50)
                    .background(color.opacity(0.1))
                    .cornerRadius(12)
                
                Text(title)
                    .font(.caption)
                    .foregroundColor(.primary)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 统计卡片

struct StatisticsCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                Spacer()
            }
            
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 我的页面

struct ProfileView: View {
    var body: some View {
        NavigationView {
            List {
                Section {
                    NavigationLink(destination: Text("账户信息")) {
                        Label("账户信息", systemImage: "person.fill")
                    }
                    
                    NavigationLink(destination: Text("设置")) {
                        Label("设置", systemImage: "gear")
                    }
                }
                
                Section {
                    NavigationLink(destination: Text("关于我们")) {
                        Label("关于我们", systemImage: "info.circle")
                    }
                }
                
                Section {
                    Button(action: {
                        // 登出
                    }) {
                        Label("退出登录", systemImage: "rectangle.portrait.and.arrow.right")
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("我的")
        }
    }
}

// MARK: - 预览

#if DEBUG
struct MainView_Previews: PreviewProvider {
    static var previews: some View {
        MainView()
            .environmentObject(AuthState())
    }
}
#endif
