//
//  SceneDelegate+OAuth.swift
//  BxApp (北极星AI - iOS端)
//
//  功能: 处理Universal Links OAuth回调
//  适用: iOS 13+ (SceneDelegate架构)
//
//  作者: 北极星AI团队
//  版本: v2.0 (2026-05-20)
//

import UIKit
import SwiftUI

extension SceneDelegate {
    
    /// 处理Universal Links回调（用户从社交APP返回北极星APP时触发）
    ///
    /// 回调URL格式:
    /// - 抖音: https://app.beijixing.com/oauth/callback/douyin?code=xxx&state=yyy
    /// - 小红书: https://app.beijixing.com/oauth/callback/xiaohongshu?code=xxx&state=yyy
    ///
    /// 触发场景:
    /// 1. 用户在北极星APP点击"绑定抖音"
    /// 2. 跳转到抖音授权页面（Safari/抖音APP）
    /// 3. 用户扫码确认授权
    /// 4. 抖音通过Universal Links跳转回北极星APP
    /// 5. 此方法被调用，提取code和state参数
    func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
        guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
              let url = userActivity.webpageURL else {
            return
        }
        
        print("🔗 [iOS] 收到Universal Links回调: \(url.absoluteString)")
        handleOAuthCallback(url: url)
    }
    
    /// 处理OAuth回调（核心逻辑）
    private func handleOAuthCallback(url: URL) {
        guard let windowScene = scene as? UIWindowScene,
              let rootViewController = windowScene.windows.first?.rootViewController else {
            print("❌ [iOS] 无法获取根视图控制器")
            return
        }
        
        // 显示加载指示器
        showLoadingIndicator(on: rootViewController, message: "正在完成授权...")
        
        Task { @MainActor in
            do {
                let result = try await OAuthService.shared.handleCallback(url: url)
                
                print("✅ [iOS] OAuth授权成功: \(result.nickname ?? "未知用户")")
                
                hideLoadingIndicator(from: rootViewController)
                
                showAlert(
                    on: rootViewController,
                    title: "🎉 绑定成功",
                    message: "已成功绑定\(Platform(rawValue: result.platform)?.displayName ?? result.platform)\n昵称: \(result.nickname ?? "未知")"
                )
                
                // 通知其他页面刷新数据
                NotificationCenter.default.post(name: .oauthBindingCompleted, object: nil, userInfo: [
                    "accountId": result.accountId,
                    "platform": result.platform,
                    "nickname": result.nickname as Any
                ])
                
            } catch {
                print("❌ [iOS] OAuth授权失败: \(error.localizedDescription)")
                
                hideLoadingIndicator(from: rootViewController)
                
                showAlert(
                    on: rootViewController,
                    title: "⚠️ 授权失败",
                    message: error.localizedDescription
                )
            }
        }
    }
    
    // MARK: - UI Helpers
    
    private func showLoadingIndicator(on viewController: UIViewController, message: String) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        
        let loadingIndicator = UIActivityIndicatorView(style: .large)
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.startAnimating()
        
        alert.view.addSubview(loadingIndicator)
        alert.view.centerXAnchor.constraint(equalTo: alert.view.centerXAnchor).isActive = true
        alert.view.centerYAnchor.constraint(equalTo: alert.view.centerYAnchor).isActive = true
        
        viewController.present(alert, animated: true)
    }
    
    private func hideLoadingIndicator(from viewController: UIViewController) {
        viewController.dismiss(animated: false)
    }
    
    private func showAlert(on viewController: UIViewController, title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "确定", style: .default))
        viewController.present(alert, animated: true)
    }
}

// MARK: - Notification Names

extension Notification.Name {
    static let oauthBindingCompleted = Notification.Name("oauthBindingCompleted")
}
