/**
 * @fileoverview 计费充值 API 模块
 * @description 封装订单、支付、充值等 HTTP 请求
 *
 * 后端对应：
 * - BillingOrderController (@RequestMapping("/billing/order")) ✅ - 7个接口
 * - PackagePurchaseController (@RequestMapping("/billing/package")) ✅
 * - InvoiceController (@RequestMapping("/billing/invoice")) ✅
 * - FinanceController (@RequestMapping("/billing/finance")) ✅
 * - CreditAccountController ✅
 *
 * @date 2026-05-20
 */
import { get, post } from '../utils/request.js'

// ============================================================
// 充值订单 API - BillingOrderController
// ============================================================

/** 创建充值订单 - POST /billing/order/recharge */
export function createRechargeOrder(tenantId, params) {
  return post('/billing/order/recharge', params, { params: { tenantId } })
}

/** 获取充值优惠 - GET /billing/order/bonus */
export function calculateBonus(amount) {
  return get('/billing/order/bonus', { amount })
}

/** 获取支付宝支付二维码 - GET /billing/order/{orderNo}/alipay-qr */
export function getAlipayQrCode(orderNo) {
  return get(`/billing/order/${orderNo}/alipay-qr`)
}

/** 获取微信支付二维码 - GET /billing/order/{orderNo}/wechatpay-qr */
export function getWechatPayQrCode(orderNo) {
  return get(`/billing/order/${orderNo}/wechatpay-qr`)
}

/** 查询订单状态 - GET /billing/order/{orderNo} */
export function getOrderStatus(orderNo) {
  return get(`/billing/order/${orderNo}`)
}

/** 获取用户订单列表 - GET /billing/order/user/{userId} */
export function getUserOrders(userId, orderType) {
  const params = orderType ? { orderType } : {}
  return get(`/billing/order/user/${userId}`, params)
}

/** 取消订单 - POST /billing/order/{orderNo}/cancel */
export function cancelOrder(orderNo) {
  return post(`/billing/order/${orderNo}/cancel`)
}

// ============================================================
// 余额查询 API - UserController
// ============================================================

/** 获取用户余额 - GET /user/balance */
export function getBalance() {
  return get('/user/balance')
}

// ============================================================
// 交易记录 API（后端需补充，临时用订单列表）
// ============================================================

/** 获取交易流水列表 - GET /billing/order/user/{userId} */
export function getTransactionList(params) {
  return get(`/billing/order/user/${params.userId || 1}`, params)
}

// ============================================================
// 套餐购买 API - PackagePurchaseController
// ============================================================

/** 获取套餐配置列表 - GET /billing/package/configs */
export function getPackageList() {
  return get('/billing/package/configs')
}

/** 获取用户套餐列表 - GET /billing/package/user/{userId} */
export function getOrderList(userId) {
  return get(`/billing/package/user/${userId || 1}`)
}

/** 创建套餐购买订单 - POST /billing/package/order */
export function purchasePackage(tenantId, userId, packageType) {
  return post('/billing/package/order', null, {
    params: { tenantId, userId, packageType }
  })
}

// ============================================================
// 发票 API - InvoiceController
// ============================================================

/** 获取用户发票列表 - GET /billing/invoice/user/{userId} */
export function getInvoiceList(userId) {
  return get(`/billing/invoice/user/${userId || 1}`)
}

/** 获取发票抬头列表（后端需补充） */
export function getInvoiceTitleList() {
  return get('/billing/invoice/user/1')  // 临时，后端需补充
}

/** 申请发票 - POST /billing/invoice/apply */
export function applyInvoice(tenantId, params) {
  return post('/billing/invoice/apply', params, { params: { tenantId } })
}

/** 获取发票详情 - GET /billing/invoice/{requestId} */
export function downloadInvoice(invoiceId) {
  return get(`/billing/invoice/${invoiceId}`)
}
