//
//  EditProfileView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  编辑个人资料页面
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI
import PhotosUI

/// 编辑个人资料页面
/// 
/// 支持修改用户昵称、邮箱、头像等个人信息。
struct EditProfileView: View {
    
    // MARK: - 属性
    
    /// 视图模型
    @ObservedObject var viewModel: ProfileViewModel
    
    /// 环境变量，用于关闭页面
    @Environment(\.dismiss) private var dismiss
    
    /// 昵称输入
    @State private var nickname: String = ""
    
    /// 邮箱输入
    @State private var email: String = ""
    
    /// 是否显示头像选择器
    @State private var showAvatarPicker = false
    
    /// 选中的头像
    @State private var selectedAvatarItem: PhotosPickerItem?
    
    /// 头像预览
    @State private var avatarPreview: UIImage?
    
    /// 是否显示邮箱格式错误
    @State private var showEmailError = false
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                Form {
                    // 头像编辑
                    avatarSection
                    
                    // 基本信息
                    basicInfoSection
                    
                    // 联系信息
                    contactInfoSection
                    
                    // 保存按钮
                    saveButtonSection
                }
                
                // 加载指示器
                if viewModel.isLoading || viewModel.isUploadingAvatar {
                    LoadingOverlay(message: viewModel.isUploadingAvatar ? "上传头像中..." : "保存中...")
                }
            }
            .navigationTitle("编辑资料")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") {
                        dismiss()
                    }
                }
            }
            .photosPicker(
                isPresented: $showAvatarPicker,
                selection: $selectedAvatarItem,
                matching: .images
            )
            .onChange(of: selectedAvatarItem) { item in
                Task {
                    await loadSelectedImage(item)
                }
            }
            .onAppear {
                // 初始化表单数据
                nickname = viewModel.user?.nickname ?? ""
                email = viewModel.user?.email ?? ""
            }
            .alert("邮箱格式错误", isPresented: $showEmailError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text("请输入有效的邮箱地址")
            }
        }
    }
    
    // MARK: - 头像编辑
    
    private var avatarSection: some View {
        Section {
            HStack {
                Spacer()
                
                VStack(spacing: 12) {
                    // 头像
                    ZStack {
                        if let preview = avatarPreview {
                            Image(uiImage: preview)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 100, height: 100)
                                .clipShape(Circle())
                        } else if let avatarUrl = viewModel.avatarUrl,
                                  let url = URL(string: avatarUrl) {
                            AsyncImage(url: url) { image in
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                            } placeholder: {
                                defaultAvatar
                            }
                            .frame(width: 100, height: 100)
                            .clipShape(Circle())
                        } else {
                            defaultAvatar
                                .frame(width: 100, height: 100)
                        }
                        
                        // 编辑图标
                        Circle()
                            .fill(Color.blue)
                            .frame(width: 32, height: 32)
                            .overlay(
                                Image(systemName: "camera.fill")
                                    .font(.system(size: 14))
                                    .foregroundColor(.white)
                            )
                            .offset(x: 35, y: 35)
                    }
                    .onTapGesture {
                        showAvatarPicker = true
                    }
                    
                    Text("点击更换头像")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
            .padding(.vertical, 20)
            .listRowBackground(Color.clear)
        }
    }
    
    // 默认头像
    private var defaultAvatar: some View {
        Circle()
            .fill(Color.blue.opacity(0.15))
            .overlay(
                Image(systemName: "person.fill")
                    .font(.system(size: 40))
                    .foregroundColor(.blue)
            )
    }
    
    // MARK: - 基本信息
    
    private var basicInfoSection: some View {
        Section(header: Text("基本信息")) {
            HStack {
                Text("昵称")
                    .foregroundColor(.primary)
                
                Spacer()
                
                TextField("请输入昵称", text: $nickname)
                    .multilineTextAlignment(.trailing)
                    .autocorrectionDisabled()
            }
            
            HStack {
                Text("用户名")
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text(viewModel.user?.username ?? "")
                    .foregroundColor(.secondary)
            }
            
            HStack {
                Text("用户类型")
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text(viewModel.userTypeDescription)
                    .foregroundColor(.secondary)
            }
            
            HStack {
                Text("等级")
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text("Lv.\(viewModel.userLevel)")
                    .foregroundColor(.secondary)
            }
        }
    }
    
    // MARK: - 联系信息
    
    private var contactInfoSection: some View {
        Section(header: Text("联系信息")) {
            HStack {
                Text("手机号")
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text(viewModel.maskedPhone)
                    .foregroundColor(.secondary)
            }
            
            HStack {
                Text("邮箱")
                    .foregroundColor(.primary)
                
                Spacer()
                
                TextField("请输入邮箱", text: $email)
                    .multilineTextAlignment(.trailing)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .autocorrectionDisabled()
            }
        }
    }
    
    // MARK: - 保存按钮
    
    private var saveButtonSection: some View {
        Section {
            Button(action: {
                Task {
                    await saveProfile()
                }
            }) {
                HStack {
                    Spacer()
                    
                    if viewModel.isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                            .tint(.white)
                    } else {
                        Text("保存")
                            .fontWeight(.medium)
                    }
                    
                    Spacer()
                }
            }
            .disabled(viewModel.isLoading || !hasChanges)
            .listRowBackground(hasChanges ? Color.blue : Color.gray)
            .foregroundColor(.white)
        }
        .listRowInsets(EdgeInsets())
    }
    
    // MARK: - 辅助方法
    
    /// 是否有修改
    private var hasChanges: Bool {
        let nicknameChanged = nickname != (viewModel.user?.nickname ?? "")
        let emailChanged = email != (viewModel.user?.email ?? "")
        let avatarChanged = avatarPreview != nil
        return nicknameChanged || emailChanged || avatarChanged
    }
    
    /// 加载选中的图片
    private func loadSelectedImage(_ item: PhotosPickerItem?) async {
        guard let item = item else { return }
        
        do {
            if let data = try await item.loadTransferable(type: Data.self),
               let image = UIImage(data: data) {
                await MainActor.run {
                    avatarPreview = image
                }
            }
        } catch {
            viewModel.errorMessage = error.localizedDescription
            viewModel.showError = true
        }
    }
    
    /// 保存资料
    private func saveProfile() async {
        // 验证邮箱格式
        if !email.isEmpty && !isValidEmail(email) {
            showEmailError = true
            return
        }
        
        // 先上传头像（如果有）
        if let image = avatarPreview {
            await viewModel.uploadAvatar(image)
            // 上传完成后头像URL已更新，继续保存其他信息
        }
        
        // 保存其他信息
        let finalNickname = nickname.isEmpty ? nil : nickname
        let finalEmail = email.isEmpty ? nil : email
        
        await viewModel.updateUserInfo(
            nickname: finalNickname,
            email: finalEmail,
            avatarUrl: nil // 头像已通过uploadAvatar更新
        )
        
        // 保存成功后关闭页面
        if viewModel.successMessage != nil {
            dismiss()
        }
    }
    
    /// 验证邮箱格式
    private func isValidEmail(_ email: String) -> Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let predicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return predicate.evaluate(with: email)
    }
}

// MARK: - 加载遮罩

struct LoadingOverlay: View {
    let message: String
    
    var body: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
            
            VStack(spacing: 16) {
                ProgressView()
                    .scaleEffect(1.5)
                
                Text(message)
                    .font(.subheadline)
                    .foregroundColor(.white)
            }
            .padding(24)
            .background(Color(.systemBackground))
            .cornerRadius(12)
        }
    }
}

// MARK: - 预览

#if DEBUG
struct EditProfileView_Previews: PreviewProvider {
    static var previews: some View {
        EditProfileView(viewModel: ProfileViewModel.preview)
    }
}
#endif
