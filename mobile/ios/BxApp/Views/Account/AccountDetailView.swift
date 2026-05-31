//
//  AccountDetailView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  账号详情页面
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 账号详情页面
/// 
/// 展示单个第三方平台账号的详细信息，支持同步数据、重新授权、解绑等操作。
struct AccountDetailView: View {
    
    // MARK: - 属性
    
    /// 视图模型
    @ObservedObject var viewModel: AccountViewModel
    
    /// 账号数据
    let account: Account
    
    /// 是否显示解绑确认
    @State private var showUnbindConfirm = false
    
    /// 是否正在同步
    @State private var isSyncing = false
    
    /// 环境变量，用于关闭页面
    @Environment(\.dismiss) private var dismiss
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // 账号头部信息
                    headerSection
                    
                    // 健康度卡片
                    healthCard
                    
                    // 数据统计
                    statisticsSection
                    
                    // 账号信息
                    infoSection
                    
                    // 操作按钮
                    actionButtonsSection
                }
                .padding()
            }
            .navigationTitle("账号详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("关闭") {
                        dismiss()
                    }
                }
            }
            .alert("解绑账号", isPresented: $showUnbindConfirm) {
                Button("取消", role: .cancel) {}
                Button("确认解绑", role: .destructive) {
                    Task {
                        await viewModel.unbindAccount(account)
                        dismiss()
                    }
                }
            } message: {
                Text("确定要解绑 \(account.platform.name) 账号「\(account.platformNickname)」吗？此操作不可撤销。")
            }
        }
    }
    
    // MARK: - 头部信息
    
    private var headerSection: some View {
        VStack(spacing: 16) {
            // 头像
            ZStack {
                Circle()
                    .fill(platformColor.opacity(0.15))
                    .frame(width: 100, height: 100)
                
                if let avatarUrl = account.platformAvatar,
                   let url = URL(string: avatarUrl) {
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Image(systemName: account.platform.iconName)
                            .font(.system(size: 40))
                            .foregroundColor(platformColor)
                    }
                    .frame(width: 100, height: 100)
                    .clipShape(Circle())
                } else {
                    Image(systemName: account.platform.iconName)
                        .font(.system(size: 40))
                        .foregroundColor(platformColor)
                }
                
                // 平台标识
                platformBadge
            }
            
            // 昵称
            Text(account.platformNickname)
                .font(.title2)
                .fontWeight(.bold)
            
            // 平台名称
            Text(account.platform.name)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            // 状态标签
            HStack(spacing: 12) {
                StatusBadge(
                    text: account.status.description,
                    color: Color(hex: account.status.color) ?? .gray
                )
                
                StatusBadge(
                    text: account.authStatus.description,
                    color: account.authStatus.needsReauth ? .red : .green
                )
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(16)
    }
    
    // 平台标识徽章
    private var platformBadge: some View {
        VStack {
            Spacer()
            HStack {
                Spacer()
                ZStack {
                    Circle()
                        .fill(Color.white)
                        .frame(width: 32, height: 32)
                    
                    Image(systemName: account.platform.iconName)
                        .font(.system(size: 16))
                        .foregroundColor(platformColor)
                }
            }
        }
        .frame(width: 100, height: 100)
    }
    
    // 平台颜色
    private var platformColor: Color {
        Color(hex: account.platform.brandColor) ?? .blue
    }
    
    // MARK: - 健康度卡片
    
    private var healthCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("账号健康度")
                    .font(.headline)
                
                Spacer()
                
                let level = HealthLevel.from(score: account.healthScore)
                Text(level.description)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(Color(hex: level.color))
            }
            
            // 健康度进度条
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Rectangle()
                        .fill(Color.gray.opacity(0.2))
                        .cornerRadius(4)
                    
                    let level = HealthLevel.from(score: account.healthScore)
                    Rectangle()
                        .fill(Color(hex: level.color) ?? .blue)
                        .frame(width: geometry.size.width * CGFloat(account.healthScore) / 100)
                        .cornerRadius(4)
                }
            }
            .frame(height: 12)
            
            HStack {
                Text("\(account.healthScore) 分")
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(Color(hex: HealthLevel.from(score: account.healthScore).color))
                
                Spacer()
                
                if account.healthScore < 60 {
                    Label("健康度较低，建议优化", systemImage: "exclamationmark.triangle.fill")
                        .font(.caption)
                        .foregroundColor(.orange)
                }
            }
            
            // 健康度说明
            Text("健康度反映账号的综合质量，包括内容质量、活跃度、违规记录等因素。")
                .font(.caption)
                .foregroundColor(.secondary)
                .padding(.top, 4)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 数据统计
    
    private var statisticsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("数据统计")
                    .font(.headline)
                
                Spacer()
                
                if let syncAt = account.lastSyncAt {
                    Text("同步于 \(syncAt.formatted(.relative(presentation: .named)))"
                        .replacingOccurrences(of: " ", with: ""))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            HStack(spacing: 12) {
                StatItem(
                    title: "粉丝数",
                    value: formatNumber(account.followersCount),
                    icon: "person.2.fill",
                    color: .blue
                )
                
                StatItem(
                    title: "作品数",
                    value: formatNumber(account.worksCount),
                    icon: "photo.fill",
                    color: .green
                )
                
                StatItem(
                    title: "获赞数",
                    value: formatNumber(account.likesCount),
                    icon: "heart.fill",
                    color: .red
                )
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 账号信息
    
    private var infoSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("账号信息")
                .font(.headline)
            
            VStack(spacing: 12) {
                InfoRow(title: "平台ID", value: maskString(account.platformUserId))
                
                Divider()
                
                if let phone = account.bindPhone {
                    InfoRow(title: "绑定手机", value: maskPhone(phone))
                }
                
                Divider()
                
                InfoRow(title: "绑定时间", value: account.bindAt.formatted(date: .long, time: .shortened))
                
                Divider()
                
                if let expireAt = account.authExpireAt {
                    InfoRow(
                        title: "授权过期",
                        value: expireAt.formatted(date: .long, time: .shortened),
                        valueColor: expireAt < Date() ? .red : .primary
                    )
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 操作按钮
    
    private var actionButtonsSection: some View {
        VStack(spacing: 12) {
            // 同步数据按钮
            Button(action: {
                Task {
                    isSyncing = true
                    await viewModel.syncAccount(account)
                    isSyncing = false
                }
            }) {
                HStack {
                    if isSyncing {
                        ProgressView()
                            .scaleEffect(0.8)
                    } else {
                        Image(systemName: "arrow.clockwise")
                    }
                    Text(isSyncing ? "同步中..." : "同步数据")
                }
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.blue)
                .cornerRadius(12)
            }
            .disabled(isSyncing)
            
            // 重新授权按钮
            if account.authStatus.needsReauth {
                Button(action: {
                    Task {
                        await viewModel.reauthorize(account)
                    }
                }) {
                    HStack {
                        Image(systemName: "arrow.clockwise.circle")
                        Text("重新授权")
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.orange)
                    .cornerRadius(12)
                }
            }
            
            // 解绑按钮
            Button(action: {
                showUnbindConfirm = true
            }) {
                HStack {
                    Image(systemName: "link.badge.minus")
                    Text("解绑账号")
                }
                .font(.headline)
                .foregroundColor(.red)
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.red.opacity(0.1))
                .cornerRadius(12)
            }
        }
    }
    
    // MARK: - 辅助方法
    
    private func formatNumber(_ number: Int) -> String {
        if number >= 10000 {
            return String(format: "%.1f万", Double(number) / 10000)
        } else if number >= 1000 {
            return String(format: "%.1f千", Double(number) / 1000)
        } else {
            return "\(number)"
        }
    }
    
    private func maskString(_ str: String) -> String {
        guard str.count > 8 else { return str }
        let prefix = str.prefix(4)
        let suffix = str.suffix(4)
        return "\(prefix)****\(suffix)"
    }
    
    private func maskPhone(_ phone: String) -> String {
        guard phone.count == 11 else { return phone }
        let prefix = phone.prefix(3)
        let suffix = phone.suffix(4)
        return "\(prefix)****\(suffix)"
    }
}

// MARK: - 状态标签

struct StatusBadge: View {
    let text: String
    let color: Color
    
    var body: some View {
        Text(text)
            .font(.caption)
            .fontWeight(.medium)
            .foregroundColor(color)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(color.opacity(0.15))
            .cornerRadius(6)
    }
}

// MARK: - 统计项

struct StatItem: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
    }
}

// MARK: - 信息行

struct InfoRow: View {
    let title: String
    let value: String
    var valueColor: Color = .primary
    
    var body: some View {
        HStack {
            Text(title)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.subheadline)
                .foregroundColor(valueColor)
        }
    }
}

// MARK: - 预览

#if DEBUG
struct AccountDetailView_Previews: PreviewProvider {
    static var previews: some View {
        AccountDetailView(
            viewModel: AccountViewModel.preview,
            account: Account.preview
        )
    }
}
#endif
