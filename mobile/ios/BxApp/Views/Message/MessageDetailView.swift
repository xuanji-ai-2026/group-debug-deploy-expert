//
//  MessageDetailView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  消息详情视图
//
//  Created by Lin Feng (EMP-IOS-003) on 2024-01-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 消息详情视图
///
/// 展示消息完整内容，支持操作按钮、关联业务跳转等功能。
struct MessageDetailView: View {
    
    // MARK: - 属性
    
    /// 消息
    let message: Message
    
    /// 视图模型
    @ObservedObject var viewModel: MessageViewModel
    
    /// 是否显示删除确认
    @State private var showDeleteConfirm = false
    
    /// 是否显示关联详情
    @State private var showRelatedDetail = false
    
    /// 环境变量
    @Environment(\.presentationMode) var presentationMode
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    // 头部信息
                    headerSection
                    
                    Divider()
                        .padding(.horizontal)
                    
                    // 消息内容
                    contentSection
                    
                    // 关联信息
                    if let relatedId = message.relatedId,
                       let relatedType = message.relatedType {
                        relatedSection(relatedId: relatedId, relatedType: relatedType)
                    }
                    
                    // 操作按钮
                    if let actions = message.actions, !actions.isEmpty {
                        actionsSection(actions: actions)
                    }
                    
