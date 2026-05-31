//
//  LoginViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  登录视图模型
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI

/// 登录视图模型
/// 
/// 负责处理用户登录逻辑，包括手机验证码登录、密码登录、第三方登录等。
/// 遵循MVVM架构模式，通过@Published属性实现数据绑定。
@MainActor
class LoginViewModel: ObservableObject {
    
    // MARK: - Published属性
    
    /// 手机号
    @Published var phone: String = ""
    
    /// 验证码
    @Published var verificationCode: String = ""
    
    /// 密码
    @Published var password: String = ""
    
    /// 确认密码（注册时使用）
    @Published var confirmPassword: String = ""
    
    /// 登录类型
    @Published var loginType: LoginType = .phoneCode
    
    /// 是否记住手机号
    @Published var rememberPhone: Bool = true
    
    /// 是否显示密码
    @Published var isPasswordVisible: Bool = false
    
    /// 是否同意用户协议
    @Published var agreeToTerms: Bool = false
    
    /// 是否正在加载
    @Published var isLoading: Bool = false
    
    /// 是否正在发送验证码
    @Published var isSendingCode: Bool = false
    
    /// 验证码倒计时
    @Published var codeCountdown: Int = 0
    
    /// 错误信息
    @Published var errorMessage: String?
    
    /// 是否显示错误
    @Published var showError: Bool = false
    
    // MARK: - Computed属性
    
    /// 手机号是否有效
    var isPhoneValid: Bool {
        let phoneRegex = "^1[3-9]\\d{9}$"
        let predicate = NSPredicate(format: "SELF MATCHES %@", phoneRegex)
        return predicate.evaluate(with: phone)
    }
    
    /// 验证码是否有效
    var isCodeValid: Bool {
        verificationCode.count == 6 && verificationCode.allSatisfy { $0.isNumber }
    }
    
    /// 密码是否有效
    var isPasswordValid: Bool {
        password.count >= 6
    }
    
    /// 确认密码是否有效
    var isConfirmPasswordValid: Bool {
        password == confirmPassword
    }
    
    /// 是否可以登录
    var canLogin: Bool {
        switch loginType {
        case .phoneCode:
            return isPhoneValid && isCodeValid && agreeToTerms
        case .password:
            return isPhoneValid && isPasswordValid && agreeToTerms
        default:
            return isPhoneValid && agreeToTerms
        }
    }
    
    /// 验证码按钮标题
    var codeButtonTitle: String {
        if codeCountdown > 0 {
            return "\(codeCountdown)s后重发"
        } else {
            return "获取验证码"
        }
    }
    
    /// 登录按钮标题
    var loginButtonTitle: String {
        switch loginType {
        case .phoneCode: return "验证码登录"
        case .password: return "密码登录"
        case .wechat: return "微信登录"
        case .apple: return "Apple登录"
        }
    }
    
    // MARK: - 私有属性
    
    /// 验证码倒计时定时器
    private var countdownTimer: Timer?
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    init() {
        // 加载保存的手机号
        loadSavedPhone()
        
        // 监听手机号变化
        setupBindings()
    }
    
    deinit {
        countdownTimer?.invalidate()
    }
    
    // MARK: - 公开方法
    
    /// 发送验证码
    func sendVerificationCode() async {
        // 验证手机号
        guard isPhoneValid else {
            showErrorMessage("请输入正确的手机号")
            return
        }
        
        // 开始倒计时
        startCountdown()
        
        // 发送请求
        isSendingCode = true
        
        do {
            try await AuthService.shared.sendVerificationCode(
                phone: phone,
                type: .login
            )
            showErrorMessage("验证码已发送")
        } catch {
            // 验证码发送失败
            stopCountdown()
            showErrorMessage(error.localizedDescription)
        }
        
        isSendingCode = false
    }
    
