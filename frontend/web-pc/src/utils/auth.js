/**
 * @fileoverview 认证工具模块
 * @description 对接真实后端 bx-user 服务 (/auth/* 接口)
 * @version 2.0.0 - 完全重写，对接真实API
 */

const TOKEN_KEY = import.meta.env.VITE_TOKEN_KEY || 'bx_access_token'
const REFRESH_TOKEN_KEY = import.meta.env.VITE_REFRESH_TOKEN_KEY || 'bx_refresh_token'
const USER_INFO_KEY = 'bx_user_info'

// ============================================================
// Token 管理
// ============================================================

/**
 * 获取访问令牌
 * @returns {string|null}
 */
export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 获取刷新令牌
 * @returns {string|null}
 */
export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

/**
 * 设置令牌
 * @param {string} token - 访问令牌
 */
export function setAccessToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  }
}

/**
 * 设置刷新令牌
 * @param {string} refreshToken - 刷新令牌
 */
export function setRefreshToken(refreshToken) {
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  }
}

/**
 * 清除所有令牌
 */
export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

// ============================================================
// 用户信息管理
// ============================================================

/**
 * 获取用户信息
 * @returns {object|null}
 */
export function getUserInfo() {
  try {
    const userInfo = localStorage.getItem(USER_INFO_KEY)
    return userInfo ? JSON.parse(userInfo) : null
  } catch (e) {
    console.error('[Auth] Parse user info failed:', e)
    return null
  }
}

/**
 * 设置用户信息
 * @param {object} userInfo - 用户信息对象
 */
export function setUserInfo(userInfo) {
  if (userInfo) {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  }
}

// ============================================================
// 认证状态判断
// ============================================================

/**
 * 检查是否已登录（是否有有效Token）
 * @returns {boolean}
 */
export function isAuthenticated() {
  const token = getAccessToken()
  return !!token && token.length > 0
}

/**
 * 登出操作
 * 清除本地存储并跳转到登录页
 */
export function logout() {
  clearTokens()
  
  if (typeof window !== 'undefined') {
    window.location.href = '/login'
  }
}

// ============================================================
// 租户信息管理
// ============================================================

/**
 * 获取租户ID
 * @returns {string|null}
 */
export function getTenantId() {
  try {
    const userInfo = getUserInfo()
    return userInfo?.tenantId || null
  } catch (e) {
    return null
  }
}

/**
 * 清除用户信息
 */
export function clearUserInfo() {
  localStorage.removeItem(USER_INFO_KEY)
}

export default {
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
  clearTokens,
  getUserInfo,
  setUserInfo,
  isAuthenticated,
  logout,
  getTenantId,
  clearUserInfo,
}
