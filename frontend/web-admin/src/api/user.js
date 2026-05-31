/**
 * @fileoverview Admin用户管理 API 模块
 * @description 封装Admin端用户管理、权限控制、状态管理等接口
 *
 * 后端对应：
 * - UserController (@RequestMapping("/user")) ✅
 * - AuthController (@RequestMapping("/auth")) ✅
 *
 * @author EMP-FE-002 王强
 * @date 2026-05-20
 */
import { get, post, put } from '@/utils/request'

// ============================================================
// 用户列表与查询（Admin跨租户）
// ============================================================

/**
 * 获取用户列表（Admin可跨租户查询）
 * GET /user/list
 * @param {object} params - 查询参数
 * @param {number} [params.page=0] - 页码（从0开始）
 * @param {number} [params.size=10] - 每页数量
 * @param {string} [params.keyword] - 搜索关键词（手机号/昵称）
 * @param {string} [params.roleType] - 角色类型筛选
 * @param {number} [params.status] - 状态筛选：1正常/0禁用
 * @returns {Promise<{ data: array, total: number }>}
 */
export function getUserList(params = {}) {
  return get('/user/list', params)
}

/**
 * 获取用户详情
 * GET /user/{id}
 * @param {number} userId - 用户ID
 * @returns {Promise<object>}
 */
export function getUserDetail(userId) {
  return get(`/user/${userId}`)
}

/**
 * 获取所有用户
 * GET /user/all
 * @returns {Promise<{ data: array, total: number }>}
 */
export function getAllUsers() {
  return get('/user/all')
}

/**
 * 批量查询用户信息
 * POST /user/batch （后端需补充）
 * @param {number[]} userIds - 用户ID列表
 * @returns {Promise<array>}
 */
export function batchGetUsers(userIds) {
  return get('/user/all')  // 临时使用 /user/all，后端需补充 /user/batch
}

// ============================================================
// 用户状态管理（Admin特权）
// ============================================================

/**
 * 启用用户
 * POST /user/{id}/enable
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export function enableUser(userId) {
  return post(`/user/${userId}/enable`)
}

/**
 * 禁用用户
 * POST /user/{id}/disable
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export function disableUser(userId) {
  return post(`/user/${userId}/disable`)
}

/**
 * 启用/禁用用户（统一入口）
 * @param {number} userId - 用户ID
 * @param {boolean} enabled - 是否启用
 * @param {string} [reason] - 原因
 * @returns {Promise}
 */
export function toggleUserStatus(userId, enabled, reason = '') {
  if (enabled) {
    return post(`/user/${userId}/enable`)
  } else {
    return post(`/user/${userId}/disable`)
  }
}

/**
 * 更新用户信息
 * PUT /user/{id}
 * @param {number} userId - 用户ID
 * @param {object} params - 更新参数
 * @returns {Promise}
 */
export function updateUser(userId, params) {
  return put(`/user/${userId}`, params)
}

/**
 * 删除用户
 * DELETE /user/{id}
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export function deleteUser(userId) {
  return put(`/user/${userId}`, { status: 0 })  // 临时软删除
}

/**
 * 重置用户密码（后端需补充 /user/{userId}/reset-password）
 * PUT /user/{id}/password
 * @param {number} userId - 用户ID
 * @param {string} newPassword - 新密码
 * @returns {Promise<{ tempPassword: string }>}
 */
export function resetUserPassword(userId, newPassword) {
  return put(`/user/${userId}/password`, { oldPassword: '', newPassword })
}

/**
 * 锁定/解锁用户（后端需补充）
 * @param {number} userId - 用户ID
 * @param {boolean} locked - 是否锁定
 * @param {string} [reason] - 原因
 * @param {number} [hours=24] - 锁定时长（小时）
 * @returns {Promise}
 */
export function toggleUserLock(userId, locked, reason = '', hours = 24) {
  // 后端暂无对应端点，映射到 disable/enable
  if (locked) {
    return post(`/user/${userId}/disable`)
  } else {
    return post(`/user/${userId}/enable`)
  }
}

/**
 * 强制用户下线（后端需补充）
 * @param {number} userId - 用户ID
 * @returns {Promise<{ message: string }>}
 */
export function forceUserOffline(userId) {
  // 后端暂无对应端点，使用 auth 登出
  return post('/auth/logout')
}

// ============================================================
// 用户权限与角色管理
// ============================================================

/**
 * 获取用户角色列表（后端需补充）
 * @param {number} userId - 用户ID
 * @returns {Promise<array>}
 */
export function getUserRoles(userId) {
  return get(`/user/${userId}`)  // 临时返回用户信息，后端需补充
}

/**
 * 分配角色给用户（后端需补充）
 * @param {number} userId - 用户ID
 * @param {number[]} roleIds - 角色ID列表
 * @returns {Promise}
 */
export function assignUserRoles(userId, roleIds) {
  return put(`/user/${userId}`, { roleType: roleIds.join(',') })  // 临时映射
}

/**
 * 获取用户权限列表（后端需补充）
 * @param {number} userId - 用户ID
 * @returns {Promise<array>}
 */
export function getUserPermissions(userId) {
  return get(`/user/${userId}`)  // 临时返回用户信息
}

/**
 * 获取所有可用角色（后端需补充）
 * @returns {Promise<array>}
 */
export function getAllRoles() {
  return get('/user/all')  // 临时使用 /user/all
}

// ============================================================
// 用户统计与分析（Admin视角）
// ============================================================

/**
 * 获取用户统计数据
 * GET /user/count
 * @param {object} params - 统计参数
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @returns {Promise<{ total: number, active: number, disabled: number }>}
 */
export function getUserStats(params = {}) {
  return get('/user/count', params)
}

/**
 * 获取用户活跃度排行（后端需补充）
 * @param {object} params - 查询参数
 * @param {number} [params.limit=20] - 返回数量
 * @returns {Promise<array>}
 */
export function getActiveUserRanking(params = {}) {
  return get('/user/list', { ...params, status: 1 })  // 临时使用 /user/list
}

/**
 * 获取新注册用户趋势（后端需补充）
 * @param {object} params - 查询参数
 * @param {string} params.startDate - 开始日期
 * @param {string} params.endDate - 结束日期
 * @param {string} [params.granularity='day'] - 粒度：day/week/month
 * @returns {Promise<array>}
 */
export function getNewUserTrend(params) {
  return get('/user/count')  // 临时使用 /user/count
}

/**
 * 获取异常用户列表（后端需补充）
 * @param {object} params - 查询参数
 * @param {number} [params.limit=50] - 返回数量
 * @returns {Promise<array>}
 */
export function getAbnormalUsers(params = {}) {
  return get('/user/list', { ...params, status: 0 })  // 临时使用 /user/list
}

// ============================================================
// 用户认证管理
// ============================================================

/**
 * 获取用户登录日志（后端需补充）
 * @param {number} userId - 用户ID
 * @param {object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @returns {Promise<{ records: array, total: number }>}
 */
export function getLoginLogs(userId, params = {}) {
  return get('/system/log/list', { ...params, userId })  // 临时映射到系统日志
}

/**
 * 使某用户的Token失效（后端需补充）
 * @param {number} userId - 用户ID
 * @returns {Promise<{ message: string }>}
 */
export function invalidateUserTokens(userId) {
  return post('/auth/logout')  // 临时使用 auth logout
}
