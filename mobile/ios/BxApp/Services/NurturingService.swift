//
//  NurturingService.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  养号策略服务 - 管理养号策略的创建、执行和监控
//
//  Created by 北极星AI团队 on 2026-05-20
//  Copyright © 2026 北极星AI. All rights reserved.
//

import Foundation
import Combine

/// 养号策略数据模型
struct NurturingStrategy: Codable, Identifiable {
    let id: Int64
    var accountId: Int64
    var accountName: String?
    var accountAvatar: String?
    var platform: String?
    var strategyName: String?
    var dailyTargets: DailyTargets?
    var durationDays: Int
    var riskLevel: String
    var enabled: Int
    var nurturingStatus: Int
    var createTime: String?
    var updateTime: String?
}

/// 每日目标
struct DailyTargets: Codable {
    var likeCount: Int = 10
    var commentCount: Int = 5
    var shareCount: Int = 2
    var followCount: Int = 3
    var browseDuration: Int = 30
}

/// 养号策略请求
struct NurturingStrategyRequest: Codable {
    let accountId: Int64
    let strategyName: String
    let dailyTargets: DailyTargets
    var durationDays: Int = 7
    var riskLevel: String = "LOW"
}

/// 养号执行进度
struct NurturingProgress: Codable {
    let strategyId: Int64
    let accountId: Int64
    var status: String
    var dailyTargets: [String: Int]?
    var completedCounts: [String: Int]?
    var progressPercentage: Double
    var startTime: String?
}

/// 养号模板
struct NurturingTemplate: Codable, Identifiable {
    var id: String { name }
    let name: String
    let description: String
    let dailyTargets: DailyTargets?
    let duration: Int
    let riskLevel: String
}

/// 风控检查请求
struct RiskCheckRequest: Codable {
    let eventType: String
    let userId: String
    var content: String?
    var targetId: String?
    var context: [String: Any]?

    enum CodingKeys: String, CodingKey {
        case eventType = "event_type"
        case userId = "user_id"
        case content
        case targetId = "target_id"
        case context
    }
}

/// 风控检查结果
struct RiskCheckResult: Codable {
    let riskLevel: Int
    let riskScore: Double
    let matchedRules: [String]
    let action: Int
    var message: String?
}

/// 风控规则
struct RiskRule: Codable, Identifiable {
    var id: Int64
    let ruleName: String
    let ruleType: Int
    let ruleConfig: String
    let action: Int
    let priority: Int
    let status: Int
}

/// 账号风险评分
struct RiskScore: Codable {
    let accountId: Int64
    let score: Int
    let level: String
    let lastCheckTime: String?
    let factors: [RiskFactor]?
}

/// 风险因子
struct RiskFactor: Codable {
    let name: String
    let score: Int
    let status: String
    var message: String?
}

/// 养号策略服务
class NurturingService {
    
    // MARK: - 单例
    
    static let shared = NurturingService()
    
    // MARK: - 属性
    
    private let apiService = APIService.shared
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    private init() {}
    
    // MARK: - 养号策略 CRUD
    
    /// 获取养号策略列表
    /// - Parameters:
    ///   - accountId: 可选账号ID筛选
    ///   - enabled: 可选状态筛选
    /// - Returns: 策略列表
    func getStrategies(accountId: Int64? = nil, enabled: Int? = nil) async throws -> [NurturingStrategy] {
        var params: Parameters = [:]
        if let accountId = accountId {
            params["account_id"] = accountId
        }
        if let enabled = enabled {
            params["enabled"] = enabled
        }
        
        let response: APIResponse<[NurturingStrategy]> = try await apiService.get(
            "nurturing/strategies",
            parameters: params
        )
        
        guard response.code == 200 else {
            throw NSError(domain: "NurturingService", code: response.code, userInfo: [NSLocalizedDescriptionKey: response.message ?? "获取策略列表失败"])
        }
        
        return response.data ?? []
    }
    
    /// 获取策略详情
    /// - Parameter strategyId: 策略ID
    /// - Returns: 策略详情
    func getStrategyDetail(strategyId: Int64) async throws -> NurturingStrategy {
        let response: APIResponse<NurturingStrategy> = try await apiService.get(
            "nurturing/strategy/\(strategyId)"
        )
        
        guard response.code == 200, let strategy = response.data else {
            throw NSError(domain: "NurturingService", code: response.code ?? 404, userInfo: [NSLocalizedDescriptionKey: response.message ?? "策略不存在"])
        }
        
        return strategy
    }
    
