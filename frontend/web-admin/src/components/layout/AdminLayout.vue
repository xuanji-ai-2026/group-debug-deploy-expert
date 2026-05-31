<template>
  <div class="admin-layout">
    <!-- 左侧边栏 -->
    <AdminSidebar :collapsed="sidebarCollapsed" />

    <!-- 主内容区域 -->
    <div class="admin-main" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <!-- 顶部导航 -->
      <AdminHeader @toggle-sidebar="toggleSidebar" />

      <!-- 页面内容 -->
      <AdminContent>
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </AdminContent>
    </div>
  </div>
</template>

<script setup>
/**
 * AdminLayout.vue - 管理后台主布局组件
 * 功能：整合侧边栏、顶部栏、内容区，形成完整的后台布局结构
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { computed } from 'vue'
import { useAdminStore } from '@/store/modules/admin'
import AdminSidebar from './AdminSidebar.vue'
import AdminHeader from './AdminHeader.vue'
import AdminContent from './AdminContent.vue'

// 使用 Pinia store
const adminStore = useAdminStore()

// 侧边栏折叠状态
const sidebarCollapsed = computed(() => adminStore.sidebarCollapsed)

/**
 * 切换侧边栏折叠状态
 */
function toggleSidebar() {
  adminStore.toggleSidebar()
}
</script>

<style lang="scss" scoped>
// 布局容器
.admin-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

// 主内容区
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: $sidebar-width;
  transition: margin-left 0.3s ease;
  overflow: hidden;

  // 侧边栏折叠时的样式
  &.sidebar-collapsed {
    margin-left: $sidebar-collapsed-width;
  }
}
</style>
