//
//  SecuritySettingsView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  安全设置页面
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 安全设置页面
/// 
/// 提供修改密码、账号绑定、登录设备等安全相关功能。
struct SecuritySettingsView: View {
    
    // MARK: - 属性
    
    /// 视图模型
    @ObservedObject var viewModel: ProfileViewModel
    
    /// 环境变量，用于关闭页面
    @Environment(\.dismiss) private var dismiss
    
    /// 旧密码
    @State private var oldPassword: String = ""
    
    /// 新密码
    @State private var newPassword: String = ""
    
    /// 确认新密码
    @State private var confirmPassword: String = ""
    
    /// 是否显示旧密码
    @State private var showOldPassword = false
    
    /// 是否显示新密码
    @State private var showNewPassword = false
    
    /// 是否显示确认密码
    @State private var showConfirmPassword = false
    
    /// 是否显示修改密码成功
    @State private var showPasswordChanged = false
    
    /// 密码修改错误信息
    @State private var passwordError: String?
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                Form {
                    // 账号安全状态
                    securityStatusSection
                    
                    // 修改密码
                    changePasswordSection
                    
                    // 账号绑定
                    accountBindingSection
                    
                    // 登录设备
                    loginDevicesSection
                    
                    // 其他安全选项
                    otherSecuritySection
                }
                
