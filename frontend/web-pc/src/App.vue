<template>
  <div id="app">
    <!-- 空白布局（登录页等） -->
    <router-view v-if="layout === 'blank'" />

    <!-- 默认布局 -->
    <template v-else>
      <bx-header
        :fixed="appStore.fixedHeader"
        :show-breadcrumb="appStore.showBreadcrumb"
        :breadcrumbs="appStore.breadcrumbs"
        :height="`${headerHeight}px`"
        :show-mobile-menu-btn="true"
        :show-collapse-btn="true"
      />
      <bx-sidebar
        :collapsed="appStore.sidebarCollapsed"
        :mobile-open="appStore.mobileSidebarOpen"
        :fixed="appStore.fixedSidebar"
      />
      <bx-content :show-header="false" :show-breadcrumb="false">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </bx-content>
    </template>
  </div>
</template>

<script setup>
/**
 * @fileoverview App.vue - 根组件
 * @description 应用根组件，负责整体布局和全局状态初始化
 * @author EMP-FE-001 张婷
 */
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/modules/app.js'
import { useUserStore } from '@/store/modules/user.js'
import { isAuthenticated } from '@/utils/auth.js'

// 引入布局组件
import BxHeader from '@/components/layout/BxHeader.vue'
import BxSidebar from '@/components/layout/BxSidebar.vue'
import BxContent from '@/components/layout/BxContent.vue'

// ============================================================
// Store
// ============================================================
const appStore = useAppStore()
const userStore = useUserStore()
const route = useRoute()

// ============================================================
// 常量
// ============================================================
const headerHeight = 60

// ============================================================
// 计算属性
// ============================================================

/**
 * 当前路由使用的布局
 */
const layout = computed(() => route.meta?.layout || 'default')

// ============================================================
// 生命周期
// ============================================================

/**
 * 组件挂载时
 */
onMounted(() => {
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)

  // 初始检测设备类型
  handleResize()

  // 如果已登录，获取用户信息
  if (isAuthenticated()) {
    userStore.fetchUserInfo().catch((err) => {
      console.warn('[App] Fetch user info failed:', err)
    })
  }
})

/**
 * 组件卸载时
 */
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// ============================================================
// 方法
// ============================================================

/**
 * 处理窗口大小变化
 */
function handleResize() {
  const width = window.innerWidth
  appStore.setWindowSize({ width, height: window.innerHeight })
  appStore.detectDevice()

  // 移动端自动关闭侧边栏
  if (appStore.isMobile && appStore.mobileSidebarOpen) {
    appStore.closeMobileSidebar()
  }
}
</script>

<style lang="scss">
// 全局样式
@import '@/assets/styles/global.scss';

// ============================================================
// 路由过渡动画
// ============================================================
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: opacity 0.2s ease;
}

.fade-transform-enter-from,
.fade-transform-leave-to {
  opacity: 0;
}

// ============================================================
// 移动端响应式
// ============================================================
@media (max-width: #{$breakpoint-md}) {
  #app {
    // 移动端不需要侧边栏偏移
  }
}
</style>
