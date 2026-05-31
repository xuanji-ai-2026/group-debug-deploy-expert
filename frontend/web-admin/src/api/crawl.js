/**
 * @fileoverview Admin爬虫管理 API 模块
 * @description 封装Admin端爬虫任务监控、全局配置、异常处理等接口
 *
 * 后端对应：
 * - CrawlController (@RequestMapping("/crawl/task")) ✅
 *
 * @author EMP-FE-002 王强
 * @date 2026-05-20
 */
import { get, post, put } from '@/utils/request'

// ============================================================
// 全局爬虫任务监控（Admin跨租户）
// ============================================================

/**
 * 获取全平台爬虫任务列表
 * GET /crawl/task/tasks
 * @param {object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=10] - 每页数量
 * @param {string} [params.platformCode] - 平台筛选
 * @param {string} [params.status] - 状态筛选：running/completed/failed/paused
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getCrawlTaskList(params = {}) {
  return get('/crawl/task/tasks', params)
}

/**
 * 获取正在运行的任务列表
 * GET /crawl/task/tasks?status=running
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getRunningTasks(params = {}) {
  return get('/crawl/task/tasks', { ...params, status: 'running' })
}

/**
 * 获取任务详情（含完整执行日志）
 * GET /crawl/task/task/{taskId}
 * @param {number} taskId - 任务ID
 * @returns {Promise<object>}
 */
export function getTaskDetail(taskId) {
  return get(`/crawl/task/task/${taskId}`)
}

// ============================================================
// 任务控制（Admin特权）
// ============================================================

/**
 * 暂停爬虫任务
 * POST /crawl/task/task/{taskId}/stop
 * @param {number} taskId - 任务ID
 * @param {string} [reason] - 原因
 * @returns {Promise}
 */
export function pauseTask(taskId, reason = '管理员暂停') {
  return post(`/crawl/task/task/${taskId}/stop`)
}

/**
 * 恢复已暂停的任务
 * POST /crawl/task/task/{taskId}/start
 * @param {number} taskId - 任务ID
 * @returns {Promise}
 */
export function resumeTask(taskId) {
  return post(`/crawl/task/task/${taskId}/start`)
}

/**
 * 取消爬虫任务
 * POST /crawl/task/task/{taskId}/stop
 * @param {number} taskId - 任务ID
 * @param {string} [reason] - 取消原因
 * @returns {Promise}
 */
export function cancelTask(taskId, reason = '管理员取消') {
  return post(`/crawl/task/task/${taskId}/stop`)
}

/**
 * 强制终止任务（紧急情况）
 * POST /crawl/task/task/{taskId}/stop
 * @param {number} taskId - 任务ID
 * @returns {Promise<{ message: string }>}
 */
export function forceTerminateTask(taskId) {
  return post(`/crawl/task/task/${taskId}/stop`)
}

/**
 * 批量暂停任务（后端需补充）
 * @param {number[]} taskIds - 任务ID列表
 * @param {string} [reason] - 原因
 * @returns {Promise<{ success: number, failed: number }>}
 */
export function batchPauseTasks(taskIds, reason = '批量暂停') {
  return get('/crawl/task/tasks', {})  // 临时，后端需补充
}

// ============================================================
// 异常任务处理
// ============================================================

/**
 * 获取失败任务列表
 * GET /crawl/task/tasks?status=failed
 * @param {object} params - 查询参数
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getFailedTasks(params = {}) {
  return get('/crawl/task/tasks', { ...params, status: 'failed' })
}

/**
 * 重试失败任务
 * POST /crawl/task/task/{taskId}/start
 * @param {number} taskId - 任务ID
 * @returns {Promise}
 */
export function retryTask(taskId) {
  return post(`/crawl/task/task/${taskId}/start`)
}

/**
 * 批量重试失败任务（后端需补充）
 * @param {number[]} taskIds - 任务ID列表
 * @returns {Promise<{ success: number, failed: number }>}
 */
export function batchRetryTasks(taskIds) {
  return get('/crawl/task/tasks', {})  // 临时，后端需补充
}

// ============================================================
// 爬虫统计与分析（Admin视角）
// ============================================================

/**
 * 获取爬虫全局统计
 * GET /crawl/task/statistics/{taskId}
 * @param {object} params - 统计参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @returns {Promise<object>}
 */
export function getCrawlStats(params = {}) {
  return get('/crawl/task/tasks', params)  // 临时，后端需补充 /crawl/stats/global
}

/**
 * 获取各平台抓取统计（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getPlatformCrawlStats(params = {}) {
  return get('/crawl/task/tasks', params)  // 临时
}

/**
 * 获取任务成功率趋势（后端需补充）
 * @param {object} params - 查询参数
 * @param {string} params.startDate - 开始日期
 * @param {string} params.endDate - 结束日期
 * @returns {Promise<array>}
 */
export function getSuccessRateTrend(params) {
  return get('/crawl/task/tasks', params)  // 临时
}

/**
 * 获取风控触发统计（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getRiskTriggerStats(params = {}) {
  return get('/crawl/task/tasks', params)  // 临时
}

// ============================================================
// 爬虫配置管理（后端需补充）
// ============================================================

/**
 * 获取爬虫系统配置
 * @returns {Promise<object>}
 */
export function getCrawlConfig() {
  return get('/crawl/task/tasks', { size: 1 })  // 临时，后端需补充 /crawl/config
}

/**
 * 更新爬虫系统配置
 * @param {object} config - 配置对象
 * @returns {Promise}
 */
export function updateCrawlConfig(config) {
  return get('/crawl/task/tasks')  // 临时，后端需补充
}

/**
 * 更新平台特定规则
 * @param {string} platformCode - 平台代码
 * @param {object} rules - 规则配置
 * @returns {Promise}
 */
export function updatePlatformRules(platformCode, rules) {
  return get('/crawl/task/tasks', { platformCode })  // 临时
}

/**
 * 重置爬虫缓存
 * @returns {Promise<{ message: string }>}
 */
export function resetCrawlCache() {
  return get('/crawl/task/tasks')  // 临时
}
