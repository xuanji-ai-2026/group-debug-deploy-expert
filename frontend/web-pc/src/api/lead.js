/**
 * @fileoverview 商机管理 API 模块
 * @description 封装商机相关的所有 HTTP 请求
 *
 * 后端对应：
 * - LeadController (@RequestMapping("/lead"))
 * - LeadFollowUpController (@RequestMapping("/lead/followup"))
 * - LeadTaskController (@RequestMapping("/lead-tasks"))
 */
import { get, post, put, del } from '../utils/request.js'

// ============================================================
// 商机管理 API (LeadController - /lead)
// ============================================================

/**
 * 获取商机列表 - POST /lead/list
 */
export function getLeadList(params) {
  return post('/lead/list', params)
}

/**
 * 获取商机详情 - GET /lead/{id}
 */
export function getLeadDetail(leadId) {
  return get(`/lead/${leadId}`)
}

/**
 * 创建商机 - POST /lead
 */
export function createLead(params) {
  return post('/lead', params)
}

/**
 * 更新商机信息 - PUT /lead/{id}
 */
export function updateLead(leadId, params) {
  return put(`/lead/${leadId}`, params)
}

/**
 * 删除商机 - DELETE /lead/{id}
 */
export function deleteLead(leadId) {
  return del(`/lead/${leadId}`)
}

/**
 * 分配商机 - POST /lead/{id}/assign?ownerId=&ownerName=&assignType=
 */
export function assignLead(leadId, assigneeId, assigneeName) {
  return post(`/lead/${leadId}/assign`, null, {
    params: { ownerId: assigneeId, ownerName: assigneeName }
  })
}

/**
 * 变更商机状态 - POST /lead/{id}/status?status=&reason=
 */
export function updateLeadStatus(leadId, status, reason) {
  return post(`/lead/${leadId}/status`, null, {
    params: { status, reason }
  })
}

/**
 * 自动分配商机 - POST /lead/{id}/auto-assign
 */
export function autoAssignLead(leadId) {
  return post(`/lead/${leadId}/auto-assign`)
}

/**
 * 添加跟进记录 - POST /lead/{id}/follow
 */
export function addLeadFollow(leadId, params) {
  return post(`/lead/${leadId}/follow`, params)
}

/**
 * 获取商机跟进记录列表 - GET /lead/{id}/follows
 */
export function getLeadFollowList(leadId) {
  return get(`/lead/${leadId}/follows`)
}

// ============================================================
// 商机跟进记录 API (LeadFollowUpController - /lead/followup)
// ============================================================

/**
 * 查询商机跟进记录列表 - GET /lead/followup/list/{leadId}
 */
export function getFollowUpList(leadId) {
  return get(`/lead/followup/list/${leadId}`)
}

/**
 * 获取跟进详情 - GET /lead/followup/{id}
 */
export function getFollowUpDetail(id) {
  return get(`/lead/followup/${id}`)
}

// ============================================================
// 商机任务 API (LeadTaskController - /lead-tasks)
// ============================================================

/**
 * 获取任务列表 - GET /lead-tasks/list
 */
export function getTaskList(params) {
  return get('/lead-tasks/list', params)
}

/**
 * 获取任务详情 - GET /lead-tasks/{id}
 */
export function getTaskDetail(taskId) {
  return get(`/lead-tasks/${taskId}`)
}

/**
 * 创建截客任务 - POST /lead-tasks/create-intercept
 */
export function createInterceptTask(params) {
  return post('/lead-tasks/create-intercept', params)
}

/**
 * 创建获客任务 - POST /lead-tasks/create-acquire
 */
export function createProspectTask(params) {
  return post('/lead-tasks/create-acquire', params)
}

/**
 * 启动任务 - POST /lead-tasks/{id}/start
 */
export function startTask(taskId) {
  return post(`/lead-tasks/${taskId}/start`)
}

/**
 * 暂停任务 - POST /lead-tasks/{id}/pause
 */
export function pauseTask(taskId) {
  return post(`/lead-tasks/${taskId}/pause`)
}

/**
 * 恢复任务 - POST /lead-tasks/{id}/resume
 */
export function resumeTask(taskId) {
  return post(`/lead-tasks/${taskId}/resume`)
}

/**
 * 停止任务 - POST /lead-tasks/{id}/stop
 */
export function stopTask(taskId) {
  return post(`/lead-tasks/${taskId}/stop`)
}

/**
 * 删除任务 - DELETE /lead-tasks/{id}
 */
export function deleteTask(taskId) {
  return del(`/lead-tasks/${taskId}`)
}

/**
 * 控制任务（统一入口）
 */
export function controlTask(taskId, action) {
  return post(`/lead-tasks/${taskId}/${action}`)
}

// ============================================================
// 商机统计 API (LeadController - /lead/stats)
// ============================================================

/**
 * 获取商机统计数据 - GET /lead/stats
 */
export function getLeadStatistics(params) {
  return get('/lead/stats', params)
}

/**
 * 导出商机 - GET /lead/export
 */
export function exportLeads(params) {
  return get('/lead/export', params)
}

/**
 * 批量分配商机 - POST /lead/batch-assign
 */
export function batchAssignLeads(params) {
  return post('/lead/batch-assign', params)
}

/**
 * 下载商机导入模板 - GET /lead/template
 */
export function downloadLeadTemplate() {
  return get('/lead/template')
}
