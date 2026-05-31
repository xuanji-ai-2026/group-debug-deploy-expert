/**
 * @fileoverview Admin内容管理 API 模块
 * @description 封装Admin端内容审核、违规处理、全局监控等接口
 *
 * 后端对应：
 * - ContentController (@RequestMapping("/content")) ✅
 * - ContentTagController (@RequestMapping("/content/tag")) ✅
 *
 * @author EMP-FE-002 王强
 * @date 2026-05-20
 */
import { get, post, del } from '@/utils/request'

// ============================================================
// 全局内容查询（Admin跨租户）
// ============================================================

/**
 * 获取全平台内容列表（Admin可跨租户查询）
 * GET /content
 * @param {object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @param {number} [params.tenantId] - 租户ID筛选
 * @param {number} [params.contentType] - 内容类型：1-图文/2-视频/3-纯文字
 * @param {number} [params.status] - 状态筛选
 * @param {string} [params.keyword] - 关键词搜索
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getContentList(params = {}) {
  return get('/content', params)
}

/**
 * 获取待审核内容列表
 * GET /content?status=pending
 * @param {object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getPendingReviewContent(params = {}) {
  return get('/content', { ...params, status: 'pending' })
}

/**
 * 获取已发布内容列表
 * GET /content?status=published
 * @param {object} params - 查询参数
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getPublishedContent(params = {}) {
  return get('/content', { ...params, status: 'published' })
}

/**
 * 获取内容详情（含完整审计信息）
 * GET /content/{id}
 * @param {number} contentId - 内容ID
 * @returns {Promise<object>}
 */
export function getContentDetail(contentId) {
  return get(`/content/${contentId}`)
}

// ============================================================
// 内容审核（Admin特权操作）
// ============================================================

/**
 * 审核通过内容
 * POST /content/audit
 * @param {number} contentId - 内容ID
 * @param {string} [reason] - 审核意见
 * @returns {Promise}
 */
export function approveContent(contentId, reason = '') {
  return post('/content/audit', { id: contentId, action: 'approve', reason })
}

/**
 * 驳回内容
 * POST /content/audit
 * @param {number} contentId - 内容ID
 * @param {object} params - 驳回参数
 * @param {string} params.reason - 驳回原因
 * @param {string} [params.violationType] - 违规类型
 * @returns {Promise}
 */
export function rejectContent(contentId, params) {
  return post('/content/audit', {
    id: contentId,
    action: 'reject',
    reason: params.reason,
    violationType: params.violationType,
  })
}

/**
 * 批量审核内容
 * POST /content/audit（循环调用）
 * @param {object} params - 批量参数
 * @param {number[]} params.contentIds - 内容ID列表
 * @param {string} params.action - 操作：approve/reject
 * @param {string} [params.reason] - 原因
 * @returns {Promise<{ success: number, failed: number }>}
 */
export function batchAuditContent(params) {
  return post('/content/audit', {
    ids: params.contentIds,
    action: params.action,
    reason: params.reason,
  })
}

/**
 * 下架已发布内容
 * POST /content/{id}/withdraw
 * @param {number} contentId - 内容ID
 * @param {string} [reason] - 下架原因
 * @returns {Promise}
 */
export function takeDownContent(contentId, reason = '违规下架') {
  return post(`/content/${contentId}/withdraw`)
}

/**
 * 恢复下架内容
 * POST /content/{id}/publish
 * @param {number} contentId - 内容ID
 * @returns {Promise}
 */
export function restoreContent(contentId) {
  return post(`/content/${contentId}/publish`)
}

// ============================================================
// 违规内容管理
// ============================================================

/**
 * 获取违规内容列表
 * GET /content?status=violated
 * @param {object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @param {string} [params.severity] - 严重程度：high/medium/low
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getViolatedContent(params = {}) {
  return get('/content', { ...params, status: 'violated' })
}

/**
 * 处理违规内容
 * POST /content/{id}/withdraw 或 DELETE /content/{id}
 * @param {number} contentId - 内容ID
 * @param {object} params - 处理参数
 * @param {string} params.action - 处理动作：warning/delete/ban_user
 * @param {string} params.reason - 处理原因
 * @param {number} [params.punishDays] - 处罚天数
 * @returns {Promise}
 */
export function handleViolation(contentId, params) {
  if (params.action === 'delete') {
    return del(`/content/${contentId}`)
  }
  return post(`/content/${contentId}/withdraw`)
}

// ============================================================
// 内容统计与分析（Admin视角）
// ============================================================

/**
 * 获取内容统计概览（后端需补充）
 * @param {object} params - 统计参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @returns {Promise<object>}
 */
export function getContentStats(params = {}) {
  return get('/content', { ...params, size: 1 })  // 临时，后端需补充 /content/stats/overview
}

/**
 * 获取内容发布趋势（后端需补充）
 * @param {object} params - 查询参数
 * @param {string} params.startDate - 开始日期
 * @param {string} params.endDate - 结束日期
 * @param {string} [params.granularity='day'] - 粒度
 * @returns {Promise<array>}
 */
export function getContentPublishTrend(params) {
  return get('/content', params)  // 临时，后端需补充 /content/stats/trend
}

/**
 * 获取各平台内容分布统计（后端需补充）
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getPlatformDistribution(params = {}) {
  return get('/content', params)  // 临时，后端需补充 /content/stats/platform-distribution
}

/**
 * 获取热门内容排行（后端需补充）
 * @param {object} params - 查询参数
 * @param {number} [params.limit=20] - 返回数量
 * @returns {Promise<array>}
 */
export function getTopContent(params = {}) {
  return get('/content', { ...params, sortBy: 'popular' })  // 临时，后端需补充
}

// ============================================================
// 敏感词管理（后端需补充）
// ============================================================

/**
 * 获取敏感词列表
 * @param {object} params - 查询参数
 * @returns {Promise<array>}
 */
export function getSensitiveWords(params = {}) {
  return get('/content', params)  // 临时，后端需补充 /content/sensitive-words
}

/**
 * 添加敏感词
 * @param {object} params - 参数
 * @param {string} params.word - 敏感词
 * @param {string} [params.category] - 分类
 * @param {number} [params.level] - 级别：1-警告/2-拦截/3-严重
 * @returns {Promise}
 */
export function addSensitiveWord(params) {
  return post('/content', params)  // 临时，后端需补充 /content/sensitive-words
}

/**
 * 删除敏感词
 * @param {number} wordId - 敏感词ID
 * @returns {Promise}
 */
export function deleteSensitiveWord(wordId) {
  return del(`/content/${wordId}`)  // 临时，后端需补充
}

/**
 * 导入敏感词库
 * @param {FormData} formData - 文件表单数据
 * @returns {Promise<{ imported: number, skipped: number }>}
 */
export function importSensitiveWords(formData) {
  return post('/content', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })  // 临时，后端需补充
}
