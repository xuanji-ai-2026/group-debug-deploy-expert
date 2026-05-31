//
//  MessageListView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  消息列表视图
//
//  Created by Lin Feng (EMP-IOS-003) on 2024-01-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 消息列表视图
///
/// 展示消息列表，支持分类筛选、已读/未读标记、滑动操作等功能。
struct MessageListView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = MessageViewModel()
    
    /// 选中的消息类型过滤
    @State private var selectedType: MessageType?
    
    /// 是否显示详情
    @State private var showDetail = false
    
    /// 选中的消息
    @State private var selectedMessage: Message?
    
    /// 是否显示全部已读确认
    @State private var showMarkAllReadConfirm = false
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.isLoading && viewModel.messages.isEmpty {
                    LoadingView(message: "加载消息...")
                } else if viewModel.messages.isEmpty {
                    EmptyStateView(
                        icon: "bell.slash",
                        title: "暂无消息",
                        message: selectedType != nil ? "没有该类型的消息" : "您还没有收到任何消息",
                        actionTitle: selectedType != nil ? "清除筛选" : nil
                    ) {
                        if selectedType != nil {
                            selectedType = nil
                            Task { await viewModel.loadMessages() }
                        }
                    }
                } else {
                    messageList
                }
            }
            .navigationTitle("消息通知")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if viewModel.unreadCount > 0 {
                        Button(action: {
                            showMarkAllReadConfirm = true
                        }) {
                            Text("全部已读")
                                .font(.subheadline)
                        }
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(action: {
                            Task { await viewModel.refreshMessages() }
                        }) {
                            Label("刷新", systemImage: "arrow.clockwise")
                        }
                        
                        if viewModel.messages.contains(where: { $0.status == .read }) {
                            Button(role: .destructive, action: {
                                Task { await viewModel.clearReadMessages() }
                            }) {
                                Label("清空已读", systemImage: "trash")
                            }
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .refreshable {
                await viewModel.refreshMessages()
            }
            .sheet(item: $selectedMessage) { message in
                MessageDetailView(
                    message: message,
                    viewModel: viewModel
                )
            }
            .alert("标记全部已读", isPresented: $showMarkAllReadConfirm) {
                Button("取消", role: .cancel) {}
                Button("确定") {
                    Task { await viewModel.markAllAsRead() }
                }
            } message: {
                Text("确定要将所有未读消息标记为已读吗？")
            }
            .alert("提示", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "操作失败")
            }
        }
        .onAppear {
            if viewModel.messages.isEmpty {
                Task { await viewModel.loadMessages() }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .newMessageReceived)) { _ in
            Task { await viewModel.refreshMessages() }
        }
    }
    
    // MARK: - 组件 - 消息列表
    
    /// 消息列表
    private var messageList: some View {
        ScrollView {
            VStack(spacing: 0) {
                // 统计概览
                statisticsSection
                
                // 类型过滤
                typeFilterSection
                
                // 消息列表
                LazyVStack(spacing: 0) {
                    ForEach(groupedMessages.keys.sorted(by: { $0 > $1 }), id: \.self) { date in
                        Section(header: dateHeader(date)) {
                            ForEach(groupedMessages[date] ?? []) { message in
                                MessageRow(
                                    message: message,
                                    viewModel: viewModel
                                )
                                .onTapGesture {
                                    selectedMessage = message
                                    if message.status == .unread {
                                        Task { await viewModel.markAsRead(message) }
                                    }
                                }
                                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                    Button(role: .destructive) {
                                        Task { await viewModel.deleteMessage(message) }
                                    } label: {
                                        Label("删除", systemImage: "trash")
                                    }
                                    
                                    if message.status == .unread {
                                        Button {
                                            Task { await viewModel.markAsRead(message) }
                                        } label: {
                                            Label("已读", systemImage: "envelope.open")
                                        }
                                        .tint(.blue)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 加载更多
                if viewModel.isLoadingMore {
                    ProgressView()
                        .padding()
                }
            }
        }
    }
    
    // MARK: - 组件 - 按日期分组
    
    /// 按日期分组的消息
    private var groupedMessages: [Date: [Message]] {
        Dictionary(grouping: viewModel.messages) { message in
            Calendar.current.startOfDay(for: message.createdAt)
        }
    }
    
    // MARK: - 组件 - 日期头部
    
    /// 日期头部
    private func dateHeader(_ date: Date) -> some View {
        HStack {
            Text(date.formatted(date: .abbreviated, time: .omitted))
                .font(.caption)
                .foregroundColor(.secondary)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            
            Spacer()
        }
        .background(Color(.systemGroupedBackground))
    }
    
    // MARK: - 组件 - 统计概览
    
    /// 统计概览
    private var statisticsSection: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                // 未读消息
                MessageStatCard(
                    title: "未读",
                    count: viewModel.unreadCount,
                    color: .red,
                    icon: "bell.badge.fill"
                )
                
                // 系统消息
                MessageStatCard(
                    title: "系统",
                    count: viewModel.systemCount,
                    color: .blue,
                    icon: "gear"
                )
                
                // 商机消息
                MessageStatCard(
                    title: "商机",
                    count: viewModel.leadCount,
                    color: .orange,
                    icon: "briefcase.fill"
                )
                
                // 任务消息
                MessageStatCard(
                    title: "任务",
                    count: viewModel.taskCount,
                    color: .green,
                    icon: "checklist"
                )
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .padding(.horizontal)
        .padding(.top, 8)
    }
    
    // MARK: - 组件 - 类型过滤
    
    /// 类型过滤
    private var typeFilterSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // 全部
                MessageTypeFilterChip(
                    title: "全部",
                    count: viewModel.messages.count,
                    isSelected: selectedType == nil
                ) {
                    selectedType = nil
                    Task { await viewModel.clearFilter() }
                }
                
                // 各类型
                ForEach(MessageType.allCases, id: \.self) { type in
                    MessageTypeFilterChip(
                        title: type.description,
                        count: viewModel.countForType(type),
                        color: Color(type.colorName),
                        isSelected: selectedType == type
                    ) {
                        selectedType = type
                        Task { await viewModel.filterByType(type) }
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
    }
}

// MARK: - 消息统计卡片

struct MessageStatCard: View {
    let title: String
    let count: Int
    let color: Color
    let icon: String
    
    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.15))
                    .frame(width: 44, height: 44)
                
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(color)
            }
            
            Text("\(count)")
                .font(.title3)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

// MARK: - 消息类型过滤芯片

struct MessageTypeFilterChip: View {
    let title: String
    let count: Int
    var color: Color = .blue
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.subheadline)
                
                if count > 0 {
                    Text("\(count)")
                        .font(.caption)
                        .fontWeight(.medium)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.white.opacity(0.3))
                        .cornerRadius(10)
                }
            }
            .foregroundColor(isSelected ? .white : color)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(isSelected ? color : color.opacity(0.1))
            .cornerRadius(16)
        }
    }
}

