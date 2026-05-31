/**
 * @fileoverview 养号策略 API 模块
 * @description 封装养号策略相关的所�?HTTP 请求，包括策略管理、执行控制、进度查询等
 * 
 * 后端对应：NurturingStrategyController (@RequestMapping("/api/nurturing"))
 * 
 * @author 北极星AI团队
 * @date 2026-05-20
 */

import { get, post, put, del } from '../utils/request.js'

// ============================================================
// 养号策略 CRUD API
// ============================================================

/**
 * 获取养号策略列表
 * @param {object} params - 查询参数
 * @param {number} [params.accountId] - 账号ID（可选筛选）
 * @param {number} [params.enabled] - 状态：0-禁用�?-启用
 * @returns {Promise<{ code: number, data: array }>}
 */
export function getNurturingStrategyList(params = {}) {
  return get('/nurturing/strategies', params)
}

/**
 * 获取养号策略详情
 * @param {number} strategyId - 策略ID
 * @returns {Promise<object>}
 */
export function getNurturingStrategyDetail(strategyId) {
  return get(`/nurturing/strategy/${strategyId}`)
}

/**
 * 创建养号策略
 * @param {object} params - 策略参数
 * @param {number} params.accountId - 关联账号ID
 * @param {string} params.strategyName - 策略名称
 * @param {object} params.dailyTargets - 每日目标
 * @param {number} params.dailyTargets.likeCount - 每日点赞�? * @param {number} params.dailyTargets.commentCount - 每日评论�? * @param {number} params.dailyTargets.shareCount - 每日分享�? * @param {number} params.dailyTargets.followCount - 每日关注�? * @param {number} params.dailyTargets.browseDuration - 浏览时长（分钟）
 * @param {number} params.durationDays - 养号周期（天�? * @param {string} params.riskLevel - 风险等级：LOW/MEDIUM/HIGH
 * @returns {Promise<object>}
 */
export function createNurturingStrategy(params) {
  return post('/nurturing/strategy', params)
}

/**
 * 更新养号策略
 * @param {number} strategyId - 策略ID
 * @param {object} params - 更新参数
 * @returns {Promise<object>}
 */
export function updateNurturingStrategy(strategyId, params) {
  return put(`/nurturing/strategy/${strategyId}`, params)
}

/**
 * 删除养号策略
 * @param {number} strategyId - 策略ID
 * @returns {Promise}
 */
export function deleteNurturingStrategy(strategyId) {
  return del(`/nurturing/strategy/${strategyId}`)
}

// ============================================================
// 养号策略控制 API
// ============================================================

/**
 * 启用/禁用养号策略
 * @param {number} strategyId - 策略ID
 * @param {number} status - 状态：0-禁用�?-启用
 * @returns {Promise<{ code: number, data: boolean }>}
 */
export function toggleNurturingStatus(strategyId, status) {
  return put(`/nurturing/strategy/${strategyId}/status`, null, {
    params: { status }
  })
}

/**
 * 启动养号策略执行
 * @param {number} strategyId - 策略ID
 * @returns {Promise<{ code: number, data: boolean }>}
 */
export function startNurturingStrategy(strategyId) {
  return post(`/nurturing/strategy/${strategyId}/start`)
}

/**
 * 停止养号策略执行
 * @param {number} strategyId - 策略ID
 * @returns {Promise<{ code: number, data: boolean }>}
 */
export function stopNurturingStrategy(strategyId) {
  return post(`/nurturing/strategy/${strategyId}/stop`)
}

// ============================================================
// 进度查询 API
// ============================================================

/**
 * 获取养号执行进度
 * @param {number} strategyId - 策略ID
 * @returns {Promise<{
 *   strategyId: number,
 *   accountId: number,
 *   status: string,
 *   dailyTargets: object,
 *   completedCounts: object,
 *   progressPercentage: number,
 *   startTime: string
 * }>}
 */
export function getNurturingProgress(strategyId) {
  return get(`/nurturing/strategy/${strategyId}/progress`)
}

/**
 * 获取账号养号状�? * @param {number} accountId - 账号ID
 * @returns {Promise<{ code: number, data: number }>} 0-未开始，1-进行中，2-已完�? */
export function getAccountNurturingStatus(accountId) {
  return get(`/nurturing/account/${accountId}/status`)
}

// ============================================================
// 模板 API
// ============================================================

/**
 * 获取养号策略模板列表
 * @returns {Promise<array>} 预定义模板列�? */
export function getNurturingTemplates() {
  return get('/nurturing/templates')
}
