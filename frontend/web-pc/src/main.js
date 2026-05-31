/**
 * @fileoverview main.js - 应用入口文件
 * @description 负责 Vue 应用初始化、插件安装、全局配置等
 * @author EMP-FE-001 张婷
 */
import { createApp } from 'vue'
import App from './App.vue'

// ============================================================
// 引入样式
// ============================================================
// 全局 SCSS 样式（在 vite.config.js 中已配置自动注入 variables.scss）
import '@/assets/styles/reset.scss'
import '@/assets/styles/global.scss'

// ============================================================
// 引入状态管理
// ============================================================
import { setupStore } from './store/index.js'

// ============================================================
// 引入路由
// ============================================================
import router from './router/index.js'

// ============================================================
// 引入 Element Plus（按需引入以减小体积）
// ============================================================
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// Element Plus 图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// Element Plus 中文语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

// ============================================================
// 创建 Vue 应用
// ============================================================
const app = createApp(App)

// ============================================================
// 安装插件
// ============================================================

// 安装 Pinia 状态管理
setupStore(app)

// 安装 Vue Router
app.use(router)

// 安装 Element Plus
app.use(ElementPlus, {
  locale: zhCn, // 使用中文
  size: 'default', // 默认尺寸
})

// ============================================================
// 注册 Element Plus 图标（全局注册）
// ============================================================
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// ============================================================
// 全局错误处理
// ============================================================
app.config.errorHandler = (err, instance, info) => {
  // 生产环境中不打印详细错误
  if (import.meta.env.PROD) {
    console.error('[Vue Error]', err)
  } else {
    console.error('[Vue Error]', err, info)
  }

  // 可以在这里上报错误到监控系统
  // reportError(err, info)
}

// ============================================================
// 全局警告处理
// ============================================================
app.config.warnHandler = (msg, instance, trace) => {
  // 生产环境中不显示警告
  if (import.meta.env.PROD) return

  console.warn('[Vue Warning]', msg, trace)
}

// ============================================================
// 性能追踪（开发环境）
// ============================================================
if (import.meta.env.DEV) {
  app.config.performance = true
}

// ============================================================
// 挂载应用
// ============================================================
app.mount('#app')

// ============================================================
// 开发环境打印欢迎信息
// ============================================================
if (import.meta.env.DEV) {
  console.log(
    '%c 北极星AI商机获客系统 %c v1.0.0 ',
    'background:#4F46E5;color:#fff;padding:4px 8px;border-radius:4px 0 0 4px;',
    'background:#FBBF24;color:#000;padding:4px 8px;border-radius:0 4px 4px 0;'
  )
  console.log('%c 前端开发：张婷（EMP-FE-001）', 'color: #4F46E5; font-weight: bold;')
}
