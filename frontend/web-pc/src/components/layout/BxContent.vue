<template>
  <!--
    BxContent 组件 - 北极星内容区域组件
    提供统一的页面内容容器，包含内边距和响应式处理
  -->
  <main
    class="bx-content"
    :class="[
      `bx-content--${size}`,
      {
        'is-full-height': fullHeight,
        'is-padding': padding,
      },
    ]"
    :style="contentStyle"
  >
    <!-- 面包屑导航 -->
    <div v-if="showBreadcrumb && breadcrumbs.length > 0" class="bx-content__breadcrumb">
      <slot name="breadcrumb">
        <bx-breadcrumb :breadcrumbs="breadcrumbs" />
      </slot>
    </div>

    <!-- 页面标题 -->
    <div v-if="showHeader" class="bx-content__header">
      <slot name="header">
        <div class="bx-content__title-row">
          <div class="bx-content__title-block">
            <h1 v-if="title" class="bx-content__title">{{ title }}</h1>
            <p v-if="description" class="bx-content__description">{{ description }}</p>
          </div>
          <div class="bx-content__actions">
            <slot name="actions" />
          </div>
        </div>
      </slot>
    </div>

    <!-- 内容主体 -->
    <div class="bx-content__body" :class="{ 'is-scroll': scroll }">
      <slot />
    </div>

    <!-- 内容底部 -->
    <div v-if="$slots.footer" class="bx-content__footer">
      <slot name="footer" />
    </div>
  </main>
</template>

<script setup>
/**
 * @fileoverview BxContent 内容区域组件
 * @description 提供统一的页面内容容器，支持面包屑、标题、操作按钮等
 * @author EMP-FE-001 张婷
 */
import { computed } from 'vue'
import { useAppStore } from '@/store/modules/app.js'
import BxBreadcrumb from '@/components/common/BxBreadcrumb.vue'

// ============================================================
// Props
// ============================================================
const props = defineProps({
  /**
   * 页面标题
   */
  title: {
    type: String,
    default: '',
  },

  /**
   * 页面描述
   */
  description: {
    type: String,
    default: '',
  },

  /**
   * 是否显示面包屑
   */
  showBreadcrumb: {
    type: Boolean,
    default: true,
  },

  /**
   * 是否显示页面标题区
   */
  showHeader: {
    type: Boolean,
    default: true,
  },

  /**
   * 面包屑数据
   */
  breadcrumbs: {
    type: Array,
    default: () => [],
  },

  /**
   * 内容区域大小
   */
  size: {
    type: String,
    default: 'default',
    validator: (val) => ['small', 'default', 'large'].includes(val),
  },

  /**
   * 是否充满整个视口高度
   */
  fullHeight: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否显示内边距
   */
  padding: {
    type: Boolean,
    default: true,
  },

  /**
   * 内容是否可滚动
   */
  scroll: {
    type: Boolean,
    default: true,
  },

  /**
   * 额外样式
   */
  style: {
    type: Object,
    default: () => ({}),
  },
})

// ============================================================
// Store
// ============================================================
const appStore = useAppStore()

// ============================================================
// 计算属性
// ============================================================

/**
 * 内容样式
 */
const contentStyle = computed(() => {
  const styles = { ...props.style }

  // 如果侧边栏固定，内容需要偏移
  if (appStore.fixedSidebar) {
    styles.marginLeft = `${appStore.actualSidebarWidth}px`
  }

  // 如果顶部固定，内容需要偏移
  if (appStore.fixedHeader) {
    styles.marginTop = '60px'
  }

  return styles
})
</script>

<style lang="scss" scoped>
.bx-content {
  min-height: calc(100vh - 60px);
  background-color: $color-bg-page;
  transition: margin-left $transition-duration-base $transition-timing-ease;

  // 尺寸变体
  &--small {
    .bx-content__body {
      padding: $spacing-base;
    }
  }

  &--default {
    .bx-content__body {
      padding: $spacing-lg;
    }
  }

  &--large {
    .bx-content__body {
      padding: $spacing-xl;
    }
  }

  // 充满高度
  &.is-full-height {
    min-height: 100vh;
  }

  // 内边距
  &.is-padding {
    .bx-content__body {
      padding: $spacing-lg;
    }
  }

  // 面包屑
  &__breadcrumb {
    padding: $spacing-md $spacing-lg 0;
  }

  // 页面标题
  &__header {
    padding: $spacing-lg $spacing-lg 0;
  }

  &__title-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding-bottom: $spacing-lg;
    border-bottom: 1px solid $color-border-light;
  }

  &__title-block {
    flex: 1;
  }

  &__title {
    margin: 0;
    font-size: $font-size-2xl;
    font-weight: $font-weight-bold;
    color: $color-text-primary;
    line-height: $line-height-tight;
  }

  &__description {
    margin: $spacing-xs 0 0;
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    flex-shrink: 0;
  }

  // 内容主体
  &__body {
    // 可滚动
    &.is-scroll {
      overflow-y: auto;
      max-height: calc(100vh - 200px);
    }
  }

  // 内容底部
  &__footer {
    padding: $spacing-lg;
    border-top: 1px solid $color-border-light;
    background-color: $color-white;
  }
}
</style>
