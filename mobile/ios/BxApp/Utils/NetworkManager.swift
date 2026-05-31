//
//  NetworkManager.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  网络管理器 - 网络配置和工具方法
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Network

/// 网络管理器
/// 
/// 提供网络配置、网络状态查询等工具方法。
class NetworkManager {
    
    // MARK: - 单例
    
    static let shared = NetworkManager()
    
    // MARK: - 属性
    
    /// 网络路径监控器
    private let monitor = NWPathMonitor()
    
    /// 当前网络类型
    private(set) var currentNetworkType: NetworkType = .unknown
    
    /// 当前连接状态
    private(set) var isConnected: Bool = false
    
    /// 队列
    private let queue = DispatchQueue(label: "com.beijixing.networkmanager")
    
    // MARK: - 初始化
    
    private init() {
        setupMonitor()
    }
    
    // MARK: - 公开方法
    
    /// 获取当前网络类型描述
    var networkTypeDescription: String {
        return currentNetworkType.rawValue
    }
    
    /// 检查网络是否可用
    func checkConnection() -> Bool {
        return isConnected
    }
    
    /// 等待网络连接
    /// - Parameter timeout: 超时时间（秒）
    /// - Returns: 是否连接成功
    func waitForConnection(timeout: TimeInterval = 10) async -> Bool {
        if isConnected { return true }
        
        return await withCheckedContinuation { continuation in
            var resumed = false
            
            // 设置超时
            queue.asyncAfter(deadline: .now() + timeout) {
                if !resumed {
                    resumed = true
                    continuation.resume(returning: false)
                }
            }
            
            // 监听连接
            monitor.pathUpdateHandler = { path in
                if !resumed && path.status == .satisfied {
                    resumed = true
                    continuation.resume(returning: true)
                }
            }
        }
    }
    
    // MARK: - 私有方法
    
    /// 设置网络监控
    private func setupMonitor() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.isConnected = path.status == .satisfied
                
                if path.usesInterfaceType(.wifi) {
                    self?.currentNetworkType = .wifi
                } else if path.usesInterfaceType(.cellular) {
                    self?.currentNetworkType = .cellular
                } else if path.usesInterfaceType(.wiredEthernet) {
                    self?.currentNetworkType = .ethernet
                } else {
                    self?.currentNetworkType = .unknown
                }
            }
        }
        
        monitor.start(queue: queue)
    }
}

// MARK: - 网络类型扩展

extension NetworkManager.NetworkType {
    /// 简化的网络类型名称
    var shortName: String {
        switch self {
        case .wifi: return "WiFi"
        case .cellular: return "移动网络"
        case .ethernet: return "有线网络"
        case .unknown: return "未知"
        }
    }
}
