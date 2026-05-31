/**
 * tenant.js - 租户管理相关 API
 * 功能：租户列表查询、租户审核、租户详情获取、租户状态管理等接口
 *
 * 后端对应：
 * - TenantController (@RequestMapping("") - 直接映射)
 *   - /tenants - 注册/查询
 *   - /admin/tenants - 管理端操作
 */
import { get, post, put } from '@/utils/request'

// ============================================================
// 租户列表
// ============================================================

/**
 * 获取租户列表 - GET /admin/tenants
 */
export function getTenantList(params) {
  return get('/admin/tenants', params)
}

/**
 * 获取待审核租户列表 - GET /admin/tenants/pending
 */
export function getAuditList(params) {
  return get('/admin/tenants/pending', params)
}

/**
 * 获取审核统计 - GET /admin/tenants/audit-stats
 */
export function getAuditStatistics(params) {
  return get('/admin/tenants/audit-stats', params)
}

/**
 * 获取租户详情 - GET /tenants/{id}
 */
export function getTenantDetail(tenantId) {
  return get(`/tenants/${tenantId}`)
}

/**
 * 获取当前租户信息 - GET /tenants/me
 */
export function getCurrentTenant() {
  return get('/tenants/me')
}

// ============================================================
// 租户审核
// ============================================================

/**
 * 审核租户 - POST /admin/tenants/{id}/audit
 */
export function approveTenant(tenantId, params = {}) {
  return post(`/admin/tenants/${tenantId}/audit`, { ...params, action: 'approve' })
}

/**
 * 审核拒绝租户 - POST /admin/tenants/{id}/audit
 */
export function rejectTenant(tenantId, params) {
  return post(`/admin/tenants/${tenantId}/audit`, { ...params, action: 'reject' })
}

/**
 * 批量审核 - POST /admin/tenants/batch-audit
 */
export function batchAuditTenants(params) {
  return post('/admin/tenants/batch-audit', params)
}

// ============================================================
// 租户状态管理
// ============================================================

/**
 * 变更租户状态 - POST /admin/tenants/{id}/status
 */
export function changeTenantStatus(tenantId, status) {
  return post(`/admin/tenants/${tenantId}/status`, { status })
}

/**
 * 启用租户
 */
export function enableTenant(tenantId) {
  return changeTenantStatus(tenantId, 'active')
}

/**
 * 禁用租户
 */
export function disableTenant(tenantId) {
  return changeTenantStatus(tenantId, 'disabled')
}

/**
 * 更新租户信息 - PUT /tenants/{id}
 */
export function updateTenant(tenantId, params) {
  return put(`/tenants/${tenantId}`, params)
}

/**
 * 注册租户 - POST /tenants
 */
export function registerTenant(params) {
  return post('/tenants', params)
}

export default {
  getTenantList,
  getAuditList,
  getAuditStatistics,
  getTenantDetail,
  getCurrentTenant,
  approveTenant,
  rejectTenant,
  batchAuditTenants,
  changeTenantStatus,
  enableTenant,
  disableTenant,
  updateTenant,
  registerTenant
}
