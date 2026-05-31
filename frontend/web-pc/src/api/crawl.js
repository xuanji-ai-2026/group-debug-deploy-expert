/**
 * @fileoverview 爬虫管理 API 模块
 * @description 封装评论抓取、分析、商机生成等所有 HTTP 请求
 *
 * 后端对应：
 * - CrawlController (@RequestMapping("/crawl/task"))
 * - CommentCrawlController (@RequestMapping("/crawl"))
 * - MobileCrawlController (@RequestMapping("/crawl/mobile"))
 */
import { get, post, put } from '../utils/request.js'

// ============================================================
// 抓取任务管理 API (CrawlController - /crawl/task)
// ============================================================

/**
 * 创建抓取任务 - POST /crawl/task/task/create
 */
export function createCrawlTask(params) {
  return post('/crawl/task/task/create', params)
}

/**
 * 获取抓取任务详情 - GET /crawl/task/task/{taskId}
 */
export function getCrawlTask(taskId) {
  return get(`/crawl/task/task/${taskId}`)
}

/**
 * 获取任务的评论列表 - GET /crawl/task/task/{taskId}/comments
 */
export function getTaskComments(taskId, params = {}) {
  return get(`/crawl/task/task/${taskId}/comments`, params)
}

/**
 * 批量分析评论（AI意向分析）- POST /crawl/task/task/{taskId}/analyze
 */
export function analyzeComments(taskId) {
  return post(`/crawl/task/task/${taskId}/analyze`)
}

/**
 * 分析单条评论 - POST /crawl/task/comment/{commentId}/analyze
 */
export function analyzeSingleComment(commentId) {
  return post(`/crawl/task/comment/${commentId}/analyze`)
}

/**
 * 从评论中生成商机线索 - POST /crawl/task/task/{taskId}/generate-leads
 */
export function generateLeadsFromComments(taskId, criteria = {}) {
  return post(`/crawl/task/task/${taskId}/generate-leads`, criteria)
}

/**
 * 获取任务统计信息 - GET /crawl/task/statistics/{taskId}
 */
export function getTaskStatistics(taskId) {
  return get(`/crawl/task/statistics/${taskId}`)
}

/**
 * 获取抓取任务列表 - GET /crawl/task/tasks
 */
export function getCrawlTaskList(params = {}) {
  return get('/crawl/task/tasks', params)
}

/**
 * 启动抓取任务 - POST /crawl/task/task/{taskId}/start
 */
export function startCrawlTask(taskId) {
  return post(`/crawl/task/task/${taskId}/start`)
}

/**
 * 停止抓取任务 - POST /crawl/task/task/{taskId}/stop
 */
export function stopCrawlTask(taskId) {
  return post(`/crawl/task/task/${taskId}/stop`)
}

/**
 * 删除抓取任务 - DELETE /crawl/task/task/{taskId}
 */
export function deleteCrawlTask(taskId) {
  return post(`/crawl/task/task/${taskId}/stop`)  // 后端无直接DELETE，先stop
}

// ============================================================
// 种子数据 / 爬取管理 (CommentCrawlController - /crawl)
// ============================================================

/**
 * 从URL抓取 - POST /crawl/url
 */
export function crawlFromUrl(params) {
  return post('/crawl/url', params)
}

/**
 * 按平台抓取 - POST /crawl/platform/{platform}
 */
export function crawlByPlatform(platform, params) {
  return post(`/crawl/platform/${platform}`, params)
}

/**
 * 批量抓取 - POST /crawl/batch
 */
export function batchCrawl(params) {
  return post('/crawl/batch', params)
}

/**
 * 导入CSV并抓取 - POST /crawl/import/csv
 */
export function importAndCrawl(params) {
  return post('/crawl/import/csv', params)
}

/**
 * 获取平台列表 - GET /crawl/platforms
 */
export function getPlatforms() {
  return get('/crawl/platforms')
}

/**
 * 获取抓取历史 - GET /crawl/history
 */
export function getCrawlHistory(params) {
  return get('/crawl/history', params)
}

// ============================================================
// 抓取目标池管理 (CommentCrawlController - /crawl/targets)
// ============================================================

/**
 * 添加抓取目标 - POST /crawl/targets/add
 */
export function addCrawlTarget(params) {
  return post('/crawl/targets/add', params)
}

/**
 * 批量添加抓取目标 - POST /crawl/targets/batch-add
 */
export function batchAddCrawlTargets(params) {
  return post('/crawl/targets/batch-add', params)
}

/**
 * 获取抓取目标列表 - GET /crawl/targets
 */
export function getCrawlTargets(params) {
  return get('/crawl/targets', params)
}

/**
 * 删除抓取目标 - DELETE /crawl/targets/{targetId}
 */
export function deleteCrawlTarget(targetId) {
  return post(`/crawl/targets/${targetId}/delete`)
}

/**
 * 获取抓取目标详情 - GET /crawl/targets/{targetId}/detail
 */
export function getCrawlTargetDetail(targetId) {
  return get(`/crawl/targets/${targetId}/detail`)
}

/**
 * 获取目标池统计 - GET /crawl/targets/stats
 */
export function getTargetPoolStats() {
  return get('/crawl/targets/stats')
}

// ============================================================
// 移动端快捷操作 (MobileCrawlController - /crawl/mobile)
// ============================================================

/**
 * 快速创建任务 - POST /crawl/mobile/quick-create
 */
export function quickCreateTask(params) {
  return post('/crawl/mobile/quick-create', params)
}

/**
 * 获取任务进度 - GET /crawl/mobile/task/{taskId}/progress
 */
export function getTaskProgress(taskId) {
  return get(`/crawl/mobile/task/${taskId}/progress`)
}

/**
 * 快速生成商机 - POST /crawl/mobile/task/{taskId}/quick-generate-leads
 */
export function quickGenerateLeads(taskId) {
  return post(`/crawl/mobile/task/${taskId}/quick-generate-leads`)
}

/**
 * 我的任务列表 - GET /crawl/mobile/my-tasks
 */
export function getMyTasks() {
  return get('/crawl/mobile/my-tasks')
}
