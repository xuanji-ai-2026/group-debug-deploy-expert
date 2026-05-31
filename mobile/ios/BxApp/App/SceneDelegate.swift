//
//  SceneDelegate.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  场景委托 - 处理UIScene生命周期事件
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import UIKit
import SwiftUI

/// SceneDelegate
/// 
/// UIScene代理类，负责处理场景生命周期事件：
/// - 场景连接和断开
/// - 场景激活和停用
/// - 场景大小变化
/// - 多任务切换
class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    
    // MARK: - 属性
    
    /// 窗口引用 - 用于SwiftUI视图展示
    var window: UIWindow?
    
    /// 场景标识
    var sceneId: String?
    
    /// 是否正在活动
    var isActive: Bool = false
    
    /// 最后活跃时间
    var lastActiveTime: Date?
    
    // MARK: - 场景生命周期
    
    /// 场景即将连接 - 应用启动或从后台恢复时调用
    /// - Parameters:
    ///   - scene: UIScene实例
    ///   - session: UISceneSession实例
    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        // 确保是UIWindowScene
        guard let windowScene = scene as? UIWindowScene else { return }
        
        // 生成场景标识
        sceneId = session.persistentIdentifier
        
        // 配置窗口
        let window = UIWindow(windowScene: windowScene)
        self.window = window
        
        // 设置根视图
        setupRootViewController(window: window)
        
        // 处理URL上下文
        if let urlContext = connectionOptions.urlContexts.first {
            handleURL(urlContext.url)
        }
        
        // 处理通知响应
        if let notificationResponse = connectionOptions.notificationResponse {
            handleNotificationResponse(notificationResponse)
        }
        
        // 配置场景
        configureScene(scene)
        
        print("[BxApp] 场景已连接: \(session.persistentIdentifier)")
    }
    
    /// 场景断开连接
    /// - Parameters:
    ///   - scene: UIScene实例
    ///   - session: UISceneSession实例
    func sceneDidDisconnect(_ scene: UIScene) {
        // 保存场景状态
        saveSceneState(scene)
        
        // 清理场景相关资源
        cleanupSceneResources()
        
        print("[BxApp] 场景已断开连接")
    }
    
    /// 场景成为活跃状态
    /// - Parameter scene: UIScene实例
    func sceneDidBecomeActive(_ scene: UIScene) {
        // 标记为活跃
        isActive = true
        lastActiveTime = Date()
        
        // 重新开始动画
        UIApplication.shared.beginIgnoringInteractionEvents()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            UIApplication.shared.endIgnoringInteractionEvents()
        }
        
        // 刷新数据
        refreshData()
        
        // 重置Badge
        UIApplication.shared.applicationIconBadgeNumber = 0
        
        // 更新活跃时间记录
        updateActiveTimeRecord()
        
        print("[BxApp] 场景已激活")
    }
    
    /// 场景变为非活跃状态
    /// - Parameter scene: UIScene实例
    func sceneWillResignActive(_ scene: UIScene) {
        // 标记为非活跃
        isActive = false
        
        // 保存当前状态
        saveCurrentState()
        
        print("[BxApp] 场景即将变为非活跃")
    }
    
    /// 场景即将进入前台
    /// - Parameter scene: UIScene实例
    func sceneWillEnterForeground(_ scene: UIScene) {
        // 准备UI刷新
        prepareUIForForeground()
        
        // 检查认证状态
        checkAuthenticationState()
        
        // 加载新数据
        loadPendingData()
        
        print("[BxApp] 场景即将进入前台")
    }
    
    /// 场景进入后台
    /// - Parameter scene: UIScene实例
    func sceneDidEnterBackground(_ scene: UIScene) {
        // 保存数据
        saveDataForBackground()
        
        // 降低网络请求优先级
        lowerNetworkPriority()
        
        // 记录进入后台时间
        recordBackgroundEntryTime()
        
        print("[BxApp] 场景已进入后台")
    }
    
    // MARK: - 场景交互
    
    /// 收到URL请求
    /// - Parameters:
    ///   - scene: UIScene实例
    ///   - URL: 请求的URL
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        // 处理URL
        guard let url = URLContexts.first?.url else { return }
        handleURL(url)
    }
    
    /// 场景持续性能监控
    /// - Parameters:
    ///   - scene: UIScene实例
    ///   - didEncounterProposal: 性能建议
    func scene(
        _ scene: UIScene,
        didEncounterPerfidicWarning warning: UIPerfidicWarning
    ) {
        // 处理性能警告
        handlePerformanceWarning(warning)
    }
    
    // MARK: - 状态恢复
    
    /// 请求场景状态保存
    /// - Parameters:
    ///   - scene: UIScene实例
    ///   - disturbance: 扰动量
    func scene(
        _ scene: UIScene,
        stateRestorationActivityFor building: UIScene.Title: String
    ) {
        print("[SceneDelegate] 状态恢复(预留)")
    }
    
    // MARK: - 私有方法
    
    /// 设置根视图控制器
    private func setupRootViewController(window: UIWindow) {
        // 创建根视图
        let rootView = RootView()
            .environmentObject(AppState().authState)
            .environmentObject(AppState().leadState)
            .environmentObject(AppState().taskState)
        
        // 将SwiftUI视图包装为UIHostingController
        let hostingController = UIHostingController(rootView: rootView)
        
        // 设置为窗口根视图
        window.rootViewController = hostingController
        window.makeKeyAndVisible()
    }
    
    /// 配置场景
    private func configureScene(_ scene: UIScene) {
        guard let windowScene = scene as? UIWindowScene else { return }
        
        // 配置窗口尺寸
        #if targetEnvironment(macCatalyst)
        // Mac Catalyst配置
        windowScene.sizeRestrictions?.minimumSize = CGSize(width: 800, height: 600)
        #endif
    }
    
    /// 处理URL
    private func handleURL(_ url: URL) {
        // 解析URL
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: true) else {
            return
        }
        
        // 处理自定义协议
        if url.scheme == "beijixing" {
            handleCustomScheme(components)
            return
        }
        
        // 处理https链接
        if url.scheme == "https" || url.scheme == "http" {
            handleWebURL(components)
            return
        }
    }
    
    /// 处理自定义协议
    private func handleCustomScheme(_ components: URLComponents) {
        guard let host = components.host else { return }
        
        switch host {
        case "lead":
            // 商机详情
            if let leadId = components.queryItems?.first(where: { $0.name == "id" })?.value {
                NotificationCenter.default.post(
                    name: .navigateToLeadDetail,
                    object: nil,
                    userInfo: ["leadId": leadId]
                )
            }
            
        case "task":
            // 任务详情
            if let taskId = components.queryItems?.first(where: { $0.name == "id" })?.value {
                NotificationCenter.default.post(
                    name: .navigateToTaskDetail,
                    object: nil,
                    userInfo: ["taskId": taskId]
                )
            }
            
        case "login":
            // 跳转登录
            NotificationCenter.default.post(name: .authStateChanged, object: nil)
            
        default:
            break
        }
    }
    
    /// 处理网页URL
    private func handleWebURL(_ components: URLComponents) {
        // 处理深度链接
        let path = components.path
        
        if path.hasPrefix("/lead/") {
            // 商机详情页
            let leadId = String(path.dropFirst("/lead/".count))
            NotificationCenter.default.post(
                name: .navigateToLeadDetail,
                object: nil,
                userInfo: ["leadId": leadId]
            )
        } else if path.hasPrefix("/task/") {
            // 任务详情页
            let taskId = String(path.dropFirst("/task/".count))
            NotificationCenter.default.post(
                name: .navigateToTaskDetail,
                object: nil,
                userInfo: ["taskId": taskId]
            )
        }
    }
    
    /// 处理通知响应
    private func handleNotificationResponse(_ response: UNNotificationResponse) {
        let userInfo = response.notification.request.content.userInfo
        
        if let type = userInfo["type"] as? String {
            handleNotificationTap(type: type, userInfo: userInfo)
        }
    }
    
    /// 处理通知点击
    private func handleNotificationTap(type: String, userInfo: [AnyHashable: Any]) {
        switch type {
        case "lead_detail":
            if let leadId = userInfo["lead_id"] as? String {
                NotificationCenter.default.post(
                    name: .navigateToLeadDetail,
                    object: nil,
                    userInfo: ["leadId": leadId]
                )
            }
            
        case "task_detail":
            if let taskId = userInfo["task_id"] as? String {
                NotificationCenter.default.post(
                    name: .navigateToTaskDetail,
                    object: nil,
                    userInfo: ["taskId": taskId]
                )
            }
            
        default:
            break
        }
    }
    
    /// 保存场景状态
    private func saveSceneState(_ scene: UIScene) {
        print("[SceneDelegate] 状态保存(预留)")
    }
    
    /// 清理场景资源
    private func cleanupSceneResources() {
        // 清理缓存
        URLCache.shared.removeAllCachedResponses()
    }
    
    /// 刷新数据
    private func refreshData() {
        // 刷新商机数据
        NotificationCenter.default.post(name: .leadUpdated, object: nil)
        
        // 刷新任务数据
        NotificationCenter.default.post(name: .taskUpdated, object: nil)
    }
    
    /// 更新活跃时间记录
    private func updateActiveTimeRecord() {
        UserDefaults.standard.set(Date(), forKey: "lastActiveTime")
    }
    
    /// 为后台保存数据
    private func saveDataForBackground() {
        // 同步保存用户数据
        UserDefaults.standard.synchronize()
    }
    
    /// 降低网络优先级
    private func lowerNetworkPriority() {
        // 取消非关键请求
        URLSession.shared.getTasksWithCompletionHandler { dataTasks, _, _ in
            for task in dataTasks {
                if task.priority < URLSessionTask.highPriority {
                    task.suspend()
                }
            }
        }
    }
    
    /// 记录进入后台时间
    private func recordBackgroundEntryTime() {
        UserDefaults.standard.set(Date(), forKey: "backgroundEntryTime")
    }
    
    /// 为前台准备UI
    private func prepareUIForForeground() {
        // 恢复被挂起的网络请求
        URLSession.shared.getTasksWithCompletionHandler { _, _, uploadTasks in
            for task in uploadTasks {
                task.resume()
            }
        }
    }
    
    /// 检查认证状态
    private func checkAuthenticationState() {
        // 检查Token有效性
        if let token = KeychainManager.shared.getToken() {
            if !AuthService.shared.validateToken(token) {
                // Token失效
                NotificationCenter.default.post(name: .authStateChanged, object: nil)
            }
        }
    }
    
    /// 加载待处理数据
    private func loadPendingData() {
        print("[SceneDelegate] 待处理数据加载(预留)")
    }
    
    /// 保存当前状态
    private func saveCurrentState() {
        // 保存当前界面状态
        UserDefaults.standard.synchronize()
    }
    
    /// 处理性能警告
    private func handlePerformanceWarning(_ warning: UIPerfidicWarning) {
        switch warning {
        case .lowMemory:
            // 内存不足
            clearMemoryCache()
            
        case .lowPower:
            // 低电量模式
            enableLowPowerMode()
            
        @unknown default:
            break
        }
    }
    
    /// 清理内存缓存
    private func clearMemoryCache() {
        // 清理图片缓存
        URLCache.shared.removeAllCachedResponses()
        
        // 清理内存缓存
        URLCache.shared.diskCacheSize = 0
    }
    
    /// 启用低功耗模式
    private func enableLowPowerMode() {
        // 降低动画效果
        // 减少网络请求频率
    }
}

// MARK: - UNNotificationResponse扩展

extension UNNotificationResponse {
    /// 获取通知类型
    var notificationType: String? {
        notification.request.content.userInfo["type"] as? String
    }
}
