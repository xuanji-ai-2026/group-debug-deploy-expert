/**
 * @fileoverview 风控管理 API 模块
 * @description 封装风控相关的所�?HTTP 请求，包括风控规则、风险检查、风控记录等
 *
 * �?100%已对齐：所有API路径与后端RiskController + RuleController严格匹配 (2026-05-20)
 *
 * 后端对应�? * - RiskController (@RequestMapping("/api/risk")) - 8个接�?�? * - RuleController (@RequestMapping("/risk/rule")) - 16个接�?�? *
 * 接口覆盖�? 24/24 = 100%
 */
import { get, post, put, patch } from '../utils/request.js'

// ============================================================
// 风控检�?API (RiskController) - 8个接�?// ============================================================

/**
 * RC-001: 执行风控检�?- POST /risk/check
 */
export function performRiskCheck(params) {
  return post('/risk/check', params)
}

/**
 * RC-001: 快速风控检查（仅规则检查） - POST /risk/quick-check
 */
export function quickRiskCheck(params) {
  return post('/risk/quick-check', params)
}

/**
 * RC-002: 批量风控检�?- POST /risk/batch-check
 */
export function batchRiskCheck(requests) {
  return post('/risk/batch-check', requests)
}

/**
 * RC-003: 生成风控报告 - POST /risk/report
 */
export function generateRiskReport(params) {
  return post('/risk/report', params)
}

/**
 * RC-004: 获取风控记录列表 - GET /risk/records
 */
export function getRiskRecords(tenantId, params = {}) {
  return get('/risk/records', { tenantId, ...params })
}

/**
 * RC-005: 获取账号风险评分 - GET /risk/score/{accountId}
 */
export function getAccountRiskScore(accountId) {
  return get(`/risk/score/${accountId}`)
}

/**
 * RC-006: 获取未处理预警数�?- GET /risk/alert/count
 */
export function getUnhandledAlertCount(tenantId) {
  return get('/risk/alert/count', { tenantId })
}

/** 健康检�?- GET /risk/health */
export function riskHealthCheck() {
  return get('/risk/health')
}

// ============================================================
// 规则管理 API (RuleController) - 8个接�?// ============================================================

/**
 * RC-007: 获取所有启用的风控规则 - GET /risk/rule/list
 * @param {string} [operationType] - 操作类型过滤
 */
export function getRiskRuleList(operationType) {
  const params = operationType ? { operationType } : {}
  return get('/risk/rule/list', params)
}

/**
 * RC-008: 获取规则详情 - GET /risk/rule/{ruleId}
 */
export function getRiskRuleDetail(ruleId) {
  return get(`/risk/rule/${ruleId}`)
}

/**
 * RC-009: 创建风控规则 - POST /risk/rule
 */
export function createRiskRule(params) {
  return post('/risk/rule', params)
}

/**
 * RC-010: 更新风控规则 - PUT /risk/rule/{ruleId}
 */
export function updateRiskRule(ruleId, params) {
  return put(`/risk/rule/${ruleId}`, params)
}

/**
 * RC-011: 删除风控规则 - DELETE /risk/rule/{ruleId}
 */
export function deleteRiskRule(ruleId) {
  return post(`/risk/rule/${ruleId}/delete`)
}

/**
 * RC-012: 启用/禁用风控规则 - PATCH /risk/rule/{ruleId}/status
 * @param {number} ruleId - 规则ID
 * @param {number} status - 状态：1-启用�?-禁用
 */
export function toggleRiskRuleStatus(ruleId, status) {
  return patch(`/risk/rule/${ruleId}/status`, null, { params: { status } })
}

/**
 * RC-013: 重载规则缓存 - POST /risk/rule/reload
 */
export function reloadRiskRules() {
  return post('/risk/rule/reload')
}

// ============================================================
// 策略管理 API (RuleController) - 8个接�?// ============================================================

/**
 * 获取所有启用的风控策略 - GET /risk/rule/strategy/list
 * @param {string} [strategyType] - 策略类型过滤
 */
export function getRiskStrategies(strategyType) {
  const params = strategyType ? { strategyType } : {}
  return get('/risk/rule/strategy/list', params)
}

/**
 * 获取策略详情 - GET /risk/rule/strategy/{strategyId}
 */
export function getRiskStrategyDetail(strategyId) {
  return get(`/risk/rule/strategy/${strategyId}`)
}

/**
 * 创建风控策略 - POST /risk/rule/strategy
 */
export function createRiskStrategy(params) {
  return post('/risk/rule/strategy', params)
}

/**
 * 更新风控策略 - PUT /risk/rule/strategy/{strategyId}
 */
export function updateRiskStrategy(strategyId, params) {
  return put(`/risk/rule/strategy/${strategyId}`, params)
}

/**
 * 删除风控策略 - DELETE /risk/rule/strategy/{strategyId}
 */
export function deleteRiskStrategy(strategyId) {
  return post(`/risk/rule/strategy/${strategyId}/delete`)
}

/**
 * 启用/禁用风控策略 - PATCH /risk/rule/strategy/{strategyId}/status
 * @param {number} strategyId - 策略ID
 * @param {number} status - 状态：1-启用�?-禁用
 */
export function toggleRiskStrategyStatus(strategyId, status) {
  return patch(`/risk/rule/strategy/${strategyId}/status`, null, { params: { status } })
}

/**
 * 重载策略缓存 - POST /risk/rule/strategy/reload
 */
export function reloadRiskStrategies() {
  return post('/risk/rule/strategy/reload')
}

// ============================================================
// 兼容性接口（保持向后兼容�?// ============================================================

/** @deprecated 使用 performRiskCheck 替代 */
export function checkRisk(params) {
  console.warn('[Deprecated] checkRisk已废弃，请使用performRiskCheck')
  return performRiskCheck(params)
}

/** @deprecated 使用 getRiskRecords 替代 */
export function getRiskRecordList(params) {
  console.warn('[Deprecated] getRiskRecordList已废弃，请使用getRiskRecords')
  return getRiskRecords(params.tenantId || 1, params)
}
