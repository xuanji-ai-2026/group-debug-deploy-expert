//
//  ContentService.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  内容运营服务 - AI生成、模板管理、多平台发布
//
//  Created by 北极星AI团队 on 2026-05-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine

/// 内容类型
enum ContentType: String, Codable {
    case text = "TEXT"
    case image = "IMAGE"
    case video = "VIDEO"
    case article = "ARTICLE"
}

/// 发布状态
enum PublishStatus: String, Codable {
    case draft = "DRAFT"
    case pending = "PENDING"
    case publishing = "PUBLISHING"
    case published = "PUBLISHED"
    case failed = "FAILED"
    case rejected = "REJECTED"
}

/// AI生成的内容
struct GeneratedContent: Codable, Identifiable {
    let id: Int64?
    var title: String?
    var content: String?
    var contentType: ContentType
    var platformCode: PlatformCode
    var status: PublishStatus?
    var publishUrl: String?
    var publishTime: String?
    var createTime: String?
}

/// 内容生成请求
struct GenerateContentRequest {
    let topic: String
    let contentType: ContentType
    let platformCode: PlatformCode
    var style: String? = "professional"
    var keywords: [String]?
    var length: Int? = 500
    
    func toParameters() -> [String: Any] {
        var params: [String: Any] = [
            "topic": topic,
            "content_type": contentType.rawValue,
            "platform_code": platformCode.rawValue
        ]
        if let style = style {
            params["style"] = style
        }
        if let keywords = keywords, !keywords.isEmpty {
            params["keywords"] = keywords.joined(separator: ",")
        }
        if let length = length {
            params["length"] = length
        }
        return params
    }
}

/// 发布请求
struct PublishContentRequest {
    let contentId: Int64
    let platformCodes: [PlatformCode]
    var scheduleTime: String?
    
    func toParameters() -> [String: Any] {
        var params: [String: Any] = [
            "content_id": contentId,
            "platform_codes": platformCodes.map { $0.rawValue }.joined(separator: ",")
        ]
        if let scheduleTime = scheduleTime {
            params["schedule_time"] = scheduleTime
        }
        return params
    }
}

/// 内容模板
struct ContentTemplate: Codable, Identifiable {
    let id: Int64
    var name: String
    var type: ContentType
    var platformCode: PlatformCode
    var templateContent: String?
    var variables: [String]?
    var usageCount: Int?
}

/// 内容服务
class ContentService {
    
    // MARK: - 单例
    
    static let shared = ContentService()
    
    // MARK: - 属性
    
    private let apiService = APIService.shared
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    private init() {}
    
    // MARK: - AI内容生成
    
    /// 使用AI生成内容
    /// - Parameter request: 生成参数
    /// - Returns: 生成的内容
    func generateContent(_ request: GenerateContentRequest) async throws -> GeneratedContent {
        let response: APIResponse<GeneratedContent> = try await apiService.post(
            "/content/generate",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let content = response.data else {
            throw NSError(domain: "ContentService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "内容生成失败"])
        }
        
        return content
    }
    
    /// 获取内容模板列表
    /// - Parameter type: 类型筛选
    /// - Returns: 模板列表
    func getTemplates(type: ContentType? = nil) async throws -> [ContentTemplate] {
        var params: Parameters = [:]
        if let type = type {
            params["type"] = type.rawValue
        }
        
        let response: APIResponse<[ContentTemplate]> = try await apiService.get(
            "/content/templates",
            parameters: params
        )
        
        return response.data ?? []
    }
    
    // MARK: - 多平台发布
    
    /// 发布内容到指定平台（支持多平台同时发布）
    /// - Parameter request: 发布参数
    /// - Returns: 发布结果
    func publishContent(_ request: PublishContentRequest) async throws -> [PublishResult] {
        let response: APIResponse<[PublishResult]> = try await apiService.post(
            "/content/publish",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let results = response.data else {
            throw NSError(domain: "ContentService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "发布失败"])
        }
        
        return results
    }
    
    /// 获取发布历史记录
    /// - Parameters:
    ///   - page: 页码
    ///   - size: 每页数量
    /// - Returns: 历史记录
    func getPublishHistory(page: Int = 1, size: Int = 20) async throws -> PageResponse<GeneratedContent> {
        let params: Parameters = [
            "page": page,
            "size": size
        ]
        
        let response: APIResponse<PageResponse<GeneratedContent>> = try await apiService.get(
            "/content/history",
            parameters: params
        )
        
        guard response.code == 200, let data = response.data else {
            throw NSError(domain: "ContentService", code: response.code ?? 404, userInfo: [NSLocalizedDescriptionKey: response.message ?? "获取历史失败"])
        }
        
        return data
    }
}

/// 发布结果
struct PublishResult: Codable {
    let platformCode: PlatformCode
    let success: Bool
    let publishUrl: String?
    let errorMessage: String?
}
