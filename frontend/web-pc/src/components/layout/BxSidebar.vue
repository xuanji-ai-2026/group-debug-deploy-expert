<template>
  <!--
    BxSidebar 组件 - 北极星侧边栏组件
    提供后台管理系统的导航菜单
  -->
  <aside
    class="bx-sidebar"
    :class="[
      `bx-sidebar--${theme}`,
      {
        'is-collapsed': collapsed,
        'is-fixed': fixed,
        'is-mobile-open': mobileOpen,
      },
    ]"
    :style="sidebarStyle"
  >
    <!-- 移动端遮罩层 -->
    <div v-if="mobileOpen" class="bx-sidebar__mask" @click="handleCloseMobile" />

    <!-- 侧边栏内容 -->
    <div class="bx-sidebar__content">
      <!-- Logo 区域（展开时） -->
      <div v-if="!collapsed && showLogo" class="bx-sidebar__logo">
        <router-link to="/" class="bx-sidebar__logo-link">
          <img v-if="logoUrl" :src="logoUrl" :alt="appName" class="bx-sidebar__logo-img" />
          <span v-else class="bx-sidebar__logo-text">
            <i class="el-icon-star-on" />
            {{ appName }}
          </span>
        </router-link>
      </div>

      <!-- 简洁 Logo（折叠时） -->
      <div v-if="collapsed && showLogo" class="bx-sidebar__logo-collapsed">
        <router-link to="/" class="bx-sidebar__logo-link">
          <i class="el-icon-star-on bx-sidebar__logo-icon" />
        </router-link>
      </div>

      <!-- 菜单 -->
      <el-scrollbar class="bx-sidebar__scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :background-color="menuBgColor"
          :text-color="menuTextColor"
          :active-text-color="menuActiveTextColor"
          :unique-opened="uniqueOpened"
          :menu-trigger="menuTrigger"
          :router="menuRouter"
          :class="['bx-sidebar__menu', { 'is-collapse': collapsed }]"
          @select="handleSelect"
        >
          <template v-for="item in computedMenuList" :key="item.path">
            <!-- 有子菜单 -->
            <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
              <template #title>
                <i v-if="item.icon" :class="item.icon" />
                <span>{{ item.title }}</span>
              </template>

              <el-menu-item
                v-for="child in item.children.filter((c) => !c.hidden)"
                :key="child.path"
                :index="child.path"
                :route="{ path: child.path }"
              >
                <i v-if="child.icon" :class="child.icon" />
                <span>{{ child.title }}</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 无子菜单 -->
            <el-menu-item v-else :index="item.path" :route="{ path: item.path }">
              <i v-if="item.icon" :class="item.icon" />
              <span>{{ item.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </div>
  </aside>
</template>

<script setup>
/**
 * @fileoverview BxSidebar 侧边栏组件
 * @description 提供侧边栏导航菜单，支持折叠、响应式等功能
 * @author EMP-FE-001 张婷
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/store/modules/app.js'
import { getMenuRoutes } from '@/router/routes.js'

// ============================================================
// Props
// ============================================================
const props = defineProps({
  /**
   * 是否折叠
   */
  collapsed: {
    type: Boolean,
    default: false,
  },

  /**
   * 移动端是否展开
   */
  mobileOpen: {
    type: Boolean,
    default: false,
  },

  /**
   * 主题：light / dark
   */
  theme: {
    type: String,
    default: 'light',
  },

  /**
   * 是否固定
   */
  fixed: {
    type: Boolean,
    default: true,
  },

  /**
   * Logo 图片
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
   * 是否显示 Logo
   */
  showLogo: {
    type: Boolean,
    default: true,
  },

  /**
   * 菜单列表（为空则使用路由自动生成）
   */
  menuList: {
    type: Array,
    default: () => [],
  },

  /**
   * 是否只展开一个子菜单
   */
  uniqueOpened: {
    type: Boolean,
    default: true,
  },

  /**
   * 菜单触发方式
   */
  menuTrigger: {
    type: String,
    default: 'hover',
  },

  /**
   * 菜单是否使用 Vue Router
   */
  menuRouter: {
    type: Boolean,
    default: true,
  },
})

// ============================================================
// Emits
// ============================================================
const emit = defineEmits(['close-mobile', 'select'])

// ============================================================
// Store
// ============================================================
const appStore = useAppStore()
const router = useRouter()

