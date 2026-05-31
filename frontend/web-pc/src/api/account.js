/**
 * @fileoverview 账号管理 API 模块
 * @description 封装社媒账号相关的所�?HTTP 请求
 * 已修复：所有API路径与后端AccountController严格对齐�?026-05-01�? *
 * 后端对应：bx-social服务 AccountController (@RequestMapping("/api/account"))
 */
import { get, post, put, del } from '../utils/request.js'

// ============================================================
// 账号管理 API (AccountController)
// ============================================================

/**
 * 分页查询账号列表
 * @param {object} params - 查询参数
 * @param {number} [params.pageNum] - 页码
 * @param {number} [params.pageSize] - 每页数量
 * @param {string} [params.platformCode] - 平台代码：DOUYIN/XIAOHONGSHU/SHIPINHAO
 * @param {number} [params.groupId] - 分组ID
 * @param {number} [params.status] - 状态：1正常/2异常/3封禁/4授权过期
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getAccountList(params) {
  return get('/account/page', params)
}

/**
 * 获取账号详情
 * @param {number} accountId - 账号ID
 * @returns {Promise<object>}
 */
export function getAccountDetail(accountId) {
  return get(`/account/${accountId}`)
}

/**
 * 创建/保存账号
 * @param {object} params - 账号参数
 * @returns {Promise<object>}
 */
export function saveAccount(params) {
  return post('/account/save', params)
}

/**
 * 更新账号状�? * @param {number} accountId - 账号ID
 * @param {object} params - 状态参�? * @param {number} params.status - 新状�? * @param {string} [params errorMsg] - 错误信息（状态为异常时）
 * @returns {Promise}
 */
export function updateAccountStatus(accountId, params) {
  return post(`/account/${accountId}/status`, params)
}

/**
 * 解绑账号
 * @param {number} accountId - 账号ID
 * @returns {Promise}
 */
export function unbindAccount(accountId) {
  return post(`/account/${accountId}/unbind`)
}

/**
 * 获取用户分组列表
 * @param {number} userId - 用户ID
 * @param {string} [platformCode] - 平台筛�? * @returns {Promise<array>}
 */
export function getAccountGroups(userId, platformCode) {
  const params = {}
  if (platformCode) {
    params.platformCode = platformCode
  }
  return get('/account/groups', params, {
    headers: { 'X-User-Id': userId }
  })
}

/**
 * 根据平台查询账号
 * @param {string} platformCode - 平台代码
 * @returns {Promise<array>}
 */
export function getAccountsByPlatform(platformCode) {
  return get(`/account/platform/${platformCode}`)
}

/**
 * 获取异常账号列表
 * @returns {Promise<array>}
 */
export function getAbnormalAccounts() {
  return get('/account/abnormal')
}

// ============================================================
// 账号健康度相关（扩展功能�?// ============================================================

/**
 * 检查账号健康状态（轮询接口�? * @param {number} accountId - 账号ID
 * @returns {Promise<{ healthScore: number, status: string, issues: array }>}
 */
export function checkAccountHealth(accountId) {
  return get(`/account/${accountId}/health-check`)
}

/**
 * 批量检查账号健康状�? * @param {number[]} accountIds - 账号ID数组
 * @returns {Promise<array>}
 */
export function batchCheckHealth(accountIds) {
  return post('/account/batch-health', { accountIds })
}

export function refreshHealthScore(accountId) {
  return post(`/account/${accountId}/refresh-health`)
}

export function updateAccount(accountId, params) {
  return put(`/account/${accountId}`, params)
}

export function deleteAccount(accountId) {
  return del(`/account/${accountId}`)
}

export function createAccountBind(params) {
  return post('/account/bind', params)
}

export function getBindQrCode(params) {
  return post('/account/bind/qrcode', params)
}

export function getBindStatus(taskId) {
  return get(`/account/bind/status/${taskId}`)
}

export function getDeviceList(accountId) {
  return get(`/account/${accountId}/devices`)
}

export function unbindDevice(deviceId) {
  return post(`/account/device/${deviceId}/unbind`)
}

export function batchUnbindDevices(deviceIds) {
  return post('/account/device/batch-unbind', { deviceIds })
}
