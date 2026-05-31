//
//  MessageViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  消息视图模型 - 已修复：移除所有假数据，使用真实API
//
//  Created by Lin Feng (EMP-IOS-003) on 2024-01-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI

/// 消息视图模型
///
/// 负责管理消息列表和消息详情的逻辑，包括消息的加载、WebSocket实时更新、状态管理等。
/// 所有数据均从后端API获取，不再使用任何硬编码数据。
@MainActor
class MessageViewModel: ObservableObject {
    
    // MARK: - Published属性 - 列表
    
    /// 消息列表
    @Published var messages: [Message] = []
    
    /// 筛选条件
    @Published var filter: MessageFilter = MessageFilter()
    
    /// 是否加载中
    @Published var isLoading: Bool = false
    
    /// 是否正在刷新
    @Published var isRefreshing: Bool = false
    
    /// 是否正在加载更多
    @Published var isLoadingMore: Bool = false
    
    /// 错误信息
    @Published var errorMessage: String?
    
    /// 是否显示错误
    @Published var showError: Bool = false
    
    // MARK: - Published属性 - 统计
    
    /// 未读消息数量
    @Published var unreadCount: Int = 0
    
    /// 系统消息数量
    @Published var systemCount: Int = 0
    
    /// 商机消息数量
    @Published var leadCount: Int = 0
    
    /// 任务消息数量
    @Published var taskCount: Int = 0
    
    /// 账户消息数量
    @Published var accountCount: Int = 0
    
    /// 活动消息数量
    @Published var activityCount: Int = 0
    
    // MARK: - Published属性 - WebSocket
    
    /// WebSocket连接状态
    @Published var isWebSocketConnected: Bool = false
    
    /// 连接错误信息
    @Published var connectionError: String?
    
    // MARK: - 私有属性
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    /// WebSocket任务
    private var webSocketTask: URLSessionWebSocketTask?
    
    /// 心跳定时器
    private var heartbeatTimer: Timer?
    
    /// 重连次数
    private var reconnectCount: Int = 0
    
    /// 最大重连次数
    private let maxReconnectCount = 5
    
    // MARK: - 初始化
    
    init() {
        setupBindings()
    }
    
    deinit {
        disconnectWebSocket()
    }
    
    // MARK: - Computed属性
    
    /// 是否有更多数据
    var hasMore: Bool {
        filter.page < calculateTotalPages()
    }
    
    // MARK: - 公开方法 - 列表（真实API调用）
    
