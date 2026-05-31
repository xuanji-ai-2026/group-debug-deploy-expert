//
//  RechargeOrder.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  充值订单数据模型
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation

// MARK: - 充值订单模型

/// 积分充值订单模型
/// 
/// 存储用户的积分充值订单信息。
struct RechargeOrder: Codable, Identifiable, Equatable {
    
    // MARK: - 基本信息
    
    /// 订单ID - 唯一标识
    var id: String
    
    /// 订单编号
    var orderNo: String
    
    /// 用户ID
    var userId: String
    
    /// 充值套餐ID
    var packageId: String
    
    /// 套餐名称
    var packageName: String
    
    /// 充值积分数量
    var points: Int
    
    /// 赠送积分数量
    var bonusPoints: Int
    
    /// 订单金额（分）
    var amount: Int
    
    /// 实际支付金额（分）
    var paidAmount: Int?
    
    /// 优惠金额（分）
    var discountAmount: Int
    
    // MARK: - 支付信息
    
    /// 支付方式
    var paymentMethod: PaymentMethod?
    
    /// 支付渠道
    var paymentChannel: String?
    
    /// 第三方交易号
    var transactionId: String?
    
    /// 支付时间
    var paidAt: Date?
    
    // MARK: - 订单状态
    
    /// 订单状态
    var status: OrderStatus
    
    /// 订单描述
    var description: String?
    
    // MARK: - 时间戳
    
    /// 创建时间
    var createdAt: Date
    
    /// 更新时间
    var updatedAt: Date
    
    /// 过期时间
    var expireAt: Date?
    
    // MARK: - 初始化
    
    init(
        id: String = UUID().uuidString,
        orderNo: String,
        userId: String,
        packageId: String,
        packageName: String,
        points: Int,
        bonusPoints: Int = 0,
        amount: Int,
        paidAmount: Int? = nil,
        discountAmount: Int = 0,
        paymentMethod: PaymentMethod? = nil,
        paymentChannel: String? = nil,
        transactionId: String? = nil,
        paidAt: Date? = nil,
        status: OrderStatus = .pending,
        description: String? = nil,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        expireAt: Date? = nil
    ) {
        self.id = id
        self.orderNo = orderNo
        self.userId = userId
        self.packageId = packageId
        self.packageName = packageName
        self.points = points
        self.bonusPoints = bonusPoints
        self.amount = amount
        self.paidAmount = paidAmount
        self.discountAmount = discountAmount
        self.paymentMethod = paymentMethod
        self.paymentChannel = paymentChannel
        self.transactionId = transactionId
        self.paidAt = paidAt
        self.status = status
        self.description = description
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.expireAt = expireAt
    }
}

// MARK: - 订单状态

/// 订单状态枚举
enum OrderStatus: String, Codable, CaseIterable {
    /// 待支付
    case pending = "pending"
    
    /// 支付中
    case processing = "processing"
    
    /// 已支付
    case paid = "paid"
    
    /// 已完成
    case completed = "completed"
    
    /// 已取消
    case cancelled = "cancelled"
    
    /// 已退款
    case refunded = "refunded"
    
    /// 支付失败
    case failed = "failed"
    
    /// 状态描述
    var description: String {
        switch self {
        case .pending: return "待支付"
        case .processing: return "支付中"
        case .paid: return "已支付"
        case .completed: return "已完成"
        case .cancelled: return "已取消"
        case .refunded: return "已退款"
        case .failed: return "支付失败"
        }
    }
    
    /// 状态颜色
    var color: String {
        switch self {
        case .pending: return "#FF9500"
        case .processing: return "#007AFF"
        case .paid: return "#34C759"
        case .completed: return "#34C759"
        case .cancelled: return "#8E8E93"
        case .refunded: return "#5856D6"
        case .failed: return "#FF3B30"
        }
    }
    
    /// 是否完成
    var isCompleted: Bool {
        self == .completed || self == .paid
    }
    
    /// 是否失败/取消
    var isFailedOrCancelled: Bool {
        self == .failed || self == .cancelled
    }
}

// MARK: - 支付方式

/// 支付方式枚举
enum PaymentMethod: String, Codable, CaseIterable {
    /// 微信支付
    case wechatPay = "wechat_pay"
    
    /// 支付宝
    case alipay = "alipay"
    
