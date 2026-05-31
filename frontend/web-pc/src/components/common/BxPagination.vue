<template>
  <!--
    BxPagination 组件 - 北极星分页组件
    基于 Element Plus el-pagination 封装
  -->
  <el-pagination
    :current-page="currentPage"
    :page-size="pageSize"
    :page-sizes="pageSizes"
    :total="total"
    :layout="layout"
    :background="background"
    :small="small"
    :pager-count="pagerCount"
    :prev-text="prevText"
    :next-text="nextText"
    :disabled="disabled"
    :class="[
      'bx-pagination',
      `bx-pagination--${size}`,
      {
        'bx-pagination--simple': isSimple,
      },
    ]"
    @size-change="handleSizeChange"
    @current-change="handleCurrentChange"
  />
</template>

<script setup>
/**
 * @fileoverview BxPagination 分页组件
 * @description 统一的分页组件封装
 * @author EMP-FE-001 张婷
 */
import { computed } from 'vue'

// ============================================================
// Props
// ============================================================
const props = defineProps({
  /**
   * 当前页码
   */
  modelValue: {
    type: Number,
    default: 1,
  },

  /**
   * 每页数量
   */
  pageSize: {
    type: Number,
    default: 10,
  },

  /**
   * 每页数量选项
   */
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100],
  },

  /**
   * 总数据量
   */
  total: {
    type: Number,
    default: 0,
  },

  /**
   * 布局
   */
  layout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper',
  },

  /**
   * 背景色
   */
  background: {
    type: Boolean,
    default: true,
  },

  /**
   * 小型分页
   */
  small: {
    type: Boolean,
    default: false,
  },

  /**
   * 尺寸
   */
  size: {
    type: String,
    default: 'default',
    validator: (val) => ['large', 'default', 'small'].includes(val),
  },

  /**
   * 页码按钮数量
   */
  pagerCount: {
    type: Number,
    default: 7,
  },

  /**
   * 上一页文本
   */
  prevText: {
    type: String,
    default: '',
  },

  /**
   * 下一页文本
   */
  nextText: {
    type: String,
    default: '',
  },

  /**
   * 是否禁用
   */
  disabled: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否简洁模式
   */
  simple: {
    type: Boolean,
    default: false,
  },
})

// ============================================================
// Emits
// ============================================================
const emit = defineEmits(['update:modelValue', 'update:pageSize', 'change', 'page-change', 'size-change'])

// ============================================================
// 计算属性
// ============================================================
const currentPage = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const isSimple = computed(() => props.simple)

// ============================================================
// 方法
// ============================================================

function handleSizeChange(size) {
  emit('update:pageSize', size)
  emit('size-change', size)
  // 重置到第一页
  emit('update:modelValue', 1)
  emit('page-change', 1)
  emit('change', { page: 1, size })
}

function handleCurrentChange(page) {
  emit('update:modelValue', page)
  emit('page-change', page)
  emit('change', { page, size: props.pageSize })
}
</script>

<style lang="scss" scoped>
.bx-pagination {
  display: flex;
  justify-content: flex-end;

  // 尺寸变体
  &--large {
    :deep(.el-pagination__total) {
      font-size: $font-size-base;
    }
  }

  &--small {
    :deep(.el-pagination__total) {
      font-size: $font-size-xs;
    }
  }

  // 简洁模式
  &--simple {
    :deep(.el-pagination__goto) {
      display: none;
    }
  }
}
</style>
