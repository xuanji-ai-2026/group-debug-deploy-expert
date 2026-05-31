<template>
  <header class="admin-header">
    <!-- 左侧：面包屑 + 页面标题 -->
    <div class="header-left">
      <!-- 菜单折叠按钮 -->
      <el-button
        text
        class="collapse-btn"
        @click="$emit('toggle-sidebar')"
      >
        <el-icon size="20">
          <Expand v-if="sidebarCollapsed" />
          <Fold v-else />
        </el-icon>
      </el-button>

      <!-- 面包屑导航 -->
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 右侧：操作按钮 -->
    <div class="header-right">
      <!-- 刷新页面 -->
      <el-tooltip content="刷新页面" placement="bottom">
        <el-button text @click="refreshPage">
          <el-icon size="18"><Refresh /></el-icon>
        </el-button>
      </el-tooltip>

      <!-- 全屏切换 -->
      <el-tooltip :content="isFullScreen ? '退出全屏' : '全屏'" placement="bottom">
        <el-button text @click="toggleFullScreen">
          <el-icon size="18">
            <FullScreen v-if="!isFullScreen" />
            <Close v-else />
          </el-icon>
        </el-button>
      </el-tooltip>

      <!-- 用户信息下拉菜单 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <!-- 头像 -->
          <el-avatar :size="32" class="user-avatar">
            {{ adminInfo?.nickname?.[0] || adminInfo?.username?.[0] || 'A' }}
          </el-avatar>
          <!-- 用户名 -->
          <span class="user-name">{{ nickname }}</span>
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </div>

        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon> 个人设置
            </el-dropdown-item>
            <el-dropdown-item command="password">
              <el-icon><Lock /></el-icon> 修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
/**
 * AdminHeader.vue - 顶部导航栏组件
 * 功能：面包屑导航、用户信息显示、登出、全屏切换等
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/store/modules/admin'
import { ElMessageBox } from 'element-plus'
import { Expand, Fold, Refresh, FullScreen, Close, ArrowDown, User, Lock, SwitchButton } from '@element-plus/icons-vue'

defineEmits(['toggle-sidebar'])

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

// Pinia 状态
const adminInfo = computed(() => adminStore.adminInfo)
const nickname = computed(() => adminStore.nickname)
const sidebarCollapsed = computed(() => adminStore.sidebarCollapsed)
const isFullScreen = computed(() => adminStore.isFullScreen)

// 面包屑：从路由 meta 获取
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta?.title && item.meta?.title !== '首页')
  return matched.map(item => ({
    title: item.meta.title,
    path: item.path
  }))
})

/**
 * 刷新当前页面
 */
function refreshPage() {
  window.location.reload()
}

/**
 * 切换全屏模式
 */
function toggleFullScreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    adminStore.setFullScreen(true)
  } else {
    document.exitFullscreen()
    adminStore.setFullScreen(false)
  }
}

/**
 * 下拉菜单命令处理
 */
async function handleCommand(command) {
  switch (command) {
    case 'profile':
      // 跳转到个人设置
      router.push('/system/settings')
      break
    case 'password':
      // 修改密码弹窗
      showPasswordDialog()
      break
    case 'logout':
      // 退出登录确认
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      adminStore.logout()
      router.push('/login')
      break
  }
}

/**
 * 修改密码弹窗（预留）
 */
function showPasswordDialog() {
  ElMessageBox.alert('修改密码功能开发中...', '提示')
}
</script>

<style lang="scss" scoped>
.admin-header {
  height: $header-height;
  background-color: $bg-color;
  border-bottom: 1px solid $border-color-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $spacing-base;
  position: sticky;
  top: 0;
  z-index: $z-index-sticky;
}

// 左侧区域
.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-base;
}

.collapse-btn {
  padding: $spacing-xs $spacing-sm;
  color: $text-secondary;

  &:hover {
    color: $primary-color;
  }
}

// 用户信息
.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.user-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-base;
  cursor: pointer;
  transition: $transition-fast;

  &:hover {
    background-color: $bg-color-page;
  }
}

.user-avatar {
  background-color: $primary-color;
  color: #ffffff;
  font-weight: 600;
}

.user-name {
  font-size: $font-size-base;
  color: $text-primary;
  font-weight: 500;
}
</style>
