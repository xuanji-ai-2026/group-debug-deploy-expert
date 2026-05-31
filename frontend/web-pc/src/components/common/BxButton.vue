<template>
  <!--
    BxButton 组件 - 北极星按钮组件
    基于 Element Plus el-button 封装的业务按钮组件
  -->
  <el-button
    :type="realType"
    :size="realSize"
    :disabled="disabled || loading"
    :loading="loading"
    :icon="computedIcon"
    :plain="plain"
    :round="round"
    :circle="circle"
    :autofocus="autofocus"
    :native-type="nativeType"
    :class="[
      'bx-button',
      `bx-button--${realType}`,
      `bx-button--${realSize}`,
      {
        'bx-button--loading': loading,
        'bx-button--icon-only': iconOnly,
        'bx-button--block': block,
      },
    ]"
    @click="handleClick"
  >
    <!-- 加载中的插槽 -->
    <template v-if="loading && $slots.loading">
      <slot name="loading" />
    </template>
    <!-- 默认插槽 -->
    <template v-else>
      <slot />
    </template>
  </el-button>
</template>

<script setup>
/**
 * @fileoverview BxButton 按钮组件
 * @description 统一的按钮封装，支持多种类型、尺寸、状态和图标
 * @author EMP-FE-001 张婷
 */
import { computed } from 'vue'

// ============================================================
// Props 定义
// ============================================================
const props = defineProps({
  /**
   * 按钮类型
   * @values primary, success, warning, danger, info, default, text
   */
  type: {
    type: String,
    default: 'default',
    validator: (val) =>
      ['primary', 'success', 'warning', 'danger', 'info', 'default', 'text'].includes(val),
  },

  /**
   * 按钮尺寸
   * @values large, default, small, small
   */
  size: {
    type: String,
    default: 'default',
    validator: (val) => ['large', 'default', 'small'].includes(val),
  },

  /**
   * 是否朴素按钮（背景透明，边框）
   */
  plain: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否圆角按钮
   */
  round: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否圆形按钮
   */
  circle: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否加载中
   */
  loading: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否禁用
   */
  disabled: {
    type: Boolean,
    default: false,
  },

  /**
   * 图标名称（Element Plus 图标）
   */
  icon: {
    type: String,
    default: '',
  },

  /**
   * 原生 type 属性
   */
  nativeType: {
    type: String,
    default: 'button',
  },

  /**
   * 自动聚焦
   */
  autofocus: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否为块级按钮（宽度100%）
   */
  block: {
    type: Boolean,
    default: false,
  },
})

// ============================================================
// Emits
// ============================================================
const emit = defineEmits(['click'])

// ============================================================
// 计算属性
// ============================================================

/**
 * 实际类型（text 类型没有 loading 状态）
 */
const realType = computed(() => {
  if (props.type === 'text') return 'text'
  if (props.loading) return 'info' // 加载中时降级为 info
  return props.type
})

/**
 * 实际尺寸
 */
const realSize = computed(() => {
  return props.size
})

/**
 * 计算图标
 */
const computedIcon = computed(() => {
  if (props.icon) return props.icon
  return ''
})

/**
 * 是否仅图标按钮（无文字内容）
 */
const iconOnly = computed(() => {
  // 检查是否有默认插槽内容
  return false // 简化处理，实际可通过 slots 判断
})

// ============================================================
// 方法
// ============================================================

/**
 * 点击处理
 */
function handleClick(event) {
  if (props.disabled || props.loading) {
    event.preventDefault()
    return
  }
  emit('click', event)
}
</script>

<style lang="scss" scoped>
/**
 * 按钮样式覆盖
 */

// 基础样式
.bx-button {
  font-weight: $font-weight-medium;
  transition: all $transition-duration-fast $transition-timing-ease;

  // 图标按钮样式
  &--icon-only {
    padding: 8px;
    min-width: 32px;
    min-height: 32px;
  }

  // 块级按钮
  &--block {
    display: block;
    width: 100%;
  }

  // 加载中样式
  &--loading {
    opacity: 0.8;
    cursor: wait;
  }

  // 类型样式
  &--primary {
    &.is-plain {
      background-color: rgba($color-primary, 0.1);
      border-color: rgba($color-primary, 0.3);
      color: $color-primary;

      &:hover,
      &:focus {
        background-color: rgba($color-primary, 0.2);
        border-color: $color-primary;
        color: $color-primary;
      }
    }
  }

  &--success {
    &.is-plain {
      background-color: rgba($color-success, 0.1);
      border-color: rgba($color-success, 0.3);
      color: $color-success;
    }
  }

  &--warning {
    &.is-plain {
      background-color: rgba($color-warning, 0.1);
      border-color: rgba($color-warning, 0.3);
      color: $color-warning;
    }
  }

  &--danger {
    &.is-plain {
      background-color: rgba($color-danger, 0.1);
      border-color: rgba($color-danger, 0.3);
      color: $color-danger;
    }
  }

  // 文字按钮
  &--text {
    padding: 0;
    border: none;
    background: none;

    &:hover,
    &:focus {
      background: none;
      color: $color-primary;
    }

    &.is-disabled {
      color: $color-text-disabled;
      background: none;
    }
  }
}
</style>
