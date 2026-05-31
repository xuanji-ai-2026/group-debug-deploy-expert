<template>
  <div class="admin-search" :class="{ inline: inline }">
    <el-form
      ref="formRef"
      :model="modelValue"
      :inline="inline"
      :label-width="labelWidth"
      class="search-form"
    >
      <!-- 搜索字段插槽：外部传入的搜索表单项 -->
      <slot />

      <!-- 操作按钮区域 -->
      <el-form-item class="search-actions">
        <!-- 搜索按钮 -->
        <el-button type="primary" :icon="Search" @click="handleSearch" :loading="loading">
          搜索
        </el-button>
        <!-- 重置按钮 -->
        <el-button :icon="Refresh" @click="handleReset">
          重置
        </el-button>
        <!-- 扩展插槽 -->
        <slot name="extra" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
/**
 * AdminSearch.vue - 通用搜索栏组件
 * 功能：封装搜索表单，提供统一的搜索、重置操作
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  // 搜索表单数据（v-model）
  modelValue: {
    type: Object,
    default: () => ({})
  },
  // 是否行内布局
  inline: {
    type: Boolean,
    default: true
  },
  // 表单标签宽度
  labelWidth: {
    type: String,
    default: '100px'
  },
  // 搜索按钮加载状态
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'search', 'reset'])

const formRef = ref(null)

/**
 * 搜索：收集表单数据并通知父组件
 */
function handleSearch() {
  emit('search', { ...props.modelValue })
}

/**
 * 重置：清空表单数据并重新搜索
 */
function handleReset() {
  // 重置为初始值
  formRef.value?.resetFields()
  emit('reset')
}
</script>

<style lang="scss" scoped>
.admin-search {
  background-color: $bg-color;
  padding: $spacing-base $spacing-base $spacing-sm;
  border-radius: $border-radius-base;
  margin-bottom: $spacing-base;

  // 行内布局时去除多余的 padding
  &.inline {
    padding-bottom: 0;
  }
}

.search-actions {
  margin-bottom: 0;
}
</style>
