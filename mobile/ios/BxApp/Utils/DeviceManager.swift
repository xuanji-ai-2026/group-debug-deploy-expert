//
//  DeviceManager.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  设备管理器 - 设备信息获取和管理
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import UIKit

/// 设备管理器
/// 
/// 提供设备信息获取、唯一标识生成等功能。
class DeviceManager {
    
    // MARK: - 单例
    
    static let shared = DeviceManager()
    
    // MARK: - 属性
    
    /// 缓存的设备ID
    private var cachedDeviceId: String?
    
    /// 缓存的设备Token
    private var cachedDeviceToken: String?
    
    // MARK: - 初始化
    
    private init() {}
    
    // MARK: - 设备标识
    
    /// 获取设备唯一标识
    /// 
    /// 如果已存在则返回缓存值，否则生成新的UUID并保存。
    /// - Returns: 设备唯一标识符
    func getDeviceIdentifier() -> String {
        if let cached = cachedDeviceId {
            return cached
        }
        
        // 尝试从钥匙串获取
        if let stored = KeychainManager.shared.getDeviceId() {
            cachedDeviceId = stored
            return stored
        }
        
        // 生成新的
        let newId = UUID().uuidString
        cachedDeviceId = newId
        
        // 保存到钥匙串
        KeychainManager.shared.saveDeviceId(newId)
        
        return newId
    }
    
    // MARK: - 设备Token
    
    /// 保存设备Token（推送通知用）
    /// - Parameter token: 设备Token
    func saveDeviceToken(_ token: String) {
        cachedDeviceToken = token
        UserDefaults.standard.set(token, forKey: "deviceToken")
    }
    
    /// 获取设备Token
    /// - Returns: 设备Token
    func getDeviceToken() -> String? {
        if let cached = cachedDeviceToken {
            return cached
        }
        
        return UserDefaults.standard.string(forKey: "deviceToken")
    }
    
    // MARK: - 设备信息
    
    /// 获取设备名称
    /// - Returns: 设备名称（如 "iPhone 14 Pro"）
    var deviceName: String {
        return UIDevice.current.name
    }
    
    /// 获取设备型号
    /// - Returns: 设备型号标识符（如 "iPhone14,2"）
    var deviceModel: String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let identifier = machineMirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return identifier }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }
        return identifier
    }
    
    /// 获取用户友好的设备名称
    /// - Returns: 用户友好的设备名称
    var friendlyDeviceName: String {
        return UIDevice.current.localizedModel
    }
    
    /// 获取操作系统版本
    /// - Returns: 操作系统版本（如 "iOS 16.0"）
    var osVersion: String {
        return "iOS \(UIDevice.current.systemVersion)"
    }
    
    /// 获取App版本
    /// - Returns: App版本（如 "1.0.0"）
    var appVersion: String {
        return Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }
    
    /// 获取构建号
    /// - Returns: 构建号（如 "1"）
    var buildNumber: String {
        return Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
    }
    
    /// 获取屏幕尺寸
    /// - Returns: 屏幕尺寸（如 "393x852"）
    var screenSize: String {
        let screen = UIScreen.main.bounds
        return "\(Int(screen.width))x\(Int(screen.height))"
    }
    
    /// 获取屏幕比例
    /// - Returns: 屏幕比例（如 3.0）
    var screenScale: CGFloat {
        return UIScreen.main.scale
    }
    
    /// 获取设备信息字典
    /// - Returns: 设备信息字典
    func getDeviceInfo() -> [String: Any] {
        return [
            "deviceId": getDeviceIdentifier(),
            "deviceName": deviceName,
            "deviceModel": deviceModel,
            "friendlyModel": friendlyDeviceName,
            "osVersion": osVersion,
            "appVersion": appVersion,
            "buildNumber": buildNumber,
            "screenSize": screenSize,
            "screenScale": screenScale,
            "bundleId": Bundle.main.bundleIdentifier ?? ""
        ]
    }
    
    /// 获取设备信息JSON字符串
    /// - Returns: JSON字符串
    func getDeviceInfoJSON() -> String {
        let info = getDeviceInfo()
        
        guard let data = try? JSONSerialization.data(withJSONObject: info, options: .prettyPrinted),
              let json = String(data: data, encoding: .utf8) else {
            return "{}"
        }
        
        return json
    }
    
    // MARK: - 设备能力检测
    
    /// 是否支持Face ID
    var supportsFaceID: Bool {
        return LAContext().canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil)
    }
    
    /// 是否支持Touch ID
    var supportsTouchID: Bool {
        let context = LAContext()
        var error: NSError?
        return context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    }
    
    /// 是否是模拟器
    var isSimulator: Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }
    
    /// 是否是iPad
    var isIPad: Bool {
        return UIDevice.current.userInterfaceIdiom == .pad
    }
    
    /// 是否是iPhone
    var isIPhone: Bool {
        return UIDevice.current.userInterfaceIdiom == .phone
    }
    
    // MARK: - 设备指纹
    
    /// 获取简化的设备指纹（用于日志标识）
    var simplifiedFingerprint: String {
        let model = deviceModel
        let os = UIDevice.current.systemVersion
        let app = appVersion
        return "\(model)_iOS\(os)_\(app)"
    }
}

// MARK: - LocalAuthentication导入

import LocalAuthentication
