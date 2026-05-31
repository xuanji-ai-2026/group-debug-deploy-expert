//
//  AccountListView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  账号列表页面
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 账号列表页面
/// 
/// 展示用户绑定的所有第三方平台账号，支持查看详情、解绑、重新授权等操作。
struct AccountListView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = AccountViewModel()
    
    /// 是否显示绑定页面
    @State private var showBindView = false
    
    /// 是否显示账号详情
    @State private var showDetailView = false
    
    /// 当前选中的账号
    @State private var selectedAccount: Account?
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                ScrollView {
                    VStack(spacing: 16) {
                        // 统计概览
                        statisticsSection
                        
                        // 账号列表
                        accountsSection
                    }
                    .padding()
                }
                .refreshable {
                    await viewModel.refreshAccounts()
                }
                
                // 加载指示器
                if viewModel.isLoading {
                    LoadingView()
                }
            }
            .navigationTitle("账号管理")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        showBindView = true
                    }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                    }
                }
            }
            .sheet(isPresented: $showBindView) {
                BindAccountView(viewModel: viewModel)
            }
            .sheet(item: $selectedAccount) { account in
                AccountDetailView(viewModel: viewModel, account: account)
            }
            .alert("解绑账号", isPresented: $viewModel.showUnbindConfirm) {
                Button("取消", role: .cancel) {
                    viewModel.cancelUnbind()
                }
                Button("确认解绑", role: .destructive) {
                    Task {
                        await viewModel.confirmUnbind()
                    }
                }
            } message: {
                if let account = viewModel.selectedAccount {
                    Text("确定要解绑 \(account.platform.name) 账号「\(account.platformNickname)」吗？解绑后将无法使用该账号进行获客任务。")
                }
            }
            .alert("绑定成功", isPresented: $viewModel.showBindSuccess) {
                Button("确定", role: .cancel) {}
            } message: {
                Text("账号绑定成功！")
            }
            .alert("错误", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "操作失败")
            }
        }
        .task {
            await viewModel.loadAccounts()
        }
    }
    
    // MARK: - 统计概览
    
    private var statisticsSection: some View {
        HStack(spacing: 12) {
            // 已绑定账号数
            StatisticCard(
                title: "已绑定",
                value: "\(viewModel.accounts.count)",
                icon: "link.circle.fill",
                color: .blue
            )
            
            // 正常账号数
            StatisticCard(
                title: "正常",
                value: "\(viewModel.activeAccountCount)",
                icon: "checkmark.circle.fill",
                color: .green
            )
            
            // 异常账号数
            StatisticCard(
                title: "异常",
                value: "\(viewModel.abnormalAccountCount)",
                icon: "exclamationmark.circle.fill",
                color: viewModel.abnormalAccountCount > 0 ? .red : .gray
            )
        }
    }
    
    // MARK: - 账号列表
    
    private var accountsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("已绑定账号")
                    .font(.headline)
                
                Spacer()
                
                Text("\(viewModel.accounts.count) 个")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            if viewModel.accounts.isEmpty {
                // 空状态
                emptyStateView
            } else {
                // 账号列表
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.accounts) { account in
                        AccountCard(account: account, viewModel: viewModel)
                            .onTapGesture {
                                selectedAccount = account
                            }
                    }
                }
            }
        }
    }
    
    // MARK: - 空状态
    
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text("暂无绑定账号")
                .font(.headline)
                .foregroundColor(.primary)
            
            Text("绑定第三方平台账号，开启AI智能获客")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            Button(action: {
                showBindView = true
            }) {
                Label("立即绑定", systemImage: "plus.circle")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.blue)
                    .cornerRadius(8)
            }
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 60)
    }
}

// MARK: - 统计卡片

struct StatisticCard: View {
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
                .font(.title2)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 账号卡片

struct AccountCard: View {
    let account: Account
    let viewModel: AccountViewModel
    
    var body: some View {
        HStack(spacing: 12) {
            // 平台图标
            platformIcon
            
            // 账号信息
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(account.platformNickname)
                        .font(.headline)
                        .lineLimit(1)
                    
                    Spacer()
                    
                    // 状态标签
                    statusBadge
                }
                
                HStack(spacing: 8) {
                    Text(account.platform.name)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text("·")
                        .foregroundColor(.secondary)
                    
                    Text("粉丝 \(formatNumber(account.followersCount))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                // 健康度
                HStack(spacing: 4) {
                    Text("健康度")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    HealthBar(score: account.healthScore)
                }
                .padding(.top, 4)
            }
            
            Spacer()
            
            // 箭头
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    // 平台图标
    private var platformIcon: some View {
        ZStack {
            Circle()
                .fill(platformColor.opacity(0.15))
                .frame(width: 50, height: 50)
            
            Image(systemName: account.platform.iconName)
                .font(.system(size: 24))
                .foregroundColor(platformColor)
        }
    }
    
    // 平台颜色
    private var platformColor: Color {
        Color(hex: account.platform.brandColor) ?? .blue
    }
    
    // 状态标签
    private var statusBadge: some View {
        Group {
            if account.status == .abnormal || account.authStatus.needsReauth {
                Label("需处理", systemImage: "exclamationmark.triangle.fill")
                    .font(.caption2)
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.red)
                    .cornerRadius(4)
            } else {
                Label("正常", systemImage: "checkmark.circle.fill")
                    .font(.caption2)
                    .foregroundColor(.green)
            }
        }
    }
    
    // 格式化数字
    private func formatNumber(_ number: Int) -> String {
        if number >= 10000 {
            return String(format: "%.1f万", Double(number) / 10000)
        } else if number >= 1000 {
            return String(format: "%.1f千", Double(number) / 1000)
        } else {
            return "\(number)"
        }
    }
}

// MARK: - 健康度条

struct HealthBar: View {
    let score: Int
    
    var body: some View {
        HStack(spacing: 4) {
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Rectangle()
                        .fill(Color.gray.opacity(0.2))
                        .cornerRadius(2)
                    
                    Rectangle()
                        .fill(healthColor)
                        .frame(width: geometry.size.width * CGFloat(score) / 100)
                        .cornerRadius(2)
                }
            }
            .frame(width: 60, height: 6)
            
            Text("\(score)")
                .font(.caption2)
                .fontWeight(.medium)
                .foregroundColor(healthColor)
        }
    }
    
    private var healthColor: Color {
        let level = HealthLevel.from(score: score)
        return Color(hex: level.color) ?? .gray
    }
}

// MARK: - 颜色扩展

extension Color {
    init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")
        
        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }
        
        let red = Double((rgb & 0xFF0000) >> 16) / 255.0
        let green = Double((rgb & 0x00FF00) >> 8) / 255.0
        let blue = Double(rgb & 0x0000FF) / 255.0
        
        self.init(red: red, green: green, blue: blue)
    }
}

// MARK: - 预览

#if DEBUG
struct AccountListView_Previews: PreviewProvider {
    static var previews: some View {
        AccountListView()
    }
}
#endif
