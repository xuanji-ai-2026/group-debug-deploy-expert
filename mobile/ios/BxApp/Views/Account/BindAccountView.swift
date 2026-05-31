//
//  BindAccountView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  绑定账号页面
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI
import SafariServices

/// 绑定账号页面
/// 
/// 支持绑定多个第三方平台账号，包括抖音、小红书、快手等。
struct BindAccountView: View {
    
    // MARK: - 属性
    
    /// 视图模型
    @ObservedObject var viewModel: AccountViewModel
    
    /// 环境变量，用于关闭页面
    @Environment(\.dismiss) private var dismiss
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // 说明文字
                    instructionSection
                    
                    // 平台列表
                    platformListSection
                    
                    // 绑定说明
                    bindingInfoSection
                }
                .padding()
            }
            .navigationTitle("绑定账号")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") {
                        dismiss()
                    }
                }
            }
            .alert("错误", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "操作失败")
            }
        }
    }
    
    // MARK: - 说明文字
    
    private var instructionSection: some View {
        VStack(spacing: 12) {
            Image(systemName: "link.circle.fill")
                .font(.system(size: 60))
                .foregroundColor(.blue)
            
            Text("绑定第三方账号")
                .font(.title2)
                .fontWeight(.bold)
            
            Text("绑定后可通过AI系统自动获取商机线索，提升获客效率")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.vertical, 20)
    }
    
    // MARK: - 平台列表
    
    private var platformListSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("选择平台")
                .font(.headline)
            
            LazyVStack(spacing: 12) {
                ForEach(PlatformType.allCases, id: \.self) { platform in
                    PlatformRow(
                        platform: platform,
                        isBound: viewModel.isPlatformBound(platform),
                        account: viewModel.getAccount(for: platform)
                    )
                    .onTapGesture {
                        Task {
                            await viewModel.startBindProcess(for: platform)
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - 绑定说明
    
    private var bindingInfoSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("绑定说明")
                .font(.headline)
            
            VStack(alignment: .leading, spacing: 12) {
                InfoItem(
                    icon: "checkmark.shield.fill",
                    title: "安全可靠",
                    description: "采用官方OAuth授权，不保存您的密码"
                )
                
                InfoItem(
                    icon: "lock.fill",
                    title: "隐私保护",
                    description: "仅获取必要的公开信息，保护您的隐私"
                )
                
                InfoItem(
                    icon: "arrow.triangle.2.circlepath",
                    title: "随时解绑",
                    description: "您可以随时解绑账号，解除所有授权"
                )
                
                InfoItem(
                    icon: "bolt.fill",
                    title: "智能获客",
                    description: "绑定后AI自动分析内容，获取精准商机"
                )
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 平台行

struct PlatformRow: View {
    let platform: PlatformType
    let isBound: Bool
    let account: Account?
    
    var body: some View {
        HStack(spacing: 16) {
            // 平台图标
            ZStack {
                Circle()
                    .fill(platformColor.opacity(0.15))
                    .frame(width: 50, height: 50)
                
                Image(systemName: platform.iconName)
                    .font(.system(size: 24))
                    .foregroundColor(platformColor)
            }
            
            // 平台信息
            VStack(alignment: .leading, spacing: 4) {
                Text(platform.name)
                    .font(.headline)
                
                if isBound, let account = account {
                    HStack(spacing: 4) {
                        Text(account.platformNickname)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                        
                        Text("·")
                            .foregroundColor(.secondary)
                        
                        Text("已绑定")
                            .font(.caption)
                            .foregroundColor(.green)
                    }
                } else {
                    Text("点击绑定")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            // 状态图标
            if isBound {
                Image(systemName: "checkmark.circle.fill")
                    .font(.title3)
                    .foregroundColor(.green)
            } else {
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .opacity(isBound ? 0.7 : 1.0)
    }
    
    private var platformColor: Color {
        Color(hex: platform.brandColor) ?? .blue
    }
}

// MARK: - 信息项

struct InfoItem: View {
    let icon: String
    let title: String
    let description: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 20))
                .foregroundColor(.blue)
                .frame(width: 24)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
    }
}

// MARK: - Safari 视图控制器

struct SafariView: UIViewControllerRepresentable {
    let url: URL
    
    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.dismissButtonStyle = .done
        return controller
    }
    
    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}

// MARK: - 预览

#if DEBUG
struct BindAccountView_Previews: PreviewProvider {
    static var previews: some View {
        BindAccountView(viewModel: AccountViewModel.preview)
    }
}
#endif