    /// Apple Pay
    case applePay = "apple_pay"
    
    /// 银行卡
    case bankCard = "bank_card"
    
    /// 支付方式名称
    var name: String {
        switch self {
        case .wechatPay: return "微信支付"
        case .alipay: return "支付宝"
        case .applePay: return "Apple Pay"
        case .bankCard: return "银行卡"
        }
    }
    
    /// 支付方式图标
    var iconName: String {
        switch self {
        case .wechatPay: return "message.circle.fill"
        case .alipay: return "a.circle.fill"
        case .applePay: return "apple.logo"
        case .bankCard: return "creditcard.fill"
        }
    }
    
    /// 支付方式描述
    var description: String {
        switch self {
        case .wechatPay: return "推荐使用微信支付"
        case .alipay: return "数亿用户的选择"
        case .applePay: return "安全便捷的支付体验"
        case .bankCard: return "支持主流银行卡"
        }
    }
}

// MARK: - 充值套餐

/// 积分充值套餐
struct RechargePackage: Codable, Identifiable, Equatable {
    
    /// 套餐ID
    var id: String
    
    /// 套餐名称
    var name: String
    
    /// 积分数量
    var points: Int
    
    /// 赠送积分
    var bonusPoints: Int
    
    /// 原价（分）
    var originalPrice: Int
    
    /// 售价（分）
    var price: Int
    
    /// 是否推荐
    var isRecommended: Bool
    
    /// 是否热门
    var isHot: Bool
    
    /// 排序
    var sortOrder: Int
    
    /// 套餐标签
    var tags: [String]
    
    /// 显示价格（元）
    var displayPrice: String {
        String(format: "%.2f", Double(price) / 100)
    }
    
    /// 显示原价（元）
    var displayOriginalPrice: String {
        String(format: "%.2f", Double(originalPrice) / 100)
    }
    
    /// 实际获得积分
    var totalPoints: Int {
        points + bonusPoints
    }
    
    /// 折扣比例
    var discountRate: Double {
        guard originalPrice > 0 else { return 1.0 }
        return Double(price) / Double(originalPrice)
    }
    
    /// 每积分价格（分）
    var pricePerPoint: Double {
        guard totalPoints > 0 else { return 0 }
        return Double(price) / Double(totalPoints)
    }
}

// MARK: - 充值请求

/// 创建充值订单请求
struct CreateRechargeRequest: Codable {
    /// 套餐ID
    var packageId: String
    
    /// 支付方式
    var paymentMethod: PaymentMethod
    
    /// 优惠券ID（可选）
    var couponId: String?
}

/// 创建充值订单响应
struct CreateRechargeResponse: Codable {
    /// 订单信息
    var order: RechargeOrder
    
    /// 支付参数
    var paymentParams: PaymentParams
}

/// 支付参数
struct PaymentParams: Codable {
    /// 支付类型
    var type: String
    
    /// 应用ID
    var appId: String?
    
    /// 商户号
    var partnerId: String?
    
    /// 预支付交易会话ID
    var prepayId: String?
    
    /// 随机字符串
    var nonceStr: String?
    
    /// 时间戳
    var timeStamp: String?
    /// 签名
    var sign: String?
    
    /// 支付包（支付宝用）
    var payOrder: String?
    
    /// Apple Pay 支付参数
    var applePayParams: ApplePayParams?
}

/// Apple Pay 参数
struct ApplePayParams: Codable {
    /// 商户标识
    var merchantIdentifier: String
    
    /// 国家代码
    var countryCode: String
    
    /// 货币代码
    var currencyCode: String
    
    /// 支付摘要
    var summaryItems: [PaymentSummaryItem]
}

/// 支付摘要项
struct PaymentSummaryItem: Codable {
    /// 标签
    var label: String
    
    /// 金额（分）
    var amount: Int
}

// MARK: - 积分记录

/// 积分变动记录
struct PointsRecord: Codable, Identifiable {
    /// 记录ID
    var id: String
    
    /// 用户ID
    var userId: String
    
    /// 变动类型
    var type: PointsChangeType
    
    /// 变动积分
    var points: Int
    
    /// 变动前余额
    var balanceBefore: Int
    
    /// 变动后余额
    var balanceAfter: Int
    
    /// 关联订单ID
    var orderId: String?
    
