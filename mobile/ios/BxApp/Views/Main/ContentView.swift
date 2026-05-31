//
//  ContentView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  内容视图 - 页面内容包装器
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 内容视图
/// 
/// 页面内容包装器，提供统一的背景、分页导航等功能。
struct ContentView<Content: View>: View {
    
    /// 标题
    let title: String
    
    /// 内容
    @ViewBuilder let content: Content
    
    /// 是否显示返回按钮
    var showBackButton: Bool = true
    
    /// 是否显示导航栏
    var showNavigationBar: Bool = true
    
    /// 导航栏显示模式
    var navigationBarDisplayMode: NavigationBarItem.TitleDisplayMode = .large
    
    /// 环境
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        VStack(spacing: 0) {
            // 内容区域
            content
        }
        .background(Color(.systemBackground))
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(navigationBarDisplayMode)
        .toolbar {
            if showBackButton {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                    }
                }
            }
        }
    }
}

// MARK: - 页面容器视图

/// 页面容器视图
struct PageContainer<Content: View>: View {
    @ViewBuilder let content: Content
    
    var body: some View {
        ScrollView {
            content
                .padding()
        }
        .background(Color(.systemGroupedBackground))
    }
}

// MARK: - 空状态视图

/// 空状态视图
struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text(title)
                .font(.headline)
                .foregroundColor(.primary)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
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
        }
        .padding()
    }
}

// MARK: - 错误视图

/// 错误视图
struct ErrorView: View {
    let message: String
    var retryAction: (() -> Void)?
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 50))
                .foregroundColor(.orange)
            
            Text("出错了")
                .font(.headline)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            if let retryAction = retryAction {
                Button(action: retryAction) {
                    Label("重试", systemImage: "arrow.clockwise")
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                .padding(.top, 8)
            }
        }
        .padding()
    }
}

// MARK: - 加载视图

/// 加载视图
struct LoadingView: View {
    var message: String = "加载中..."
    
    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(1.5)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground).opacity(0.9))
    }
}

// MARK: - 预览

#if DEBUG
struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            ContentView(title: "示例页面") {
                Text("页面内容")
            }
        }
    }
}
#endif
