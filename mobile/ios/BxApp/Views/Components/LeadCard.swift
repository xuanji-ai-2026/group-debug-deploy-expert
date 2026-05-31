//
//  LeadCard.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  商机卡片组件
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 商机卡片组件
/// 
/// 用于在商机列表中展示单个商机的主要信息。
struct LeadCard: View {
    
    // MARK: - 属性
    
    /// 商机
    let lead: Lead
    
    // MARK: - 界面
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // 头部：客户名称和意向等级
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(lead.customerName)
                        .font(.headline)
                        .lineLimit(1)
                    
                    Text(lead.customerPhone)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // 意向等级
                IntentionLevelIndicator(level: lead.intentionLevel)
            }
            
            // 来源信息
            HStack(spacing: 8) {
                // 来源平台
                SourcePlatformBadge(platform: lead.sourcePlatform)
                
                // 关键词
                if let keyword = lead.sourceKeyword, !keyword.isEmpty {
                    Text(keyword)
                        .font(.caption2)
                        .foregroundColor(.blue)
                        .lineLimit(1)
                }
            }
            
            // 截客内容预览
            if let content = lead.interceptedContent, !content.isEmpty {
                Text(content)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                    .padding(8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(8)
            }
            
            // 底部：跟进状态和跟进记录数
            HStack {
                // 跟进状态
                FollowUpStatusBadge(status: lead.followUpStatus)
                
                Spacer()
                
                // 跟进记录数
                HStack(spacing: 4) {
                    Image(systemName: "message.fill")
                        .font(.caption2)
                    Text("\(lead.followUpCount)")
                        .font(.caption)
                }
                .foregroundColor(.secondary)
                
                // 最后跟进时间
                if let lastFollowUp = lead.lastFollowUpAt {
                    Text(lastFollowUp.formatted(date: .abbreviated, time: .omitted))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            
            // 标签
            if !lead.tags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(lead.tags.prefix(3), id: \.self) { tag in
                            Text(tag)
                                .font(.caption2)
                                .foregroundColor(.secondary)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color(.secondarySystemBackground))
                                .cornerRadius(4)
                        }
                        
                        if lead.tags.count > 3 {
                            Text("+\(lead.tags.count - 3)")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

// MARK: - 意向等级指示器

struct IntentionLevelIndicator: View {
    let level: IntentionLevel
    
    var body: some View {
        VStack(spacing: 2) {
            Text(levelLevelText)
                .font(.caption)
                .fontWeight(.bold)
            
            Text(level.name)
                .font(.caption2)
        }
        .foregroundColor(levelColor)
        .frame(width: 50, height: 40)
        .background(levelColor.opacity(0.1))
        .cornerRadius(8)
    }
    
    private var levelLevelText: String {
        switch level {
        case .high: return "高"
        case .medium: return "中"
        case .low: return "低"
        case .none: return "无"
        }
    }
    
    private var levelColor: Color {
        switch level {
        case .high: return .red
        case .medium: return .orange
        case .low: return .blue
        case .none: return .gray
        }
    }
}

// MARK: - 来源平台徽章

struct SourcePlatformBadge: View {
    let platform: SourcePlatform
    
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: platform.iconName)
                .font(.caption2)
            Text(platform.name)
                .font(.caption)
        }
        .foregroundColor(.secondary)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(4)
    }
}

// MARK: - 跟进状态徽章

struct FollowUpStatusBadge: View {
    let status: FollowUpStatus
    
    var body: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(statusColor)
                .frame(width: 6, height: 6)
            Text(status.name)
                .font(.caption)
        }
        .foregroundColor(statusColor)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(statusColor.opacity(0.1))
        .cornerRadius(4)
    }
    
    private var statusColor: Color {
        switch status {
        case .notStarted: return .gray
        case .inProgress: return .blue
        case .closed: return .green
        case .lost: return .red
        case .paused: return .orange
        }
    }
}

// MARK: - 预览

#if DEBUG
struct LeadCard_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 16) {
            LeadCard(lead: .preview)
            LeadCard(lead: Lead(
                id: "2",
                customerName: "李四",
                customerPhone: "13900139000",
                sourcePlatform: .xiaohongshu,
                intentionLevel: .medium,
                followUpStatus: .inProgress,
                followUpCount: 2,
                ownerId: "1",
                tenantId: "1"
            ))
        }
        .padding()
        .background(Color(.systemGroupedBackground))
    }
}
#endif
