/**
 * @fileoverview 内容管理 API 模块
 * @description 封装内容CRUD、发布、审核等所有 HTTP 请求
 *
 * 后端对应：
 * - ContentController (@RequestMapping("/content")) ✅ - 20个接口
 * - AiServiceController (@RequestMapping("/v1")) ✅ - AI生成
 *
 * @date 2026-05-20
 */
import { get, post, put, del } from '../utils/request.js'

// ============================================================
// 内容 CRUD API - 6个接口
// ============================================================

/** CO-001: 创建内容 - POST /content */
export function createContent(params) {
  return post('/content', params)
}

/** 更新内容 - PUT /content/{id} */
export function updateContent(id, params) {
  return put(`/content/${id}`, params)
}

/** 删除内容 - DELETE /content/{id} */
export function deleteContent(id) {
  return del(`/content/${id}`)
}

/** 批量删除内容 - DELETE /content/batch */
export function batchDeleteContent(ids) {
  return del('/content/batch', { data: { ids } })
}

/** 获取内容详情 - GET /content/{id} */
export function getContentDetail(id) {
  return get(`/content/${id}`)
}

/** 分页查询内容列表 - GET /content */
export function getContentList(params = {}) {
  return get('/content', params)
}

// ============================================================
// 草稿与发布 API - 5个接口
// ============================================================

/** CO-010: 保存草稿 - POST /content/draft */
export function saveDraft(params) {
  return post('/content/draft', params)
}

/** 立即发布内容 - POST /content/{id}/publish */
export function publishContent(id, platforms = []) {
  return post(`/content/${id}/publish`, null, { params: { platforms } })
}

/** 批量发布 - POST /content/batch/publish */
export function batchPublish(ids, platforms = []) {
  return post('/content/batch/publish', null, { params: { ids, platforms } })
}

/** 撤回内容 - POST /content/{id}/withdraw */
export function withdrawContent(id) {
  return post(`/content/${id}/withdraw`)
}

/** 置顶/取消置顶 - POST /content/{id}/top */
export function toggleContentTop(id, isTop) {
  return post(`/content/${id}/top`, null, { params: { isTop } })
}

// ============================================================
// 审核与检测 API - 3个接口
// ============================================================

/** CO-005: 违禁词检测 - POST /content/check-sensitive */
export function checkSensitiveWords(content) {
  return post('/content/check-sensitive', null, { params: { content } })
}

/** CO-007: 提交审核 - POST /content/{id}/submit-audit */
export function submitAudit(id) {
  return post(`/content/${id}/submit-audit`)
}

/** 审核内容（管理员） - POST /content/audit */
export function auditContent(params) {
  return post('/content/audit', params)
}

// ============================================================
// 定时发布 API - 2个接口
// ============================================================

/** CO-002: 定时发布 - POST /content/{id}/schedule */
export function schedulePublish(id, scheduledTime, platforms = []) {
  return post(`/content/${id}/schedule`, { scheduledTime, platforms })
}

/** 取消定时发布 - POST /content/{id}/cancel-schedule */
export function cancelSchedulePublish(id) {
  return post(`/content/${id}/cancel-schedule`)
}

// ============================================================
// 版本管理 API - 2个接口
// ============================================================

/** CO-009: 获取版本历史 - GET /content/{id}/versions */
export function getContentVersions(id) {
  return get(`/content/${id}/versions`)
}

/** 回滚到指定版本 - POST /content/{id}/rollback */
export function rollbackContentVersion(id, version) {
  return post(`/content/${id}/rollback`, null, { params: { version } })
}

// ============================================================
// 发布记录 API - 1个接口
// ============================================================

/** CO-004: 获取发布记录 - GET /content/{id}/publish-records */
export function getPublishRecords(id) {
  return get(`/content/${id}/publish-records`)
}

// ============================================================
// AI 生成内容 API
// ============================================================

/**
 * 生成文案
 * POST /v1/text/generate
 * @param {object} params - 生成参数
 * @returns {Promise<object>}
 */
export function generateText(params) {
  return post('/v1/text/generate', params)
}

/**
 * 撤回发布
 * POST /content/{id}/withdraw
 * @param {number} id - 内容ID
 * @returns {Promise}
 */
export function revokePublish(id) {
  return post(`/content/${id}/withdraw`)
}