                    // 底部信息
                    footerSection
                }
            }
            .navigationTitle("消息详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("关闭") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        if message.status == .unread {
                            Button(action: {
                                Task { await viewModel.markAsRead(message) }
                            }) {
                                Label("标记已读", systemImage: "envelope.open")
                            }
                        }
                        
                        Button(role: .destructive, action: {
                            showDeleteConfirm = true
                        }) {
                            Label("删除", systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .alert("删除消息", isPresented: $showDeleteConfirm) {
                Button("取消", role: .cancel) {}
                Button("删除", role: .destructive) {
                    Task {
                        await viewModel.deleteMessage(message)
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            } message: {
                Text("确定要删除这条消息吗？删除后无法恢复。")
            }
        }
    }
    
    // MARK: - 组件 - 头部信息
    
    /// 头部信息
    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                // 类型图标
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color(message.type.colorName).opacity(0.15))
                        .frame(width: 56, height: 56)
                    
                    Image(systemName: message.type.iconName)
                        .font(.system(size: 28))
                        .foregroundColor(Color(message.type.colorName))
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    // 类型标签
                    HStack {
                        Text(message.type.description)
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color(message.type.colorName))
                            .cornerRadius(4)
                        
                        if message.status == .unread {
                            Text("未读")
                                .font(.caption)
                                .fontWeight(.medium)
                                .foregroundColor(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(Color.red)
                                .cornerRadius(4)
                        }
                        
                        Spacer()
                    }
                    
                    // 标题
                    Text(message.title)
                        .font(.title3)
                        .fontWeight(.bold)
                        .lineLimit(2)
                }
            }
        }
        .padding()
    }
    
    // MARK: - 组件 - 内容区域
    
    /// 内容区域
    private var contentSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            // 内容文本
            Text(message.content)
                .font(.body)
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)
            
            // 如果有额外数据，尝试解析并显示
            if let extraData = message.extraData,
               let data = extraData.data(using: .utf8),
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                extraDataSection(json: json)
            }
        }
        .padding()
    }
    
    // MARK: - 组件 - 额外数据
    
    /// 额外数据显示
    private func extraDataSection(json: [String: Any]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Divider()
            
            Text("详细信息")
                .font(.headline)
                .padding(.top, 8)
            
            ForEach(json.sorted(by: { $0.key < $1.key }), id: \.key) { key, value in
                HStack(alignment: .top) {
                    Text(formatKey(key))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .frame(width: 80, alignment: .leading)
                    
                    Text(formatValue(value))
                        .font(.subheadline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }
    
    // MARK: - 组件 - 关联信息
    
    /// 关联信息区域
    private func relatedSection(relatedId: String, relatedType: RelatedType) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Divider()
                .padding(.horizontal)
            
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("关联\(relatedType.description)")
                        .font(.headline)
                    
                    Text("ID: \(relatedId)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Button(action: {
                    showRelatedDetail = true
                }) {
                    HStack(spacing: 4) {
                        Text("查看")
                            .font(.subheadline)
                        Image(systemName: "chevron.right")
                            .font(.caption)
                    }
                    .foregroundColor(.blue)
                }
            }
            .padding()
            .background(Color.blue.opacity(0.05))
            .cornerRadius(8)
            .padding(.horizontal)
        }
    }
    
    // MARK: - 组件 - 操作按钮
    
    /// 操作按钮区域
    private func actionsSection(actions: [MessageAction]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Divider()
                .padding(.horizontal)
            
            Text("可操作")
                .font(.headline)
                .padding(.horizontal)
            
            VStack(spacing: 8) {
                ForEach(actions.indices, id: \.self) { index in
                    let action = actions[index]
                    actionButton(action)
                }
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 8)
    }
    
    /// 单个操作按钮
    private func actionButton(_ action: MessageAction) -> some View {
        Button(action: {
            handleAction(action)
        }) {
            HStack {
                Text(action.title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(action.backgroundColor)
            .foregroundColor(action.foregroundColor)
            .cornerRadius(8)
        }
    }
    
    // MARK: - 组件 - 底部信息
    
    /// 底部信息
    private var footerSection: some View {
        VStack(spacing: 8) {
            Divider()
                .padding(.horizontal)
            
            HStack {
                Text("接收时间: \(message.createdAt.formatted(date: .long, time: .shortened))")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Spacer()
            }
            .padding(.horizontal)
            
            if let readAt = message.readAt {
                HStack {
                    Text("阅读时间: \(readAt.formatted(date: .long, time: .shortened))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                }
                .padding(.horizontal)
            }
            
            if let sender = message.sender {
                HStack {
                    Text("发送者: \(sender.name)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                }
                .padding(.horizontal)
            }
        }
        .padding(.vertical, 8)
    }
    
    // MARK: - 私有方法
    
    /// 处理操作
    private func handleAction(_ action: MessageAction) {
        switch action.type {
        case .openDetail:
            if let payload = action.payload {
                print("[MessageDetailView] 跳转到详情页: \(payload)")
            }
        case .openLink:
            if let payload = action.payload,
               let url = URL(string: payload) {
                UIApplication.shared.open(url)
            }
        case .execute:
            print("[MessageDetailView] 执行操作: \(action.id)")
        case .confirm:
            print("[MessageDetailView] 确认操作: \(action.id)")
        case .cancel:
            presentationMode.wrappedValue.dismiss()
        }
    }
    
    /// 格式化键名
    private func formatKey(_ key: String) -> String {
        let map: [String: String] = [
            "leadName": "客户名称",
            "leadPhone": "联系电话",
            "leadCompany": "公司名称",
            "leadStatus": "客户状态",
            "taskName": "任务名称",
            "taskProgress": "任务进度",
            "orderAmount": "订单金额",
            "orderStatus": "订单状态",
            "balance": "账户余额",
            "expiredAt": "过期时间"
        ]
        return map[key] ?? key
    }
    
    /// 格式化值
    private func formatValue(_ value: Any) -> String {
        if let string = value as? String {
            return string
        } else if let number = value as? NSNumber {
            return number.stringValue
        } else if let bool = value as? Bool {
            return bool ? "是" : "否"
        }
        return String(describing: value)
    }
}

// MARK: - 消息操作扩展

extension MessageAction {
    /// 背景颜色
    var backgroundColor: Color {
        switch style {
        case .default:
            return Color(.systemGray6)
        case .primary:
            return .blue
        case .danger:
            return .red
        case .link:
            return Color.clear
        }
    }
    
    /// 前景颜色
    var foregroundColor: Color {
        switch style {
        case .default:
            return .primary
        case .primary, .danger:
            return .white
        case .link:
            return .blue
        }
    }
}

// MARK: - 预览

#if DEBUG
struct MessageDetailView_Previews: PreviewProvider {
    static var previews: some View {
        MessageDetailView(
            message: Message.preview,
            viewModel: MessageViewModel()
        )
    }
}
#endif
