//
//  LoginView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  登录页面视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 登录视图
/// 
/// 提供手机号验证码登录、密码登录等登录方式。
struct LoginView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = LoginViewModel()
    
    /// 是否显示用户协议
    @State private var showTerms = false
    
    /// 是否显示隐私政策
    @State private var showPrivacy = false
    
    // MARK: - 界面
    
    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 0) {
                    // 顶部Logo区域
                    headerSection
                        .frame(height: geometry.size.height * 0.3)
                    
                    // 表单区域
                    formSection
                        .padding(.horizontal, 24)
                    
                    // 登录按钮
                    loginButton
                        .padding(.horizontal, 24)
                        .padding(.top, 24)
                    
                    // 其他登录方式
                    otherLoginSection
                        .padding(.top: 32)
                    
                    // 底部协议
                    termsSection
                        .padding(.top: 24)
                }
                .frame(minHeight: geometry.size.height)
            }
        }
        .background(Color(.systemBackground))
        .ignoresSafeArea(.keyboard)
        .alert("提示", isPresented: $viewModel.showError) {
            Button("确定", role: .cancel) {}
        } message: {
            Text(viewModel.errorMessage ?? "操作失败")
        }
        .sheet(isPresented: $showTerms) {
            TermsView()
        }
        .sheet(isPresented: $showPrivacy) {
            PrivacyView()
        }
    }
    
    // MARK: - 组件 - 头部
    
    /// 头部区域
    private var headerSection: some View {
        VStack(spacing: 16) {
            // Logo
            Image(systemName: "star.fill")
                .font(.system(size: 60))
                .foregroundColor(.blue)
            
            // 标题
            Text("北极星AI")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(.primary)
            
            // 副标题
            Text("智能商机获客系统")
                .font(.system(size: 16))
                .foregroundColor(.secondary)
        }
        .padding(.top, 40)
    }
    
    // MARK: - 组件 - 表单
    
    /// 表单区域
    private var formSection: some View {
        VStack(spacing: 20) {
            // 手机号输入
            phoneInput
            
            // 验证码/密码输入（根据登录方式）
            conditionalInput
            
            // 登录方式切换
            loginTypeToggle
            
            // 用户协议勾选
            termsToggle
        }
    }
    
    /// 手机号输入
    private var phoneInput: some View {
        HStack(spacing: 12) {
            // 区号
            Text("+86")
                .font(.system(size: 16))
                .foregroundColor(.secondary)
                .padding(.horizontal, 8)
            
            // 分隔线
            Rectangle()
                .fill(Color(.separator))
                .frame(width: 1, height: 20)
            
            // 输入框
            TextField("请输入手机号", text: $viewModel.phone)
                .keyboardType(.phonePad)
                .font(.system(size: 16))
                .autocapitalization(.none)
                .disableAutocorrection(true)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    /// 条件输入（验证码或密码）
    @ViewBuilder
    private var conditionalInput: some View {
        if viewModel.loginType == .phoneCode {
            verificationCodeInput
        } else {
            passwordInput
        }
    }
    
    /// 验证码输入
    private var verificationCodeInput: some View {
        HStack(spacing: 12) {
            // 验证码输入框
            TextField("请输入验证码", text: $viewModel.verificationCode)
                .keyboardType(.numberPad)
                .font(.system(size: 16))
                .autocapitalization(.none)
            
            Spacer()
            
            // 获取验证码按钮
            Button(action: {
                Task {
                    await viewModel.sendVerificationCode()
                }
            }) {
                Text(viewModel.codeButtonTitle)
                    .font(.system(size: 14))
                    .foregroundColor(viewModel.codeCountdown > 0 ? .secondary : .blue)
            }
            .disabled(viewModel.codeCountdown > 0 || !viewModel.isPhoneValid)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    /// 密码输入
    private var passwordInput: some View {
        HStack(spacing: 12) {
            // 密码输入框
            if viewModel.isPasswordVisible {
                TextField("请输入密码", text: $viewModel.password)
                    .font(.system(size: 16))
            } else {
                SecureField("请输入密码", text: $viewModel.password)
                    .font(.system(size: 16))
            }
            
            // 显示/隐藏密码
            Button(action: {
                viewModel.isPasswordVisible.toggle()
            }) {
                Image(systemName: viewModel.isPasswordVisible ? "eye.slash" : "eye")
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    /// 登录方式切换
    private var loginTypeToggle: some View {
        HStack {
            if viewModel.loginType == .phoneCode {
                Button("密码登录") {
                    viewModel.switchLoginType(.password)
                }
                .font(.system(size: 14))
                .foregroundColor(.blue)
            } else {
                Button("验证码登录") {
                    viewModel.switchLoginType(.phoneCode)
                }
                .font(.system(size: 14))
                .foregroundColor(.blue)
            }
            
            Spacer()
            
            // 第三方登录按钮
            HStack(spacing: 16) {
                Button(action: {}) {
                    Image(systemName: "logo.wechat")
                        .font(.system(size: 24))
                        .foregroundColor(.green)
                }
                
                Button(action: {}) {
                    Image(systemName: "apple.logo")
                        .font(.system(size: 24))
                }
            }
        }
    }
    
    /// 用户协议勾选
    private var termsToggle: some View {
        HStack(alignment: .center, spacing: 8) {
            Toggle(isOn: $viewModel.agreeToTerms) {
                EmptyView()
            }
            .labelsHidden()
            .toggleStyle(CheckboxToggleStyle())
            
            // 协议文本
            HStack(spacing: 2) {
                Text("我已阅读并同意")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
                
                Button("《用户协议》") {
                    showTerms = true
                }
                .font(.system(size: 12))
                .foregroundColor(.blue)
                
                Text("和")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
                
                Button("《隐私政策》") {
                    showPrivacy = true
                }
                .font(.system(size: 12))
                .foregroundColor(.blue)
            }
        }
    }
    
    // MARK: - 组件 - 登录按钮
    
    /// 登录按钮
    private var loginButton: some View {
        Button(action: {
            Task {
                let success = await viewModel.login()
                if success {
                    // 登录成功，通知状态变更
                    NotificationCenter.default.post(name: .authStateChanged, object: nil)
                }
            }
        }) {
            HStack {
                if viewModel.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text(viewModel.loginButtonTitle)
                        .font(.system(size: 17, weight: .semibold))
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(viewModel.canLogin ? Color.blue : Color.gray)
            .foregroundColor(.white)
            .cornerRadius(12)
        }
        .disabled(!viewModel.canLogin || viewModel.isLoading)
    }
    
    // MARK: - 组件 - 其他登录方式
    
    /// 其他登录方式
    private var otherLoginSection: some View {
        VStack(spacing: 16) {
            // 分隔线
            HStack {
                Rectangle()
                    .fill(Color(.separator))
                    .frame(height: 1)
                
                Text("其他登录方式")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 12)
                
                Rectangle()
                    .fill(Color(.separator))
                    .frame(height: 1)
            }
            
            // 第三方登录图标
            HStack(spacing: 40) {
                Button(action: {}) {
                    VStack(spacing: 8) {
                        Image(systemName: "logo.wechat")
                            .font(.system(size: 32))
                            .foregroundColor(.green)
                        Text("微信")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                }
                
                Button(action: {}) {
                    VStack(spacing: 8) {
                        Image(systemName: "apple.logo")
                            .font(.system(size: 32))
                        Text("Apple")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
    }
    
    // MARK: - 组件 - 底部协议
    
    /// 底部协议
    private var termsSection: some View {
        Text("登录即表示同意我们的服务条款")
            .font(.system(size: 11))
            .foregroundColor(.secondary)
            .padding(.bottom, 20)
    }
}

// MARK: - 自定义Checkbox样式

/// Checkbox切换样式
struct CheckboxToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        Button(action: {
            configuration.isOn.toggle()
        }) {
            Image(systemName: configuration.isOn ? "checkmark.square.fill" : "square")
                .foregroundColor(configuration.isOn ? .blue : .secondary)
                .font(.system(size: 20))
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - 辅助视图

/// 用户协议视图
struct TermsView: View {
    var body: some View {
        NavigationView {
            ScrollView {
                Text("用户协议内容...")
                    .padding()
            }
            .navigationTitle("用户协议")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

/// 隐私政策视图
struct PrivacyView: View {
    var body: some View {
        NavigationView {
            ScrollView {
                Text("隐私政策内容...")
                    .padding()
            }
            .navigationTitle("隐私政策")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - 预览

#if DEBUG
struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}
#endif
