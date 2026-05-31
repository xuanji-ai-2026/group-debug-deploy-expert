//
//  LeadRepository.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  商机数据仓库 - 已修复：所有API路径与后端Controller对齐
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

/// 商机数据仓库
///
/// 负责商机相关数据的获取、缓存、CRUD操作。
/// 所有API路径已与后端 LeadController (@RequestMapping("/lead")) 完全对齐。
class LeadRepository {
    
    // MARK: - 单例
    
    static let shared = LeadRepository()
    
    // MARK: - 属性
    
    /// 内存缓存
    private var cache: [String: Lead] = [:]
    
    /// 列表缓存
    private var listCache: [String: [Lead]] = [:]
    
    /// 缓存有效期（秒）
    private let cacheExpiration: TimeInterval = 120
    
    /// 缓存时间记录
    private var cacheTimestamps: [String: Date] = [:]
    
    // MARK: - 初始化
    
    private init() {}
    
    // MARK: - 公开方法 - 列表
    
    /// 获取商机列表
    /// - Parameter filter: 筛选条件
    /// - Returns: 商机分页列表
    func fetchLeads(filter: LeadFilter) async throws -> PageResponse<Lead> {
        // 构建查询参数（匹配后端LeadQueryDTO）
        var params: [String: Any] = [
            "page": filter.page,
            "pageSize": filter.pageSize
        ]
        
        if let keyword = filter.keyword {
            params["keyword"] = keyword
        }
        
        if let levels = filter.intentionLevels, !levels.isEmpty {
            params["level"] = levels.first?.rawValue
        }
        
        if let statuses = filter.followUpStatuses, !statuses.isEmpty {
            params["status"] = statuses.first?.rawValue
        }
        
        if let platforms = filter.sourcePlatforms, !platforms.isEmpty {
            params["source"] = platforms.first?.rawValue
        }
        
        // 发送请求 - 使用POST方法（与后端一致）
        let response: APIResponse<PageResponse<Lead>> = try await APIService.shared.post(
            "/lead/list",
            parameters: params
        )
        
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        updateListCache(data.list, filter: filter)
        
        return data
    }
    
    /// 搜索商机（复用列表接口，带关键词筛选）
    /// - Parameters:
    ///   - keyword: 关键词
    ///   - page: 页码
    /// - Returns: 商机分页列表
    func searchLeads(keyword: String, page: Int = 1) async throws -> PageResponse<Lead> {
        var filter = LeadFilter()
        filter.keyword = keyword
        filter.page = page
        
        return try await fetchLeads(filter: filter)
    }
    
    /// 获取商机统计数据（暂不可用，后端未实现此端点）
    /// - Returns: 商机统计数据
    func fetchLeadStatistics() async throws -> LeadStatistics {
        throw APIException(code: -1, message: "统计功能暂未实现")
    }
    
    // MARK: - 公开方法 - 详情
    
    /// 获取商机详情
    /// - Parameter id: 商机ID
    /// - Returns: 商机详情
    func getLeadDetail(id: String) async throws -> Lead {
        if let cached = getCachedLead(id: id) {
            Task { try? await refreshLead(id: id) }
            return cached
        }
        
        let response: APIResponse<Lead> = try await APIService.shared.get(
            "/lead/\(id)"
        )
        
        guard let lead = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        cacheLead(lead)
        
        return lead
    }
    
    /// 刷新商机
    /// - Parameter id: 商机ID
    func refreshLead(id: String) async throws {
        let response: APIResponse<Lead> = try await APIService.shared.get(
            "/lead/\(id)"
        )
        
        guard let lead = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        cacheLead(lead)
    }
    
    // MARK: - 公开方法 - CRUD
    
    /// 创建商机
    /// - Parameter lead: 商机信息
    /// - Returns: 创建的商机
    func createLead(_ lead: Lead) async throws -> Lead {
        let params: [String: Any] = [
            "customerName": lead.customerName,
            "customerPhone": lead.customerPhone,
            "customerWechat": lead.customerWechat ?? "",
            "customerCompany": lead.customerCompany ?? "",
            "customerPosition": lead.customerPosition ?? "",
            "customerAddress": lead.customerAddress ?? "",
            "sourcePlatform": lead.sourcePlatform.rawValue,
            "sourceKeyword": lead.sourceKeyword ?? "",
            "interceptedContent": lead.interceptedContent ?? "",
            "intentionLevel": lead.intentionLevel.rawValue,
            "intentionTags": lead.intentionTags.joined(separator: ","),
            "remark": lead.remark ?? ""
        ]
        
        let response: APIResponse<Lead> = try await APIService.shared.post(
            "/lead",
            parameters: params
        )
        
        guard let newLead = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        clearListCache()
        
        return newLead
    }
    
