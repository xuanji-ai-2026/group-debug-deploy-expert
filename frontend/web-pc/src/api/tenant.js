/**
 * @fileoverview 租户管理 API 模块
 * @description 封装租户相关的所有 HTTP 请求
 *
 * 后端对应：
 * - TenantController (@RequestMapping("")) ✅
 * - AccountController (@RequestMapping("/account")) ✅
 *
 * @author EMP-FE-001 张婷
 */
import { get, post, put } from '../utils/request.js'

// ============================================================
// 租户信息 API
// ============================================================

/**
 * 获取当前租户信息
 * GET /tenants/me
 * @returns {Promise<object>} 租户信息
 */
export function getCurrentTenant() {
  return get('/tenants/me')
}

/**
 * 更新当前租户信息
 * PUT /tenants/{id}
 * @param {number} tenantId - 租户ID
 * @param {object} params - 更新参数
 * @returns {Promise}
 */
export function updateCurrentTenant(tenantId, params) {
  return put(`/tenants/${tenantId}`, params)
}

/**
 * 获取租户套餐信息（后端需补充）
 * @returns {Promise<object>}
 */
export function getTenantPackage() {
  return get('/tenants/me')  // 临时，后端需补充
}

// ============================================================
// 社交平台 API
// ============================================================

/**
 * 获取社交平台列表
 * GET /account/platforms
 * @returns {Promise<array>} 平台列表
 */
export function getSocialPlatforms() {
  return get('/account/platforms')
}

// ============================================================
// 社交账号 API
// ============================================================

/**
 * 获取社交账号列表
 * GET /account/page
 * @param {object} params - 查询参数
 * @param {number} [params.pageNum=1] - 页码
 * @param {number} [params.pageSize=10] - 每页数量
 * @param {string} [params.platformCode] - 平台代码
 * @param {number} [params.groupId] - 分组ID
 * @param {number} [params.status] - 账号状态
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getSocialAccounts(params) {
  return get('/account/page', {
    pageNum: params.page || 1,
    pageSize: params.size || 10,
    platformCode: params.platformId ? `PLATFORM_${params.platformId}` : undefined,
    groupId: params.groupId,
    status: params.status,
  })
}

/**
 * 获取社交账号详情
 * GET /account/{id}
 * @param {number} accountId - 账号ID
 * @returns {Promise<object>}
 */
export function getSocialAccountDetail(accountId) {
  return get(`/account/${accountId}`)
}

/**
 * 创建/更新社交账号
 * POST /account/save
 * @param {object} params - 绑定参数
 * @returns {Promise<object>}
 */
export function bindSocialAccount(params) {
  return post('/account/save', params)
}

/**
 * 解绑社交账号
 * POST /account/{id}/unbind
 * @param {number} accountId - 账号ID
 * @returns {Promise}
 */
export function unbindSocialAccount(accountId) {
  return post(`/account/${accountId}/unbind`)
}

/**
 * 更新账号分组
 * @param {number} accountId - 账号ID
 * @param {number} groupId - 分组ID
 * @returns {Promise}
 */
export function updateAccountGroup(accountId, groupId) {
  return post('/account/save', { id: accountId, groupId })  // 临时映射
}

// ============================================================
// 账号分组 API
// ============================================================

/**
 * 获取账号分组列表
 * GET /account/groups
 * @returns {Promise<array>}
 */
export function getAccountGroups() {
  return get('/account/groups')
}

/**
 * 创建账号分组（后端需补充）
 * @param {object} params - 分组参数
 * @returns {Promise<object>}
 */
export function createAccountGroup(params) {
  return get('/account/groups')  // 临时
}

/**
 * 更新账号分组（后端需补充）
 * @param {number} groupId - 分组ID
 * @param {object} params - 更新参数
 * @returns {Promise}
 */
export function updateAccountGroupInfo(groupId, params) {
  return get('/account/groups')  // 临时
}

/**
 * 删除账号分组（后端需补充）
 * @param {number} groupId - 分组ID
 * @returns {Promise}
 */
export function deleteAccountGroup(groupId) {
  return get('/account/groups')  // 临时
}

// ============================================================
// 管理端租户 API
// ============================================================

/**
 * 获取租户列表（管理端）
 * GET /admin/tenants
 * @param {object} params - 查询参数
 * @returns {Promise<{ list: array, pagination: object }>}
 */
export function getTenantList(params) {
  return get('/admin/tenants', {
    page: params.page || 1,
    size: params.size || 10,
    keyword: params.keyword,
    status: params.status,
  })
}

/**
 * 获取租户详情（管理端）
 * GET /tenants/{id}
 * @param {number} tenantId - 租户ID
 * @returns {Promise<object>}
 */
export function getTenantDetail(tenantId) {
  return get(`/tenants/${tenantId}`)
}

/**
 * 审核租户（管理端）
 * POST /admin/tenants/{id}/audit
 * @param {number} tenantId - 租户ID
 * @param {object} params - 审核参数
 * @param {boolean} params.approved - 是否通过
 * @param {string} [params.reason] - 审核理由
 * @returns {Promise}
 */
export function auditTenant(tenantId, params) {
  return post(`/admin/tenants/${tenantId}/audit`, params)
}

/**
 * 变更租户状态（管理端）
 * POST /admin/tenants/{id}/status
 * @param {number} tenantId - 租户ID
 * @param {boolean} disabled - 是否禁用
 * @returns {Promise}
 */
export function toggleTenantStatus(tenantId, disabled) {
  return post(`/admin/tenants/${tenantId}/status`, {
    targetStatus: disabled ? 'DISABLED' : 'ACTIVE',
  })
}

/**
 * 获取待审核租户列表
 * GET /admin/tenants/pending
 * @returns {Promise<object>}
 */
export function getTenantStatistics() {
  return get('/admin/tenants/pending')
}

// ============================================================
// 公共 API（后端需补充）
// ============================================================

/**
 * 获取行业列表
 * @returns {Promise<array>}
 */
export function getIndustries() {
  return get('/tenants/me')  // 临时
}

/**
 * 获取地区列表
 * @param {string} [parentCode] - 父级地区编码
 * @returns {Promise<array>}
 */
export function getRegions(parentCode) {
  return get('/tenants/me')  // 临时
}

/**
 * 获取上传凭证
 * @returns {Promise<{ uploadUrl: string, fileKey: string, expiresIn: number }>}
 */
export function getUploadToken() {
  return get('/tenants/me')  // 临时
}
