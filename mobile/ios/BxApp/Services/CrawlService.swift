//
//  CrawlService.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  爬虫管理服务 - 评论抓取、分析、商机生成
//
//  Created by 北极星AI团队 on 2026-05-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine

/// 爬虫任务状态
enum CrawlTaskStatus: String, Codable {
    case pending = "PENDING"
    case running = "RUNNING"
    case paused = "PAUSED"
    case completed = "COMPLETED"
    case failed = "FAILED"
    case cancelled = "CANCELLED"
}

/// 平台代码
enum PlatformCode: String, Codable {
    case douyin = "DOUYIN"
    case xiaohongshu = "XIAOHONGSHU"
    case kuaishou = "KUAISHOU"
    case weibo = "WEIBO"
    case bilibili = "BILIBILI"
}

/// 目标类型
enum TargetType: String, Codable {
    case videoNote = "VIDEO_NOTE"
    case userProfile = "USER_PROFILE"
    case topic = "TOPIC"
    case searchKeyword = "SEARCH_KEYWORD"
}

/// 爬虫任务模型
struct CrawlTask: Codable, Identifiable {
    let id: Int64
    var tenantId: Int64?
    var platformCode: PlatformCode
    var targetType: TargetType
    var targetId: String?
    var targetUrl: String?
    var status: CrawlTaskStatus
    var progress: Double?
    var totalComments: Int?
    var fetchedComments: Int?
    var highIntentCount: Int?
    var withContactCount: Int?
    var generatedLeads: Int?
    var errorMessage: String?
    var createTime: String?
    var updateTime: String?
    var startTime: String?
    var endTime: String?
}

/// 创建任务请求
struct CreateCrawlTaskRequest {
    let platformCode: PlatformCode
    let targetType: TargetType
    let targetId: String
    var maxComments: Int? = 500
    var includeReply: Bool = false
    var keywordFilter: String?
    
    func toParameters() -> [String: Any] {
        var params: [String: Any] = [
            "platform_code": platformCode.rawValue,
            "target_type": targetType.rawValue,
            "target_id": targetId
        ]
        if let maxComments = maxComments {
            params["max_comments"] = maxComments
        }
        params["include_reply"] = includeReply
        if let keywordFilter = keywordFilter {
            params["keyword_filter"] = keywordFilter
        }
        return params
    }
}

/// 评论模型
struct CommentItem: Codable, Identifiable {
    let id: Int64
    var taskId: Int64?
    var content: String?
    var authorName: String?
    var authorAvatar: String?
    var likeCount: Int?
    var intentScore: Int?
    var intentLevel: String?
    var hasContact: Bool?
    var contactInfo: String?
    var contactType: String?
    var tags: [String]?
    var createTime: String?
}

/// 评论过滤结果
struct CommentFilterResult: Codable {
    let comments: [CommentItem]?
    let totalCount: Int?
    let highIntentCount: Int?
    let withContactCount: Int?
    let filteredBy: String?
}

/// 商机生成结果
struct LeadResult: Codable {
    let leads: [LeadFromComment]?
    let total: Int?
    let conversionRate: Double?
}

/// 从评论生成的商机
struct LeadFromComment: Codable, Identifiable {
    let id: Int64?
    var customerName: String?
    var customerPhone: String?
    var customerWechat: String?
    var sourceContent: String?
    var sourcePlatform: String?
    var intentionLevel: String?
    var intentionTags: [String]?
    var score: Int?
}

/// 生成商机请求
struct GenerateLeadsRequest {
    let minScore: Int?
    let levels: [String]?
    let requireContact: Bool
    let maxLeads: Int?
    
    func toParameters() -> [String: Any] {
        var params: [String: Any] = [
            "require_contact": requireContact
        ]
        if let minScore = minScore {
            params["min_score"] = minScore
        }
        if let levels = levels, !levels.isEmpty {
            params["levels"] = levels.joined(separator: ",")
        }
        if let maxLeads = maxLeads {
            params["max_leads"] = maxLeads
        }
        return params
    }
}

/// 爬虫管理服务
class CrawlService {
    
    // MARK: - 单例
    
    static let shared = CrawlService()
    
    // MARK: - 属性
    
    private let apiService = APIService.shared
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - 初始化
    
    private init() {}
    
    // MARK: - 任务管理 (与后端 CrawlController @RequestMapping("/api/crawl") 100%对齐)
    
    /// 获取爬虫任务列表
    /// - Returns: 任务列表
    func getCrawlTasks() async throws -> [CrawlTask] {
        let response: APIResponse<[CrawlTask]> = try await apiService.get(
            "/crawl/task/tasks"
        )
        
        guard response.code == 200 else {
            throw NSError(domain: "CrawlService", code: response.code, userInfo: [NSLocalizedDescriptionKey: response.message ?? "获取任务列表失败"])
        }
        
        return response.data ?? []
    }
    