    /// 更新商机
    /// - Parameter lead: 商机信息
    /// - Returns: 更新后的商机
    func updateLead(_ lead: Lead) async throws -> Lead {
        let params: [String: Any] = [
            "customerName": lead.customerName,
            "customerPhone": lead.customerPhone,
            "customerWechat": lead.customerWechat ?? "",
            "customerCompany": lead.customerCompany ?? "",
            "customerPosition": lead.customerPosition ?? "",
            "customerAddress": lead.customerAddress ?? "",
            "intentionLevel": lead.intentionLevel.rawValue,
            "intentionTags": lead.intentionTags.joined(separator: ","),
            "followUpStatus": lead.followUpStatus.rawValue,
            "remark": lead.remark ?? ""
        ]
        
        let response: APIResponse<Lead> = try await APIService.shared.put(
            "/lead/\(lead.id)",
            parameters: params
        )
        
        guard let updatedLead = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        cacheLead(updatedLead)
        clearListCache()
        
        return updatedLead
    }
    
    /// 更新商机状态
    /// - Parameters:
    ///   - id: 商机ID
    ///   - status: 新状态
    /// - Returns: 更新后的商机
    func updateLeadStatus(id: String, status: FollowUpStatus) async throws -> Lead {
        let params: [String: Any] = [
            "status": status.rawValue
        ]
        
        let response: APIResponse<Lead> = try await APIService.shared.post(
            "/lead/\(id)/status",
            parameters: params
        )
        
        guard let lead = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        
        cacheLead(lead)
        clearListCache()
        
        return lead
    }
    
    /// 更新商机意向等级（暂不可用，后端无此端点）
    /// - Parameters:
    ///   - id: 商机ID
    ///   - level: 新意向等级
    /// - Returns: 更新后的商机
    func updateLeadIntention(id: String, level: IntentionLevel) async throws -> Lead {
        throw APIException(code: -1, message: "意向等级修改功能暂未实现")
    }
    
    /// 删除商机
    /// - Parameter id: 商机ID
    func deleteLead(id: String) async throws {
        let response: APIResponse<String?> = try await APIService.shared.delete(
            "/lead/\(id)"
        )
        
        if !response.isSuccess {
            throw APIException(code: response.code, message: response.message)
        }
        
        cache.removeValue(forKey: id)
        clearListCache()
    }
    
    // MARK: - 公开方法 - 跟进记录（待后端实现LeadFollowUpController）
    
    /// 获取跟进记录
    /// - Parameter leadId: 商机ID
    /// - Returns: 跟进记录列表
    func getFollowUpRecords(leadId: String) async throws -> [FollowUpRecord] {
        throw APIException(code: -1, message: "跟进记录功能暂未实现")
    }
    
    /// 添加跟进记录
    /// - Parameters:
    ///   - leadId: 商机ID
    ///   - record: 跟进记录
    /// - Returns: 添加的跟进记录
    func addFollowUpRecord(leadId: String, record: FollowUpRecord) async throws -> FollowUpRecord {
        throw APIException(code: -1, message: "添加跟进记录功能暂未实现")
    }
    
    /// 同步商机
    func syncLeads() async throws {
        print("[LeadRepository] 离线数据同步(预留)")
    }
    
    /// 清除缓存
    func clearCache() {
        cache.removeAll()
        listCache.removeAll()
        cacheTimestamps.removeAll()
    }
    
    // MARK: - 私有方法
    
    /// 获取缓存的商机
    private func getCachedLead(id: String) -> Lead? {
        guard let lead = cache[id],
              let timestamp = cacheTimestamps[id] else {
            return nil
        }
        
        if Date().timeIntervalSince(timestamp) > cacheExpiration {
            cache.removeValue(forKey: id)
            cacheTimestamps.removeValue(forKey: id)
            return nil
        }
        
        return lead
    }
    
    /// 缓存商机
    private func cacheLead(_ lead: Lead) {
        cache[lead.id] = lead
        cacheTimestamps[lead.id] = Date()
    }
    
    /// 更新列表缓存
    private func updateListCache(_ leads: [Lead], filter: LeadFilter) {
        let key = cacheKey(for: filter)
        listCache[key] = leads
        cacheTimestamps[key] = Date()
    }
    
    /// 清除列表缓存
    private func clearListCache() {
        listCache.removeAll()
    }
    
    /// 生成缓存键
    private func cacheKey(for filter: LeadFilter) -> String {
        var key = "leads_page\(filter.page)"
        
        if let platforms = filter.sourcePlatforms {
            key += "_p\(platforms.map { $0.rawValue }.joined())"
        }
        
        if let levels = filter.intentionLevels {
            key += "_l\(levels.map { $0.rawValue }.joined())"
        }
        
        if let keyword = filter.keyword {
            key += "_k\(keyword)"
        }
        
        return key
    }
}

// MARK: - Date扩展

extension Date {
    func toISOString() -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.string(from: self)
    }
}
