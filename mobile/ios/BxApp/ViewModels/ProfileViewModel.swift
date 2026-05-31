//
//  ProfileViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  个人中心视图模型
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI
import PhotosUI

/// 个人中心视图模型
/// 
/// 负责管理用户信息、头像上传、个人资料编辑等功能。
@MainActor
class ProfileViewModel: ObservableObject {
    
    // MARK: - Published属性
    
    /// 当前用户
    @Published var user: User?
    
    /// 是否正在加载
    @Published var isLoading: Bool = false
    
    /// 错误信息
    @Published var errorMessage: String?
    
    /// 是否显示错误
    @Published var showError: Bool = false
    
    /// 是否显示退出登录确认
    @Published var showLogoutConfirm: Bool = false
    
    /// 是否显示编辑页面
    @Published var showEditProfile: Bool = false
    
    /// 是否显示安全设置
    @Published var showSecuritySettings: Bool = false
    
    /// 是否显示账号管理
    @Published var showAccountManagement: Bool = false
    
    /// 是否显示积分充值
    @Published var showRecharge: Bool = false
    
    /// 头像上传进度
    @Published var uploadProgress: Double = 0
    
    /// 是否正在上传头像
    @Published var isUploadingAvatar: Bool = false
    
    /// 选中的头像图片
    @Published var selectedAvatarItem: PhotosPickerItem?
    
    /// 头像预览图片
    @Published var avatarPreview: UIImage?
    
    /// 是否显示头像选择器
    @Published var showAvatarPicker: Bool = false
    
    /// 更新成功提示
    @Published var showUpdateSuccess: Bool = false
    
    /// 成功提示信息
    @Published var successMessage: String?
    
    // MARK: - Computed属性
    
    /// 用户昵称
    var nickname: String {
        user?.nickname ?? user?.username ?? "未设置"
    }
    
    /// 用户手机号
    var phone: String {
        user?.phone ?? ""
    }
    
    /// 显示的手机号（脱敏）
    var maskedPhone: String {
        guard phone.count == 11 else { return phone }
        let prefix = phone.prefix(3)
        let suffix = phone.suffix(4)
        return "\(prefix)****\(suffix)"
    }
    
    /// 用户邮箱
    var email: String? {
        user?.email
    }
    
    /// 用户头像URL
    var avatarUrl: String? {
        user?.avatarUrl
    }
    
    /// 用户类型描述
    var userTypeDescription: String {
        user?.userType.description ?? "普通用户"
    }
    
    /// 积分余额
    var pointsBalance: Int {
        user?.balance ?? 0
    }
    
    /// 用户等级
    var userLevel: Int {
        user?.level ?? 1
    }
    
    // MARK: - 私有属性
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    init() {
        loadUserInfo()
    }
    
    // MARK: - 公开方法
    
