/**
 * system.js - 系统管理相关 API
 * 功能：系统设置获取/修改、平台参数配置、日志查询等接口
 *
 * 后端对应：
 * - ConfigController (@RequestMapping("/admin/config"))
 * - LogController (@RequestMapping("/admin/logs"))
 */
import { get, post, put, del } from '@/utils/request'

// ============================================================
// 系统设置 (ConfigController - /admin/config)
// ============================================================

/**
 * 获取系统配置列表（分页）- GET /admin/config
 */
export function getSystemSettings() {
  return get('/admin/config')
}

/**
 * 创建系统配置 - POST /admin/config
 */
export function createSystemSetting(data) {
  return post('/admin/config', data)
}

/**
 * 获取配置详情 - GET /admin/config/{id}
 */
export function getSettingById(id) {
  return get(`/admin/config/${id}`)
}

/**
 * 获取单个配置项（按key）- GET /admin/config/key/{configKey}
 */
export function getSettingByKey(key) {
  return get(`/admin/config/key/${key}`)
}

/**
 * 获取分组配置 - GET /admin/config/group/{groupCode}
 */
export function getConfigsByGroup(groupCode) {
  return get(`/admin/config/group/${groupCode}`)
}

/**
 * 更新系统配置 - PUT /admin/config/{id}
 */
export function updateSystemSettings(data) {
  return put(`/admin/config/${data.id}`, data)
}

/**
 * 更新单个配置项
 */
export function updateSetting(key, value) {
  return put('/admin/config/key', { key, value })
}

/**
 * 刷新配置缓存 - POST /admin/config/refresh-cache
 */
export function refreshConfigCache() {
  return post('/admin/config/refresh-cache')
}

/**
 * 删除系统配置 - DELETE /admin/config/{id}
 */
export function deleteSystemSetting(id) {
  return del(`/admin/config/${id}`)
}

// ============================================================
// 操作日志 (LogController - /admin/logs)
// ============================================================

/**
 * 获取最近日志 - GET /admin/logs/recent
 */
export function getOperationLogList(params) {
  return get('/admin/logs/recent', params)
}

/**
 * 获取用户日志 - GET /admin/logs/user/{userId}
 */
export function getLogsByUser(userId) {
  return get(`/admin/logs/user/${userId}`)
}

/**
 * 按时间范围获取日志 - GET /admin/logs/time-range
 */
export function getLogsByTimeRange(params) {
  return get('/admin/logs/time-range', params)
}

/**
 * 获取日志详情 - GET /admin/logs/{id}
 */
export function getLogDetail(id) {
  return get(`/admin/logs/${id}`)
}

/**
 * 获取日志统计 - GET /admin/logs/statistics
 */
export function getLogStatistics() {
  return get('/admin/logs/statistics')
}

/**
 * 获取登录日志列表（复用操作日志）
 */
export function getLoginLogList(params) {
  return get('/admin/logs/recent', { ...params, type: 'LOGIN' })
}

/**
 * 清理旧日志 - DELETE /admin/logs/clean
 */
export function cleanOldLogs(params) {
  return del('/admin/logs/clean', params)
}

export default {
  getSystemSettings,
  createSystemSetting,
  getSettingById,
  getSettingByKey,
  getConfigsByGroup,
  updateSystemSettings,
  updateSetting,
  refreshConfigCache,
  deleteSystemSetting,
  getOperationLogList,
  getLogsByUser,
  getLogsByTimeRange,
  getLogDetail,
  getLogStatistics,
  getLoginLogList,
  cleanOldLogs
}
