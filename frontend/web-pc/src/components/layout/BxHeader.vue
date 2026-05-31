<template>
  <!--
    BxHeader 组件 - 北极星顶部导航栏
    提供全局顶部导航，包含 Logo、面包屑、用户信息等
  -->
  <header class="bx-header" :class="{ 'is-fixed': fixed }" :style="headerStyle">
    <!-- 左侧区域 -->
    <div class="bx-header__left">
      <!-- 移动端菜单按钮 -->
      <div v-if="showMobileMenuBtn" class="bx-header__menu-btn" @click="handleMenuToggle">
        <i class="el-icon-s-fold" :class="{ 'is-active': sidebarCollapsed }" />
      </div>

      <!-- Logo -->
      <div class="bx-header__logo">
        <router-link to="/" class="bx-header__logo-link">
          <!-- Logo 图片或文字 -->
          <img v-if="logoUrl" :src="logoUrl" :alt="appName" class="bx-header__logo-img" />
          <span v-else class="bx-header__logo-text">
            <i class="bx-header__logo-icon el-icon-star-on" />
            {{ appName }}
          </span>
        </router-link>
      </div>

      <!-- PC 端折叠按钮 -->
      <div v-if="showCollapseBtn && !isMobile" class="bx-header__collapse-btn" @click="handleCollapse">
        <i class="el-icon-s-fold" :class="{ 'is-active': sidebarCollapsed }" />
      </div>
    </div>

    <!-- 中间区域（面包屑） -->
    <div class="bx-header__center">
      <slot name="center">
        <!-- 面包屑 -->
        <bx-breadcrumb v-if="showBreadcrumb" :breadcrumbs="breadcrumbs" />
      </slot>
    </div>

    <!-- 右侧区域 -->
    <div class="bx-header__right">
      <slot name="right">
        <!-- 快捷功能 -->
        <div class="bx-header__actions">
          <!-- 刷新 -->
          <el-tooltip content="刷新" placement="bottom">
            <div class="bx-header__action-item" @click="handleRefresh">
              <i class="el-icon-refresh" />
            </div>
          </el-tooltip>

          <!-- 全屏 -->
          <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'" placement="bottom">
            <div class="bx-header__action-item" @click="handleFullscreen">
              <i :class="isFullscreen ? 'el-icon-close' : 'el-icon-full-screen'" />
            </div>
          </el-tooltip>

          <!-- 通知 -->
          <el-tooltip content="通知" placement="bottom">
            <div class="bx-header__action-item">
              <i class="el-icon-bell" />
              <span v-if="notificationCount > 0" class="bx-header__badge">
                {{ notificationCount > 99 ? '99+' : notificationCount }}
              </span>
            </div>
          </el-tooltip>
        </div>

        <!-- 用户信息 -->
        <el-dropdown trigger="click" @command="handleUserCommand">
          <div class="bx-header__user">
            <!-- 头像 -->
            <el-avatar :size="32" :src="userAvatar" class="bx-header__avatar">
              {{ userName?.charAt(0) || 'U' }}
            </el-avatar>
            <!-- 用户名 -->
            <span class="bx-header__username">{{ userName }}</span>
            <!-- 下拉箭头 -->
            <i class="el-icon-arrow-down bx-header__arrow" />
          </div>

          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <i class="el-icon-user" />
                个人信息
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <i class="el-icon-setting" />
                系统设置
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <i class="el-icon-switch-button" />
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </slot>
    </div>
  </header>
</template>

<script setup>
/**
 * @fileoverview BxHeader 顶部导航栏组件
 * @description 提供全局顶部导航，支持 Logo、面包屑、用户菜单等功能
 * @author EMP-FE-001 张婷
 */
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAppStore } from '@/store/modules/app.js'
import { useUserStore } from '@/store/modules/user.js'
import BxBreadcrumb from '@/components/common/BxBreadcrumb.vue'

// ============================================================
// Props
// ============================================================
const props = defineProps({
  /**
   * 是否固定顶部
   */
  fixed: {
    type: Boolean,
    default: true,
  },

  /**
   * Logo 图片 URL
   */
  logoUrl: {
    type: String,
    default: '',
  },

  /**
   * 应用名称
   */
  appName: {
    type: String,
    default: '北极星AI',
  },

  /**
   * 是否显示面包屑
   */
  showBreadcrumb: {
    type: Boolean,
    default: true,
  },

  /**
   * 是否显示移动端菜单按钮
   */
  showMobileMenuBtn: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否显示折叠按钮
   */
  showCollapseBtn: {
    type: Boolean,
    default: true,
  },

  /**
   * 通知数量
   */
  notificationCount: {
    type: Number,
    default: 0,
  },

  /**
   * 高度
   */
  height: {
    type: String,
    default: '60px',
  },

  /**
   * 面包屑数据
   */
  breadcrumbs: {
    type: Array,
    default: () => [],
  },
})

