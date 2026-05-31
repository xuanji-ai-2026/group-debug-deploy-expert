/**
 * main.js
 * 应用入口文件
 * 功能：创建Vue应用实例、注册全局组件/插件、挂载应用
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { createApp } from 'vue'
import App from './App.vue'

// 引入 Vue Router
import router from './router'

// 引入 Pinia 状态管理
import { createPinia } from 'pinia'

// 引入 Element Plus UI 组件库
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 引入 Element Plus 图标库（全局注册）
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 引入全局样式
import '@assets/styles/global.scss'

// 创建 Vue 应用实例
const app = createApp(App)

// 创建 Pinia 实例（状态管理）
const pinia = createPinia()

// 注册 Pinia
app.use(pinia)

// 注册 Vue Router
app.use(router)

// 注册 Element Plus
app.use(ElementPlus)

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 挂载应用到 #app 节点
app.mount('#app')