                // 加载指示器
                if viewModel.isLoading {
                    LoadingView()
                }
            }
            .navigationTitle("安全设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("关闭") {
                        dismiss()
                    }
                }
            }
            .alert("密码修改成功", isPresented: $showPasswordChanged) {
                Button("确定", role: .cancel) {
                    // 清空密码输入
                    oldPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                }
            } message: {
                Text("您的密码已成功修改，请使用新密码登录")
            }
            .alert("修改失败", isPresented: .constant(passwordError != nil)) {
                Button("确定", role: .cancel) {
                    passwordError = nil
                }
            } message: {
                Text(passwordError ?? "")
            }
        }
    }
    
    // MARK: - 安全状态
    
    private var securityStatusSection: some View {
        Section {
            HStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(Color.green.opacity(0.15))
                        .frame(width: 50, height: 50)
                    
                    Image(systemName: "shield.checkered.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.green)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("账号安全状态")
                        .font(.headline)
                    
                    Text("您的账号安全状态良好")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Text("优秀")
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.green)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.green.opacity(0.15))
                    .cornerRadius(6)
            }
            .padding(.vertical, 8)
        }
    }
    
    // MARK: - 修改密码
    
    private var changePasswordSection: some View {
        Section(header: Text("修改密码")) {
            // 旧密码
            HStack {
                if showOldPassword {
                    TextField("当前密码", text: $oldPassword)
                        .autocorrectionDisabled()
                } else {
                    SecureField("当前密码", text: $oldPassword)
                        .autocorrectionDisabled()
                }
                
                Button(action: {
                    showOldPassword.toggle()
                }) {
                    Image(systemName: showOldPassword ? "eye.slash" : "eye")
                        .foregroundColor(.secondary)
                }
            }
            
            // 新密码
            HStack {
                if showNewPassword {
                    TextField("新密码（至少6位）", text: $newPassword)
                        .autocorrectionDisabled()
                } else {
                    SecureField("新密码（至少6位）", text: $newPassword)
                        .autocorrectionDisabled()
                }
                
                Button(action: {
                    showNewPassword.toggle()
                }) {
                    Image(systemName: showNewPassword ? "eye.slash" : "eye")
                        .foregroundColor(.secondary)
                }
            }
            
            // 确认新密码
            HStack {
                if showConfirmPassword {
                    TextField("确认新密码", text: $confirmPassword)
                        .autocorrectionDisabled()
                } else {
                    SecureField("确认新密码", text: $confirmPassword)
                        .autocorrectionDisabled()
                }
                
                Button(action: {
                    showConfirmPassword.toggle()
                }) {
                    Image(systemName: showConfirmPassword ? "eye.slash" : "eye")
                        .foregroundColor(.secondary)
                }
            }
            
            // 密码强度提示
            if !newPassword.isEmpty {
                PasswordStrengthView(password: newPassword)
            }
            
            // 确认按钮
            Button(action: {
                Task {
                    await changePassword()
                }
            }) {
                HStack {
                    Spacer()
                    
                    if viewModel.isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                            .tint(.white)
                    } else {
                        Text("确认修改")
                            .fontWeight(.medium)
                    }
                    
                    Spacer()
                }
            }
            .disabled(!canChangePassword || viewModel.isLoading)
            .listRowBackground(canChangePassword ? Color.blue : Color.gray)
            .foregroundColor(.white)
        }
    }
    
    // MARK: - 账号绑定
    
    private var accountBindingSection: some View {
        Section(header: Text("账号绑定")) {
            // 手机号
            HStack {
                Image(systemName: "phone.fill")
                    .foregroundColor(.green)
                    .frame(width: 30)
                
                Text("手机号")
                
                Spacer()
                
                Text(viewModel.maskedPhone)
                    .foregroundColor(.secondary)
                
                Text("已绑定")
                    .font(.caption)
                    .foregroundColor(.green)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.green.opacity(0.15))
                    .cornerRadius(4)
            }
            
            // 邮箱
            HStack {
                Image(systemName: "envelope.fill")
                    .foregroundColor(.blue)
                    .frame(width: 30)
                
                Text("邮箱")
                
                Spacer()
                
                if let email = viewModel.user?.email, !email.isEmpty {
                    Text(maskEmail(email))
                        .foregroundColor(.secondary)
                    
                    Text("已绑定")
                        .font(.caption)
                        .foregroundColor(.green)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.green.opacity(0.15))
                        .cornerRadius(4)
                } else {
                    Text("未绑定")
                        .font(.caption)
                        .foregroundColor(.orange)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.orange.opacity(0.15))
                        .cornerRadius(4)
                }
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            // 微信
            HStack {
                Image(systemName: "message.circle.fill")
                    .foregroundColor(.green)
                    .frame(width: 30)
                
                Text("微信")
                
                Spacer()
                
                Text("未绑定")
                    .font(.caption)
                    .foregroundColor(.orange)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.orange.opacity(0.15))
                    .cornerRadius(4)
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
    
    // MARK: - 登录设备
    
    private var loginDevicesSection: some View {
        Section(header: Text("登录设备")) {
            NavigationLink(destination: Text("设备管理页面")) {
                HStack {
                    Image(systemName: "iphone")
                        .foregroundColor(.blue)
                        .frame(width: 30)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("登录设备管理")
                        
                        Text("查看和管理已登录的设备")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
    }
    
    // MARK: - 其他安全选项
    
    private var otherSecuritySection: some View {
        Section(header: Text("其他")) {
            // 指纹/面容解锁
            HStack {
                Image(systemName: "faceid")
                    .foregroundColor(.purple)
                    .frame(width: 30)
                
                Text("生物识别解锁")
                
                Spacer()
                
                Toggle("", isOn: .constant(true))
                    .labelsHidden()
            }
            
            // 登录保护
            HStack {
                Image(systemName: "lock.shield")
                    .foregroundColor(.orange)
                    .frame(width: 30)
                
                Text("登录保护")
                
                Spacer()
                
                Toggle("", isOn: .constant(true))
                    .labelsHidden()
            }
            
            // 注销账号
            Button(action: {
                print("[SecuritySettingsView] 注销账号(预留)")
            }) {
                HStack {
                    Image(systemName: "person.crop.circle.badge.xmark")
                        .foregroundColor(.red)
                        .frame(width: 30)
                    
                    Text("注销账号")
                        .foregroundColor(.red)
                    
                    Spacer()
                }
            }
        }
    }
    
    // MARK: - 辅助方法
    
    /// 是否可以修改密码
    private var canChangePassword: Bool {
        oldPassword.count >= 6 &&
        newPassword.count >= 6 &&
        newPassword == confirmPassword
    }
    
    /// 修改密码
    private func changePassword() async {
        // 验证密码
        guard newPassword == confirmPassword else {
            passwordError = "两次输入的密码不一致"
            return
        }
        
        guard newPassword.count >= 6 else {
            passwordError = "新密码长度至少6位"
            return
        }
        
        let success = await viewModel.changePassword(
            oldPassword: oldPassword,
            newPassword: newPassword
        )
        
        if success {
            showPasswordChanged = true
        }
    }
    
    /// 脱敏邮箱
    private func maskEmail(_ email: String) -> String {
        guard let atIndex = email.firstIndex(of: "@") else { return email }
        
        let prefix = email[..<atIndex]
        let domain = email[atIndex...]
        
        let maskedPrefix: String
        if prefix.count <= 2 {
            maskedPrefix = String(prefix)
        } else {
            let first = prefix.prefix(2)
            let last = prefix.suffix(1)
            maskedPrefix = "\(first)***\(last)"
        }
        
        return "\(maskedPrefix)\(domain)"
    }
}

// MARK: - 密码强度视图

struct PasswordStrengthView: View {
    let password: String
    
    var strength: PasswordStrength {
        calculateStrength(password)
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                ForEach(0..<4) { index in
                    Rectangle()
                        .fill(barColor(for: index))
                        .frame(height: 4)
                        .cornerRadius(2)
                }
            }
            
            Text(strength.description)
                .font(.caption)
                .foregroundColor(strength.color)
        }
    }
    
    private func barColor(for index: Int) -> Color {
        let level = strength.rawValue
        if index < level {
            return strength.color
        } else {
            return Color.gray.opacity(0.3)
        }
    }
    
    private func calculateStrength(_ password: String) -> PasswordStrength {
        var score = 0
        
        // 长度检查
        if password.count >= 8 {
            score += 1
        }
        if password.count >= 12 {
            score += 1
        }
        
        // 复杂度检查
        let hasLowercase = password.range(of: "[a-z]", options: .regularExpression) != nil
        let hasUppercase = password.range(of: "[A-Z]", options: .regularExpression) != nil
        let hasNumber = password.range(of: "[0-9]", options: .regularExpression) != nil
        let hasSpecial = password.range(of: "[^a-zA-Z0-9]", options: .regularExpression) != nil
        
        let complexity = [hasLowercase, hasUppercase, hasNumber, hasSpecial].filter { $0 }.count
        
        if complexity >= 3 {
            score += 1
        }
        if complexity >= 4 {
            score += 1
        }
        
        switch score {
        case 0...1: return .weak
        case 2: return .fair
        case 3: return .good
        default: return .strong
        }
    }
}

// MARK: - 密码强度枚举

enum PasswordStrength: Int {
    case weak = 1
    case fair = 2
    case good = 3
    case strong = 4
    
    var description: String {
        switch self {
        case .weak: return "密码强度：弱"
        case .fair: return "密码强度：一般"
        case .good: return "密码强度：良好"
        case .strong: return "密码强度：强"
        }
    }
    
    var color: Color {
        switch self {
        case .weak: return .red
        case .fair: return .orange
        case .good: return .yellow
        case .strong: return .green
        }
    }
}

// MARK: - 预览

#if DEBUG
struct SecuritySettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SecuritySettingsView(viewModel: ProfileViewModel.preview)
    }
}
#endif