    /// 关联任务ID
    var taskId: String?
    
    /// 描述
    var description: String
    
    /// 创建时间
    var createdAt: Date
}

/// 积分变动类型
enum PointsChangeType: String, Codable, CaseIterable {
    /// 充值
    case recharge = "recharge"
    
    /// 消费
    case consume = "consume"
    
    /// 退款
    case refund = "refund"
    
    /// 赠送
    case gift = "gift"
    
    /// 系统补偿
    case compensation = "compensation"
    
    /// 类型描述
    var description: String {
        switch self {
        case .recharge: return "充值"
        case .consume: return "消费"
        case .refund: return "退款"
        case .gift: return "赠送"
        case .compensation: return "系统补偿"
        }
    }
    
    /// 变动符号
    var sign: String {
        switch self {
        case .recharge, .refund, .gift, .compensation:
            return "+"
        case .consume:
            return "-"
        }
    }
    
    /// 颜色
    var color: String {
        switch self {
        case .recharge, .refund, .gift, .compensation:
            return "#34C759"
        case .consume:
            return "#FF3B30"
        }
    }
}

// MARK: - 预览

#if DEBUG
extension RechargeOrder {
    static let preview = RechargeOrder(
        id: "order_001",
        orderNo: "R202401150001",
        userId: "user_001",
        packageId: "pkg_001",
        packageName: "基础套餐",
        points: 1000,
        bonusPoints: 100,
        amount: 10000,
        paidAmount: 10000,
        discountAmount: 0,
        paymentMethod: .wechatPay,
        paymentChannel: "wechat",
        transactionId: "wx202401150001",
        paidAt: Date(),
        status: .completed,
        description: "积分充值",
        createdAt: Date().addingTimeInterval(-3600),
        updatedAt: Date()
    )
    
    static let previewList = [
        RechargeOrder.preview,
        RechargeOrder(
            id: "order_002",
            orderNo: "R202401140002",
            userId: "user_001",
            packageId: "pkg_002",
            packageName: "超值套餐",
            points: 5000,
            bonusPoints: 800,
            amount: 50000,
            paidAmount: 50000,
            discountAmount: 0,
            paymentMethod: .alipay,
            paymentChannel: "alipay",
            transactionId: "ali202401140002",
            paidAt: Date().addingTimeInterval(-24 * 3600),
            status: .completed,
            createdAt: Date().addingTimeInterval(-25 * 3600),
            updatedAt: Date().addingTimeInterval(-24 * 3600)
        ),
        RechargeOrder(
            id: "order_003",
            orderNo: "R202401130003",
            userId: "user_001",
            packageId: "pkg_003",
            packageName: "旗舰套餐",
            points: 10000,
            bonusPoints: 2500,
            amount: 100000,
            status: .pending,
            createdAt: Date().addingTimeInterval(-2 * 24 * 3600),
            updatedAt: Date().addingTimeInterval(-2 * 24 * 3600),
            expireAt: Date().addingTimeInterval(3600)
        )
    ]
}

extension RechargePackage {
    static let previewList = [
        RechargePackage(
            id: "pkg_001",
            name: "基础套餐",
            points: 1000,
            bonusPoints: 100,
            originalPrice: 10000,
            price: 10000,
            isRecommended: false,
            isHot: false,
            sortOrder: 1,
            tags: ["入门首选"]
        ),
        RechargePackage(
            id: "pkg_002",
            name: "超值套餐",
            points: 5000,
            bonusPoints: 800,
            originalPrice: 50000,
            price: 45000,
            isRecommended: true,
            isHot: true,
            sortOrder: 2,
            tags: ["热门推荐", "省10%"]
        ),
        RechargePackage(
            id: "pkg_003",
            name: "旗舰套餐",
            points: 10000,
            bonusPoints: 2500,
            originalPrice: 100000,
            price: 80000,
            isRecommended: false,
            isHot: true,
            sortOrder: 3,
            tags: ["超值", "省20%"]
        ),
        RechargePackage(
            id: "pkg_004",
            name: "企业套餐",
            points: 50000,
            bonusPoints: 15000,
            originalPrice: 500000,
            price: 350000,
            isRecommended: false,
            isHot: false,
            sortOrder: 4,
            tags: ["企业专享", "省30%"]
        )
    ]
}
#endif
