import { defineStore } from 'pinia'
import { getAdminInfo, setAdminInfo, setToken, setRefreshToken, removeToken, removeRefreshToken, removeAdminInfo } from '@/utils/adminAuth'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    adminInfo: getAdminInfo() || {
      id: '',
      username: '',
      role: '',
      avatar: '',
      phone: ''
    },

    token: localStorage.getItem('admin_token') || '',
    refreshToken: localStorage.getItem('admin_refresh_token') || '',

    sidebarCollapsed: false,

    breadcrumbs: [],

    isFullScreen: false
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,

    role: (state) => state.adminInfo?.role || '',

    isSuperAdmin: (state) => state.adminInfo?.role === 'super_admin',

    nickname: (state) => state.adminInfo?.nickname || state.adminInfo?.username || '管理员',

    avatar: (state) => state.adminInfo?.avatar || ''
  },

  actions: {
    setAdminInfo(info) {
      this.adminInfo = info
      setAdminInfo(info)
    },

    setToken(token) {
      this.token = token
      setToken(token)
    },

    setRefreshToken(token) {
      this.refreshToken = token
      setRefreshToken(token)
    },

    login({ token, refreshToken, adminInfo }) {
      this.token = token
      this.refreshToken = refreshToken
      this.adminInfo = adminInfo
      setToken(token)
      if (refreshToken) {
        setRefreshToken(refreshToken)
      }
      setAdminInfo(adminInfo)
    },

    logout() {
      this.token = ''
      this.refreshToken = ''
      this.adminInfo = {}
      removeToken()
      removeRefreshToken()
      removeAdminInfo()
    },

    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },

    setSidebarCollapsed(collapsed) {
      this.sidebarCollapsed = collapsed
    },

    setBreadcrumbs(list) {
      this.breadcrumbs = list
    },

    setFullScreen(fullScreen) {
      this.isFullScreen = fullScreen
    }
  }
})
