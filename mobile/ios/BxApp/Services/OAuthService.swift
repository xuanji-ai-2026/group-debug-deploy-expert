//
//  OAuthService.swift
//  BxApp (北极星AI - iOS端)
//
//  功能说明:
//  1. 管理OAuth授权流程（PKCE + Universal Links）
//  2. 处理社交平台回调（抖音/小红书/微信）
//  3. 安全存储Token和敏感数据（Keychain）
//  4. 提供完整的用户绑定/解绑流程
//
//  技术栈:
//  - Swift 5.9+ / SwiftUI / Combine
//  - URLSession + async/await
//  - KeychainAccess (安全存储)
//  - CryptoKit (PKCE S256计算)
//
//  作者: 北极星AI团队
//  版本: v2.0 (2026-05-20)
//

import Foundation
import AuthenticationServices
import CryptoKit
import Security

// MARK: - OAuth Service

class OAuthService: ObservableObject {
    
    static let shared = OAuthService()
    
    private let keychainKey = "com.beijixing.app.oauth"
    private let baseURL = "https://api.beijixing.com/api/mobile/oauth"
    
    @Published var isAuthorizing = false
    @Published var boundAccounts: [BoundSocialAccount] = []
    
    private init() {}
}

// MARK: - Public Methods

extension OAuthService {
    
    /// 启动OAuth授权流程
    /// - Parameter platform: 平台代码 (DOUYIN/XIAOHONGSHU/KUAISHOU)
    func startAuthorization(platform: Platform) async throws -> AuthorizationURLResponse {
        isAuthorizing = true
        defer { isAuthorizing = false }
        
        print("📱 [iOS] 开始OAuth授权流程: \(platform.rawValue)")
        
        // 1. 调用后端生成授权URL
        let url = URL(string: "\(baseURL)/authorize/\(platform.rawValue)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(AuthManager.shared.token, forHTTPHeaderField: "Authorization")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw OAuthError.networkError("生成授权链接失败")
        }
        
        let result = try JSONDecoder().decode(ApiResponse<AuthorizationURLResponse>.self, from: data)
        
        guard let authResponse = result.data else {
            throw OAuthError.serverError(result.message ?? "未知错误")
        }
        
        // 2. 安全保存PKCE参数到Keychain
        saveToKeychain(key: "code_verifier_\(platform.rawValue)", value: authResponse.codeVerifier)
        saveToKeychain(key: "state_\(platform.rawValue)", value: authResponse.state)
        
        print("✅ [iOS] 授权URL已获取，长度: \(authResponse.authUrl.count)")
        
        return authResponse
    }
    
    /// 处理Universal Links回调
    /// - Parameter url: 回调URL (https://yourapp.com/oauth/callback/{platform}?code=xxx&state=yyy)
    func handleCallback(url: URL) async throws -> OAuthSuccessResult {
        print("🔄 [iOS] 收到OAuth回调: \(url.absoluteString)")
        
        // 1. 从URL提取平台代码
        guard let platform = extractPlatform(from: url) else {
            throw OAuthError.invalidURL("无法识别平台")
        }
        
        // 2. 提取查询参数
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let code = components.queryItems?.first(where: { $0.name == "code" })?.value,
              let state = components.queryItems?.first(where: { $0.name == "state" })?.value else {
            throw OAuthError.invalidURL("缺少必要参数")
        }
        
        print("📝 [iOS] 提取参数: platform=\(platform), code=\(String(code.prefix(10)))..., state=\(state)")
        
        // 3. 验证state参数（防CSRF）
        guard let savedState = loadFromKeychain(key: "state_\(platform.rawValue)"),
              savedState == state else {
            throw OAuthError.securityViolation("State验证失败")
        }
        
        // 4. 获取code_verifier（用于PKCE验证）
        let codeVerifier = loadFromKeychain(key: "code_verifier_\(platform.rawValue)")
        
        // 5. 调用后端API交换Token
        let tokenResult = try await exchangeCodeForTokens(
            platform: platform,
            code: code,
            state: state,
            codeVerifier: codeVerifier
        )
        
        // 6. 清理临时敏感数据
        removeFromKeychain(key: "code_verifier_\(platform.rawValue)")
        removeFromKeychain(key: "state_\(platform.rawValue)")
        
        print("🎉 [iOS] OAuth授权成功: accountId=\(tokenResult.accountId)")
        
        return tokenResult
    }
    
    /// 刷新Token
    func refreshToken(accountId: Int64) async throws -> RefreshTokenResponse {
        let url = URL(string: "\(baseURL)/refresh/\(accountId)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(AuthManager.shared.token, forHTTPHeaderField: "Authorization")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw OAuthError.refreshFailed
        }
        
        let result = try JSONDecoder().decode(ApiResponse<RefreshTokenResponse>.self, from: data)
        return result.data!
    }
    
    /// 解除绑定账号
    func unbindAccount(accountId: Int64) async throws {
        let url = URL(string: "\(baseURL)/unbind/\(accountId)")!
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.setValue(AuthManager.shared.token, forHTTPHeaderField: "Authorization")
        
        let (_, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw OAuthError.unbindFailed
        }
        
        print("✅ [iOS] 账号解绑成功: accountId=\(accountId)")
    }
    