    /// 创建养号策略
    /// - Parameter request: 策略请求
    /// - Returns: 创建后的策略
    func createStrategy(_ request: NurturingStrategyRequest) async throws -> NurturingStrategy {
        let params: Parameters = [
            "account_id": request.accountId,
            "strategy_name": request.strategyName,
            "daily_targets": try request.dailyTargets.asDictionary(),
            "duration_days": request.durationDays,
            "risk_level": request.riskLevel
        ]
        
        let response: APIResponse<NurturingStrategy> = try await apiService.post(
            "nurturing/strategy",
            parameters: params
        )
        
        guard response.code == 200, let strategy = response.data else {
            throw NSError(domain: "NurturingService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "创建策略失败"])
        }
        
        return strategy
    }
    
    /// 更新养号策略
    /// - Parameters:
    ///   - strategyId: 策略ID
    ///   - request: 更新请求
    /// - Returns: 更新后的策略
    func updateStrategy(strategyId: Int64, _ request: NurturingStrategyRequest) async throws -> NurturingStrategy {
        let params: Parameters = [
            "account_id": request.accountId,
            "strategy_name": request.strategyName,
            "daily_targets": try request.dailyTargets.asDictionary(),
            "duration_days": request.durationDays,
            "risk_level": request.riskLevel
        ]
        
        let response: APIResponse<NurturingStrategy> = try await apiService.put(
            "nurturing/strategy/\(strategyId)",
            parameters: params
        )
        
        guard response.code == 200, let strategy = response.data else {
            throw NSError(domain: "NurturingService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "更新策略失败"])
        }
        
        return strategy
    }
    
    /// 删除养号策略
    /// - Parameter strategyId: 策略ID
    func deleteStrategy(strategyId: Int64) async throws {
        let _: APIResponse<[String: Any]> = try await apiService.delete(
            "nurturing/strategy/\(strategyId)"
        )
    }
    
    // MARK: - 策略控制
    
    /// 启用/禁用策略
    /// - Parameters:
    ///   - strategyId: 策略ID
    ///   - enabled: 是否启用
    /// - Returns: 操作结果
    func toggleStatus(strategyId: Int64, enabled: Bool) async throws -> Bool {
        let response: APIResponse<Bool> = try await apiService.put(
            "nurturing/strategy/\(strategyId)/status",
            parameters: ["status": enabled ? 1 : 0]
        )
        
        return response.data ?? false
    }
    
    /// 启动策略执行
    /// - Parameter strategyId: 策略ID
    /// - Returns: 是否成功启动
    func startStrategy(strategyId: Int64) async throws -> Bool {
        let response: APIResponse<Bool> = try await apiService.post(
            "nurturing/strategy/\(strategyId)/start"
        )
        
        return response.data ?? false
    }
    
    /// 停止策略执行
    /// - Parameter strategyId: 策略ID
    /// - Returns: 是否成功停止
    func stopStrategy(strategyId: Int64) async throws -> Bool {
        let response: APIResponse<Bool> = try await apiService.post(
            "nurturing/strategy/\(strategyId)/stop"
        )
        
        return response.data ?? false
    }
    
    // MARK: - 进度查询
    
    /// 获取执行进度
    /// - Parameter strategyId: 策略ID
    /// - Returns: 进度信息
    func getProgress(strategyId: Int64) async throws -> NurturingProgress {
        let response: APIResponse<NurturingProgress> = try await apiService.get(
            "nurturing/strategy/\(strategyId)/progress"
        )
        
        guard response.code == 200, let progress = response.data else {
            throw NSError(domain: "NurturingService", code: response.code ?? 404, userInfo: [NSLocalizedDescriptionKey: response.message ?? "无进度信息"])
        }
        
        return progress
    }
    
    /// 获取账号养号状态
    /// - Parameter accountId: 账号ID
    /// - Returns: 状态码（0-未开始，1-进行中，2-已完成）
    func getAccountStatus(accountId: Int64) async throws -> Int {
        let response: APIResponse<Int> = try await apiService.get(
            "nurturing/account/\(accountId)/status"
        )
        
        return response.data ?? 0
    }
    
    // MARK: - 模板
    
    /// 获取养号策略模板列表
    /// - Returns: 模板列表
    func getTemplates() async throws -> [NurturingTemplate] {
        let response: APIResponse<[NurturingTemplate]> = try await apiService.get(
            "nurturing/templates"
        )
        
        return response.data ?? []
    }
    
    // MARK: - 风控
    
    /// 执行风控检查
    /// - Parameter request: 检查请求
    /// - Returns: 检查结果
    func performRiskCheck(_ request: RiskCheckRequest) async throws -> RiskCheckResult {
        var params: Parameters = [
            "event_type": request.eventType,
            "user_id": request.userId
        ]
        
        if let content = request.content {
            params["content"] = content
        }
        if let targetId = request.targetId {
            params["target_id"] = targetId
        }
        if let context = request.context {
            params["context"] = context
        }
        
        let response: APIResponse<RiskCheckResult> = try await apiService.post(
            "risk/check",
            parameters: params
        )
        
        guard response.code == 200, let result = response.data else {
            throw NSError(domain: "NurturingService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "风控检查失败"])
        }
        
        return result
    }
    
    /// 获取风控规则列表
    /// - Parameters:
    ///   - page: 页码
    ///   - size: 每页数量
    /// - Returns: 规则列表
    func getRiskRules(page: Int = 1, size: Int = 20) async throws -> [RiskRule] {
        let response: APIResponse<[RiskRule]> = try await apiService.get(
            "risk/rules",
            parameters: ["page": page, "size": size]
        )
        
        return response.data ?? []
    }
    
    /// 获取账号风险评分
    /// - Parameter accountId: 账号ID
    /// - Returns: 风险评分
    func getAccountRiskScore(accountId: Int64) async throws -> RiskScore {
        let response: APIResponse<RiskScore> = try await apiService.get(
            "risk/score/\(accountId)"
        )
        
        guard response.code == 200, let score = response.data else {
            throw NSError(domain: "NurturingService", code: response.code ?? 404, userInfo: [NSLocalizedDescriptionKey: response.message ?? "获取风险评分失败"])
        }
        
        return score
    }
}

// MARK: - DailyTargets 扩展

extension DailyTargets {
    func asDictionary() throws -> [String: Any] {
        let data = try JSONEncoder().encode(self)
        guard let dict = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] else {
            throw NSError(domain: "DailyTargets", code: -1, userInfo: [NSLocalizedDescriptionKey: "序列化失败"])
        }
        return dict
    }
}
