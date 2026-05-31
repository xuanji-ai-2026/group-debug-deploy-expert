/**
 * @fileoverview 认证状态组合式函数
 * @description 统一认证逻辑，使用request工具调用API
 * @version 2.1.0 - API路径统一优化
 */
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

import {
  getAccessToken,
  setAccessToken,
  setRefreshToken,
  setUserInfo,
  clearTokens,
  isAuthenticated,
  logout as authLogout,
} from '@/utils/auth.js'

import {
  login,
  loginWithSmsCode,
  loginWithEmail,
  registerAndLogin,
  sendSmsCode,
  sendEmailCode,
  getCurrentUser,
  logout as apiLogout
} from '@/api/user.js'

/**
 * 认证状态组合式函数
 * @returns {object} 认证相关状态和方法
 */
export function useAuth() {
  const router = useRouter()
  const route = useRoute()

  // 本地加载状态
  const loading = ref(false)

  // ============================================================
  // 计算属性（响应式）
  // ============================================================

  /** 是否已登录 */
  const isLoggedIn = computed(() => isAuthenticated())

  /** 访问令牌 */
  const accessToken = computed(() => getAccessToken())

  /** 用户信息 */
  const userInfo = computed(() => {
    try {
      const info = localStorage.getItem('bx_user_info')
      return info ? JSON.parse(info) : null
    } catch (e) {
      return null
    }
  })

  /** 用户名 */
  const userName = computed(() => userInfo.value?.nickname || userInfo.value?.phone || userInfo.value?.email || '')

  /** 用户头像 */
  const userAvatar = computed(() => userInfo.value?.avatar || '')

  /** 用户角色类型 */
  const roleType = computed(() => userInfo.value?.roleType || null)

  /** 用户权限列表 */
  const permissions = computed(() => userInfo.value?.permissions || [])

  /** 是否超级管理员（数值1） */
  const isSuperAdmin = computed(() => roleType.value === 1)

  /** 是否租户管理员（数值3） */
  const isTenantAdmin = computed(() => roleType.value === 3)

  /** 是否普通用户 */
  const isNormalUser = computed(() => roleType.value === 2 || !isSuperAdmin.value && !isTenantAdmin.value)

  // ============================================================
  // 核心方法：统一API调用
  // ============================================================

  /**
   * 用户登录（统一入口）
   * @param {object} loginParams - 登录参数
   * @param {string} [loginParams.phone] - 手机号
   * @param {string} [loginParams.password] - 密码
   * @param {string} [loginParams.email] - 邮箱
   * @param {string} [loginParams.verifyCode] - 验证码
   * @param {string} [loginParams.loginType] - 登录类型: password/phone/email
   * @returns {Promise<object>} 登录结果
   */
  async function loginHandler(loginParams) {
    loading.value = true

    try {
      let response

      // 根据登录类型调用不同的API
      switch (loginParams.loginType) {
        case 'email':
          // 邮箱验证码登录
          response = await loginWithEmail({
            email: loginParams.email,
            code: loginParams.verifyCode,
            loginType: 'email_code'
          })
          break

        case 'phone':
          // 手机号验证码登录
          response = await loginWithSmsCode({
            phone: loginParams.phone,
            code: loginParams.verifyCode,
            loginType: 'sms_code'
          })
          break

        default:
          // 密码登录
          response = await login({
            phone: loginParams.phone,
            password: loginParams.password,
            loginType: 'password'
          })
      }

      const data = response

      // 检查是否是新用户需要设置密码
      if ((data.code === 0 || data.code === 200) && data.data?.isNewUser) {
        ElMessage.warning('新用户检测：请设置密码完成注册')
        throw { message: 'NEW_USER_NEED_PASSWORD', code: 10001 }
      }

      // 登录成功 - 存储Token和用户信息
      if ((data.code === 0 || data.code === 200) && data.data) {
        const responseData = data.data
        const token = responseData.token
        const refreshToken = responseData.refresh_token
        const user = responseData.user

        setAccessToken(token)
        if (refreshToken) {
          setRefreshToken(refreshToken)
        }

        if (user) {
          setUserInfo(user)
        }

        ElMessage.success('登录成功！欢迎回来，' + (user?.nickname || user?.phone || user?.email))
        return { success: true, data: responseData }
      } else {
        throw new Error(data.message || '登录失败')
      }

    } catch (error) {
      console.error('[Auth] 登录失败:', error)

      if (error.code === 10001 || error.message === 'NEW_USER_NEED_PASSWORD') {
        throw error // 抛出新用户异常，让前端处理
      }

      const errorMessage = error.message || '登录失败，请稍后重试'
      ElMessage.error(errorMessage)
      return { success: false, error: errorMessage }

    } finally {
      loading.value = false
    }
  }

  /**
   * 发送短信验证码
   * @param {object} params - 参数
   * @param {string} params.phone - 手机号
   * @returns {Promise<boolean>}
   */
  async function sendSmsCodeHandler(params) {
    try {
      await sendSmsCode(params)
      ElMessage.success('验证码已发送')
      return true
    } catch (error) {
      console.error('[Auth] 发送短信验证码失败:', error)
      ElMessage.error(error.message || '发送失败')
      return false
    }
  }

  /**
   * 发送邮箱验证码
   * @param {object} params - 参数
   * @param {string} params.email - 邮箱地址
   * @returns {Promise<boolean>}
   */
  async function sendEmailCodeHandler(params) {
    try {
      await sendEmailCode(params)
      ElMessage.success('邮箱验证码已发送')
      return true
    } catch (error) {
      console.error('[Auth] 发送邮箱验证码失败:', error)
      ElMessage.error(error.message || '发送失败')
      return false
    }
  }

  /**
   * 注册并登录（新用户设置密码后）
   * @param {object} params - 参数
   * @param {string} params.phone - 手机号
   * @param {string} params.code - 验证码
   * @param {string} params.password - 密码
   * @returns {Promise<boolean>}
   */
  async function registerAndLoginHandler(params) {
    loading.value = true

    try {
      const response = await registerAndLogin(params)

      if ((response.code === 0 || response.code === 200) && response.data) {
        const token = response.data.token
        const refreshToken = response.data.refresh_token
        const user = response.data.user

        setAccessToken(token)
        if (refreshToken) {
          setRefreshToken(refreshToken)
        }
        if (user) {
          setUserInfo(user)
        }

        ElMessage.success('注册并登录成功！')
        return true
      } else {
        throw new Error(response.message || '注册失败')
      }

    } catch (error) {
      console.error('[Auth] 注册失败:', error)
      ElMessage.error(error.message || '注册失败')
      return false

    } finally {
      loading.value = false
    }
  }

  /**
   * 获取当前用户信息
   * @returns {Promise<object|null>}
   */
  async function fetchUserInfo() {
    try {
      const token = getAccessToken()
      if (!token) return null

      const response = await getCurrentUser()

      if ((response.code === 0 || response.code === 200) && response.data) {
        setUserInfo(response.data)
        return response.data
      }

      return null

    } catch (error) {
      console.error('[Auth] 获取用户信息失败:', error)
      return null
    }
  }

  /**
   * 用户退出登录
   * @param {boolean} [showMessage=true] - 是否显示提示
   * @returns {Promise}
   */
  async function logout(showMessage = true) {
    loading.value = true

    try {
      // 尝试调用后端登出接口
      try {
        await apiLogout()
      } catch (e) {
        // 即使后端登出失败也要清除本地数据
        console.warn('[Auth] 后端登出接口调用失败:', e)
      }

      // 清除本地存储
      clearTokens()

      if (showMessage) {
        ElMessage.success('已退出登录')
      }

      // 跳转到登录页
      const redirect = route.fullPath
      await router.push(redirect && !redirect.startsWith('/login') ? `/login?redirect=${encodeURIComponent(redirect)}` : '/login')

    } catch (error) {
      console.error('[Auth] 登出失败:', error)
      clearTokens()
      router.push('/login')

    } finally {
      loading.value = false
    }
  }

  /**
   * 检查是否有指定权限
   * @param {string} permission - 权限编码
   * @returns {boolean}
   */
  function hasPermission(permission) {
    return permissions.value.includes(permission)
  }

  /**
   * 检查是否有所有指定权限
   * @param {string[]} permissionList - 权限编码数组
   * @returns {boolean}
   */
  function hasAllPermissions(permissionList) {
    return permissionList.every((p) => hasPermission(p))
  }

  /**
   * 检查是否有任一指定权限
   * @param {string[]} permissionList - 权限编码数组
   * @returns {boolean}
   */
  function hasAnyPermission(permissionList) {
    return permissionList.some((p) => hasPermission(p))
  }

  /**
   * 验证页面访问权限
   * @param {string[]} allowedRoles - 允许的角色类型数组
   * @returns {boolean}
   */
  function checkAccess(allowedRoles) {
    if (!allowedRoles || allowedRoles.length === 0) {
      return true
    }
    return allowedRoles.includes(roleType.value)
  }

  // ============================================================
  // 返回
  // ============================================================
  return {
    // 状态
    loading,
    isLoggedIn,
    accessToken,
    userInfo,
    userName,
    userAvatar,
    roleType,
    permissions,
    isSuperAdmin,
    isTenantAdmin,
    isNormalUser,

    // 方法（统一API调用）
    login: loginHandler,
    sendSmsCode: sendSmsCodeHandler,
    sendEmailCode: sendEmailCodeHandler,
    registerAndLogin: registerAndLoginHandler,
    fetchUserInfo,
    logout,
    hasPermission,
    hasAllPermissions,
    hasAnyPermission,
    checkAccess,
  }
}

export default useAuth
