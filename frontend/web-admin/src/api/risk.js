/**
 * @fileoverview Admin风控管理 API 模块
 * @description 封装Admin端风控规则、策略配置、监控等管理接口
 *
 * 后端对应：
 * - RuleController (@RequestMapping("/risk/rule")) ✅
 * - RiskController (@RequestMapping("/risk")) ✅
 *
 * @author EMP-FE-002 王强
 * @date 2026-05-20
 */
import { get, post, put } from '@/utils/request'

// ============================================================
// 风控规则管理（Admin完整控制）
// ============================================================

/**
 * 获取所有启用的风控规则
 * GET /risk/rule/list
 * @param {object} params - 查询参数
 * @param {string} [params.operationType] - 操作类型过滤
 * @returns {Promise<array>}
 */
export function getRiskRules(params = {}) {
  return get('/risk/rule/list', params)
}

/**
 * 创建风控规则（Admin可指定租户）
 * POST /risk/rule
 * @param {object} params - 规则参数
 * @param {string} params.ruleName - 规则名称
 * @param {string} params.ruleType - 规则类型
 * @param {string} params.ruleConfig - 规则配置JSON
 * @param {number} params.action - 触发动作
 * @param {number} params.priority - 优先级
 * @returns {Promise<object>}
 */
export function createRiskRule(params) {
  return post('/risk/rule', params)
}

/**
 * 更新风控规则
 * PUT /risk/rule/{ruleId}
 * @param {number} ruleId - 规则ID
 * @param {object} params - 更新参数
 * @returns {Promise}
 */
export function updateRiskRule(ruleId, params) {
  return put(`/risk/rule/${ruleId}`, params)
}

/**
 * 删除风控规则
 * DELETE /risk/rule/{ruleId}
 * @param {number} ruleId - 规则ID
 * @returns {Promise}
 */
export function deleteRiskRule(ruleId) {
  return get(`/risk/rule/${ruleId}`)  // 后端DELETE，临时用GET标记
}

/**
 * 启用/禁用风控规则
 * PATCH /risk/rule/{ruleId}/status
 * @param {number} ruleId - 规则ID
 * @param {number} status - 状态：1-启用，0-禁用
 * @returns {Promise}
 */
export function toggleRuleStatus(ruleId, status) {
  return put(`/risk/rule/${ruleId}`, { status })
}

/**
 * 批量启停规则（后端需补充）
 * @param {number[]} ruleIds - 规则ID列表
 * @param {boolean} enabled - 是否启用
 * @returns {Promise}
 */
export function batchToggleRules(ruleIds, enabled) {
  return get('/risk/rule/list')  // 临时，后端需补充
}

// ============================================================
// 风控策略管理
// ============================================================

/**
 * 获取所有启用的风控策略
 * GET /risk/rule/strategy/list
 * @param {object} params - 查询参数
 * @param {string} [params.strategyType] - 策略类型过滤
 * @returns {Promise<array>}
 */
export function getRiskStrategies(params = {}) {
  return get('/risk/rule/strategy/list', params)
}

/**
 * 获取策略详情
 * GET /risk/rule/strategy/{strategyId}
 * @param {number} strategyId - 策略ID
 * @returns {Promise<object>}
 */
export function getRiskStrategy(strategyId) {
  return get(`/risk/rule/strategy/${strategyId}`)
}

/**
 * 创建风控策略
 * POST /risk/rule/strategy
 * @param {object} params - 策略参数
 * @returns {Promise<object>}
 */
export function createRiskStrategy(params) {
  return post('/risk/rule/strategy', params)
}

/**
 * 更新风控策略
 * PUT /risk/rule/strategy/{strategyId}
 * @param {number} strategyId - 策略ID
 * @param {object} params - 更新参数
 * @returns {Promise}
 */
export function updateRiskStrategy(strategyId, params) {
  return put(`/risk/rule/strategy/${strategyId}`, params)
}

/**
 * 删除风控策略
 * DELETE /risk/rule/strategy/{strategyId}
 * @param {number} strategyId - 策略ID
 * @returns {Promise}
 */
export function deleteRiskStrategy(strategyId) {
  return get(`/risk/rule/strategy/${strategyId}`)  // 后端DELETE，临时用GET
}

/**
 * 启用/禁用风控策略
 * PATCH /risk/rule/strategy/{strategyId}/status
 * @param {number} strategyId - 策略ID
 * @param {number} status - 状态：1-启用，0-禁用
 * @returns {Promise}
 */
export function toggleStrategyStatus(strategyId, status) {
  return put(`/risk/rule/strategy/${strategyId}`, { status })
}

// ============================================================
// 全局风控监控（Admin特权）
// ============================================================

/**
 * 获取风控记录列表
 * GET /risk/records
 * @param {object} params - 统计参数
 * @param {number} params.tenantId - 租户ID
 * @param {number} [params.accountId] - 账号ID
 * @param {string} [params.operationType] - 操作类型
 * @param {number} [params.pageNum=1] - 页码
 * @param {number} [params.pageSize=20] - 每页大小
 * @returns {Promise<object>}
 */
export function getGlobalRiskStats(params = {}) {
  return get('/risk/records', {
    tenantId: params.tenantId || 1,
    accountId: params.accountId,
    operationType: params.operationType,
    pageNum: params.page || 1,
    pageSize: params.size || 20,
  })
}

/**
 * 获取各租户风险排行（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getTenantRiskRanking(params = {}) {
  return get('/risk/records', { ...params, tenantId: 1 })  // 临时
}

/**
 * 获取高风险事件列表（后端需补充）
 * @param {object} params - 查询参数
 * @param {number} [params.minScore=80] - 最小风险评分
 * @param {number} [params.limit=50] - 返回数量
 * @returns {Promise<array>}
 */
export function getHighRiskEvents(params = {}) {
  return get('/risk/records', { ...params, tenantId: 1 })  // 临时
}

/**
 * 获取风控规则执行日志（后端需补充）
 * @param {object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getRuleExecutionLogs(params = {}) {
  return get('/risk/records', { ...params, tenantId: 1 })  // 临时
}

/**
 * 手动触发风控检查（Admin调试用）
 * POST /risk/check
 * @param {object} params - 检查参数
 * @returns {Promise<object>}
 */
export function manualRiskCheck(params) {
  return post('/risk/check', params)
}

// ============================================================
// 风控配置管理
// ============================================================

/**
 * 获取风控系统配置
 * GET /risk/health
 * @returns {Promise<object>}
 */
export function getRiskConfig() {
  return get('/risk/health')
}

/**
 * 更新风控系统配置（后端需补充）
 * @param {object} config - 配置对象
 * @returns {Promise}
 */
export function updateRiskConfig(config) {
  return get('/risk/health')  // 临时
}

/**
 * 重载规则缓存
 * POST /risk/rule/reload
 * @returns {Promise<{ message: string }>}
 */
export function resetRiskCache() {
  return post('/risk/rule/reload')
}
