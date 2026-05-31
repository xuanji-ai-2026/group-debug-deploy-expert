//
//  EmptyStateView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  空状态视图组件
//
//  Created by Lin Feng (EMP-IOS-003) on 2024-01-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 空状态视图
///
/// 用于显示无数据时的友好提示界面。
struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil
    
    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            
            Image(systemName: icon)
                .font(.system(size: 60))
                .foregroundColor(.secondary.opacity(0.6))
            
            Text(title)
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.primary)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            if let actionTitle = actionTitle, let action = action {
                Button(action: action) {
                    Text(actionTitle)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.blue)
                        .cornerRadius(8)
                }
                .padding(.top, 8)
            }
            
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
    }
}

// MARK: - 加载视图

/// 简单加载视图
struct LoadingView: View {
    var message: String = "加载中..."
    
    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(1.2)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground).opacity(0.95))
    }
}

// MARK: - 预览

#if DEBUG
struct EmptyStateView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            EmptyStateView(
                icon: "bell.slash",
                title: "暂无消息",
                message: "您还没有收到任何消息"
            )
            
            EmptyStateView(
                icon: "tray",
                title: "暂无数据",
                message: "开始创建您的第一个任务吧",
                actionTitle: "创建任务"
            ) {}
        }
    }
}
#endif
