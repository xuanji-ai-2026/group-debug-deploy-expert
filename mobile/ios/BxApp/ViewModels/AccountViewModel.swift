//
//  AccountViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  账号管理视图模型
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI

/// 账号管理视图模型
/// 
/// 负责管理第三方平台账号的绑定、解绑、查询等操作。
@MainActor
class AccountViewModel: ObservableObject {
    
    // MARK: - Published属性
    
    /// 已绑定的账号列表
    @Published var accounts: [Account] = []
    
    /// 是否正在加载
    @Published var isLoading: Bool = false
    
    /// 错误信息
    @Published var errorMessage: String?
    
    /// 是否显示错误
    @Published var showError: Bool = false
    
    /// 是否显示绑定成功提示
    @Published var showBindSuccess: Bool = false
    
    /// 是否显示解绑确认弹窗
    @Published var showUnbindConfirm: Bool = false
    
    /// 当前选中的账号（用于解绑）
    @Published var selectedAccount: Account?
    
    /// 绑定授权状态
    @Published var authState: String?
    
    /// 可用的平台列表
    @Published var availablePlatforms: [PlatformType] = []
    
    // MARK: - Computed属性
    
    /// 正常状态的账号数
    var activeAccountCount: Int {
        accounts.filter { $0.status == .active }.count
    }
    
    /// 异常状态的账号数
    var abnormalAccountCount: Int {
        accounts.filter { $0.status == .abnormal || $0.authStatus.needsReauth }.count
    }
    
    /// 平均健康度
    var averageHealthScore: Int {
        guard !accounts.isEmpty else { return 0 }
        let total = accounts.reduce(0) { $0 + $1.healthScore }
        return total / accounts.count
    }
    
    /// 按平台分组的账号
    var accountsByPlatform: [PlatformType: [Account]] {
        Dictionary(grouping: accounts) { $0.platform }
    }
    
    // MARK: - 私有属性
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    init() {
        // 初始化可用平台
        availablePlatforms = PlatformType.allCases
    }
    
    // MARK: - 公开方法
    
    /// 加载账号列表
    func loadAccounts() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let accounts = try await APIService.shared.getAccounts()
            self.accounts = accounts
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 刷新账号列表
    func refreshAccounts() async {
        await loadAccounts()
    }
    
    /// 同步账号数据
    func syncAccount(_ account: Account) async {
        isLoading = true
        
        do {
            let response = try await APIService.shared.syncAccount(accountId: account.id)
            if response.success {
                // 更新本地账号数据
                if let index = accounts.firstIndex(where: { $0.id == account.id }) {
                    var updatedAccount = accounts[index]
                    updatedAccount.followersCount = response.data?.followersCount ?? updatedAccount.followersCount
                    updatedAccount.worksCount = response.data?.worksCount ?? updatedAccount.worksCount
                    updatedAccount.likesCount = response.data?.likesCount ?? updatedAccount.likesCount
                    updatedAccount.lastSyncAt = response.syncAt
                    accounts[index] = updatedAccount
                }
            }
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 解绑账号
    func unbindAccount(_ account: Account) async {
        isLoading = true
        
        do {
            try await APIService.shared.unbindAccount(accountId: account.id)
            // 从列表中移除
            accounts.removeAll { $0.id == account.id }
            selectedAccount = nil
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 准备解绑账号
    func prepareUnbind(_ account: Account) {
        selectedAccount = account
        showUnbindConfirm = true
    }
    
    /// 取消解绑
    func cancelUnbind() {
        selectedAccount = nil
        showUnbindConfirm = false
    }
    
    /// 确认解绑
    func confirmUnbind() async {
        guard let account = selectedAccount else { return }
        await unbindAccount(account)
        showUnbindConfirm = false
    }
    
    /// 重新授权
    func reauthorize(_ account: Account) async {
        // 打开授权页面
        await startBindProcess(for: account.platform)
    }
    
    /// 开始绑定流程
    func startBindProcess(for platform: PlatformType) async {
        do {
            // 获取授权URL
            let authURL = try await APIService.shared.getAuthURL(platform: platform)
            
            // 生成state防止CSRF
            authState = UUID().uuidString
            
            // 打开授权页面（实际项目中使用SFSafariViewController或ASWebAuthenticationSession）
            if let url = URL(string: authURL) {
                await UIApplication.shared.open(url)
            }
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
    }
    
    /// 处理授权回调
    func handleAuthCallback(code: String, state: String) async {
        // 验证state
        guard state == authState else {
            errorMessage = "授权验证失败"
            showError = true
            return
        }
        
        isLoading = true
        
        do {
            // 完成绑定
            let newAccount = try await APIService.shared.completeBind(
                code: code,
                state: state
            )
            
            // 添加到列表
            accounts.append(newAccount)
            showBindSuccess = true
            
            // 清除state
            authState = nil
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 检查是否已绑定指定平台
    func isPlatformBound(_ platform: PlatformType) -> Bool {
        accounts.contains { $0.platform == platform && $0.status == .active }
    }
    
    /// 获取指定平台的账号
    func getAccount(for platform: PlatformType) -> Account? {
        accounts.first { $0.platform == platform && $0.status == .active }
    }
    
    /// 获取健康度等级
    func healthLevel(for score: Int) -> HealthLevel {
        HealthLevel.from(score: score)
    }
}

// MARK: - API扩展

extension APIService {
    
    /// 获取账号列表
    func getAccounts() async throws -> [Account] {
        let response: APIResponse<[Account]> = try await get("/accounts")
        return response.data ?? []
    }
    
    /// 获取授权URL
    func getAuthURL(platform: PlatformType) async throws -> String {
        let response: APIResponse<AuthURLResponse> = try await get("/accounts/auth-url", parameters: [
            "platform": platform.rawValue
        ])
        return response.data?.url ?? ""
    }
    
    /// 完成绑定
    func completeBind(code: String, state: String) async throws -> Account {
        let response: APIResponse<Account> = try await post("/accounts/bind", parameters: [
            "code": code,
            "state": state
        ])
        guard let account = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        return account
    }
    
    /// 解绑账号
    func unbindAccount(accountId: String) async throws {
        let _: APIResponse<EmptyResponse> = try await delete("/accounts/\(accountId)")
    }
    
    /// 同步账号数据
    func syncAccount(accountId: String) async throws -> AccountSyncResponse {
        let response: APIResponse<AccountSyncResponse> = try await post("/accounts/\(accountId)/sync")
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        return data
    }
}

// MARK: - 响应模型

/// 授权URL响应
struct AuthURLResponse: Codable {
    var url: String
}

/// 空响应
struct EmptyResponse: Codable {}

// MARK: - 预览

#if DEBUG
extension AccountViewModel {
    static let preview = AccountViewModel()
}
#endif
