/**
 * store/index.js
 * Pinia Store 统一导出文件
 * 功能：整合所有 Store 模块，统一导出
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { createPinia } from 'pinia'

// 创建 Pinia 实例
const pinia = createPinia()

export default pinia

// 导出各模块
export { useAdminStore } from './modules/admin'