// MARK: - 消息行

struct MessageRow: View {
    let message: Message
    @ObservedObject var viewModel: MessageViewModel
    
    var body: some View {
        HStack(spacing: 12) {
            // 图标
            messageIcon
            
            // 内容
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    // 标题
                    Text(message.title)
                        .font(.subheadline)
                        .fontWeight(message.status == .unread ? .semibold : .regular)
                        .lineLimit(1)
                    
                    Spacer()
                    
                    // 时间
                    Text(message.createdAt.relativeTime)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                // 内容摘要
                Text(message.content)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
        }
        .padding()
        .background(message.status == .unread ? Color.blue.opacity(0.05) : Color(.systemBackground))
        .overlay(
            HStack {
                if message.status == .unread {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 8, height: 8)
                        .padding(.leading, 4)
                }
                Spacer()
            }
        )
    }
    
    /// 消息图标
    private var messageIcon: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(message.type.colorName).opacity(0.15))
                .frame(width: 48, height: 48)
            
            Image(systemName: message.type.iconName)
                .font(.system(size: 22))
                .foregroundColor(Color(message.type.colorName))
        }
    }
}

// MARK: - 日期扩展

extension Date {
    /// 相对时间描述
    var relativeTime: String {
        let calendar = Calendar.current
        let now = Date()
        let components = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: self, to: now)
        
        if let year = components.year, year > 0 {
            return formatted(date: .numeric, time: .omitted)
        } else if let month = components.month, month > 0 {
            return "\(month)个月前"
        } else if let day = components.day, day > 6 {
            return "\(day / 7)周前"
        } else if let day = components.day, day > 0 {
            return "\(day)天前"
        } else if let hour = components.hour, hour > 0 {
            return "\(hour)小时前"
        } else if let minute = components.minute, minute > 0 {
            return "\(minute)分钟前"
        } else {
            return "刚刚"
        }
    }
}

// MARK: - 通知扩展

extension Notification.Name {
    static let newMessageReceived = Notification.Name("newMessageReceived")
}

// MARK: - 预览

#if DEBUG
struct MessageListView_Previews: PreviewProvider {
    static var previews: some View {
        MessageListView()
    }
}
#endif
