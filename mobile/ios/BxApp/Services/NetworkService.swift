//
//  NetworkService.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  网络服务 - 网络状态监控、请求管理
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Network
import Combine

/// 网络服务
/// 
/// 提供网络状态监控、连接管理等功能。
class NetworkService {
    
    // MARK: - 单例
    
    static let shared = NetworkService()
    
    // MARK: - 属性
    
    /// 网络路径监控器
    private let monitor = NWPathMonitor()
    
    /// 网络队列
    private let queue = DispatchQueue(label: "com.beijixing.network")
    
    /// 当前网络状态
    @Published private(set) var networkStatus: NetworkStatus = .unknown
    
    /// 当前网络类型
    @Published private(set) var currentNetworkType: NetworkType = .unknown
    
    /// 网络是否可用
    var isConnected: Bool {
        networkStatus == .satisfied
    }
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    private init() {
        startMonitoring()
    }
    
    // MARK: - 公开方法
    
    /// 配置请求拦截器
    func setupInterceptors() {
        // 配置Alamofire拦截器
        // 已在APIService中配置
    }
    
    /// 设置请求超时
    func setTimeout(interval: TimeInterval) {
        // 配置超时时间
        URLSession.shared.configuration.timeoutIntervalForRequest = interval
        URLSession.shared.configuration.timeoutIntervalForResource = interval * 2
    }
    
    /// 取消低优先级请求
    func cancelLowPriorityRequests() {
        // 取消标记为低优先级的请求
        URLSession.shared.getTasksWithCompletionHandler { dataTasks, uploadTasks, downloadTasks in
            for task in dataTasks {
                if task.priority < URLSessionTask.highPriority {
                    task.cancel()
                }
            }
        }
    }
    
    /// 获取当前网络类型
    func getCurrentNetworkType() -> NetworkType {
        return currentNetworkType
    }
    
    /// 等待网络连接
    func waitForConnection(timeout: TimeInterval = 30) async -> Bool {
        if isConnected { return true }
        
        // 最多等待指定时间
        let deadline = Date().addingTimeInterval(timeout)
        
        while Date() < deadline {
            if isConnected { return true }
            try? await Task.sleep(nanoseconds: 500_000_000) // 0.5秒
        }
        
        return false
    }
    
    // MARK: - 私有方法
    
    /// 开始网络监控
    private func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.updateNetworkStatus(path)
            }
        }
        monitor.start(queue: queue)
    }
    
    /// 更新网络状态
    private func updateNetworkStatus(_ path: NWPath) {
        // 更新连接状态
        switch path.status {
        case .satisfied:
            networkStatus = .satisfied
        case .unsatisfied:
            networkStatus = .unsatisfied
        case .requiresConnection:
            networkStatus = .requiresConnection
        @unknown default:
            networkStatus = .unknown
        }
        
        // 更新网络类型
        if path.usesInterfaceType(.wifi) {
            currentNetworkType = .wifi
        } else if path.usesInterfaceType(.cellular) {
            currentNetworkType = .cellular
        } else if path.usesInterfaceType(.wiredEthernet) {
            currentNetworkType = .ethernet
        } else {
            currentNetworkType = .unknown
        }
        
        // 发送网络状态变更通知
        NotificationCenter.default.post(
            name: .networkStatusChanged,
            object: nil,
            userInfo: [
                "status": networkStatus,
                "type": currentNetworkType
            ]
        )
    }
}

// MARK: - 网络状态

/// 网络状态
enum NetworkStatus {
    /// 未知
    case unknown
    
    /// 已连接
    case satisfied
    
    /// 未连接
    case unsatisfied
    
    /// 需要连接
    case requiresConnection
}

/// 网络类型
enum NetworkType: String {
    /// WiFi
    case wifi = "WiFi"
    
    /// 蜂窝网络
    case cellular = "Cellular"
    
    /// 以太网
    case ethernet = "Ethernet"
    
    /// 未知
    case unknown = "Unknown"
    
    /// 图标名称
    var iconName: String {
        switch self {
        case .wifi: return "wifi"
        case .cellular: return "antenna.radiowaves.left.and.right"
        case .ethernet: return "cable.connector"
        case .unknown: return "network.slash"
        }
    }
}

// MARK: - 通知名称

extension Notification.Name {
    /// 网络状态变更通知
    static let networkStatusChanged = Notification.Name("networkStatusChanged")
}
