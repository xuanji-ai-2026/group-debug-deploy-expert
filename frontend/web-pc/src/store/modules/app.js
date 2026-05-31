/**
 * @fileoverview 应用全局状态管理模块
 * @description 管理应用全局配置、布局、主题、加载状态等
 * @author EMP-FE-001 张婷
 */
import { defineStore } from 'pinia'
import { localCache } from '@/utils/storage.js'

// ============================================================
// 常量定义
// ============================================================

/** 侧边栏折叠状态 Key */
const SIDEBAR_COLLAPSED_KEY = 'bx_sidebar_collapsed'
/** 侧边栏展开菜单 Key */
const SIDEBAR_MENU_KEY = 'bx_sidebar_menu'
/** 主题 Key */
const THEME_KEY = 'bx_theme'

/**
 * 主题枚举
 */
export const ThemeMode = {
  LIGHT: 'light',
  DARK: 'dark',
}

/**
 * 侧边栏宽度
 */
export const SidebarWidth = {
  EXPANDED: 220,
  COLLAPSED: 64,
}

// ============================================================
// Store 定义
// ============================================================

/**
 * 应用 Store
 */
export const useAppStore = defineStore('app', {
  // ============================================================
  // 状态
  // ============================================================
  state: () => ({
    // ============================================================
    // 布局相关
    // ============================================================

    // 侧边栏是否折叠
    sidebarCollapsed: localCache.get(SIDEBAR_COLLAPSED_KEY, false),
    // 侧边栏当前宽度
    sidebarWidth: SidebarWidth.EXPANDED,
    // 移动端侧边栏是否打开
    mobileSidebarOpen: false,

    // ============================================================
    // 界面配置
    // ============================================================

    // 主题模式
    theme: localCache.get(THEME_KEY, ThemeMode.LIGHT),
    // 固定顶部导航
    fixedHeader: true,
    // 固定侧边栏
    fixedSidebar: true,
    // 显示面包屑
    showBreadcrumb: true,
    // 显示标签栏（多页签）
    showTagsView: false,

    // ============================================================
    // 全局状态
    // ============================================================

    // 全局加载状态
    loading: false,
    // 全局加载文案
    loadingText: '加载中...',
    // 设备类型
    device: 'desktop', // desktop / mobile / tablet
    // 窗口尺寸
    windowSize: {
      width: window.innerWidth,
      height: window.innerHeight,
    },

    // ============================================================
    // 菜单数据
    // ============================================================

    // 菜单列表
    menuList: [],
    // 激活的菜单
    activeMenu: '',
    // 面包屑数据
    breadcrumbs: [],

    // ============================================================
    // 租户信息（缓存）
    // ============================================================

    tenantInfo: null,
  }),

  // ============================================================
  // Getters
  // ============================================================
  getters: {
    /**
     * 侧边栏实际宽度
     */
    actualSidebarWidth: (state) =>
      state.sidebarCollapsed ? SidebarWidth.COLLAPSED : SidebarWidth.EXPANDED,

    /**
     * 是否是移动端
     */
    isMobile: (state) => state.device === 'mobile',

    /**
     * 是否是平板
     */
    isTablet: (state) => state.device === 'tablet',

    /**
     * 是否是桌面端
     */
    isDesktop: (state) => state.device === 'desktop',

    /**
     * 是否深色主题
     */
    isDarkTheme: (state) => state.theme === ThemeMode.DARK,

    /**
     * 内容区域样式
     */
    contentStyle: (state) => {
      const styles = {}
      if (state.fixedSidebar) {
        styles.marginLeft = `${state.sidebarCollapsed ? SidebarWidth.COLLAPSED : SidebarWidth.EXPANDED}px`
      }
      return styles
    },
  },

  // ============================================================
  // Actions
  // ============================================================
  actions: {
    // ============================================================
    // 布局操作
    // ============================================================

    /**
     * 切换侧边栏折叠状态
     * @param {boolean} [collapsed] - 是否折叠，不传则切换
     */
    toggleSidebar(collapsed) {
      if (collapsed !== undefined) {
        this.sidebarCollapsed = collapsed
      } else {
        this.sidebarCollapsed = !this.sidebarCollapsed
      }
      localCache.set(SIDEBAR_COLLAPSED_KEY, this.sidebarCollapsed)
    },

    /**
     * 打开移动端侧边栏
     */
    openMobileSidebar() {
      this.mobileSidebarOpen = true
    },

    /**
     * 关闭移动端侧边栏
     */
    closeMobileSidebar() {
      this.mobileSidebarOpen = false
    },

    /**
     * 切换移动端侧边栏
     */
    toggleMobileSidebar() {
      this.mobileSidebarOpen = !this.mobileSidebarOpen
    },

    // ============================================================
    // 主题操作
    // ============================================================

    /**
     * 设置主题
     * @param {string} theme - 主题名称
     */
    setTheme(theme) {
      this.theme = theme
      localCache.set(THEME_KEY, theme)

      // 可以在这里触发 CSS 变量更新
      if (theme === ThemeMode.DARK) {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    },

    /**
     * 切换主题
     */
    toggleTheme() {
      const newTheme = this.theme === ThemeMode.LIGHT ? ThemeMode.DARK : ThemeMode.LIGHT
      this.setTheme(newTheme)
    },

    // ============================================================
    // 设备检测
    // ============================================================

    /**
     * 更新设备类型
     * @param {string} device - 设备类型
     */
    setDevice(device) {
      this.device = device
    },

    /**
     * 更新窗口尺寸
     * @param {object} size - { width, height }
     */
    setWindowSize(size) {
      this.windowSize = size
    },

    /**
     * 根据窗口宽度自动检测设备类型
     */
    detectDevice() {
      const width = this.windowSize.width
      if (width < 768) {
        this.device = 'mobile'
      } else if (width < 992) {
        this.device = 'tablet'
      } else {
        this.device = 'desktop'
      }
    },

    // ============================================================
    // 全局加载状态
    // ============================================================

    /**
     * 显示全局加载
     * @param {string} [text] - 加载文案
     */
    showLoading(text = '加载中...') {
      this.loading = true
      this.loadingText = text
    },

    /**
     * 隐藏全局加载
     */
    hideLoading() {
      this.loading = false
      this.loadingText = ''
    },

    // ============================================================
    // 菜单操作
    // ============================================================

    /**
     * 设置菜单列表
     * @param {array} menuList - 菜单列表
     */
    setMenuList(menuList) {
      this.menuList = menuList
      localCache.set(SIDEBAR_MENU_KEY, menuList)
    },

    /**
     * 设置激活菜单
     * @param {string} menuPath - 菜单路径
     */
    setActiveMenu(menuPath) {
      this.activeMenu = menuPath
    },

    /**
     * 设置面包屑
     * @param {array} breadcrumbs - 面包屑数据
     */
    setBreadcrumbs(breadcrumbs) {
      this.breadcrumbs = breadcrumbs
    },

    // ============================================================
    // 租户信息
    // ============================================================

    /**
     * 设置租户信息
     * @param {object} tenantInfo - 租户信息
     */
    setTenantInfo(tenantInfo) {
      this.tenantInfo = tenantInfo
    },

    /**
     * 清除租户信息
     */
    clearTenantInfo() {
      this.tenantInfo = null
    },
  },
})
