//
//  LoadingView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  加载状态组件
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 加载状态视图
/// 
/// 显示不同状态的加载界面，包括加载中、空状态、错误等。
struct LoadingViewComponent: View {
    
    /// 状态类型
    enum State {
        case loading
        case empty
        case error(String)
    }
    
    /// 当前状态
    let state: State
    
    /// 空状态图标
    var emptyIcon: String = "tray"
    
    /// 空状态标题
    var emptyTitle: String = "暂无数据"
    
    /// 空状态描述
    var emptyMessage: String = ""
    
    /// 重新加载操作
    var onRetry: (() -> Void)?
    
    var body: some View {
        switch state {
        case .loading:
            loadingView
        case .empty:
            emptyView
        case .error(let message):
            errorView(message: message)
        }
    }
    
    // MARK: - 加载视图
    
    /// 加载视图
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(1.2)
            
            Text("加载中...")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground).opacity(0.95))
    }
    
    // MARK: - 空状态视图
    
    /// 空状态视图
    private var emptyView: some View {
        VStack(spacing: 16) {
            Image(systemName: emptyIcon)
                .font(.system(size: 50))
                .foregroundColor(.secondary)
            
            Text(emptyTitle)
                .font(.headline)
                .foregroundColor(.primary)
            
            if !emptyMessage.isEmpty {
                Text(emptyMessage)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            
            if let onRetry = onRetry {
                Button(action: onRetry) {
                    Label("重新加载", systemImage: "arrow.clockwise")
                        .font(.subheadline)
                        .foregroundColor(.blue)
                }
                .padding(.top, 8)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    // MARK: - 错误视图
    
    /// 错误视图
    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 50))
                .foregroundColor(.orange)
            
            Text("出错了")
                .font(.headline)
                .foregroundColor(.primary)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            if let onRetry = onRetry {
                Button(action: onRetry) {
                    Label("重试", systemImage: "arrow.clockwise")
                        .font(.subheadline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.blue)
                        .cornerRadius(8)
                }
                .padding(.top, 8)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - 加载指示器

/// 加载指示器（内联使用）
struct InlineLoadingIndicator: View {
    var message: String = "加载中..."
    
    var body: some View {
        HStack(spacing: 8) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(0.8)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - 骨架屏

/// 骨架屏组件
struct SkeletonView: View {
    var width: CGFloat? = nil
    var height: CGFloat = 20
    
    @State private var isAnimating = false
    
    var body: some View {
        RoundedRectangle(cornerRadius: 4)
            .fill(
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color(.systemGray5),
                        Color(.systemGray4),
                        Color(.systemGray5)
                    ]),
                    startPoint: isAnimating ? .trailing : .leading,
                    endPoint: isAnimating ? .leading : .trailing
                )
            )
            .frame(width: width, height: height)
            .onAppear {
                withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) {
                    isAnimating = true
                }
            }
    }
}

// MARK: - 骨架屏列表项

struct SkeletonListItem: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                SkeletonView(width: 120, height: 20)
                Spacer()
                SkeletonView(width: 50, height: 40)
            }
            
            SkeletonView(height: 16)
            
            HStack {
                SkeletonView(width: 80, height: 24)
                Spacer()
                SkeletonView(width: 60, height: 16)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 预览

#if DEBUG
struct LoadingViewComponent_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 40) {
            LoadingViewComponent(state: .loading)
                .frame(height: 200)
            
            LoadingViewComponent(
                state: .empty,
                emptyIcon: "tray",
                emptyTitle: "暂无商机",
                emptyMessage: "开始添加您的第一个商机吧",
                onRetry: {}
            )
            .frame(height: 200)
            
            LoadingViewComponent(
                state: .error("网络连接失败，请检查网络设置"),
                onRetry: {}
            )
            .frame(height: 200)
        }
    }
}
#endif
