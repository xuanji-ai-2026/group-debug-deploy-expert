//
//  View+Extension.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  View扩展
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

// MARK: - View扩展

extension View {
    // MARK: - 条件修饰符
    
    /// 条件应用修饰符
    /// - Parameters:
    ///   - condition: 条件
    ///   - transform: 要应用的修饰符
    /// - Returns: 应用了修饰符的视图
    @ViewBuilder
    func `if`<Content: View>(_ condition: Bool, transform: (Self) -> Content) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
    
    /// 条件应用修饰符（带else分支）
    /// - Parameters:
    ///   - condition: 条件
    ///   - ifTransform: 条件为true时应用的修饰符
    ///   - elseTransform: 条件为false时应用的修饰符
    /// - Returns: 应用了修饰符的视图
    @ViewBuilder
    func `if`<TrueContent: View, FalseContent: View>(
        _ condition: Bool,
        if ifTransform: (Self) -> TrueContent,
        else elseTransform: (Self) -> FalseContent
    ) -> some View {
        if condition {
            ifTransform(self)
        } else {
            elseTransform(self)
        }
    }
    
    // MARK: - 隐藏键盘
    
    /// 隐藏键盘
    func hideKeyboard() {
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil,
            from: nil,
            for: nil
        )
    }
    
    /// 点击背景隐藏键盘
    func hideKeyboardOnTap() -> some View {
        self.onTapGesture {
            hideKeyboard()
        }
    }
    
    // MARK: - 圆角
    
    /// 设置圆角
    /// - Parameter radius: 圆角半径
    /// - Returns: 圆角视图
    func cornerRadius(_ radius: CGFloat) -> some View {
        self.clipShape(RoundedRectangle(cornerRadius: radius))
    }
    
    // MARK: - 阴影
    
    /// 添加阴影
    /// - Parameters:
    ///   - color: 阴影颜色
    ///   - radius: 阴影半径
    ///   - x: x偏移
    ///   - y: y偏移
    /// - Returns: 带阴影的视图
    func shadow(
        color: Color = .black.opacity(0.1),
        radius: CGFloat = 4,
        x: CGFloat = 0,
        y: CGFloat = 2
    ) -> some View {
        self.shadow(color: color, radius: radius, x: x, y: y)
    }
    
    // MARK: - Loading状态
    
    /// 显示加载状态
    /// - Parameter isLoading: 是否加载中
    /// - Returns: 带加载遮罩的视图
    func loading(_ isLoading: Bool) -> some View {
        self.overlay {
            if isLoading {
                ZStack {
                    Color.black.opacity(0.3)
                    
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                }
            }
        }
    }
    
    // MARK: - 错误提示
    
    /// 显示错误提示
    /// - Parameters:
    ///   - message: 错误消息
    ///   - isPresented: 是否显示
    ///   - action: 操作回调
    /// - Returns: 带错误提示的视图
    func errorAlert(
        message: String?,
        isPresented: Binding<Bool>,
        action: (() -> Void)? = nil
    ) -> some View {
        self.alert("错误", isPresented: isPresented) {
            Button("确定", role: .cancel) {
                action?()
            }
        } message: {
            Text(message ?? "未知错误")
        }
    }
    
    // MARK: - 导航
    
    /// 安全地推送到导航堆栈
    /// - Parameter destination: 目标视图
    /// - Returns: 可导航的视图
    func navigate<Destination: View>(to destination: Destination) -> some View {
        navigationDestination {
            destination
        }
    }
}

// MARK: - 可动画视图

extension View {
    /// 动画显示/隐藏
    /// - Parameters:
    ///   - show: 是否显示
    ///   - animation: 动画
    /// - Returns: 动画视图
    @ViewBuilder
    func animateAppear(_ show: Bool, animation: Animation = .easeInOut) -> some View {
        self
            .opacity(show ? 1 : 0)
            .animation(animation, value: show)
    }
}

// MARK: - 预览支持

#if DEBUG
struct ViewExtensions_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            Text("Hello World")
                .if(true) { $0.bold() }
                .if(false) { $0.italic() }
            
            Text("Loading Demo")
                .loading(true)
        }
        .padding()
    }
}
#endif
