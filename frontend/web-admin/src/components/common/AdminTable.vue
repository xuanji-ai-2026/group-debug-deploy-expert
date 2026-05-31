<template>
  <div class="admin-table-wrapper">
    <!-- 表格区域 -->
    <el-table
      v-loading="loading"
      :data="data"
      :stripe="stripe"
      :border="border"
      :height="height"
      :max-height="maxHeight"
      row-key="id"
      class="admin-table"
      v-bind="$attrs"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
    >
      <!-- 选择框列 -->
      <el-table-column
        v-if="showSelection"
        type="selection"
        width="50"
        align="center"
      />

      <!-- 序号列 -->
      <el-table-column
        v-if="showIndex"
        type="index"
        label="序号"
        width="60"
        align="center"
        :index="indexMethod"
      />

      <!-- 动态渲染表格列 -->
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :align="column.align || 'left'"
        :fixed="column.fixed"
        :sortable="column.sortable"
        :show-overflow-tooltip="column.showOverflowTooltip !== false"
      >
        <!-- 自定义列插槽 -->
        <template #default="{ row }">
          <slot :name="`column-${column.prop}`" :row="row" :column="column">
            <!-- 默认渲染：文字 -->
            <span>{{ row[column.prop] ?? '-' }}</span>
          </slot>
        </template>
      </el-table-column>

      <!-- 操作列（固定在右侧） -->
      <el-table-column
        v-if="actions.length > 0"
        label="操作"
        :width="actionsWidth"
        :fixed="actionsFixed || 'right'"
        align="center"
      >
        <template #default="{ row }">
          <slot name="actions" :row="row">
            <template v-for="action in actions" :key="action.key">
              <!-- 普通按钮 -->
              <el-button
                v-if="!action.hidden"
                :type="action.type || 'primary'"
                :size="action.size || 'small'"
                :link="action.link"
                :disabled="action.disabled"
                :loading="action.loading"
                :icon="action.icon"
                :style="action.style"
                @click="action.handler(row)"
              >
                {{ action.label }}
              </el-button>

              <!-- 分隔符 -->
              <el-divider
                v-if="action.divider"
                direction="vertical"
              />
            </template>
          </slot>
        </template>
      </el-table-column>

      <!-- 无数据插槽 -->
      <template #empty>
        <slot name="empty">
          <div class="table-empty">
            <el-icon size="48" color="#c0c4cc"><WarningFilled /></el-icon>
            <p>{{ emptyText }}</p>
          </div>
        </slot>
      </template>
    </el-table>

    <!-- 分页组件 -->
    <div v-if="showPagination" class="table-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="pageSizes"
        :layout="paginationLayout"
        :background="true"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
/**
 * AdminTable.vue - 通用表格组件
 * 功能：封装 Element Plus Table，提供统一的表格展示能力
 * 特性：
 *   - 支持自定义列配置
 *   - 支持序号、多选
 *   - 支持分页
 *   - 支持自定义操作按钮
 *   - 支持排序
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { ref, watch, computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  // 表格数据
  data: {
    type: Array,
    default: () => []
  },
  // 表格列配置
  // [{ prop, label, width, minWidth, align, fixed, sortable, showOverflowTooltip }]
  columns: {
    type: Array,
    default: () => []
  },
  // 操作按钮配置
  // [{ key, label, type, handler, hidden, disabled, divider }]
  actions: {
    type: Array,
    default: () => []
  },
  // 是否显示斑马纹
  stripe: {
    type: Boolean,
    default: true
  },
  // 是否显示边框
  border: {
    type: Boolean,
    default: false
  },
  // 固定高度（px）
  height: {
    type: [String, Number],
    default: null
  },
  // 最大高度
  maxHeight: {
    type: [String, Number],
    default: null
  },
  // 加载状态
  loading: {
    type: Boolean,
    default: false
  },
  // 是否显示选择框
  showSelection: {
    type: Boolean,
    default: false
  },
  // 是否显示序号
  showIndex: {
    type: Boolean,
    default: true
  },
  // 操作列宽度
  actionsWidth: {
    type: [String, Number],
    default: 180
  },
  // 操作列是否固定
  actionsFixed: {
    type: String,
    default: 'right'
  },
  // 空数据提示文字
  emptyText: {
    type: String,
    default: '暂无数据'
  },

  // ========== 分页相关 ==========
  // 是否显示分页
  showPagination: {
    type: Boolean,
    default: true
  },
  // 当前页码
  page: {
    type: Number,
    default: 1
  },
  // 每页条数
  pageSize: {
    type: Number,
    default: 10
  },
  // 总数据条数
  total: {
    type: Number,
    default: 0
  },
  // 分页布局
  paginationLayout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper'
  },
  // 分页可选条数
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100]
  }
})

const emit = defineEmits([
  'update:page',
  'update:pageSize',
  'page-change',
  'size-change',
  'selection-change',
  'sort-change'
])

// 内部分页状态
const currentPage = ref(props.page)
const pageSize = ref(props.pageSize)

// 同步外部 page/pageSize
watch(() => props.page, (val) => { currentPage.value = val })
watch(() => props.pageSize, (val) => { pageSize.value = val })

/**
 * 序号计算方法
 */
function indexMethod(index) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

/**
 * 页码变更
 */
function handlePageChange(page) {
  currentPage.value = page
  emit('update:page', page)
  emit('page-change', page)
}

/**
 * 每页条数变更
 */
function handleSizeChange(size) {
  pageSize.value = size
  emit('update:pageSize', size)
  emit('size-change', size)
}

/**
 * 选择变更
 */
function handleSelectionChange(selection) {
  emit('selection-change', selection)
}

/**
 * 排序变更
 */
function handleSortChange({ prop, order }) {
  emit('sort-change', { prop, order })
}
</script>

<style lang="scss" scoped>
.admin-table-wrapper {
  width: 100%;
}

.table-empty {
  padding: 40px 0;
  color: $text-secondary;
  text-align: center;

  p {
    margin-top: $spacing-sm;
    font-size: $font-size-base;
  }
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-base;
}
</style>
