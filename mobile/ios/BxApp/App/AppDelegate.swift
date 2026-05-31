//
//  AppDelegate.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  应用委托 - 处理UIApplication生命周期事件
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import UIKit
import UserNotifications

/// AppDelegate
/// 
/// UIApplication的代理类，负责处理应用生命周期事件：
/// - 应用启动和终止
/// - 应用进入前台和后台
/// - 远程通知处理
/// - 应用配置初始化
class AppDelegate: NSObject, UIApplicationDelegate {
    
    // MARK: - 应用生命周期
    
    /// 应用启动完成 - 只会调用一次
    /// - Parameters:
    ///   - application: UIApplication实例
    ///   - launchOptions: 启动选项
    /// - Returns: 是否处理了启动
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        
        // 1. 配置应用外观
        configureAppearance()
        
        // 2. 初始化网络服务
        setupNetworkService()
        
        // 3. 配置推送通知
        setupPushNotifications()
        
        // 4. 配置设备标识
        setupDeviceIdentifier()
        
        // 5. 初始化错误处理
        setupErrorHandling()
        
        // 6. 配置日志系统
        setupLogging()
        
        // 7. 检查应用更新
        checkForUpdates()
        
        // 8. 恢复未完成的同步任务
        resumePendingSyncTasks()
        
