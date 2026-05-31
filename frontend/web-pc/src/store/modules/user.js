import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  getAccessToken,
  setAccessToken,
  getRefreshToken,
  setRefreshToken,
  getUserInfo,
  setUserInfo,
  clearTokens,
  clearUserInfo,
  isAuthenticated,
  getTenantId,
} from '@/utils/auth.js'
import * as userApi from '@/api/user.js'

export const useUserStore = defineStore('user', {
  state: () => ({
    accessToken: getAccessToken(),
    refreshToken: getRefreshToken(),
    userInfo: getUserInfo(),
    tenantId: getTenantId(),
    isLoggedIn: isAuthenticated(),
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,

    userName: (state) => state.userInfo?.nickname || state.userInfo?.phone || state.userInfo?.email || '',

    userAvatar: (state) => state.userInfo?.avatar || '',

    roleType: (state) => state.userInfo?.roleType || null,

    userId: (state) => state.userInfo?.id || null,

    isSuperAdmin: (state) => state.userInfo?.roleType === 1,

    isTenantAdmin: (state) => state.userInfo?.roleType === 3,

    permissions: (state) => state.userInfo?.permissions || [],

    tenantName: (state) => state.userInfo?.tenantName || '',
  },

  actions: {
    /**
     * 统一登录方法
     * @param {object} loginParams - 登录参数
     * @returns {Promise}
     */
    async login(loginParams) {
      try {
        let response

        // 根据登录类型调用不同API
        switch (loginParams.loginType) {
          case 'email':
            response = await userApi.loginWithEmail({
              email: loginParams.email,
              code: loginParams.verifyCode,
              loginType: 'email_code'
            })
            break
          case 'phone':
            response = await userApi.loginWithSmsCode({
              phone: loginParams.phone,
              code: loginParams.verifyCode,
              loginType: 'sms_code'
            })
            break
          default:
            response = await userApi.login({
              phone: loginParams.phone,
              password: loginParams.password,
              loginType: 'password'
            })
        }

        if (response.code !== 200) {
          throw new Error(response.message || '登录失败')
        }

        const { accessToken: token, refreshToken: newRefreshToken, user, isNewUser } = response.data || {}

        if (isNewUser) {
          // 新用户需要设置密码，返回特殊标识
          return { isNewUser: true, phone: loginParams.phone, email: loginParams.email }
        }

        if (!token) {
          throw new Error('登录失败：未获取到token')
        }

        this.accessToken = token
        this.refreshToken = newRefreshToken
        setAccessToken(token)
        if (newRefreshToken) {
          setRefreshToken(newRefreshToken)
        }

        if (user) {
          this.userInfo = user
          this.tenantId = user?.tenantId
          setUserInfo(user)
        }

        this.isLoggedIn = true
        return { success: true, isNewUser: false }

      } catch (error) {
        throw error
      }
    },

    /**
     * 发送短信验证码
     * @param {object} params - 参数
     * @returns {Promise}
     */
    async sendSmsCode(params) {
      return await userApi.sendSmsCode(params)
    },

    /**
     * 发送邮箱验证码
     * @param {object} params - 参数
     * @returns {Promise}
     */
    async sendEmailCode(params) {
      return await userApi.sendEmailCode(params)
    },

    /**
     * 注册并登录（新用户设置密码后）
     * @param {object} params - 参数
     * @returns {Promise}
     */
    async registerAndLogin(params) {
      try {
        const response = await userApi.registerAndLogin(params)

        if (response.code !== 200) {
          throw new Error(response.message || '注册失败')
        }

        const { accessToken: token, refreshToken: newRefreshToken, user } = response.data || {}

        this.accessToken = token
        this.refreshToken = newRefreshToken
        setAccessToken(token)
        if (newRefreshToken) {
          setRefreshToken(newRefreshToken)
        }

        if (user) {
          this.userInfo = user
          this.tenantId = user?.tenantId
          setUserInfo(user)
        }

        this.isLoggedIn = true
        return { success: true }

      } catch (error) {
        throw error
      }
    },

    /**
     * 用户登出
     */
    async logout() {
      try {
        // 尝试调用后端登出接口
        await userApi.logout()
      } catch (error) {
        // 忽略错误，继续本地登出
        console.warn('[UserStore] Logout API failed:', error)
      } finally {
        this.clearUserState()
      }
    },

    /**
     * 获取当前用户信息
     */
    async fetchUserInfo() {
      try {
        const response = await userApi.getCurrentUser()

        if (response.code === 200 && response.data) {
          const user = response.data
          this.userInfo = user
          this.tenantId = user?.tenantId
          setUserInfo(user)
        }

        return response.data

      } catch (error) {
        throw error
      }
    },

    /**
     * 更新用户信息
     * @param {object} params - 参数
     */
    async updateUserInfo(params) {
      const response = await userApi.updateCurrentUser(params)
      await this.fetchUserInfo()
      ElMessage.success('用户信息更新成功')
      return response
    },

    /**
     * 修改密码
     * @param {object} params - 参数
     */
    async changePassword(params) {
      const response = await userApi.changePassword(params)
      ElMessage.success('密码修改成功，请重新登录')
      await this.logout()
      return response
    },

    /**
     * 刷新Token
     */
    async refreshToken() {
      if (!this.refreshToken) {
        throw new Error('No refresh token available')
      }

      try {
        const response = await userApi.refreshToken(this.refreshToken)

        if (response.code === 200 && response.data) {
          const { accessToken: token, refreshToken: newRefreshToken } = response.data

          this.accessToken = token
          if (newRefreshToken) {
            this.refreshToken = newRefreshToken
            setRefreshToken(newRefreshToken)
          }
          setAccessToken(token)

          return token
        }

        throw new Error('Token刷新失败')

      } catch (error) {
        this.clearUserState()
        throw error
      }
    },

    /**
     * 检查是否有指定权限
     * @param {string} permission - 权限标识
     */
    hasPermission(permission) {
      return this.permissions.includes(permission)
    },

    /**
     * 检查是否有所有指定权限
     * @param {string[]} permissions - 权限标识列表
     */
    hasAllPermissions(permissions) {
      return permissions.every((p) => this.hasPermission(p))
    },

    /**
     * 清除用户状态（登出调用）
     */
    clearUserState() {
      this.accessToken = null
      this.refreshToken = null
      this.userInfo = null
      this.tenantId = null
      this.isLoggedIn = false
      clearTokens()
      clearUserInfo()
    },
  },
})
