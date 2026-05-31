//
//  RechargeViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  积分充值视图模型 - 已修复：完整对接后端BillingOrderController
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Combine
import SwiftUI

/// 积分充值视图模型
///
/// 负责管理积分充值流程，包括套餐展示、订单创建、支付处理等。
/// 已完整对接后端 BillingOrderController API。
@MainActor
class RechargeViewModel: ObservableObject {
    
    // MARK: - Published属性
    
    /// 当前积分余额
    @Published var currentPoints: Int = 0
    
    /// 充值套餐列表
    @Published var packages: [RechargePackage] = []
    
    /// 选中的套餐
    @Published var selectedPackage: RechargePackage?
    
    /// 选中的支付方式
    @Published var selectedPaymentMethod: PaymentMethod = .alipay
    
    /// 是否正在加载
    @Published var isLoading: Bool = false
    
    /// 错误信息
    @Published var errorMessage: String?
    
    /// 是否显示错误
    @Published var showError: Bool = false
    
    /// 是否显示支付成功
    @Published var showPaySuccess: Bool = false
    
    /// 当前订单
    @Published var currentOrder: RechargeOrder?
    
    /// 是否显示支付确认
    @Published var showPaymentConfirm: Bool = false
    
    /// 是否显示支付二维码弹窗
    @Published var showQrCodeDialog: Bool = false
    
    /// 支付二维码URL
    @Published var qrCodeUrl: String?
    
    /// 充值记录
    @Published var rechargeHistory: [RechargeOrder] = []
    
    /// 是否有更多历史记录
    @Published var hasMoreHistory: Bool = true
    
    /// 当前页码
    @Published var currentPage: Int = 1
    
    /// 订单轮询状态
    @Published var isPolling: Bool = false
    
    // MARK: - Computed属性
    
    /// 可用的支付方式
    var availablePaymentMethods: [PaymentMethod] {
        [.alipay, .wechatPay]
    }
    
    /// 推荐套餐
    var recommendedPackage: RechargePackage? {
        packages.first { $0.isRecommended }
    }
    
    /// 热门套餐
    var hotPackages: [RechargePackage] {
        packages.filter { $0.isHot }
    }
    
    /// 所有套餐按排序
    var sortedPackages: [RechargePackage] {
        packages.sorted { $0.sortOrder < $1.sortOrder }
    }
    
    /// 选中套餐的实际支付金额（分）
    var selectedAmount: Int {
        selectedPackage?.price ?? 0
    }
    
    /// 选中套餐的显示金额（元）
    var displayAmount: String {
        selectedPackage?.displayPrice ?? "0.00"
    }
    
    /// 是否可以创建订单
    var canCreateOrder: Bool {
        selectedPackage != nil
    }
    
    // MARK: - 私有属性
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    /// 轮询任务
    private var pollingTask: Task<Void, Never>?
    
    // MARK: - 初始化
    
    init() {
        loadUserPoints()
        loadPackages()
    }
    
    deinit {
        stopOrderPolling()
    }
    
    // MARK: - 公开方法
    
