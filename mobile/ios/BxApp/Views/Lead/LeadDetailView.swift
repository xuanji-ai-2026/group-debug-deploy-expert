//
//  LeadDetailView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  商机详情视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 商机详情视图
/// 
/// 展示商机的详细信息，包括客户资料、跟进记录等。
struct LeadDetailView: View {
    
    // MARK: - 属性
    
    /// 商机
    let lead: Lead
    
    /// 环境
    @Environment(\.dismiss) private var dismiss
    
    /// 跟进记录
    @State private var followUpRecords: [FollowUpRecord] = []
    
    /// 是否加载中
    @State private var isLoading = true
    
    /// 是否显示添加跟进
    @State private var showAddFollowUp = false
    
    /// 选中的标签页
    @State private var selectedTab = 0
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 0) {
                    // 基本信息卡片
                    basicInfoCard
                        .padding()
                    
                    // 标签页选择器
                    tabSelector
                        .padding(.horizontal)
                    
                    // 标签页内容
                    tabContent
                        .padding()
                }
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("商机详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(action: {}) {
                            Label("编辑", systemImage: "pencil")
                        }
                        
                        Button(action: {}) {
                            Label("复制", systemImage: "doc.on.doc")
                        }
                        
                        Divider()
                        
                        Button(role: .destructive, action: {}) {
                            Label("删除", systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .sheet(isPresented: $showAddFollowUp) {
                AddFollowUpView(leadId: lead.id) { record in
                    followUpRecords.insert(record, at: 0)
                }
            }
        }
        .onAppear {
            Task { await loadFollowUpRecords() }
        }
    }
    
    // MARK: - 组件 - 基本信息卡片
    
    /// 基本信息卡片
    private var basicInfoCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            // 头部：客户名称和意向等级
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(lead.customerName)
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    HStack(spacing: 8) {
                        // 来源平台
                        Label(lead.sourcePlatform.name, systemImage: lead.sourcePlatform.iconName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        // 跟进状态
                        Text(lead.followUpStatus.name)
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(lead.followUpStatus.colorName.color.opacity(0.1))
                            .foregroundColor(lead.followUpStatus.colorName.color)
                            .cornerRadius(4)
                    }
                }
                
                Spacer()
                
                // 意向等级
                IntentionLevelBadge(level: lead.intentionLevel)
            }
            
            Divider()
            
            // 联系信息
            VStack(alignment: .leading, spacing: 12) {
                InfoRow(icon: "phone.fill", title: "手机", value: lead.customerPhone)
                
                if let wechat = lead.customerWechat, !wechat.isEmpty {
                    InfoRow(icon: "message.fill", title: "微信", value: wechat)
                }
                
                if let company = lead.customerCompany, !company.isEmpty {
                    InfoRow(icon: "building.2.fill", title: "公司", value: company)
                }
                
                if let position = lead.customerPosition, !position.isEmpty {
                    InfoRow(icon: "briefcase.fill", title: "职位", value: position)
                }
                
                if let address = lead.customerAddress, !address.isEmpty {
                    InfoRow(icon: "location.fill", title: "地址", value: address)
                }
            }
            
            // 意向标签
            if !lead.intentionTags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(lead.intentionTags, id: \.self) { tag in
                            Text(tag)
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.blue.opacity(0.1))
                                .foregroundColor(.blue)
                                .cornerRadius(6)
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 组件 - 标签页选择器
    
    /// 标签页选择器
    private var tabSelector: some View {
        HStack(spacing: 0) {
            TabButton(title: "跟进记录", isSelected: selectedTab == 0) {
                selectedTab = 0
            }
            
            TabButton(title: "任务记录", isSelected: selectedTab == 1) {
                selectedTab = 1
            }
            
            TabButton(title: "基本信息", isSelected: selectedTab == 2) {
                selectedTab = 2
            }
        }
        .padding(4)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(8)
    }
    
    // MARK: - 组件 - 标签页内容
    
    /// 标签页内容
    @ViewBuilder
    private var tabContent: some View {
        switch selectedTab {
        case 0:
            followUpRecordsContent
        case 1:
            taskRecordsContent
        case 2:
            basicInfoContent
        default:
            EmptyView()
        }
    }
    
    /// 跟进记录内容
    private var followUpRecordsContent: some View {
        VStack(spacing: 12) {
            // 添加跟进按钮
            Button(action: { showAddFollowUp = true }) {
                Label("添加跟进记录", systemImage: "plus.circle.fill")
                    .font(.subheadline)
                    .foregroundColor(.blue)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            if followUpRecords.isEmpty {
                Text("暂无跟进记录")
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
            } else {
                ForEach(followUpRecords) { record in
                    FollowUpRecordCard(record: record)
                }
            }
        }
    }
    
    /// 任务记录内容
    private var taskRecordsContent: some View {
        VStack(spacing: 12) {
            Text("暂无任务记录")
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
        }
    }
    
    /// 基本信息内容
    private var basicInfoContent: some View {
        VStack(alignment: .leading, spacing: 16) {
            // 商机编号
            InfoRow(icon: "number", title: "商机编号", value: lead.leadNo)
            
            // 创建时间
            InfoRow(icon: "calendar", title: "创建时间", value: lead.createdAt.formatted())
            
            // 更新时间
            InfoRow(icon: "clock", title: "更新时间", value: lead.updatedAt.formatted())
            
            // 负责人
            InfoRow(icon: "person.fill", title: "负责人", value: lead.ownerName)
            
            // 备注
            if let remark = lead.remark, !remark.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("备注")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text(remark)
                        .font(.body)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    // MARK: - 方法
    
    /// 加载跟进记录
    private func loadFollowUpRecords() async {
        isLoading = true
        
        do {
            followUpRecords = try await LeadRepository.shared.getFollowUpRecords(leadId: lead.id)
        } catch {
            // 处理错误
        }
        
        isLoading = false
    }
}

// MARK: - 意向等级徽章

struct IntentionLevelBadge: View {
    let level: IntentionLevel
    
    var body: some View {
        VStack(spacing: 2) {
            Text("\(levelLevel)")
                .font(.title3)
                .fontWeight(.bold)
            
            Text(level.name)
                .font(.caption2)
        }
        .foregroundColor(level.color)
        .frame(width: 60, height: 50)
        .background(level.color.opacity(0.1))
        .cornerRadius(8)
    }
    
    private var levelLevel: Int {
        switch level {
        case .high: return 3
        case .medium: return 2
        case .low: return 1
        case .none: return 0
        }
    }
}

// MARK: - 信息行

struct InfoRow: View {
    let icon: String
    let title: String
    let value: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.secondary)
                .frame(width: 24)
            
            Text(title)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .foregroundColor(.primary)
        }
    }
}

