/**
 * @fileoverview 数据看板 API 模块
 * @description 封装数据看板相关的所有 HTTP 请求
 *
 * 后端对应：
 * - DashboardController (@RequestMapping("/data/dashboard")) ✅
 * - TrendController (@RequestMapping("/data/trend")) ✅
 * - ReportController (@RequestMapping("/data/report")) ✅
 *
 * @author EMP-FE-001 张婷
 */
import { get, post } from '../utils/request.js'

// ============================================================
// 数据看板
// ============================================================

/**
 * 获取运营数据看板
 * GET /data/dashboard/operation
 * @param {object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期（YYYY-MM-DD）
 * @param {string} [params.endDate] - 结束日期（YYYY-MM-DD）
 * @returns {Promise<object>}
 */
export function getDashboardOverview(params) {
  return get('/data/dashboard/operation', params)
}

/**
 * 获取获客数据看板
 * GET /data/dashboard/lead
 * @param {object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @returns {Promise<object>}
 */
export function getLeadDashboard(params) {
  return get('/data/dashboard/lead', params)
}

/**
 * 获取账号数据看板
 * GET /data/dashboard/account
 * @param {object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @returns {Promise<object>}
 */
export function getAccountDashboard(params) {
  return get('/data/dashboard/account', params)
}

/**
 * 获取消费数据看板
 * GET /data/dashboard/billing
 * @param {object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @returns {Promise<object>}
 */
export function getBillingDashboard(params) {
  return get('/data/dashboard/billing', params)
}

/**
 * 获取运营数据看板（别名）
 * GET /data/dashboard/operation
 * @param {object} params - 查询参数
 * @returns {Promise<object>}
 */
export function getOperationDashboard(params) {
  return get('/data/dashboard/operation', params)
}

/**
 * 获取运营趋势
 * GET /data/dashboard/operation/trend
 * @param {object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @returns {Promise<array>}
 */
export function getRealtimeDashboard(params) {
  return get('/data/dashboard/operation/trend', params)
}

/**
 * 获取数据看板配置（后端需补充）
 * @returns {Promise<array>}
 */
export function getDashboardConfig() {
  return get('/data/dashboard/operation', { size: 1 })  // 临时
}

/**
 * 保存数据看板配置（后端需补充）
 * @param {object[]} config - 看板配置列表
 * @returns {Promise}
 */
export function saveDashboardConfig(config) {
  return post('/data/dashboard/operation', config)  // 临时
}

// ============================================================
// 趋势分析
// ============================================================

/**
 * 获取趋势分析
 * GET /data/trend/{type}
 * @param {string} type - 趋势类型：OPERATION/LEAD/ACCOUNT/BILLING
 * @param {object} params - 查询参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @param {string} [params.compareType=MOM] - 对比类型：YOY同比/MOM环比
 * @returns {Promise<array>}
 */
export function getLeadTrend(params) {
  return get('/data/trend/LEAD', {
    startDate: params.startDate,
    endDate: params.endDate,
    compareType: 'MOM',
  })
}

/**
 * 获取转化漏斗分析（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<object>}
 */
export function getConversionFunnel(params) {
  return get('/data/trend/LEAD', params)  // 临时
}

/**
 * 获取账号表现分析
 * GET /data/trend/ACCOUNT
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getAccountPerformance(params) {
  return get('/data/trend/ACCOUNT', {
    startDate: params.startDate,
    endDate: params.endDate,
  })
}

/**
 * 获取消费趋势分析
 * GET /data/trend/BILLING
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getBillingTrend(params) {
  return get('/data/trend/BILLING', {
    startDate: params.startDate,
    endDate: params.endDate,
  })
}

/**
 * 获取来源分析（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<object>}
 */
export function getSourceAnalysis(params) {
  return get('/data/trend/LEAD', params)  // 临时
}

/**
 * 获取意向等级分布（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getIntentDistribution(params) {
  return get('/data/trend/LEAD', params)  // 临时
}

/**
 * 获取跟进效果分析（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<object>}
 */
export function getFollowUpAnalysis(params) {
  return get('/data/trend/LEAD', params)  // 临时
}

/**
 * 获取关键词效果分析（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getKeywordPerformance(params) {
  return get('/data/trend/LEAD', params)  // 临时
}

/**
 * 对比分析（多指标、多时间段对比）（后端需补充）
 * @param {object} params - 对比参数
 * @returns {Promise<object>}
 */
export function getComparisonAnalysis(params) {
  return post('/data/trend/LEAD', params)  // 临时
}

// ============================================================
// 报表导出
// ============================================================

/**
 * 导出商机报表
 * GET /data/report/export?reportType=LEAD
 * @param {object} params - 导出参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @param {string} [params.format='EXCEL'] - 导出格式：EXCEL/CSV
 * @returns {Promise<Blob>}
 */
export function exportLeadReport(params) {
  return get('/data/report/export', {
    reportType: 'LEAD',
    startDate: params.startDate,
    endDate: params.endDate,
    format: params.format || 'EXCEL',
  }, { responseType: 'blob' })
}

/**
 * 导出账号数据报表
 * GET /data/report/export?reportType=ACCOUNT
 * @param {object} params - 导出参数
 * @returns {Promise<Blob>}
 */
export function exportAccountReport(params) {
  return get('/data/report/export', {
    reportType: 'ACCOUNT',
    startDate: params.startDate,
    endDate: params.endDate,
    format: params.format || 'EXCEL',
  }, { responseType: 'blob' })
}

/**
 * 导出消费报表
 * GET /data/report/export?reportType=BILLING
 * @param {object} params - 导出参数
 * @returns {Promise<Blob>}
 */
export function exportBillingReport(params) {
  return get('/data/report/export', {
    reportType: 'BILLING',
    startDate: params.startDate,
    endDate: params.endDate,
    format: params.format || 'EXCEL',
  }, { responseType: 'blob' })
}

/**
 * 导出消息报表
 * GET /data/report/export?reportType=OPERATION
 * @param {object} params - 导出参数
 * @returns {Promise<Blob>}
 */
export function exportMessageReport(params) {
  return get('/data/report/export', {
    reportType: 'OPERATION',
    startDate: params.startDate,
    endDate: params.endDate,
    format: params.format || 'EXCEL',
  }, { responseType: 'blob' })
}

/**
 * 导出综合数据报表（后端需补充）
 * @param {object} params - 导出参数
 * @returns {Promise<{ fileUrl: string, fileName: string }>}
 */
export function exportComprehensiveReport(params) {
  return get('/data/report/export', {
    reportType: 'OPERATION',
    startDate: params.startDate,
    endDate: params.endDate,
    format: params.format || 'EXCEL',
  }, { responseType: 'blob' })
}

/**
 * 获取导出记录列表（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<{ list: array, pagination: object }>}
 */
export function getExportRecordList(params) {
  return get('/data/report/export', params)  // 临时
}

/**
 * 下载导出文件（后端需补充）
 * @param {string} recordId - 导出记录ID
 * @returns {Promise}
 */
export function downloadExportFile(recordId) {
  return get('/data/report/export', { recordId })  // 临时
}
