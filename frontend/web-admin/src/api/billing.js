/**
 * billing.js - 计费管理相关 API
 * 功能：计费概览、订单管理、充值记录等接口
 *
 * 后端对应：
 * - BillingOrderController (@RequestMapping("/billing/order"))
 */
import { get, post } from '@/utils/request'

// ============================================================
// 订单管理 (BillingOrderController - /billing/order)
// ============================================================

/**
 * 创建充值订单 - POST /billing/order/recharge
 */
export function createRechargeOrder(params) {
  return post('/billing/order/recharge', params)
}

/**
 * 计算赠送金额 - GET /billing/order/bonus
 */
export function getBillingOverview() {
  return get('/billing/order/bonus')
}

/**
 * 获取收支趋势
 */
export function getBillingTrend(params) {
  return get('/billing/order/user/trend', params)
}

/**
 * 获取订单列表 - GET /billing/order/user/{userId}
 */
export function getOrderList(params) {
  return get(`/billing/order/user/${params.userId || 'current'}`, params)
}

/**
 * 获取订单详情 - GET /billing/order/{orderNo}
 */
export function getOrderDetail(orderNo) {
  return get(`/billing/order/${orderNo}`)
}

/**
 * 取消订单 - POST /billing/order/{orderNo}/cancel
 */
export function cancelOrder(orderNo) {
  return post(`/billing/order/${orderNo}/cancel`)
}

// ============================================================
// 充值管理
// ============================================================

/**
 * 获取充值记录列表
 */
export function getRechargeList(params) {
  return get('/billing/order/user/current', params)
}

/**
 * 确认充值到账
 */
export function confirmRecharge(rechargeId, params = {}) {
  return post(`/billing/order/${rechargeId}/cancel`, params)
}

// ============================================================
// 消费统计
// ============================================================

/**
 * 获取消费排行
 */
export function getConsumeRanking(params) {
  return get('/billing/order/bonus', params)
}

/**
 * 获取余额列表
 */
export function getBalanceList(params) {
  return get('/billing/order/bonus', params)
}

export default {
  createRechargeOrder,
  getBillingOverview,
  getBillingTrend,
  getOrderList,
  getOrderDetail,
  cancelOrder,
  getRechargeList,
  confirmRecharge,
  getConsumeRanking,
  getBalanceList
}
