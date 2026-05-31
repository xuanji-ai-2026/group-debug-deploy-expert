/**
 * @fileoverview 用户设置 API 模块
 * @description 封装用户个人设置相关的所有 HTTP 请求
 *
 * 后端对应：
 * - UserController (@RequestMapping("/user")) ✅
 * - AuthController (@RequestMapping("/auth")) ✅
 *
 * @author EMP-FE-004 赵云
 */
import { get, post, put, del } from '../utils/request.js'

// ============================================================
// 用户资料 API
// ============================================================

/**
 * 获取当前用户信息
 * GET /user/info
 * @returns {Promise<object>} 用户信息
 */
export function getUserProfile() {
  return get('/user/info')
}

/**
 * 更新用户资料
 * PUT /user/{id}
 * @param {number} userId - 用户ID
 * @param {object} params - 更新参数
 * @param {string} [params.nickname] - 昵称
 * @param {string} [params.email] - 邮箱
 * @param {string} [params.realName] - 真实姓名
 * @param {string} [params.avatar] - 头像 URL
 * @returns {Promise}
 */
export function updateUserProfile(userId, params) {
  return put(`/user/${userId}`, params)
}

/**
 * 上传用户头像
 * POST /system/file/upload（假设有文件上传端点）
 * @param {FormData} formData - 包含头像文件的 FormData
 * @param {Function} onProgress - 上传进度回调
 * @returns {Promise<{ url: string }>}
 */
export function uploadAvatar(formData, onProgress = null) {
  return post('/system/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    },
  })
}

// ============================================================
// 安全设置 API
// ============================================================

/**
 * 修改密码
 * PUT /user/{id}/password
 * @param {number} userId - 用户ID
 * @param {object} params - 修改密码参数
 * @param {string} params.oldPassword - 原密码
 * @param {string} params.newPassword - 新密码
 * @returns {Promise}
 */
export function changePassword(userId, params) {
  return put(`/user/${userId}/password`, params)
}

/**
 * 发送短信验证码
 * POST /auth/send-code
 * @param {string} phone - 手机号
 * @returns {Promise}
 */
export function sendBindCode(phone) {
  return post('/auth/send-code', { phone })
}

/**
 * 发送邮箱验证码
 * POST /auth/send-email-code
 * @param {string} email - 邮箱
 * @returns {Promise}
 */
export function sendEmailCode(email) {
  return post('/auth/send-email-code', { email })
}

/**
 * 绑定手机号
 * PUT /user/{id}
 * @param {number} userId - 用户ID
 * @param {string} phone - 手机号
 * @returns {Promise}
 */
export function bindPhone(userId, phone) {
  return put(`/user/${userId}`, { phone })
}

/**
 * 解绑手机号
 * PUT /user/{id}
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export function unbindPhone(userId) {
  return put(`/user/${userId}`, { phone: '' })
}

/**
 * 绑定邮箱
 * PUT /user/{id}
 * @param {number} userId - 用户ID
 * @param {string} email - 邮箱地址
 * @returns {Promise}
 */
export function bindEmail(userId, email) {
  return put(`/user/${userId}`, { email })
}

/**
 * 解绑邮箱
 * PUT /user/{id}
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export function unbindEmail(userId) {
  return put(`/user/${userId}`, { email: '' })
}

// ============================================================
// 登录设备管理 API（暂用 auth 模块）
// ============================================================

/**
 * 获取登录设备列表
 * 注：后端暂无对应端点，返回空数据
 * @returns {Promise<array>} 设备列表
 */
export function getLoginDevices() {
  return get('/user/list') // 临时映射，后端需补充
}

/**
 * 下线指定设备
 * @param {string} deviceId - 设备ID
 * @returns {Promise}
 */
export function logoutDevice(deviceId) {
  return del(`/user/device/${deviceId}`) // 后端需补充
}

/**
 * 下线所有其他设备
 * @returns {Promise}
 */
export function logoutAllOtherDevices() {
  return del('/user/devices/others') // 后端需补充
}

// ============================================================
// 操作日志 API
// ============================================================

/**
 * 获取操作日志列表
 * GET /system/log/list
 * @param {object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @returns {Promise<{ list: array, pagination: object }>}
 */
export function getOperationLogs(params = {}) {
  return get('/system/log/list', params)
}

// ============================================================
// 账号注销 API
// ============================================================

/**
 * 申请注销账号
 * POST /user/{id}/disable
 * @param {number} userId - 用户ID
 * @param {string} reason - 注销原因
 * @returns {Promise}
 */
export function applyAccountDeletion(userId, reason) {
  return post(`/user/${userId}/disable`, { reason })
}

/**
 * 取消账号注销
 * POST /user/{id}/enable
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export function cancelAccountDeletion(userId) {
  return post(`/user/${userId}/enable`)
}

/**
 * 获取账号注销状态
 * GET /user/{id}
 * @param {number} userId - 用户ID
 * @returns {Promise<{ status: number, scheduledDate: string }>}
 */
export function getAccountDeletionStatus(userId) {
  return get(`/user/${userId}`)
}

/**
 * 发送注销账号验证码
 * POST /auth/send-code
 * @param {string} phone - 手机号
 * @returns {Promise}
 */
export function sendDeletionCode(phone) {
  return post('/auth/send-code', { phone })
}
