//
//  Color+Extension.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  Color扩展
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

// MARK: - Color扩展

extension Color {
    // MARK: - 品牌色
    
    /// 主色调
    static let primaryColor = Color.blue
    
    /// 次要色调
    static let secondaryColor = Color.gray
    
    /// 成功色
    static let successColor = Color.green
    
    /// 警告色
    static let warningColor = Color.orange
    
    /// 错误色
    static let errorColor = Color.red
    
    // MARK: - 商机状态色
    
    /// 高意向色
    static let highIntentionColor = Color.red
    
    /// 中意向色
    static let mediumIntentionColor = Color.orange
    
    /// 低意向色
    static let lowIntentionColor = Color.blue
    
    // MARK: - 任务状态色
    
    /// 待执行色
    static let pendingTaskColor = Color.gray
    
    /// 执行中色
    static let runningTaskColor = Color.blue
    
    /// 已完成色
    static let completedTaskColor = Color.green
    
    /// 失败色
    static let failedTaskColor = Color.red
    
    // MARK: - 十六进制初始化
    
    /// 从十六进制字符串初始化颜色
    /// - Parameter hex: 十六进制颜色值，如 "#FF5733" 或 "FF5733"
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// MARK: - 从字符串初始化颜色

extension Color {
    /// 从颜色名称字符串初始化颜色
    /// - Parameter colorName: 颜色名称，如 "systemBlue", "red", "blue" 等
    init(_ colorName: String) {
        switch colorName {
        case "systemBlue": self = .blue
        case "systemRed": self = .red
        case "systemGreen": self = .green
        case "systemOrange": self = .orange
        case "systemPurple": self = .purple
        case "systemYellow": self = .yellow
        case "systemPink": self = .pink
        case "systemTeal": self = .teal
        case "systemIndigo": self = .indigo
        case "systemCyan": self = .cyan
        case "systemMint": self = .mint
        case "systemBrown": self = .brown
        case "systemGray": self = .gray
        case "black": self = .black
        case "white": self = .white
        case "clear": self = .clear
        default: self = .blue
        }
    }
}

// MARK: - UIColor扩展

extension UIColor {
    /// 从十六进制字符串初始化颜色
    convenience init(hex: String) {
        let color = Color(hex: hex)
        self.init(color)
    }
}