// ============================================================
// 计算属性
// ============================================================

/**
 * 激活的菜单路径
 */
const activeMenu = computed(() => appStore.activeMenu)

/**
 * 侧边栏样式
 */
const sidebarStyle = computed(() => {
  const width = props.collapsed ? '64px' : '220px'
  return {
    width,
  }
})

/**
 * 菜单背景色
 */
const menuBgColor = computed(() => (props.theme === 'dark' ? '#1f1d1d' : '#ffffff'))

/**
 * 菜单文字颜色
 */
const menuTextColor = computed(() =>
  props.theme === 'dark' ? 'rgba(255, 255, 255, 0.7)' : '#303133'
)

/**
 * 菜单激活文字颜色
 */
const menuActiveTextColor = computed(() =>
  props.theme === 'dark' ? '#ffffff' : '#4F46E5'
)

/**
 * 菜单列表（使用路由生成）
 */
const computedMenuList = computed(() => {
  if (props.menuList.length > 0) {
    return props.menuList
  }
  // 从路由生成菜单
  return getMenuRoutes()
    .filter((route) => !route.meta?.hidden)
    .map((route) => ({
      path: route.path,
      title: route.meta?.title || route.name,
      icon: route.meta?.icon || 'el-icon-document',
      children: (route.children || []).map((child) => ({
        path: child.path,
        title: child.meta?.title || child.name,
        icon: child.meta?.icon,
        hidden: child.meta?.hidden,
      })),
    }))
})

// ============================================================
// 方法
// ============================================================

function handleSelect(index, indexPath) {
  emit('select', index, indexPath)

  // 移动端选中后关闭侧边栏
  if (appStore.isMobile) {
    handleCloseMobile()
  }
}

function handleCloseMobile() {
  emit('close-mobile')
  appStore.closeMobileSidebar()
}
</script>

<style lang="scss" scoped>
.bx-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  background-color: $color-white;
  border-right: 1px solid $color-border-light;
  transition: width $transition-duration-base $transition-timing-ease;
  z-index: $z-index-fixed;
  overflow: hidden;

  // 折叠状态
  &.is-collapsed {
    width: 64px;
  }

  // 移动端
  &__mask {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.5);
    z-index: -1;

    @media (min-width: #{$breakpoint-md}) {
      display: none;
    }
  }

  // 内容
  &__content {
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: hidden;
  }

  // Logo
  &__logo {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 60px;
    padding: 0 $spacing-base;
    border-bottom: 1px solid $color-border-light;

    &-link {
      display: flex;
      align-items: center;
      text-decoration: none;
    }

    &-img {
      height: 32px;
    }

    &-text {
      display: flex;
      align-items: center;
      font-size: $font-size-lg;
      font-weight: $font-weight-bold;
      color: $color-primary;

      i {
        margin-right: $spacing-xs;
        font-size: 20px;
        color: $color-warning;
      }
    }
  }

  &__logo-collapsed {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 60px;
    border-bottom: 1px solid $color-border-light;

    &-link {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: $radius-lg;
      transition: background-color $transition-duration-fast;

      &:hover {
        background-color: $color-bg-hover;
      }
    }

    .bx-sidebar__logo-icon {
      font-size: 24px;
      color: $color-warning;
    }
  }

  // 滚动条
  &__scroll {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;

    :deep(.el-scrollbar__wrap) {
      overflow-x: hidden;
    }
  }

  // 菜单
  &__menu {
    border-right: none;

    &:not(.el-menu--collapse) {
      width: 100%;
    }

    // 图标
    .el-menu-item,
    .el-sub-menu__title {
      i {
        margin-right: 10px;
        font-size: 18px;
        vertical-align: middle;
      }
    }

    // 子菜单
    .el-sub-menu {
      .el-menu {
        background-color: transparent;
      }

      .el-menu-item {
        min-width: 0;
        padding-left: 50px !important;
      }
    }
  }

  // 深色主题
  &--dark {
    background-color: #1f1d1d;
    border-right-color: #2d2b2b;

    .bx-sidebar__logo {
      border-bottom-color: #2d2b2b;
    }
  }

  // 响应式
  @media (max-width: #{$breakpoint-md}) {
    position: fixed;
    transform: translateX(-100%);
    transition: transform $transition-duration-base $transition-timing-ease;

    &.is-mobile-open {
      transform: translateX(0);
    }
  }
}
</style>