    /// 加载消息列表
    func loadMessages() async {
        if filter.page > 1 {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        
        errorMessage = nil
        
        do {
            let params: [String: Any] = [
                "page": filter.page,
                "pageSize": filter.pageSize
            ]
            
            if let types = filter.types, !types.isEmpty {
                params["types"] = types.map { $0.rawValue }.joined(separator: ",")
            }
            
            if let status = filter.status {
                params["status"] = status.rawValue
            }
            
            if let keyword = filter.keyword, !keyword.isEmpty {
                params["keyword"] = keyword
            }
            
            let response: APIResponse<PageResponse<Message>> = try await APIService.shared.get(
                "/api/v1/messages",
                parameters: params
            )
            
            guard let data = response.data else {
                throw APIException(code: response.code, message: response.message)
            }
            
            if filter.page == 1 {
                messages = data.list
            } else {
                messages.append(contentsOf: data.list)
            }
            
            updateStatistics()
            
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
        isLoadingMore = false
    }
    
    /// 刷新消息列表
    func refreshMessages() async {
        isRefreshing = true
        filter.page = 1
        
        await loadMessages()
        
        connectWebSocket()
        
        isRefreshing = false
    }
    
    /// 加载更多消息
    func loadMoreMessages() async {
        guard hasMore && !isLoadingMore else { return }
        
        filter.page += 1
        await loadMessages()
    }
    
    /// 按类型筛选
    func filterByType(_ type: MessageType) async {
        filter.types = [type]
        filter.page = 1
        await loadMessages()
    }
    
    /// 清空筛选
    func clearFilter() async {
        filter.clear()
        await loadMessages()
    }
    
    /// 搜索消息
    func searchMessages(keyword: String) async {
        filter.keyword = keyword.isEmpty ? nil : keyword
        filter.page = 1
        await loadMessages()
    }
    
    // MARK: - 公开方法 - 消息操作（真实API调用）
    
    /// 标记消息已读（调用后端API）
    func markAsRead(_ message: Message) async {
        guard message.status == .unread else { return }
        
        do {
            let _: APIResponse<String?> = try await APIService.shared.put(
                "/api/v1/messages/\(message.id)/read",
                parameters: ["userId": getCurrentUserId()]
            )
            
            if let index = messages.firstIndex(where: { $0.id == message.id }) {
                messages[index].status = .read
                messages[index].readAt = Date()
            }
            
            updateStatistics()
            
        } catch {
            errorMessage = "标记已读失败: \(error.localizedDescription)"
            showError = true
        }
    }
    
    /// 标记全部已读（调用后端API）
    func markAllAsRead() async {
        do {
            let unreadIds = messages.filter { $0.status == .unread }.map { $0.id }
            
            let _: APIResponse<String?> = try await APIService.shared.post(
                "/api/v1/messages/batch-read",
                parameters: [
                    "messageIds": unreadIds,
                    "userId": getCurrentUserId()
                ]
            )
            
            for index in messages.indices {
                if messages[index].status == .unread {
                    messages[index].status = .read
                    messages[index].readAt = Date()
                }
            }
            
            updateStatistics()
            
        } catch {
            errorMessage = "批量标记已读失败: \(error.localizedDescription)"
            showError = true
        }
    }
    
    /// 删除消息（调用后端API）
    func deleteMessage(_ message: Message) async {
        do {
            let _: APIResponse<String?> = try await APIService.shared.delete(
                "/api/v1/messages/\(message.id)/delete",
                parameters: ["userId": getCurrentUserId()]
            )
            
            messages.removeAll { $0.id == message.id }
            
            updateStatistics()
            
        } catch {
            errorMessage = "删除消息失败: \(error.localizedDescription)"
            showError = true
        }
    }
    
    /// 清空已读消息（调用后端API）
    func clearReadMessages() async {
        do {
            let readIds = messages.filter { $0.status == .read }.map { $0.id }
            
            let _: APIResponse<String?> = try await APIService.shared.post(
                "/api/v1/messages/batch-delete",
                parameters: [
                    "messageIds": readIds,
                    "userId": getCurrentUserId()
                ]
            )
            
            messages.removeAll { $0.status == .read }
            
            updateStatistics()
            
        } catch {
            errorMessage = "清空消息失败: \(error.localizedDescription)"
            showError = true
        }
    }
    
    /// 获取某类型的消息数量
    func countForType(_ type: MessageType) -> Int {
        messages.filter { $0.type == type }.count
    }
    
    // MARK: - 公开方法 - WebSocket（保持原有实现）
    
    /// 连接WebSocket
    func connectWebSocket() {
        disconnectWebSocket()
        
        guard let url = URL(string: NetworkConfig.websocketURL) else {
            connectionError = "WebSocket URL无效"
            return
        }
        
        var request = URLRequest(url: url)
        request.timeoutInterval = 30
        
        if let token = KeychainManager.shared.getToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        let session = URLSession(configuration: .default)
        webSocketTask = session.webSocketTask(with: request)
        webSocketTask?.delegate = self
        
        webSocketTask?.resume()
        
        receiveMessage()
    }
    
    /// 断开WebSocket连接
    func disconnectWebSocket() {
        heartbeatTimer?.invalidate()
        heartbeatTimer = nil
        
        webSocketTask?.cancel(with: .normalClosure, reason: nil)
        webSocketTask = nil
        
        isWebSocketConnected = false
    }
    
    // MARK: - 私有方法
    
    /// 设置数据绑定
    private func setupBindings() {
        $filter
            .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
            .dropFirst()
            .sink { [weak self] _ in
                Task { @MainActor in
                    await self?.loadMessages()
                }
            }
            .store(in: &cancellables)
    }
    
    /// 更新统计信息
    private func updateStatistics() {
        unreadCount = messages.filter { $0.status == .unread }.count
        systemCount = messages.filter { $0.type == .system }.count
        leadCount = messages.filter { $0.type == .lead }.count
        taskCount = messages.filter { $0.type == .task }.count
        accountCount = messages.filter { $0.type == .account }.count
        activityCount = messages.filter { $0.type == .activity }.count
    }
    
    /// 计算总页数
    private func calculateTotalPages() -> Int {
        let total = messages.count
        guard total > 0 else { return 1 }
        return (total + filter.pageSize - 1) / filter.pageSize
    }
    
    /// 获取当前用户ID
    private func getCurrentUserId() -> String {
        if let userData = UserDefaults.standard.data(forKey: "currentUser"),
           let user = try? JSONDecoder().decode(User.self, from: userData) {
            return user.id
        }
        return "unknown"
    }
    
    /// 接收WebSocket消息
    private func receiveMessage() {
        webSocketTask?.receive { [weak self] result in
            Task { @MainActor in
                switch result {
                case .success(let message):
                    self?.handleWebSocketMessage(message)
                    self?.receiveMessage()
                    
                case .failure(let error):
                    self?.connectionError = error.localizedDescription
                    self?.isWebSocketConnected = false
                    self?.scheduleReconnect()
                }
            }
        }
    }
    
    /// 处理WebSocket消息
    private func handleWebSocketMessage(_ message: URLSessionWebSocketTask.Message) {
        switch message {
        case .string(let text):
            guard let data = text.data(using: .utf8) else { return }
            
            do {
                let wsMessage = try JSONDecoder().decode(WebSocketMessage.self, from: data)
                
                switch wsMessage.type {
                case .newMessage:
                    if let newMessage = wsMessage.data {
                        handleNewMessage(newMessage)
                    }
                case .messageRead:
                    if let msg = wsMessage.data {
                        handleMessageRead(msg.id)
                    }
                case .connected:
                    isWebSocketConnected = true
                    reconnectCount = 0
                    startHeartbeat()
                case .ping:
                    sendPong()
                case .error:
                    connectionError = "服务器错误"
                default:
                    break
                }
            } catch {
                print("解析WebSocket消息失败: \(error)")
            }
            
        case .data(let data):
            print("收到二进制数据: \(data.count) bytes")
            
        @unknown default:
            break
        }
    }
    
    /// 处理新消息
    private func handleNewMessage(_ message: Message) {
        messages.insert(message, at: 0)
        updateStatistics()
        NotificationCenter.default.post(name: .newMessageReceived, object: message)
    }
    
    /// 处理消息已读
    private func handleMessageRead(_ messageId: String) {
        if let index = messages.firstIndex(where: { $0.id == messageId }) {
            messages[index].status = .read
            messages[index].readAt = Date()
            updateStatistics()
        }
    }
    
    /// 启动心跳
    private func startHeartbeat() {
        heartbeatTimer?.invalidate()
        
        heartbeatTimer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
            self?.sendPing()
        }
    }
    
    /// 发送心跳
    private func sendPing() {
        let pingMessage: [String: Any] = [
            "type": "ping",
            "timestamp": ISO8601DateFormatter().string(from: Date())
        ]
        
        do {
            let data = try JSONSerialization.data(withJSONObject: pingMessage)
            if let string = String(data: data, encoding: .utf8) {
                webSocketTask?.send(.string(string)) { _ in }
            }
        } catch {
            print("发送心跳失败: \(error)")
        }
    }
    
    /// 发送心跳响应
    private func sendPong() {
        let pongMessage: [String: Any] = [
            "type": "pong",
            "timestamp": ISO8601DateFormatter().string(from: Date())
        ]
        
        do {
            let data = try JSONSerialization.data(withJSONObject: pongMessage)
            if let string = String(data: data, encoding: .utf8) {
                webSocketTask?.send(.string(string)) { _ in }
            }
        } catch {
            print("发送Pong失败: \(error)")
        }
    }
    
    /// 计划重连
    private func scheduleReconnect() {
        guard reconnectCount < maxReconnectCount else {
            connectionError = "连接失败，请稍后重试"
            return
        }
        
        reconnectCount += 1
        let delay = min(pow(2.0, Double(reconnectCount)), 30)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            self?.connectWebSocket()
        }
    }
}

// MARK: - WebSocket代理

extension MessageViewModel: URLSessionWebSocketDelegate {
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        Task { @MainActor in
            isWebSocketConnected = true
            connectionError = nil
        }
    }
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        Task { @MainActor in
            isWebSocketConnected = false
            
            if closeCode != .normalClosure {
                scheduleReconnect()
            }
        }
    }
}

// MARK: - 通知扩展

extension Notification.Name {
    static let newMessageReceived = Notification.Name("newMessageReceived")
}

// MARK: - 预览

#if DEBUG
extension MessageViewModel {
    static let preview = MessageViewModel()
}
#endif