    /// 登录
    func login() async -> Bool {
        // 验证表单
        guard canLogin else {
            if !agreeToTerms {
                showErrorMessage("请先同意用户协议")
            }
            return false
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            // 根据登录类型调用不同的登录方法
            let user: User
            let authInfo: AuthInfo
            
            switch loginType {
            case .phoneCode:
                let result = try await AuthService.shared.loginWithCode(
                    phone: phone,
                    code: verificationCode
                )
                user = result.user
                authInfo = result.authInfo
                
            case .password:
                let result = try await AuthService.shared.loginWithPassword(
                    phone: phone,
                    password: password
                )
                user = result.user
                authInfo = result.authInfo
                
            case .wechat:
                print("[LoginViewModel] 微信登录(预留)")
                throw NSError(domain: "Login", code: -1, userInfo: [NSLocalizedDescriptionKey: "微信登录暂不支持"])
                
            case .apple:
                print("[LoginViewModel] Apple登录(预留)")
                throw NSError(domain: "Login", code: -1, userInfo: [NSLocalizedDescriptionKey: "Apple登录暂不支持"])
            }
            
            // 保存认证信息
            saveAuthInfo(authInfo)
            
            // 保存用户信息
            saveUserInfo(user)
            
            // 记住手机号
            if rememberPhone {
                savePhone()
            } else {
                clearSavedPhone()
            }
            
            isLoading = false
            return true
            
        } catch {
            isLoading = false
            showErrorMessage(error.localizedDescription)
            return false
        }
    }
    
    /// 切换登录方式
    func switchLoginType(_ type: LoginType) {
        loginType = type
        clearInput()
    }
    
    /// 重置输入
    func resetInput() {
        verificationCode = ""
        password = ""
        confirmPassword = ""
        errorMessage = nil
    }
    
    /// 清空输入
    func clearInput() {
        phone = ""
        verificationCode = ""
        password = ""
        confirmPassword = ""
        errorMessage = nil
    }
    
    // MARK: - 私有方法
    
    /// 设置数据绑定
    private func setupBindings() {
        // 监听手机号变化
        $phone
            .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.errorMessage = nil
            }
            .store(in: &cancellables)
        
        // 监听验证码变化
        $verificationCode
            .sink { [weak self] _ in
                self?.errorMessage = nil
            }
            .store(in: &cancellables)
        
        // 监听密码变化
        $password
            .sink { [weak self] _ in
                self?.errorMessage = nil
            }
            .store(in: &cancellables)
    }
    
    /// 开始倒计时
    private func startCountdown() {
        codeCountdown = 60
        
        countdownTimer?.invalidate()
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self = self else { return }
                
                if self.codeCountdown > 0 {
                    self.codeCountdown -= 1
                } else {
                    self.stopCountdown()
                }
            }
        }
    }
    
    /// 停止倒计时
    private func stopCountdown() {
        countdownTimer?.invalidate()
        countdownTimer = nil
        codeCountdown = 0
    }
    
    /// 显示错误信息
    private func showErrorMessage(_ message: String) {
        errorMessage = message
        showError = true
    }
    
    /// 保存认证信息
    private func saveAuthInfo(_ authInfo: AuthInfo) {
        // 保存Token到Keychain
        KeychainManager.shared.saveToken(authInfo.accessToken)
        KeychainManager.shared.saveRefreshToken(authInfo.refreshToken)
        KeychainManager.shared.saveTokenExpiration(authInfo.expiresAt)
    }
    
    /// 保存用户信息
    private func saveUserInfo(_ user: User) {
        if let data = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(data, forKey: "currentUser")
        }
    }
    
    /// 加载保存的手机号
    private func loadSavedPhone() {
        if let savedPhone = UserDefaults.standard.string(forKey: "savedPhone") {
            phone = savedPhone
            rememberPhone = true
        }
    }
    
    /// 保存手机号
    private func savePhone() {
        UserDefaults.standard.set(phone, forKey: "savedPhone")
    }
    
    /// 清除保存的手机号
    private func clearSavedPhone() {
        UserDefaults.standard.removeObject(forKey: "savedPhone")
    }
}

// MARK: - 登录请求响应

/// 登录响应数据
struct LoginResponse {
    var user: User
    var authInfo: AuthInfo
}

// MARK: - 预览

#if DEBUG
extension LoginViewModel {
    static let preview = LoginViewModel()
}
#endif