    /// 获取已绑定账号列表
    func fetchBoundAccounts() async throws {
        let url = URL(string: "\(baseURL)/accounts")!
        var request = URLRequest(url: url)
        request.setValue(AuthManager.shared.token, forHTTPHeaderField: "Authorization")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw OAuthError.networkError("获取账号列表失败")
        }
        
        let result = try JSONDecoder().decode(ApiResponse<[BoundSocialAccount]>.self, from: data)
        self.boundAccounts = result.data ?? []
    }
}

// MARK: - Private Methods

private extension OAuthService {
    
    func exchangeCodeForTokens(platform: Platform, code: String, 
                                state: String, codeVerifier: String?) async throws -> OAuthSuccessResult {
        var components = URLComponents(string: "\(baseURL)/callback/\(platform.rawValue)")!
        
        var queryItems = [
            URLQueryItem(name: "code", value: code),
            URLQueryItem(name: "state", value: state)
        ]
        
        if let verifier = codeVerifier {
            queryItems.append(URLQueryItem(name: "codeVerifier", value: verifier))
        }
        
        components.queryItems = queryItems
        
        var request = URLRequest(url: components.url!)
        request.setValue(AuthManager.shared.token, forHTTPHeaderField: "Authorization")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw OAuthError.tokenExchangeFailed
        }
        
        let result = try JSONDecoder().decode(ApiResponse<OAuthSuccessResult>.self, from: data)
        return result.data!
    }
    
    func extractPlatform(from url: URL) -> Platform? {
        let pathComponents = url.pathComponents
        
        guard pathComponents.count >= 4,
              pathComponents[0] == "/",
              pathComponents[1] == "oauth",
              pathComponents[2] == "callback" else {
            return nil
        }
        
        let platformStr = pathComponents[3].uppercased()
        return Platform(rawValue: platformStr)
    }
}

// MARK: - Keychain Helpers

private extension OAuthService {
    
    func saveToKeychain(key: String, value: String) {
        let data = Data(value.utf8)
        
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: "\(keychainKey).\(key)",
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
    }
    
    func loadFromKeychain(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: "\(keychainKey).\(key)",
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        
        guard status == errSecSuccess,
              let data = item as? Data,
              let string = String(data: data, encoding: .utf8) else {
            return nil
        }
        
        return string
    }
    
    func removeFromKeychain(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: "\(keychainKey).\(key)"
        ]
        
        SecItemDelete(query as CFDictionary)
    }
}

// MARK: - Data Models

struct AuthorizationURLResponse: Codable {
    let platform: String
    let authUrl: String
    let state: String
    let codeVerifier: String
    let expiresIn: Int
}

struct OAuthSuccessResult: Codable {
    let accountId: Int64
    let platform: String
    let nickname: String?
    let avatar: String?
    let openId: String?
    let scopes: [String]?
    let bindTime: String?
}

struct RefreshTokenResponse: Codable {
    let accountId: Int64
    let refreshedAt: String?
    let message: String?
}

struct BoundSocialAccount: Codable, Identifiable {
    let id: Int64
    let platformCode: String
    let platformName: String?
    let nickname: String?
    let avatar: String?
    let openId: String?
    let status: Int
    let tokenExpireTime: String?
    let boundAt: String?
    
    var displayName: String {
        switch platformCode.uppercase() {
        case "DOUYIN": return "抖音"
        case "XIAOHONGSHU": return "小红书"
        case "KUAISHOU": return "快手"
        case "WECHAT": return "微信"
        default: return platformCode
        }
    }
    
    var isActive: Bool { status == 1 }
    
    var statusText: String {
        switch status {
        case 1: return "✅ 正常"
        case 0: return "❌ 已解绑"
        case -1: return "⚠️ Token已过期"
        default: return "未知状态"
        }
    }
}

enum Platform: String, CaseIterable {
    case douyin = "DOUYIN"
    case xiaohongshu = "XIAOHONGSHU"
    case kuaishou = "KUAISHOU"
    case wechat = "WECHAT"
    
    var displayName: String {
        switch self {
        case .douyin: return "抖音"
        case .xiaohongshu: return "小红书"
        case .kuaishou: return "快手"
        case .wechat: return "微信"
        }
    }
}

enum OAuthError: LocalizedError {
    case networkError(String)
    case serverError(String)
    case invalidURL(String)
    case securityViolation(String)
    case tokenExchangeFailed
    case refreshFailed
    case unbindFailed
    
    var errorDescription: String? {
        switch self {
        case .networkError(let msg): return msg
        case .serverError(let msg): return msg
        case .invalidURL(let msg): return msg
        case .securityViolation(let msg): return "安全错误: \(msg)"
        case .tokenExchangeFailed: return "Token交换失败"
        case .refreshFailed: return "Token刷新失败"
        case .unbindFailed: return "解绑失败"
        }
    }
}
