/**
 * index.js - Vue Router 配置文件
 * 功能：创建路由实例、配置路由守卫（权限验证）
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { createRouter, createWebHashHistory } from 'vue-router'
import routes from './routes'
import { isLoggedIn, getAdminRole } from '@/utils/adminAuth'

// ============================================================
// 创建路由实例
// ============================================================
const router = createRouter({
  // 使用 Hash 模式路由（无需服务端配置）
  history: createWebHashHistory(),

  // 路由列表
  routes,

  // 路由切换时，页面是否滚动到顶部
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// ============================================================
// 全局前置守卫：在路由跳转前进行权限验证
// ============================================================
router.beforeEach((to, from, next) => {
  // 1. 设置页面标题
  const title = to.meta?.title
  document.title = title ? `${title} - 北极星AI管理后台` : '北极星AI管理后台'

  // 2. 判断目标路由是否需要登录
  if (to.meta?.requireAuth !== false) {
    if (!isLoggedIn()) {
      // 未登录：重定向到登录页
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }

    // 3. 角色权限校验
    const allowedRoles = to.meta?.roles
    if (allowedRoles && Array.isArray(allowedRoles) && allowedRoles.length > 0) {
      const currentRole = getAdminRole()
      if (!allowedRoles.includes(currentRole)) {
        // 无权限访问该页面
        next({ name: 'Dashboard' })
        return
      }
    }
  }

  // 4. 已登录用户访问登录页时，重定向到首页
  if (to.name === 'Login' && isLoggedIn()) {
    next({ name: 'Dashboard' })
    return
  }

  // 5. 放行
  next()
})

// ============================================================
// 全局后置守卫：路由跳转完成后执行
// ============================================================
router.afterEach((to) => {
  // 可在此处记录页面访问日志
  // console.log('[Router] Navigation to:', to.fullPath)
})

// ============================================================
// 导出路由实例
// ============================================================
export default router
