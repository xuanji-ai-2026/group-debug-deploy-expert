/**
 * @fileoverview Vite 配置文件
 * @author EMP-FE-001 张婷
 * @description 北极星AI商机获客系统 PC端Web应用构建配置
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { fileURLToPath, URL } from 'node:url'
const __dirname = fileURLToPath(new URL('.', import.meta.url))

export default defineConfig({
  // 启用 Vue 插件
  plugins: [vue()],

  // 基础路径配置
  base: '/',

  // 路径别名配置
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@components': resolve(__dirname, 'src/components'),
      '@views': resolve(__dirname, 'src/views'),
      '@utils': resolve(__dirname, 'src/utils'),
      '@api': resolve(__dirname, 'src/api'),
      '@store': resolve(__dirname, 'src/store'),
      '@router': resolve(__dirname, 'src/router'),
      '@assets': resolve(__dirname, 'src/assets'),
    },
  },

  // 开发服务器配置
  server: {
    host: '0.0.0.0',
    port: 5173,
    // 开启 HTTPS（如果需要）
    // https: true,
    // 代理配置（开发环境API转发）
    proxy: {
      // API 网关代理
      '/api': {
        target: process.env.VITE_API_BASE_URL || 'http://localhost:8080',
        changeOrigin: true,
        // 重写路径（移除 /api 前缀）
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // 静态资源代理
      '/cdn': {
        target: 'https://cdn.beijixing.com',
        changeOrigin: true,
      },
    },
  },

  // 构建配置
  build: {
    // 输出目录
    outDir: 'dist',
    // 静态资源目录
    assetsDir: 'assets',
    // 生产环境 Source Map（生产构建时关闭）
    sourcemap: false,
    // 代码分割策略
    rollupOptions: {
      output: {
        // 手动分包配置
        manualChunks: {
          // 第三方库分包
          'element-plus': ['element-plus'],
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
        },
      },
    },
    // 构建体积警告阈值
    chunkSizeWarningLimit: 1000,
  },

  esbuild: {
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : []
  },

  // CSS 配置
  css: {
    // CSS 预处理器配置
    preprocessorOptions: {
      scss: {
        // 注入 SCSS 变量
        additionalData: `@import "@/assets/styles/variables.scss";`,
      },
    },
  },

  // 优化配置
  optimizeDeps: {
    // 预构建依赖
    include: ['vue', 'vue-router', 'pinia', 'axios', 'element-plus'],
  },
})