    /// 创建抓取任务
    /// - Parameter request: 创建参数
    /// - Returns: 创建的任务
    func createCrawlTask(_ request: CreateCrawlTaskRequest) async throws -> CrawlTask {
        let response: APIResponse<CrawlTask> = try await apiService.post(
            "/crawl/task/task/create",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let task = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "创建任务失败"])
        }
        
        return task
    }
    
    /// 获取任务详情
    /// - Parameter taskId: 任务ID
    /// - Returns: 任务详情
    func getTaskDetail(taskId: Int64) async throws -> CrawlTask {
        let response: APIResponse<CrawlTask> = try await apiService.get(
            "/crawl/task/task/\(taskId)"
        )
        
        guard response.code == 200, let task = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 404, userInfo: [NSLocalizedDescriptionKey: response.message ?? "任务不存在"])
        }
        
        return task
    }
    
    /// 暂停任务
    /// - Parameter taskId: 任务ID
    func pauseTask(taskId: Int64) async throws {
        let _: APIResponse<String> = try await apiService.put(
            "/crawl/task/task/\(taskId)/pause",
            parameters: nil
        )
    }
    
    /// 停止任务
    /// - Parameter taskId: 任务ID
    func stopTask(taskId: Int64) async throws {
        let _: APIResponse<String> = try await apiService.put(
            "/crawl/task/task/\(taskId)/stop",
            parameters: nil
        )
    }
    
    /// 恢复任务
    /// - Parameter taskId: 任务ID
    func resumeTask(taskId: Int64) async throws {
        let _: APIResponse<String> = try await apiService.put(
            "/crawl/task/task/\(taskId)/resume",
            parameters: nil
        )
    }
    
    // MARK: - 评论管理 (与后端CrawlController 100%对齐)
    
    /// 获取任务评论列表（支持过滤）
    /// - Parameters:
    ///   - taskId: 任务ID
    ///   - page: 页码
    ///   - size: 每页数量
    ///   - minScore: 最小意向评分
    ///   - level: 意向等级
    ///   - onlyHighIntent: 仅高意向
    ///   - onlyWithContact: 仅含联系方式
    /// - Returns: 过滤后的评论结果
    func getTaskComments(
        taskId: Int64,
        page: Int = 1,
        size: Int = 20,
        minScore: Int? = nil,
        level: String? = nil,
        onlyHighIntent: Bool = false,
        onlyWithContact: Bool = false
    ) async throws -> CommentFilterResult {
        var params: Parameters = [
            "page": page,
            "size": size,
            "only_high_intent": onlyHighIntent,
            "only_with_contact": onlyWithContact
        ]
        
        if let minScore = minScore {
            params["min_score"] = minScore
        }
        if let level = level {
            params["level"] = level
        }
        
        let response: APIResponse<CommentFilterResult> = try await apiService.get(
            "/crawl/task/task/\(taskId)/comments",
            parameters: params
        )
        
        guard response.code == 200, let result = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 404, userInfo: [NSLocalizedDescriptionKey: response.message ?? "获取评论失败"])
        }
        
        return result
    }
    
    /// 分析评论（触发AI意图识别）
    /// - Parameter taskId: 任务ID
    /// - Returns: 分析结果消息
    func analyzeComments(taskId: Int64) async throws -> String {
        let response: APIResponse<String> = try await apiService.post(
            "/crawl/task/task/\(taskId)/analyze"
        )
        
        guard response.code == 200, let message = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "分析失败"])
        }
        
        return message
    }
    
    // MARK: - 商机生成 (与后端CrawlController 100%对齐)
    
    /// 从评论中批量生成商机线索
    /// - Parameters:
    ///   - taskId: 任务ID
    ///   - request: 生成条件
    /// - Returns: 生成的商机结果
    func generateLeads(
        taskId: Int64,
        _ request: GenerateLeadsRequest = GenerateLeadsRequest(minScore: nil, levels: nil, requireContact: true, maxLeads: nil)
    ) async throws -> LeadResult {
        let response: APIResponse<LeadResult> = try await apiService.post(
            "/crawl/task/task/\(taskId)/generate-leads",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let result = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "生成商机失败"])
        }
        
        return result
    }
    
    /// 从单条评论生成商机
    /// - Parameter commentId: 评论ID
    /// - Returns: 生成的商机
    func generateSingleLead(commentId: Int64) async throws -> LeadResult {
        let response: APIResponse<LeadResult> = try await apiService.post(
            "/crawl/task/comment/\(commentId)/analyze",
            parameters: ["comment_id": commentId]
        )
        
        guard response.code == 200, let result = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "生成商机失败"])
        }
        
        return result
    }
    
    // MARK: - 移动端专用接口 (MobileCrawlController - 轻量级优化)
    
    /// 快速创建请求（简化参数）
    struct QuickCreateRequest: Codable {
        let platformCode: String
        let targetType: String
        let targetId: String
        var maxComments: Int? = 500
        var includeReply: Bool = false
        
        func toParameters() -> [String: Any] {
            var params: [String: Any] = [
                "platform_code": platformCode,
                "target_type": targetType,
                "target_id": targetId
            ]
            if let maxComments = maxComments {
                params["max_comments"] = maxComments
            }
            params["include_reply"] = includeReply
            return params
        }
    }
    
    /// 移动端任务响应
    struct MobileTaskResponse: Codable {
        let taskId: Int64?
        let status: String?
        let platformCode: String?
        let message: String?
    }
    
    /// 移动端进度响应
    struct MobileProgressResponse: Codable {
        let taskId: Int64?
        let status: String?
        let progress: Double?
        let totalComments: Int?
        let fetchedComments: Int?
        let highIntentCount: Int?
        let estimatedRemainingMinutes: Int64?
    }
    
    /// 快速生成商机请求
    struct QuickGenerateLeadsRequest: Codable {
        var minScore: Int? = 70
        var maxLeads: Int? = 50
        
        func toParameters() -> [String: Any] {
            var params: [String: Any] = [:]
            if let minScore = minScore {
                params["min_score"] = minScore
            }
            if let maxLeads = maxLeads {
                params["max_leads"] = maxLeads
            }
            return params
        }
    }
    
    /// 移动端商机项
    struct MobileLeadItem: Codable, Identifiable {
        let id: Int64?
        var customerName: String?
        var customerPhone: String?       // 已脱敏
        var intentionLevel: String?
        var score: Int?
    }
    
    /// 移动端商机响应
    struct MobileLeadResponse: Codable {
        let total: Int?
        let conversionRate: Double?
        let leads: [MobileLeadItem]?
    }
    
    // MARK: - 移动端专用方法
    
    /// MOBILE-001: 一键创建抓取任务（简化参数）
    /// - Parameter request: 简化创建参数
    /// - Returns: 创建结果
    func quickCreateTask(_ request: QuickCreateRequest) async throws -> MobileTaskResponse {
        let response: APIResponse<MobileTaskResponse> = try await apiService.post(
            "/crawl/mobile/quick-create",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let result = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "快速创建失败"])
        }
        
        return result
    }
    
    /// MOBILE-002: 获取任务实时进度（轻量级）
    /// - Parameter taskId: 任务ID
    /// - Returns: 进度信息
    func getTaskProgress(taskId: Int64) async throws -> MobileProgressResponse {
        let response: APIResponse<MobileProgressResponse> = try await apiService.get(
            "/crawl/mobile/task/\(taskId)/progress"
        )
        
        guard response.code == 200, let progress = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 404, userInfo: [NSLocalizedDescriptionKey: response.message ?? "获取进度失败"])
        }
        
        return progress
    }
    
    /// MOBILE-003: 一键生成商机（批量+过滤）
    /// - Parameters:
    ///   - taskId: 任务ID
    ///   - request: 生成条件（可选，有默认值）
    /// - Returns: 生成的商机列表
    func quickGenerateLeads(
        taskId: Int64,
        _ request: QuickGenerateLeadsRequest = QuickGenerateLeadsRequest()
    ) async throws -> MobileLeadResponse {
        let response: APIResponse<MobileLeadResponse> = try await apiService.post(
            "/crawl/mobile/task/\(taskId)/quick-generate-leads",
            parameters: request.toParameters()
        )
        
        guard response.code == 200, let result = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 500, userInfo: [NSLocalizedDescriptionKey: response.message ?? "一键生成失败"])
        }
        
        return result
    }
    
    /// MOBILE-004: 获取我的任务列表（移动端分页优化）
    /// - Parameters:
    ///   - status: 任务状态筛选
    ///   - page: 页码
    ///   - size: 每页数量
    /// - Returns: 任务列表
    func getMyTasks(
        status: String = "RUNNING",
        page: Int = 1,
        size: Int = 10
    ) async throws -> [CrawlTask] {
        let response: APIResponse<[CrawlTask]> = try await apiService.get(
            "/crawl/mobile/my-tasks",
            parameters: [
                "status": status,
                "page": page,
                "size": size
            ]
        )
        
        guard response.code == 200, let tasks = response.data else {
            throw NSError(domain: "CrawlService", code: response.code ?? 404, userInfo: [NSLocalizedDescriptionKey: response.message ?? "获取任务列表失败"])
        }
        
        return tasks
    }
}
