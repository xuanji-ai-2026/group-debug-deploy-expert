/**
 * @fileoverview 数据看板 API 模块
 * @description 封装运营/获客/账号/消费数据看板 HTTP 请求
 *
 * 后端对应：
 * - DashboardController (@RequestMapping("/data/dashboard")) ✅ - 5个接口
 *
 * @date 2026-05-20
 */
import { get } from '../utils/request.js'

/** DA-001: 运营数据看板 - GET /data/dashboard/operation */
export function getOperationDashboard(params = {}) {
  return get('/data/dashboard/operation', params)
}

/** DA-002: 获客数据看板 - GET /data/dashboard/lead */
export function getLeadDashboard(params = {}) {
  return get('/data/dashboard/lead', params)
}

/** DA-003: 账号数据看板 - GET /data/dashboard/account */
export function getAccountDashboard(params = {}) {
  return get('/data/dashboard/account', params)
}

/** DA-004: 消费数据看板 - GET /data/dashboard/billing */
export function getBillingDashboard(params = {}) {
  return get('/data/dashboard/billing', params)
}

/** 获取看板概览 - GET /data/dashboard/operation */
export function getDashboardData() {
  return get('/data/dashboard/operation')
}

/** 运营趋势 - GET /data/dashboard/operation/trend */
export function getOperationTrend(params = {}) {
  return get('/data/dashboard/operation/trend', params)
}
