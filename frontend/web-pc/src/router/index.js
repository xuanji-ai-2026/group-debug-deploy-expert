/**
 * @fileoverview 路由配置文件
 * @description 路由实例创建、全局导航守卫配置
 * @author EMP-FE-001 张婷
 */
import { createRouter, createWebHistory, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/store/modules/user.js'
import { useAppStore } from '@/store/modules/app.js'
import { isAuthenticated } from '@/utils/auth.js'
import { getRoutes } from './routes.js'

// ============================================================
// 创建 Router 实例
// ============================================================

/**
 * 获取路由模式
 * 根据环境变量配置选择 hash 或 history 模式
 */
const getRouterMode = () => {
  const mode = import.meta.env.VITE_ROUTER_MODE || 'hash'
  return mode === 'history' ? createWebHistory : createWebHashHistory
}

/**
 * 创建路由实例
 */
const router = createRouter({
  // 路由模式
  history: getRouterMode()(import.meta.env.BASE_URL),
  // 路由表（动态加载）
  routes: getRoutes(),
  // 路由滚动行为
  scrollBehavior(to, from, savedPosition) {
    // 如果有保存的滚动位置（如浏览器后退）
    if (savedPosition) {
      return savedPosition
    }
    // 滚动到锚点
    if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth',
      }
    }
    // 滚动到顶部
    return {
      top: 0,
      behavior: 'smooth',
    }
  },
})

// ============================================================
// 全局导航守卫
// ============================================================

/**
 * 路由加载前守卫
 * 用于：登录校验、权限校验、加载状态处理
 */
router.beforeEach(async (to, from, next) => {
  // 更新页面标题
  const title = to.meta?.title
  if (title) {
    document.title = `${title} - 北极星AI商机获客系统`
  } else {
    document.title = '北极星AI商机获客系统'
  }

  // 获取用户 Store
  const userStore = useUserStore()

  // 判断目标路由是否需要登录
  const requiresAuth = to.meta?.requiresAuth !== false

  // 需要登录但未登录
  if (requiresAuth && !isAuthenticated()) {
    // 保存跳转目标，登录后可返回
    const redirectPath = to.fullPath
    next({
      path: '/login',
      query: redirectPath !== '/' ? { redirect: redirectPath } : {},
    })
    return
  }

  // 已登录访问登录页，跳转到首页或指定页面
  if (to.path === '/login' && isAuthenticated()) {
    const redirect = to.query.redirect
    next(redirect || '/')
    return
  }

  // 权限校验（如果有角色要求）
  const requiredRoles = to.meta?.roles
  if (requiredRoles && requiredRoles.length > 0) {
    const userRoleType = userStore.roleType
    if (!requiredRoles.includes(userRoleType)) {
      // 没有权限，跳转到 403 页面
      next({ path: '/403' })
      return
    }
  }

  // 放行
  next()
})

/**
 * 路由加载后守卫
 */
router.afterEach((to, from) => {
  // 更新激活菜单
  const appStore = useAppStore()
  appStore.setActiveMenu(to.path)

  // 更新面包屑
  updateBreadcrumbs(to, appStore)
})

/**
 * 路由错误处理
 */
router.onError((error) => {
  console.error('[Router] Navigation error:', error)

  // 动态路由加载失败处理
  if (error.message.includes('Failed to fetch dynamically imported module')) {
    // 强制刷新页面
    window.location.reload()
  }
})

// ============================================================
// 工具函数
// ============================================================

/**
 * 更新面包屑
 * @param {Route} to - 目标路由
 * @param {AppStore} appStore - 应用 Store
 */
function updateBreadcrumbs(to, appStore) {
  // 匹配路由对应的面包屑
  const breadcrumbs = []

  // 添加首页
  breadcrumbs.push({
    path: '/',
    title: '首页',
    icon: 'home',
  })

  // 根据路由路径生成面包屑
  const matched = to.matched.filter((record) => record.meta?.title)

  matched.forEach((record, index) => {
    // 跳过参数路由（如 /user/:id）
    if (record.path.includes(':')) {
      return
    }

    breadcrumbs.push({
      path: record.path,
      title: record.meta.title,
      icon: record.meta.icon || '',
    })
  })

  // 如果是详情页，追加当前页标题
  if (to.meta?.breadcrumbTitle) {
    breadcrumbs.push({
      path: to.fullPath,
      title: to.meta.breadcrumbTitle,
    })
  }

  appStore.setBreadcrumbs(breadcrumbs)
}

// ============================================================
// 导出
// ============================================================

export default router
export { getRoutes } from './routes.js'