        // 应用启动成功
        print("[BxApp] 应用启动完成")
        return true
    }
    
    /// 应用即将进入前台
    func applicationWillEnterForeground(_ application: UIApplication) {
        // 刷新认证状态
        refreshAuthState()
        
        // 重新同步数据
        syncData()
        
        // 更新Badge数量
        updateBadgeCount()
        
        print("[BxApp] 应用即将进入前台")
    }
    
    /// 应用进入后台
    func applicationDidEnterBackground(_ application: UIApplication) {
        // 保存当前状态
        saveCurrentState()
        
        // 取消不必要的网络请求
        cancelUnnecessaryRequests()
        
        // 提交待发送的埋点数据
        submitPendingAnalytics()
        
        print("[BxApp] 应用进入后台")
    }
    
    /// 应用即将终止
    func applicationWillTerminate(_ application: UIApplication) {
        // 保存关键数据
        saveCriticalData()
        
        // 清理临时文件
        cleanupTemporaryFiles()
        
        print("[BxApp] 应用即将终止")
    }
    
    // MARK: - 远程通知
    
    /// 注册远程通知成功
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        // 将Token转换为字符串
        let tokenParts = deviceToken.map { data in String(format: "%02.2hhx", data) }
        let token = tokenParts.joined()
        
        // 保存设备Token
        DeviceManager.shared.saveDeviceToken(token)
        
        // 将Token发送到服务器
        sendDeviceTokenToServer(token)
        
        print("[BxApp] 设备Token: \(token)")
    }
    
    /// 注册远程通知失败
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("[BxApp] 注册远程通知失败: \(error.localizedDescription)")
    }
    
    /// 收到远程通知
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        // 处理通知内容
        handleRemoteNotification(userInfo, completionHandler: completionHandler)
    }
    
    // MARK: - Scene配置
    
    /// 配置Scene代理
    /// - Parameters:
    ///   - application: UIApplication实例
    ///   - configurationForConnecting: 连接场景配置
    ///   - options: 场景连接选项
    /// - Returns: UISceneConfiguration实例
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        // 创建场景配置
        let sceneConfig = UISceneConfiguration(
            name: "Default Configuration",
            sessionRole: connectingSceneSession.role
        )
        sceneConfig.delegateClass = SceneDelegate.self
        
        return sceneConfig
    }
    
    // MARK: - 私有方法
    
    /// 配置应用外观
    private func configureAppearance() {
        // 配置导航栏外观
        let navBarAppearance = UINavigationBarAppearance()
        navBarAppearance.configureWithOpaqueBackground()
        navBarAppearance.backgroundColor = UIColor.systemBackground
        navBarAppearance.titleTextAttributes = [
            .foregroundColor: UIColor.label,
            .font: UIFont.systemFont(ofSize: 17, weight: .semibold)
        ]
        
        UINavigationBar.appearance().standardAppearance = navBarAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navBarAppearance
        UINavigationBar.appearance().compactAppearance = navBarAppearance
        
        // 配置标签栏外观
        let tabBarAppearance = UITabBarAppearance()
        tabBarAppearance.configureWithOpaqueBackground()
        tabBarAppearance.backgroundColor = UIColor.systemBackground
        
        UITabBar.appearance().standardAppearance = tabBarAppearance
        UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
        
        // 配置全局tint颜色
        UIView.appearance(whenContainedInInstancesOf: [UIAlertController.self]).tintColor = UIColor(named: "AccentColor")
    }
    
    /// 初始化网络服务
    private func setupNetworkService() {
        // 配置网络请求拦截器
        NetworkService.shared.setupInterceptors()
        
        // 配置请求超时时间
        NetworkService.shared.setTimeout(interval: 30)
        
        print("[BxApp] 网络服务初始化完成")
    }
    
    /// 配置推送通知
    private func setupPushNotifications() {
        // 请求通知权限
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .badge, .sound]
        ) { granted, error in
            if granted {
                // 注册远程通知
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
                print("[BxApp] 通知权限已授权")
            } else if let error = error {
                print("[BxApp] 通知权限请求失败: \(error.localizedDescription)")
            }
        }
        
        // 设置通知代理
        UNUserNotificationCenter.current().delegate = self
    }
    
    /// 配置设备标识
    private func setupDeviceIdentifier() {
        // 获取或生成设备唯一标识
        let deviceId = DeviceManager.shared.getDeviceIdentifier()
        
        // 保存到Keychain
        KeychainManager.shared.saveDeviceId(deviceId)
        
        print("[BxApp] 设备标识: \(deviceId)")
    }
    
    /// 初始化错误处理
    private func setupErrorHandling() {
        // 设置全局未捕获异常处理器
        NSSetUncaughtExceptionHandler { exception in
            // 记录异常信息
            let errorInfo: [String: Any] = [
                "name": exception.name.rawValue,
                "reason": exception.reason ?? "Unknown",
                "callStack": exception.callStackSymbols.joined(separator: "\n")
            ]
            
            // 发送错误日志到服务器
            AnalyticsService.shared.logError(errorInfo)
            
            print("[BxApp] 未捕获异常: \(errorInfo)")
        }
    }
    
    /// 配置日志系统
    private func setupLogging() {
        #if DEBUG
        // Debug模式下启用详细日志
        print("[BxApp] Debug模式已启用")
        #else
        // Release模式下记录关键日志
        print("[BxApp] Release模式已启用")
        #endif
    }
    
    /// 检查应用更新
    private func checkForUpdates() {
        print("[AppDelegate] 检查应用更新(预留)")
        // CheckUpdateService.shared.checkForUpdates()
    }
    
    /// 恢复未完成的同步任务
    private func resumePendingSyncTasks() {
        print("[AppDelegate] 离线同步恢复(预留)")
        // SyncService.shared.resumePendingTasks()
    }
    
    /// 刷新认证状态
    private func refreshAuthState() {
        // 验证Token有效性
        guard let token = KeychainManager.shared.getToken() else { return }
        
        AuthService.shared.validateToken(token) { result in
            switch result {
            case .success(let isValid):
                if !isValid {
                    // Token失效，清除认证状态
                    KeychainManager.shared.deleteToken()
                    NotificationCenter.default.post(name: .authStateChanged, object: nil)
                }
            case .failure:
                break
            }
        }
    }
    
    /// 同步数据
    private func syncData() {
        // 同步商机数据
        // LeadRepository.shared.syncLeads()
        
        // 同步任务数据
        // TaskRepository.shared.syncTasks()
    }
    
    /// 更新Badge数量
    private func updateBadgeCount() {
        // 获取未读消息数量
        let unreadCount = NotificationService.shared.getUnreadCount()
        
        DispatchQueue.main.async {
            UIApplication.shared.applicationIconBadgeNumber = unreadCount
        }
    }
    
    /// 保存当前状态
    private func saveCurrentState() {
        // 保存用户偏好设置
        UserDefaults.standard.synchronize()
    }
    
    /// 取消不必要的网络请求
    private func cancelUnnecessaryRequests() {
        // 取消非关键的加载请求
        NetworkService.shared.cancelLowPriorityRequests()
    }
    
    /// 提交待发送的埋点数据
    private func submitPendingAnalytics() {
        // 提交待发送的埋点
        AnalyticsService.shared.submitPendingEvents()
    }
    
    /// 保存关键数据
    private func saveCriticalData() {
        // 确保关键数据已保存
        KeychainManager.shared.synchronize()
        UserDefaults.standard.synchronize()
    }
    
    /// 清理临时文件
    private func cleanupTemporaryFiles() {
        // 清理图片缓存
        ImageCacheManager.shared.clearExpiredCache()
        
        // 清理临时下载文件
        FileManager.default.clearTemporaryDirectory()
    }
    
    /// 将设备Token发送到服务器
    private func sendDeviceTokenToServer(_ token: String) {
        print("[AppDelegate] 设备Token上传(预留): \(token)")
    }
    
    /// 处理远程通知
    private func handleRemoteNotification(
        _ userInfo: [AnyHashable: Any],
        completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        // 解析通知类型
        guard let type = userInfo["type"] as? String else {
            completionHandler(.noData)
            return
        }
        
        switch type {
        case "lead_update":
            // 商机更新通知
            handleLeadUpdateNotification(userInfo)
            completionHandler(.newData)
            
        case "task_update":
            // 任务更新通知
            handleTaskUpdateNotification(userInfo)
            completionHandler(.newData)
            
        case "message":
            // 新消息通知
            handleMessageNotification(userInfo)
            completionHandler(.newData)
            
        default:
            completionHandler(.noData)
        }
    }
    
    /// 处理商机更新通知
    private func handleLeadUpdateNotification(_ userInfo: [AnyHashable: Any]) {
        // 刷新商机数据
        NotificationCenter.default.post(name: .leadUpdated, object: nil)
    }
    
    /// 处理任务更新通知
    private func handleTaskUpdateNotification(_ userInfo: [AnyHashable: Any]) {
        // 刷新任务数据
        NotificationCenter.default.post(name: .taskUpdated, object: nil)
    }
    
    /// 处理消息通知
    private func handleMessageNotification(_ userInfo: [AnyHashable: Any]) {
        // 更新消息Badge
        updateBadgeCount()
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension AppDelegate: UNUserNotificationCenterDelegate {
    
    /// 应用在前台时收到通知
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // 在前台也显示通知
        completionHandler([.banner, .badge, .sound])
    }
    
    /// 用户点击通知
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        
        // 根据通知类型处理
        if let type = userInfo["type"] as? String {
            handleNotificationTap(type: type, userInfo: userInfo)
        }
        
        completionHandler()
    }
    
    /// 处理通知点击
    private func handleNotificationTap(type: String, userInfo: [AnyHashable: Any]) {
        switch type {
        case "lead_detail":
            // 跳转到商机详情
            if let leadId = userInfo["lead_id"] as? String {
                NotificationCenter.default.post(
                    name: .navigateToLeadDetail,
                    object: nil,
                    userInfo: ["leadId": leadId]
                )
            }
            
        case "task_detail":
            // 跳转到任务详情
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
}

// MARK: - 通知名称扩展

extension Notification.Name {
    /// 认证状态变更通知
    static let authStateChanged = Notification.Name("authStateChanged")
    
    /// 商机更新通知
    static let leadUpdated = Notification.Name("leadUpdated")
    
    /// 任务更新通知
    static let taskUpdated = Notification.Name("taskUpdated")
    
    /// 导航到商机详情
    static let navigateToLeadDetail = Notification.Name("navigateToLeadDetail")
    
    /// 导航到任务详情
    static let navigateToTaskDetail = Notification.Name("navigateToTaskDetail")
}

// MARK: - 服务扩展

/// 分析服务 - 用于埋点
class AnalyticsService {
    static let shared = AnalyticsService()
    
    /// 记录错误
    func logError(_ info: [String: Any]) {
        print("[AnalyticsService] 错误日志记录(预留): \(info)")
    }
    
    /// 提交待发送事件
    func submitPendingEvents() {
        print("[AnalyticsService] 事件提交(预留)")
    }
}

/// 图片缓存管理
class ImageCacheManager {
    static let shared = ImageCacheManager()
    
    /// 清理过期缓存
    func clearExpiredCache() {
        print("[ImageCacheManager] 缓存清理(预留)")
    }
}

/// 文件管理器扩展
extension FileManager {
    /// 清理临时目录
    func clearTemporaryDirectory() {
        let tempDir = temporaryDirectory
        if let files = try? contentsOfDirectory(atPath: tempDir.path) {
            for file in files {
                let filePath = tempDir.appendingPathComponent(file)
                try? removeItem(at: filePath)
            }
        }
    }
}
