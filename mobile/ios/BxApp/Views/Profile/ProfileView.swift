//
//  ProfileView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  个人中心页面
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI
import PhotosUI

/// 个人中心页面
/// 
/// 展示用户个人信息、积分余额，提供编辑资料、安全设置、账号管理等功能入口。
struct ProfileView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = ProfileViewModel()
    
    /// 是否退出登录
    @Binding var isLoggedOut: Bool
    
    // MARK: - 初始化
    
    init(isLoggedOut: Binding<Bool> = .constant(false)) {
        self._isLoggedOut = isLoggedOut
    }
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                ScrollView {
                    VStack(spacing: 20) {
                        // 用户信息卡片
                        userInfoCard
                        
                        // 积分余额卡片
                        pointsCard
                        
                        // 功能列表
                        functionsList
                    }
                    .padding()
                }
                
                // 加载指示器
                if viewModel.isLoading {
                    LoadingView()
                }
            }
            .navigationTitle("我的")
            .navigationBarTitleDisplayMode(.large)
            .sheet(isPresented: $viewModel.showEditProfile) {
                EditProfileView(viewModel: viewModel)
            }
            .sheet(isPresented: $viewModel.showSecuritySettings) {
                SecuritySettingsView(viewModel: viewModel)
            }
            .sheet(isPresented: $viewModel.showAccountManagement) {
                AccountListView()
            }
            .sheet(isPresented: $viewModel.showRecharge) {
                RechargeView()
            }
            .alert("退出登录", isPresented: $viewModel.showLogoutConfirm) {
                Button("取消", role: .cancel) {
                    viewModel.cancelLogout()
                }
                Button("确认退出", role: .destructive) {
                    Task {
                        let success = await viewModel.confirmLogout()
                        if success {
                            isLoggedOut = true
                        }
                    }
                }
            } message: {
                Text("确定要退出登录吗？")
            }
            .alert("更新成功", isPresented: $viewModel.showUpdateSuccess) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.successMessage ?? "操作成功")
            }
            .alert("错误", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "操作失败")
            }
        }
        .task {
            await viewModel.refreshUserInfo()
        }
    }
    
    // MARK: - 用户信息卡片
    
    private var userInfoCard: some View {
        VStack(spacing: 16) {
            HStack(spacing: 16) {
                // 头像
                avatarView
                
                // 用户信息
                VStack(alignment: .leading, spacing: 4) {
                    Text(viewModel.nickname)
                        .font(.title3)
                        .fontWeight(.bold)
                    
                    Text(viewModel.maskedPhone)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 8) {
                        // 用户类型标签
                        Text(viewModel.userTypeDescription)
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.blue)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.blue.opacity(0.15))
                            .cornerRadius(4)
                        
                        // 等级标签
                        Text("Lv.\(viewModel.userLevel)")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.orange)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.orange.opacity(0.15))
                            .cornerRadius(4)
                    }
                }
                
                Spacer()
                
                // 编辑按钮
                Button(action: {
                    viewModel.showEditProfile = true
                }) {
                    Image(systemName: "pencil.circle.fill")
                        .font(.title3)
                        .foregroundColor(.blue)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(16)
    }
    
    // 头像视图
    private var avatarView: some View {
        ZStack {
            if let avatarUrl = viewModel.avatarUrl,
               let url = URL(string: avatarUrl) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    defaultAvatar
                }
                .frame(width: 70, height: 70)
                .clipShape(Circle())
            } else {
                defaultAvatar
                    .frame(width: 70, height: 70)
            }
            
            // 编辑图标
            Circle()
                .fill(Color.blue)
                .frame(width: 24, height: 24)
                .overlay(
                    Image(systemName: "camera.fill")
                        .font(.caption2)
                        .foregroundColor(.white)
                )
                .offset(x: 24, y: 24)
        }
        .onTapGesture {
            viewModel.showAvatarPicker = true
        }
        .photosPicker(
            isPresented: $viewModel.showAvatarPicker,
            selection: $viewModel.selectedAvatarItem,
            matching: .images
        )
        .onChange(of: viewModel.selectedAvatarItem) { _ in
            Task {
                await viewModel.handleSelectedAvatar()
            }
        }
    }
    
    // 默认头像
    private var defaultAvatar: some View {
        Circle()
            .fill(Color.blue.opacity(0.15))
            .overlay(
                Image(systemName: "person.fill")
                    .font(.system(size: 32))
                    .foregroundColor(.blue)
            )
    }
    
    // MARK: - 积分卡片
    
    private var pointsCard: some View {
        VStack(spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("积分余额")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    Text("\(viewModel.pointsBalance)")
                        .font(.system(size: 32, weight: .bold))
                }
                
                Spacer()
                
                Button(action: {
                    viewModel.showRecharge = true
                }) {
                    Label("充值", systemImage: "plus.circle")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color.orange)
                        .cornerRadius(20)
                }
            }
        }
        .padding()
        .background(
            LinearGradient(
                colors: [Color.orange.opacity(0.2), Color.yellow.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.orange.opacity(0.3), lineWidth: 1)
        )
    }
    
    // MARK: - 功能列表
    
    private var functionsList: some View {
        VStack(spacing: 12) {
            // 账号管理
            FunctionRow(
                icon: "person.2.fill",
                iconColor: .blue,
                title: "账号管理",
                subtitle: "管理绑定的第三方账号"
            )
            .onTapGesture {
                viewModel.showAccountManagement = true
            }
            
            // 安全设置
            FunctionRow(
                icon: "shield.fill",
                iconColor: .green,
                title: "安全设置",
                subtitle: "修改密码、账号安全"
            )
            .onTapGesture {
                viewModel.showSecuritySettings = true
            }
            
            // 通知设置
            FunctionRow(
                icon: "bell.fill",
                iconColor: .orange,
                title: "消息通知",
                subtitle: "设置推送通知偏好"
            )
            
            // 帮助与反馈
            FunctionRow(
                icon: "questionmark.circle.fill",
                iconColor: .purple,
                title: "帮助与反馈",
                subtitle: "常见问题、联系客服"
            )
            
            // 关于我们
            FunctionRow(
                icon: "info.circle.fill",
                iconColor: .gray,
                title: "关于我们",
                subtitle: "版本信息、用户协议"
            )
            
            // 退出登录
            Button(action: {
                viewModel.prepareLogout()
            }) {
                HStack {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .font(.title3)
                        .foregroundColor(.red)
                        .frame(width: 40)
                    
                    Text("退出登录")
                        .font(.subheadline)
                        .foregroundColor(.red)
                    
                    Spacer()
                }
                .padding()
                .background(Color.red.opacity(0.05))
                .cornerRadius(12)
            }
        }
    }
}

// MARK: - 功能行

struct FunctionRow: View {
    let icon: String
    let iconColor: Color
    let title: String
    let subtitle: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(iconColor)
                .frame(width: 40)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 预览

#if DEBUG
struct ProfileView_Previews: PreviewProvider {
    static var previews: some View {
        ProfileView()
    }
}
#endif