// ============================================================
// Emits
// ============================================================
const emit = defineEmits(['menu-toggle', 'collapse', 'refresh'])

// ============================================================
// Store
// ============================================================
const appStore = useAppStore()
const userStore = useUserStore()

// ============================================================
// 计算属性
// ============================================================
const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)
const isMobile = computed(() => appStore.isMobile)
const userAvatar = computed(() => userStore.userAvatar || userStore.userInfo?.avatar)
const userName = computed(() => userStore.userName)

const headerStyle = computed(() => ({
  height: props.height,
}))

// 全屏状态
const isFullscreen = ref(false)

// ============================================================
// 方法
// ============================================================

function handleMenuToggle() {
  emit('menu-toggle')
  appStore.toggleMobileSidebar()
}

function handleCollapse() {
  emit('collapse')
  appStore.toggleSidebar()
}

function handleRefresh() {
  emit('refresh')
  window.location.reload()
}

function handleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

function handleUserCommand(command) {
  const router = useRouter()
  switch (command) {
    case 'profile':
      router.push('/settings/profile')
      break
    case 'settings':
      router.push('/settings/security')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          userStore.logout()
        })
        .catch(() => {})
      break
  }
}
</script>

<style lang="scss" scoped>
.bx-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  background-color: $color-white;
  border-bottom: 1px solid $color-border-light;
  padding: 0 $spacing-lg;
  z-index: $z-index-sticky;

  // 固定顶部
  &.is-fixed {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
  }

  // 左侧
  &__left {
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }

  // 菜单按钮（移动端）
  &__menu-btn {
    display: none;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    margin-right: $spacing-sm;
    cursor: pointer;
    border-radius: $radius-md;
    transition: background-color $transition-duration-fast;

    &:hover {
      background-color: $color-bg-hover;
    }

    @media (max-width: #{$breakpoint-md}) {
      display: flex;
    }
  }

  // Logo
  &__logo {
    margin-right: $spacing-base;

    &-link {
      display: flex;
      align-items: center;
      text-decoration: none;
    }

    &-img {
      height: 36px;
    }

    &-text {
      display: flex;
      align-items: center;
      font-size: $font-size-xl;
      font-weight: $font-weight-bold;
      color: $color-primary;
    }

    &-icon {
      margin-right: $spacing-xs;
      font-size: 20px;
      color: $color-warning;
    }
  }

  // 折叠按钮
  &__collapse-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    cursor: pointer;
    border-radius: $radius-md;
    transition: all $transition-duration-fast;
    color: $color-text-secondary;

    &:hover {
      background-color: $color-bg-hover;
      color: $color-text-primary;
    }

    .is-active {
      transform: rotate(180deg);
    }
  }

  // 中间
  &__center {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  // 右侧
  &__right {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    gap: $spacing-base;
  }

  // 操作按钮组
  &__actions {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
  }

  &__action-item {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    cursor: pointer;
    border-radius: $radius-md;
    color: $color-text-secondary;
    transition: all $transition-duration-fast;
    font-size: 18px;

    &:hover {
      background-color: $color-bg-hover;
      color: $color-text-primary;
    }
  }

  &__badge {
    position: absolute;
    top: 2px;
    right: 2px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    background-color: $color-danger;
    color: $color-white;
    font-size: 10px;
    line-height: 16px;
    text-align: center;
    border-radius: $radius-full;
    transform: translate(50%, -50%);
  }

  // 用户信息
  &__user {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    padding: $spacing-xs $spacing-sm;
    cursor: pointer;
    border-radius: $radius-md;
    transition: background-color $transition-duration-fast;

    &:hover {
      background-color: $color-bg-hover;
    }
  }

  &__avatar {
    flex-shrink: 0;
    background-color: $color-primary;
    color: $color-white;
  }

  &__username {
    max-width: 100px;
    font-size: $font-size-sm;
    color: $color-text-primary;
    @include text-ellipsis(1);

    @media (max-width: #{$breakpoint-md}) {
      display: none;
    }
  }

  &__arrow {
    color: $color-text-secondary;
    font-size: 12px;
    transition: transform $transition-duration-fast;

    @media (max-width: #{$breakpoint-md}) {
      display: none;
    }
  }
}
</style>