// MARK: - 标签页按钮

struct TabButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .fontWeight(isSelected ? .semibold : .regular)
                .foregroundColor(isSelected ? .white : .secondary)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? Color.blue : Color.clear)
                .cornerRadius(6)
        }
    }
}

// MARK: - 跟进记录卡片

struct FollowUpRecordCard: View {
    let record: FollowUpRecord
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: record.type.iconName)
                    .foregroundColor(.blue)
                
                Text(record.type.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Spacer()
                
                Text(record.createdAt.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Text(record.content)
                .font(.body)
                .foregroundColor(.primary)
            
            HStack {
                Image(systemName: "person.fill")
                    .font(.caption)
                Text(record.userName)
                    .font(.caption)
            }
            .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 添加跟进视图

struct AddFollowUpView: View {
    let leadId: String
    let onComplete: (FollowUpRecord) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var selectedType: FollowUpType = .phone
    @State private var content = ""
    @State private var isSubmitting = false
    
    var body: some View {
        NavigationView {
            Form {
                Section("跟进方式") {
                    Picker("方式", selection: $selectedType) {
                        ForEach(FollowUpType.allCases, id: \.self) { type in
                            Label(type.name, systemImage: type.iconName)
                                .tag(type)
                        }
                    }
                    .pickerStyle(.inline)
                }
                
                Section("跟进内容") {
                    TextEditor(text: $content)
                        .frame(minHeight: 100)
                }
            }
            .navigationTitle("添加跟进")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") { dismiss() }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("保存") {
                        submit()
                    }
                    .disabled(content.isEmpty || isSubmitting)
                }
            }
        }
    }
    
    private func submit() {
        isSubmitting = true
        
        let record = FollowUpRecord(
            leadId: leadId,
            type: selectedType,
            content: content,
            userId: "",
            userName: ""
        )
        
        Task {
            do {
                let newRecord = try await LeadRepository.shared.addFollowUpRecord(leadId: leadId, record: record)
                await MainActor.run {
                    onComplete(newRecord)
                    dismiss()
                }
            } catch {
                // 处理错误
            }
            
            await MainActor.run {
                isSubmitting = false
            }
        }
    }
}

// MARK: - 预览

#if DEBUG
struct LeadDetailView_Previews: PreviewProvider {
    static var previews: some View {
        LeadDetailView(lead: .preview)
    }
}
#endif