    /// 加载用户信息
    func loadUserInfo() {
        if let data = UserDefaults.standard.data(forKey: "currentUser"),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            self.user = user
        }
    }
    
    /// 刷新用户信息
    func refreshUserInfo() async {
        isLoading = true
        
        do {
            let user = try await APIService.shared.getCurrentUser()
            self.user = user
            
            // 保存到本地
            if let data = try? JSONEncoder().encode(user) {
                UserDefaults.standard.set(data, forKey: "currentUser")
            }
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 更新用户信息
    func updateUserInfo(nickname: String?, email: String?, avatarUrl: String?) async {
        isLoading = true
        
        do {
            let updatedUser = try await APIService.shared.updateUserInfo(
                nickname: nickname,
                email: email,
                avatarUrl: avatarUrl
            )
            
            self.user = updatedUser
            
            // 保存到本地
            if let data = try? JSONEncoder().encode(updatedUser) {
                UserDefaults.standard.set(data, forKey: "currentUser")
            }
            
            successMessage = "更新成功"
            showUpdateSuccess = true
            
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 上传头像
    func uploadAvatar(_ image: UIImage) async {
        isUploadingAvatar = true
        uploadProgress = 0
        
        do {
            // 压缩图片
            guard let imageData = image.jpegData(compressionQuality: 0.8) else {
                throw NSError(domain: "Upload", code: -1, userInfo: [NSLocalizedDescriptionKey: "图片压缩失败"])
            }
            
            // 上传图片
            let avatarUrl = try await APIService.shared.uploadImage(
                imageData: imageData,
                fileName: "avatar_\(UUID().uuidString).jpg"
            ) { progress in
                Task { @MainActor in
                    self.uploadProgress = progress
                }
            }
            
            // 更新用户头像
            await updateUserInfo(nickname: nil, email: nil, avatarUrl: avatarUrl)
            
            isUploadingAvatar = false
            
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            isUploadingAvatar = false
        }
    }
    
    /// 处理选中的头像
    func handleSelectedAvatar() async {
        guard let item = selectedAvatarItem else { return }
        
        do {
            if let data = try await item.loadTransferable(type: Data.self),
               let image = UIImage(data: data) {
                avatarPreview = image
                await uploadAvatar(image)
            }
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
    }
    
    /// 修改密码
    func changePassword(oldPassword: String, newPassword: String) async -> Bool {
        isLoading = true
        
        do {
            try await APIService.shared.changePassword(
                oldPassword: oldPassword,
                newPassword: newPassword
            )
            
            successMessage = "密码修改成功"
            showUpdateSuccess = true
            isLoading = false
            return true
            
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            isLoading = false
            return false
        }
    }
    
    /// 退出登录
    func logout() async -> Bool {
        isLoading = true
        
        do {
            // 调用登出接口
            try await AuthService.shared.logout()
            
            // 清除本地数据
            clearLocalData()
            
            isLoading = false
            return true
            
        } catch {
            // 即使接口失败，也清除本地数据
            clearLocalData()
            isLoading = false
            return true
        }
    }
    
    /// 准备退出登录
    func prepareLogout() {
        showLogoutConfirm = true
    }
    
    /// 取消退出登录
    func cancelLogout() {
        showLogoutConfirm = false
    }
    
    /// 确认退出登录
    func confirmLogout() async -> Bool {
        showLogoutConfirm = false
        return await logout()
    }
    
    /// 检查更新
    func checkForUpdates() async -> Bool {
        print("[ProfileViewModel] 版本检查(预留)")
        return false
    }
    
    // MARK: - 私有方法
    
    /// 清除本地数据
    private func clearLocalData() {
        // 清除认证信息
        KeychainManager.shared.deleteToken()
        KeychainManager.shared.deleteRefreshToken()
        KeychainManager.shared.deleteTokenExpiration()
        
        // 清除用户信息
        UserDefaults.standard.removeObject(forKey: "currentUser")
        
        // 重置状态
        user = nil
    }
}

// MARK: - API扩展

extension APIService {
    
    /// 获取当前用户
    func getCurrentUser() async throws -> User {
        let response: APIResponse<User> = try await get("/user/me")
        guard let user = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        return user
    }
    
    /// 更新用户信息
    func updateUserInfo(nickname: String?, email: String?, avatarUrl: String?) async throws -> User {
        var parameters: [String: Any] = [:]
        if let nickname = nickname {
            parameters["nickname"] = nickname
        }
        if let email = email {
            parameters["email"] = email
        }
        if let avatarUrl = avatarUrl {
            parameters["avatarUrl"] = avatarUrl
        }
        
        let response: APIResponse<User> = try await patch("/user/me", parameters: parameters)
        guard let user = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        return user
    }
    
    /// 上传图片
    func uploadImage(
        imageData: Data,
        fileName: String,
        progressHandler: @escaping (Double) -> Void
    ) async throws -> String {
        // 模拟上传进度
        for i in 0...10 {
            try await Task.sleep(nanoseconds: 100_000_000)
            progressHandler(Double(i) / 10.0)
        }
        
        // 实际上传
        let response: APIResponse<UploadResponse> = try await upload(
            "/upload/image",
            fileData: imageData,
            fileName: fileName,
            mimeType: "image/jpeg"
        )
        
        guard let url = response.data?.url else {
            throw APIException(code: response.code, message: response.message)
        }
        return url
    }
    
    /// 修改密码
    func changePassword(oldPassword: String, newPassword: String) async throws {
        let _: APIResponse<EmptyResponse> = try await post("/user/change-password", parameters: [
            "oldPassword": oldPassword,
            "newPassword": newPassword
        ])
    }
}

// MARK: - 响应模型

/// 上传响应
struct UploadResponse: Codable {
    var url: String
    var fileName: String
    var size: Int
}

// MARK: - 预览

#if DEBUG
extension ProfileViewModel {
    static let preview = ProfileViewModel()
}
#endif
