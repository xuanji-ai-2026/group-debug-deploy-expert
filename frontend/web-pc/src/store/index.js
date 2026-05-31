/**
 * @fileoverview Pinia 状态管理入口
 * @description 统一导出所有 store 模块
 * @author EMP-FE-001 张婷
 */
import { createPinia } from 'pinia'

// 创建 Pinia 实例
const pinia = createPinia()

/**
 * 安装 Pinia 插件
 * @param {import('vue').App} app - Vue 应用实例
 */
export function setupStore(app) {
  app.use(pinia)
}

export default pinia

// 导出各模块（方便按需引入）
export { useUserStore } from './modules/user.js'
export { useAppStore } from './modules/app.js'
