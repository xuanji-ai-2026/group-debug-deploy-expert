//
//  BxAppApp.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  App入口文件 - SwiftUI应用程序主入口
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// BxApp应用主入口
/// 
/// 这是北极星AI商机获客系统iOS客户端的SwiftUI应用入口点。
/// 应用采用MVVM架构，使用@StateObject管理视图状态，
/// 通过AppDelegate处理应用生命周期事件。
@main
struct BxAppApp: App {
    
    // MARK: - 属性
    
    /// 应用状态管理器 - 观察整个应用的生命周期
    @StateObject private var appState = AppState()
    
    /// 应用委托引用 - 用于处理UIApplication生命周期事件
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    // MARK: - 应用主体
    
    var body: some Scene {
        WindowGroup {
            // 根据应用状态渲染对应界面
            RootView()
                .environmentObject(appState)
                .environmentObject(appState.authState)
                .environmentObject(appState.leadState)
                .environmentObject(appState.taskState)
        }
    }
}

// MARK: - 根视图

/// 根视图 - 根据认证状态显示登录或主界面
struct RootView: View {
    
    /// 环境对象 - 认证状态
    @EnvironmentObject var authState: AuthState
    
    var body: some View {
        Group {
            // 根据是否已登录显示对应界面
            if authState.isAuthenticated {
                // 已登录 - 显示主界面
                MainView()
            } else {
                // 未登录 - 显示登录界面
                LoginView()
            }
        }
        .animation(.easeInOut(duration: 0.3), value: authState.isAuthenticated)
    }
}

// MARK: - 应用状态

/// 应用状态管理器
/// 负责管理整个应用的状态，包括认证、商机、任务等
class AppState: ObservableObject {
    
    /// 认证状态
    @Published var authState = AuthState()
    
    /// 商机状态
    @Published var leadState = LeadState()
    
    /// 任务状态
    @Published var taskState = TaskState()
    
    /// 初始化
    init() {
        // 检查本地存储的认证信息
        checkStoredAuth()
    }
    
    /// 检查本地存储的认证信息
    private func checkStoredAuth() {
        // 从Keychain获取Token
        if let token = KeychainManager.shared.getToken() {
            // 验证Token有效性
            if AuthService.shared.validateToken(token) {
                authState.isAuthenticated = true
                authState.token = token
                // 加载用户信息
                loadUserInfo()
            }
        }
    }
    
    /// 加载用户信息
    private func loadUserInfo() {
        // 从本地存储加载用户信息
        if let userData = UserDefaults.standard.data(forKey: "currentUser"),
           let user = try? JSONDecoder().decode(User.self, from: userData) {
            authState.currentUser = user
        }
    }
}

/// 认证状态
class AuthState: ObservableObject {
    
    /// 是否已认证
    @Published var isAuthenticated: Bool = false
    
    /// 认证Token
    @Published var token: String?
    
    /// 当前用户
    @Published var currentUser: User?
}

/// 商机状态
class LeadState: ObservableObject {
    
    /// 商机列表
    @Published var leads: [Lead] = []
    
    /// 当前选中的商机
    @Published var selectedLead: Lead?
    
    /// 加载状态
    @Published var isLoading: Bool = false
    
    /// 错误信息
    @Published var errorMessage: String?
}

/// 任务状态
class TaskState: ObservableObject {
    
    /// 任务列表
    @Published var tasks: [Task] = []
    
    /// 当前选中的任务
    @Published var selectedTask: Task?
    
    /// 加载状态
    @Published var isLoading: Bool = false
    
    /// 错误信息
    @Published var errorMessage: String?
}

// MARK: - 预览

#if DEBUG
struct BxAppApp_Previews: PreviewProvider {
    static var previews: some View {
        RootView()
            .environmentObject(AuthState())
            .environmentObject(LeadState())
            .environmentObject(TaskState())
    }
}
#endif
