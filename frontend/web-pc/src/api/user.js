/**
 * @fileoverview 用户管理 API 模块
 * @description 封装用户相关的所有 HTTP 请求
 *
 * 后端对应：
 * - AuthController (@RequestMapping("/auth"))
 * - UserController (@RequestMapping("/user"))
 */
import { get, post, put, del } from '../utils/request.js'

// ============================================================
// 认证相关 API (AuthController)
// ============================================================

/**
 * 密码登录
 */
export function login(params) {
  return post('/auth/login', params)
}

/**
 * 手机号验证码登录
 */
export function loginWithSmsCode(params) {
  return post('/auth/login-sms', params);
}

/**
 * 邮箱验证码登录
 */
export function loginWithEmail(params) {
  return post('/auth/login-email', params);
}

/**
 * 注册并登录
 */
export function registerAndLogin(params) {
  return post('/auth/register-login', params);
}

/**
 * 用户注册
 */
export function register(params) {
  return post('/auth/register', params);
}

/**
 * 刷新访问令牌
 */
export function refreshToken(refreshToken) {
  return post('/auth/refresh', { refreshToken });
}

/**
 * 发送短信验证码
 */
export function sendSmsCode(params) {
  return post('/auth/send-code', params);
}

/**
 * 发送邮箱验证码
 */
export function sendEmailCode(params) {
  return post('/auth/send-email-code', params);
}

/**
 * 用户退出登录
 */
export function logout() {
  const tokenKey = import.meta.env.VITE_TOKEN_KEY || 'bx_access_token'
  const refreshTokenKey = import.meta.env.VITE_REFRESH_TOKEN_KEY || 'bx_refresh_token'
  const userInfoKey = 'bx_user_info'

  localStorage.removeItem(tokenKey)
  localStorage.removeItem(refreshTokenKey)
  localStorage.removeItem(userInfoKey)
}

// ============================================================
// 用户信息 API (UserController - /user)
// ============================================================

/**
 * 获取当前用户信息 - GET /user/info
 */
export function getCurrentUser() {
  return get('/user/info');
}

/**
 * 获取用户资料 - GET /user/profile
 */
export function getCurrentUserProfile() {
  return get('/user/profile');
}

/**
 * 修改密码（前端本地实现，后端无单独端点）
 */
export function changePassword(params) {
  return put(`/user/${params.userId}/password`, params);
}

// ============================================================
// 用户管理 API（管理端 - UserController /user）
// ============================================================

/**
 * 获取用户列表 - GET /user/list
 */
export function getUserList(params) {
  return get('/user/list', params);
}

/**
 * 获取用户详情 - GET /user/{id}
 */
export function getUserDetail(userId) {
  return get(`/user/${userId}`);
}

/**
 * 更新用户信息 - PUT /user/{id}
 */
export function updateUser(userId, params) {
  return put(`/user/${userId}`, params);
}

/**
 * 删除用户 - DELETE /user/{id}
 */
export function deleteUser(userId) {
  return del(`/user/${userId}`);
}

/**
 * 禁用用户 - POST /user/{id}/disable
 */
export function disableUser(userId) {
  return post(`/user/${userId}/disable`);
}

/**
 * 启用用户 - POST /user/{id}/enable
 */
export function enableUser(userId) {
  return post(`/user/${userId}/enable`);
}

/**
 * 切换用户状态（统一入口）
 */
export function toggleUserStatus(userId, enabled) {
  return enabled ? enableUser(userId) : disableUser(userId);
}

/**
 * 获取用户统计 - GET /user/count
 */
export function getUserCount() {
  return get('/user/count');
}

/**
 * 获取用户余额 - GET /user/balance
 */
export function getUserBalance() {
  return get('/user/balance');
}

/**
 * 获取所有用户 - GET /user/all
 */
export function getAllUsers() {
  return get('/user/all');
}

/**
 * 更新当前用户（从 token 中获取 userId）- PUT /user/{id}
 */
export function updateCurrentUser(params) {
  return put(`/user/${params.id}`, params);
}

/**
 * 创建用户 - POST /user（管理端，委托 AuthController 注册）
 */
export function createUser(params) {
  return post('/auth/register', params);
}