    /// 加载用户积分余额
    func loadUserPoints() {
        if let data = UserDefaults.standard.data(forKey: "currentUser"),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            currentPoints = user.balance
        }
    }
    
    /// 加载充值套餐（后端暂无此接口，使用默认配置）
    func loadPackages() async {
        isLoading = true
        errorMessage = nil
        
        do {
            // 后端暂未实现套餐列表API，返回默认套餐配置
            let defaultPackages: [RechargePackage] = [
                RechargePackage(
                    id: "pkg_001",
                    name: "体验包",
                    price: 990,
                    points: 100,
                    bonusPoints: 0,
                    displayPrice: "9.9",
                    description: "适合初次体验",
                    isRecommended: false,
                    isHot: false,
                    sortOrder: 1
                ),
                RechargePackage(
                    id: "pkg_002",
                    name: "基础包",
                    price: 4900,
                    points: 500,
                    bonusPoints: 50,
                    displayPrice: "49",
                    description: "适合小型团队",
                    isRecommended: false,
                    isHot: false,
                    sortOrder: 2
                ),
                RechargePackage(
                    id: "pkg_003",
                    name: "标准包",
                    price: 9900,
                    points: 1100,
                    bonusPoints: 200,
                    displayPrice: "99",
                    description: "最受欢迎",
                    isRecommended: true,
                    isHot: true,
                    sortOrder: 3
                ),
                RechargePackage(
                    id: "pkg_004",
                    name: "专业包",
                    price: 29900,
                    points: 3500,
                    bonusPoints: 800,
                    displayPrice: "299",
                    description: "适合专业用户",
                    isRecommended: false,
                    isHot: true,
                    sortOrder: 4
                ),
                RechargePackage(
                    id: "pkg_005",
                    name: "企业包",
                    price: 99900,
                    points: 12000,
                    bonusPoints: 3000,
                    displayPrice: "999",
                    description: "企业级服务",
                    isRecommended: false,
                    isHot: false,
                    sortOrder: 5
                )
            ]
            
            self.packages = defaultPackages.sorted { $0.sortOrder < $1.sortOrder }
            
            if let recommended = defaultPackages.first(where: { $0.isRecommended }) {
                selectedPackage = recommended
            } else if !defaultPackages.isEmpty {
                selectedPackage = defaultPackages.first
            }
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 刷新套餐
    func refreshPackages() async {
        await loadPackages()
    }
    
    /// 选择套餐
    func selectPackage(_ package: RechargePackage) {
        selectedPackage = package
    }
    
    /// 选择支付方式
    func selectPaymentMethod(_ method: PaymentMethod) {
        selectedPaymentMethod = method
    }
    
    /// 创建订单并获取支付二维码（已对接后端API）
    func createOrderAndFetchQrCode() async {
        guard let package = selectedPackage else {
            errorMessage = "请先选择充值套餐"
            showError = true
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            // 1. 创建充值订单 - POST /api/billing/order/recharge
            let order = try await APIService.shared.createRechargeOrder(
                amount: Double(package.price) / 100.0,  // 转换为元
                payMethod: selectedPaymentMethod == .wechatPay ? "WECHATPAY" : "ALIPAY"
            )
            
            currentOrder = order
            
            // 2. 获取支付二维码
            let qrUrl: String
            if selectedPaymentMethod == .wechatPay {
                qrUrl = try await APIService.shared.getWechatPayQrCode(orderNo: order.orderNo)
            } else {
                qrUrl = try await APIService.shared.getAlipayQrCode(orderNo: order.orderNo)
            }
            
            self.qrCodeUrl = qrUrl
            self.showQrCodeDialog = true
            self.isLoading = false
            
            // 3. 开始轮询订单状态
            startOrderPolling(orderNo: order.orderNo)
            
        } catch {
            errorMessage = error.localizedDescription
            showError = true
            isLoading = false
        }
    }
    
    /// 开始轮询订单状态
    private func startOrderPolling(orderNo: String) {
        stopOrderPolling()
        
        isPolling = true
        
        pollingTask = Task {
            // 最多轮询60次（约10分钟）
            for _ in 0..<60 {
                guard !Task.isCancelled && isPolling else { break }
                
                try? await Task.sleep(nanoseconds: 10_000_000_000)  // 10秒
                
                do {
                    let updatedOrder = try await APIService.shared.getOrderStatus(orderNo: orderNo)
                    
                    await MainActor.run {
                        self.currentOrder = updatedOrder
                        
                        if updatedOrder.status == "PAID" || updatedOrder.status == "SUCCESS" {
                            handlePaymentSuccess(order: updatedOrder)
                            return
                        } else if updatedOrder.status == "FAILED" || 
                                  updatedOrder.status == "CANCELLED" || 
                                  updatedOrder.status == "EXPIRED" {
                            handlePaymentFailure(status: updatedOrder.status)
                            return
                        }
                    }
                } catch {
                    continue
                }
            }
            
            await MainActor.run {
                if self.isPolling {
                    self.isPolling = false
                    self.showQrCodeDialog = false
                    self.errorMessage = "支付超时，请重新发起支付"
                    self.showError = true
                }
            }
        }
    }
    
    /// 停止订单轮询
    func stopOrderPolling() {
        isPolling = false
        pollingTask?.cancel()
        pollingTask = nil
    }
    
    /// 处理支付成功
    private func handlePaymentSuccess(order: RechargeOrder) {
        isPolling = false
        showQrCodeDialog = false
        
        currentPoints += order.points + order.bonusPoints
        updateUserBalance(currentPoints)
        
        showPaySuccess = true
        rechargeHistory.insert(order, at: 0)
        currentOrder = nil
    }
    
    /// 处理支付失败
    private func handlePaymentFailure(status: String) {
        isPolling = false
        showQrCodeDialog = false
        
        switch status {
        case "FAILED":
            errorMessage = "支付失败，请重试"
        case "CANCELLED":
            errorMessage = "订单已取消"
        case "EXPIRED":
            errorMessage = "订单已超时，请重新下单"
        default:
            errorMessage = "支付异常"
        }
        showError = true
    }
    
    /// 用户手动确认支付完成
    func confirmPaymentCompleted() async {
        showQrCodeDialog = false
        stopOrderPolling()
        
        guard let order = currentOrder else { return }
        
        isLoading = true
        
        do {
            let updatedOrder = try await APIService.shared.getOrderStatus(orderNo: order.orderNo)
            
            if updatedOrder.status == "PAID" || updatedOrder.status == "SUCCESS" {
                handlePaymentSuccess(order: updatedOrder)
            } else {
                errorMessage = "支付尚未完成，请稍后再试"
                showError = true
            }
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 取消支付
    func cancelPayment() async {
        showQrCodeDialog = false
        stopOrderPolling()
        
        guard let order = currentOrder else { return }
        
        isLoading = true
        
        do {
            try await APIService.shared.cancelOrder(orderNo: order.orderNo)
            currentOrder = nil
            showPaymentConfirm = false
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 加载充值历史
    func loadRechargeHistory(page: Int = 1, pageSize: Int = 20) async {
        guard page == 1 || hasMoreHistory else { return }
        
        if page == 1 {
            isLoading = true
        }
        
        do {
            let history = try await APIService.shared.getUserOrders(
                userId: getCurrentUserId(),
                orderType: nil
            )
            
            if page == 1 {
                rechargeHistory = history
            } else {
                rechargeHistory.append(contentsOf: history)
            }
            
            hasMoreHistory = history.count >= pageSize
            currentPage = page
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    /// 加载更多历史
    func loadMoreHistory() async {
        await loadRechargeHistory(page: currentPage + 1)
    }
    
    /// 刷新历史
    func refreshHistory() async {
        await loadRechargeHistory(page: 1)
    }
    
    /// 更新用户余额
    private func updateUserBalance(_ balance: Int) {
        if let data = UserDefaults.standard.data(forKey: "currentUser"),
           var user = try? JSONDecoder().decode(User.self, from: data) {
            user.balance = balance
            if let newData = try? JSONEncoder().encode(user) {
                UserDefaults.standard.set(newData, forKey: "currentUser")
            }
        }
    }
    
    /// 获取当前用户ID
    private func getCurrentUserId() -> Long {
        if let data = UserDefaults.standard.data(forKey: "currentUser"),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            return Long(user.id)!
        }
        return 1
    }
}

// MARK: - API扩展（已对接后端BillingOrderController）

extension APIService {
    
    /// 创建充值订单 - 对接 POST /api/billing/order/recharge
    func createRechargeOrder(amount: Double, payMethod: String) async throws -> RechargeOrder {
        let params: [String: Any] = [
            "amount": amount,
            "payMethod": payMethod,
            "description": "移动端充值"
        ]
        
        let response: APIResponse<RechargeOrder> = try await post(
            "/api/billing/order/recharge",
            parameters: params,
            query: ["tenantId": "1"]
        )
        
        guard let order = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        return order
    }
    
    /// 获取支付宝支付二维码 - 对接 GET /api/billing/order/{orderNo}/alipay-qr
    func getAlipayQrCode(orderNo: String) async throws -> String {
        let response: APIResponse<String> = try await get(
            "/api/billing/order/\(orderNo)/alipay-qr"
        )
        
        guard let url = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        return url
    }
    
    /// 获取微信支付二维码 - 对接 GET /api/billing/order/{orderNo}/wechatpay-qr
    func getWechatPayQrCode(orderNo: String) async throws -> String {
        let response: APIResponse<String> = try await get(
            "/api/billing/order/\(orderNo)/wechatpay-qr"
        )
        
        guard let url = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        return url
    }
    
    /// 查询订单状态 - 对接 GET /api/billing/order/{orderNo}
    func getOrderStatus(orderNo: String) async throws -> RechargeOrder {
        let response: APIResponse<RechargeOrder> = try await get(
            "/api/billing/order/\(orderNo)"
        )
        
        guard let order = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        return order
    }
    
    /// 取消订单 - 对接 POST /api/billing/order/{orderNo}/cancel
    func cancelOrder(orderNo: String) async throws {
        let _: APIResponse<EmptyResponse> = try await post(
            "/api/billing/order/\(orderNo)/cancel"
        )
    }
    
    /// 获取用户订单列表 - 对接 GET /api/billing/order/user/{userId}
    func getUserOrders(userId: Long, orderType: String?) async throws -> [RechargeOrder] {
        var params: [String: Any] = [:]
        if let type = orderType {
            params["orderType"] = type
        }
        
        let response: APIResponse<[RechargeOrder]> = try await get(
            "/api/billing/order/user/\(userId)",
            parameters: params.isEmpty ? nil : params
        )
        
        return response.data ?? []
    }
}

// MARK: - 预览

#if DEBUG
extension RechargeViewModel {
    static let preview = RechargeViewModel()
}
#endif
