//
//  MessageService.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  消息服务 - 私信群发、模板管理、发送记录
//
//  Created by 北极星AI团队 on 2026-05-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine

/// 消息类型
enum MessageType: String, Codable {
    case text = "TEXT"
    case image = "IMAGE"
    case video = "VIDEO"
    case link = "LINK"
    case card = "CARD"
}

/// 消息状态
enum MessageStatus: String, Codable {
    case pending = "PENDING"
    case sending = "SENDING"
    case sent = "SENT"
    case delivered = "DELIVERED"
    case read = "READ"
    case failed = "FAILED"
}

/// 发送结果
struct MessageResult: Codable {
    let success: Bool
    let sentCount: Int?
    let failedCount: Int?
    let messageId: String?
    let errorMessage: String?
}

/// 发送消息请求
struct SendMessageRequest {
    let targetId: String
    let platformCode: PlatformCode
    let messageType: MessageType
    let content: String
    var templateId: Int64?
    
    func toParameters() -> [String: Any] {
        var params: [String: Any] = [
            "target_id": targetId,
            "platform_code": platformCode.rawValue,
            "message_type": messageType.rawValue,
            "content": content
        ]
        if let templateId = templateId {
            params["template_id"] = templateId
        }
        return params
    }
}

/// 批量发送请求
struct BatchSendMessageRequest {
    let targetIds: [String]
    let platformCode: PlatformCode
    let messageType: MessageType
    let content: String
    var templateId: Int64?
    var intervalSeconds: Int = 30
    
    func toParameters() -> [String: Any] {
        var params: [String: Any] = [
            "target_ids": targetIds.joined(separator: ","),
            "platform_code": platformCode.rawValue,
            "message_type": messageType.rawValue,
            "content": content,
            "interval_seconds": intervalSeconds
        ]
        if let templateId = templateId {
            params["template_id"] = templateId
        }
        return params
    }
}

/// 批量生成商机请求
struct BatchGenerateLeadsRequest {
    let messageIds: [Int64]
    
    func toParameters() -> [String: Any] {
        return ["message_ids": messageIds.map { String($0) }.joined(separator: ",")]
    }
}

/// 消息模板
struct MessageTemplate: Codable, Identifiable {
    let id: Int64
    var name: String
    var content: String
    var platformCode: PlatformCode
    var intentLevel: String
    var usageCount: Int?
    var createTime: String?
    var updateTime: String?
}

/// 创建模板请求
struct CreateTemplateRequest {
    let name: String
    let content: String
    let platformCode: PlatformCode
    var intentLevel: String = "MEDIUM"
    
    func toParameters() -> [String: Any] {
        return [
            "name": name,
            "content": content,
            "platform_code": platformCode.rawValue,
            "intent_level": intentLevel
        ]
    }
}

/// 消息服务
class MessageService {
    
    // MARK: - 单例
    
    static let shared = MessageService()
    
    // MARK: - 属性
    
    private let apiService = APIService.shared
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    private init() {}
    
    // MARK: - 消息发送
    
    /// 发送单条私信
    /// - Parameter request: 发送参数
    /// - Returns: 发送结果
    func sendMessage(_ request: SendMessageRequest) async throws -> MessageResult {
        let response: APIResponse<MessageResult> = try await apiService.post(
            "/messages/send",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let result = response.data else {
            throw NSError(domain: "MessageService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "发送失败"])
        }
        
        return result
    }
    
    /// 批量发送私信（带间隔控制）
    /// - Parameter request: 批量参数
    /// - Returns: 批量发送结果
    func batchSend(_ request: BatchSendMessageRequest) async throws -> MessageResult {
        let response: APIResponse<MessageResult> = try await apiService.post(
            "/messages/batch-send",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let result = response.data else {
            throw NSError(domain: "MessageService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "批量发送失败"])
        }
        
        return result
    }
    
    /// 从发送记录批量生成商机线索
    /// - Parameter request: 参数
    /// - Returns: 商机生成结果
    func batchGenerateLeads(_ request: BatchGenerateLeadsRequest) async throws -> LeadResult {
        let response: APIResponse<LeadResult> = try await apiService.post(
            "/messages/batch-generate-leads",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let result = response.data else {
            throw NSError(domain: "MessageService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "生成商机失败"])
        }
        
        return result
    }
    
    // MARK: - 模板管理
    
    /// 获取消息模板列表
    /// - Parameters:
    ///   - platformCode: 平台筛选
    ///   - intentLevel: 意向等级筛选
    /// - Returns: 模板列表
    func getTemplates(
        platformCode: PlatformCode? = nil,
        intentLevel: String? = nil
    ) async throws -> [MessageTemplate] {
        var params: Parameters = [:]
        if let platformCode = platformCode {
            params["platform_code"] = platformCode.rawValue
        }
        if let intentLevel = intentLevel {
            params["intent_level"] = intentLevel
        }
        
        let response: APIResponse<[MessageTemplate]> = try await apiService.get(
            "/messages/templates",
            parameters: params
        )
        
        return response.data ?? []
    }
    
    /// 创建消息模板
    /// - Parameter request: 创建参数
    /// - Returns: 创建的模板
    func createTemplate(_ request: CreateTemplateRequest) async throws -> MessageTemplate {
        let response: APIResponse<MessageTemplate> = try await apiService.post(
            "/messages/template/create",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let template = response.data else {
            throw NSError(domain: "MessageService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "创建模板失败"])
        }
        
        return template
    }
    
    /// 删除消息模板
    /// - Parameter templateId: 模板ID
    func deleteTemplate(templateId: Int64) async throws {
        let _: APIResponse<String> = try await apiService.delete(
            "/messages/template/\(templateId)"
        )
    }
}
