<template>
  <aside class="admin-sidebar" :class="{ collapsed }">
    <!-- Logo 区域 -->
    <div class="sidebar-logo">
      <img v-if="!collapsed" src="@/assets/images/logo.png" alt="logo" class="logo-img" @error="handleImageError" />
      <span v-if="!collapsed" class="logo-text">北极星AI</span>
      <div v-else class="logo-icon">⭐</div>
    </div>

    <!-- 导航菜单 -->
    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      :collapse-transition="false"
      background-color="#1d2129"
      text-color="#b1b3b8"
      active-text-color="#ffffff"
      router
      class="sidebar-menu"
    >
      <!-- 首页 -->
      <el-menu-item index="/dashboard">
        <el-icon><Monitor /></el-icon>
        <template #title>控制台</template>
      </el-menu-item>

      <!-- 分隔线 + 租户管理 -->
      <div class="menu-divider">
        <span v-if="!collapsed">租户管理</span>
      </div>

      <el-menu-item index="/tenant/list">
        <el-icon><OfficeBuilding /></el-icon>
        <template #title>租户列表</template>
      </el-menu-item>

      <el-menu-item index="/tenant/audit">
        <el-icon><DocumentChecked /></el-icon>
        <template #title>租户审核</template>
      </el-menu-item>

      <!-- 分隔线 + 计费管理 -->
      <div class="menu-divider">
        <span v-if="!collapsed">计费管理</span>
      </div>

      <el-menu-item index="/billing/overview">
        <el-icon><Coin /></el-icon>
        <template #title>计费概览</template>
      </el-menu-item>

      <!-- 分隔线 + 系统设置 -->
      <div class="menu-divider">
        <span v-if="!collapsed">系统管理</span>
      </div>

      <el-menu-item index="/system/settings">
        <el-icon><Setting /></el-icon>
        <template #title>系统设置</template>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup>
/**
 * AdminSidebar.vue - 左侧边栏组件
 * 功能：后台管理系统的侧边导航菜单
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Monitor, OfficeBuilding, DocumentChecked, Coin, Setting } from '@element-plus/icons-vue'

const props = defineProps({
  // 是否折叠
  collapsed: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()

// 当前激活的菜单路径
const activeMenu = computed(() => route.path)

/**
 * 图片加载失败时的降级处理
 */
function handleImageError(e) {
  e.target.style.display = 'none'
}
</script>

<style lang="scss" scoped>
// 侧边栏容器
.admin-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: $sidebar-width;
  background-color: $bg-color-dark;
  transition: width 0.3s ease;
  z-index: 100;
  overflow: hidden;

  // 折叠状态
  &.collapsed {
    width: $sidebar-collapsed-width;
  }
}

// Logo 区域
.sidebar-logo {
  height: $header-height;
  display: flex;
  align-items: center;
  padding: 0 $spacing-base;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);

  .logo-img {
    width: 32px;
    height: 32px;
    margin-right: $spacing-sm;
  }

  .logo-text {
    font-size: $font-size-large;
    font-weight: 700;
    color: #ffffff;
    white-space: nowrap;
  }

  .logo-icon {
    font-size: 24px;
    color: #ffffff;
    margin: 0 auto;
  }
}

// 菜单
.sidebar-menu {
  border-right: none;
  background-color: transparent !important;

  // Element Plus 图标
  .el-icon {
    color: inherit;
  }

  // 菜单项
  :deep(.el-menu-item) {
    height: 50px;
    line-height: 50px;
    margin: 4px 8px;
    border-radius: $border-radius-base;

    &:hover {
      background-color: rgba(255, 255, 255, 0.08) !important;
      color: #ffffff !important;
    }

    &.is-active {
      background-color: rgba($primary-color, 0.2) !important;
      color: $primary-color !important;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 8px;
        bottom: 8px;
        width: 3px;
        background-color: $primary-color;
        border-radius: 0 2px 2px 0;
      }
    }
  }
}

// 菜单分隔线
.menu-divider {
  padding: $spacing-base $spacing-base $spacing-xs;
  font-size: $font-size-small;
  color: $text-secondary;
  text-transform: uppercase;
  letter-spacing: 1px;
  white-space: nowrap;
  overflow: hidden;
}
</style>
