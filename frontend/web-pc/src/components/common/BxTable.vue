<template>
  <!--
    BxTable 组件 - 北极星表格组件
    基于 Element Plus el-table 封装，支持统一配置和分页
  -->
  <div class="bx-table-container" :class="{ 'is-bordered': bordered, 'is-striped': striped }">
    <!-- 表格 -->
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="data"
      :columns="internalColumns"
      :border="bordered"
      :stripe="striped"
      :size="size"
      :height="height"
      :max-height="maxHeight"
      :fit="fit"
      :show-header="showHeader"
      :highlight-current-row="highlightCurrentRow"
      :row-class-name="rowClassName"
      :row-style="rowStyle"
      :cell-class-name="cellClassName"
      :cell-style="cellStyle"
      :header-row-class-name="headerRowClassName"
      :header-row-style="headerRowStyle"
      :header-cell-class-name="headerCellClassName"
      :header-cell-style="headerCellStyle"
      :default-sort="defaultSort"
      :tooltip-effect="tooltipEffect"
      :span-method="spanMethod"
      :select-on-indeterminate="selectOnIndeterminate"
      :indent="indent"
      :lazy="lazy"
      :load="load"
      :tree-props="treeProps"
      :class="['bx-table', { 'bx-table--selection': showSelection }]"
      @select="handleSelect"
      @select-all="handleSelectAll"
      @selection-change="handleSelectionChange"
      @cell-mouse-enter="handleCellMouseEnter"
      @cell-mouse-leave="handleCellMouseLeave"
      @cell-click="handleCellClick"
      @cell-dblclick="handleCellDblClick"
      @row-click="handleRowClick"
      @row-contextmenu="handleRowContextmenu"
      @row-dblclick="handleRowDblClick"
      @header-click="handleHeaderClick"
      @header-contextmenu="handleHeaderContextmenu"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
      @current-change="handleCurrentChange"
    >
      <!-- 多选列 -->
      <el-table-column
        v-if="showSelection"
        type="selection"
        :width="selectionWidth"
        :min-width="selectionMinWidth"
        :fixed="selectionFixed"
        :selectable="selectable"
        :reserve-selection="reserveSelection"
        align="center"
      />

      <!-- 序号列 -->
      <el-table-column
        v-if="showIndex"
        type="index"
        :label="indexLabel"
        :width="indexWidth"
        :min-width="indexMinWidth"
        :fixed="indexFixed"
        :index="customIndex"
        align="center"
      />

      <!-- 默认插槽（自定义列） -->
      <slot />

      <!-- 无数据插槽 -->
      <template #empty>
        <slot name="empty">
          <div class="bx-table__empty">
            <svg class="bx-table__empty-icon" viewBox="0 0 64 64" fill="none">
              <rect x="8" y="12" width="48" height="8" rx="2" fill="#D1D5DB" />
              <rect x="8" y="28" width="48" height="8" rx="2" fill="#E5E7EB" />
              <rect x="8" y="44" width="32" height="8" rx="2" fill="#F3F4F6" />
            </svg>
            <p class="bx-table__empty-text">{{ emptyText }}</p>
          </div>
        </slot>
      </template>
    </el-table>

    <!-- 分页 -->
    <div v-if="showPagination && !hidePaginationOnSingle" class="bx-table__pagination">
      <el-pagination
        :current-page="internalPage"
        :page-size="internalPageSize"
        :page-sizes="pageSizes"
        :total="total"
        :layout="paginationLayout"
        :background="paginationBackground"
        :small="paginationSmall"
        :pager-count="pagerCount"
        :prev-text="prevText"
        :next-text="nextText"
        :disabled="paginationDisabled"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
/**
 * @fileoverview BxTable 表格组件
 * @description 基于 Element Plus el-table 的业务封装，支持分页、选择等功能
 * @author EMP-FE-001 张婷
 */
import { ref, computed, watch } from 'vue'

// ============================================================
// Props
// ============================================================
const props = defineProps({
  /**
   * 表格数据
   */
  data: {
    type: Array,
    default: () => [],
  },

  /**
   * 表格列配置
   */
  columns: {
    type: Array,
    default: () => [],
  },

  /**
   * 是否显示加载状态
   */
  loading: {
    type: Boolean,
    default: false,
  },

  /**
   * 表格尺寸
   */
  size: {
    type: String,
    default: 'default',
    validator: (val) => ['large', 'default', 'small'].includes(val),
  },

  /**
   * 是否显示边框
   */
  bordered: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否斑马纹
   */
  striped: {
    type: Boolean,
    default: false,
  },

  /**
   * 表格高度
   */
  height: {
    type: [String, Number],
    default: null,
  },

  /**
   * 最大高度
   */
  maxHeight: {
    type: [String, Number],
    default: null,
  },

  /**
   * 是否自动调整列宽
   */
  fit: {
    type: Boolean,
    default: true,
  },

  /**
   * 是否显示表头
   */
  showHeader: {
    type: Boolean,
    default: true,
  },

  /**
   * 是否高亮当前行
   */
  highlightCurrentRow: {
    type: Boolean,
    default: false,
  },

  /**
   * 默认排序
   */
  defaultSort: {
    type: Object,
    default: null,
  },

  /**
   * tooltip 效果
   */
  tooltipEffect: {
    type: String,
    default: 'dark',
    validator: (val) => ['dark', 'light'].includes(val),
  },

  // ========== 选择相关 ==========

  /**
   * 是否显示多选列
   */
  showSelection: {
    type: Boolean,
    default: false,
  },

  /**
   * 多选列宽度
   */
  selectionWidth: {
    type: [String, Number],
    default: 55,
  },

  /**
   * 多选列最小宽度
   */
  selectionMinWidth: {
    type: [String, Number],
    default: null,
  },

  /**
   * 多选列是否固定
   */
  selectionFixed: {
    type: [String, Boolean],
    default: false,
  },

  /**
   * 行选择器判断函数
   */
  selectable: {
    type: Function,
    default: () => true,
  },

  /**
   * 数据更新时是否保留选择状态
   */
  reserveSelection: {
    type: Boolean,
    default: false,
  },

  // ========== 序号相关 ==========

  /**
   * 是否显示序号列
   */
  showIndex: {
    type: Boolean,
    default: false,
  },

  /**
   * 序号列标签
   */
  indexLabel: {
    type: String,
    default: '#',
  },

  /**
   * 序号列宽度
   */
  indexWidth: {
    type: [String, Number],
    default: 60,
  },

  /**
   * 序号列最小宽度
   */
  indexMinWidth: {
    type: [String, Number],
    default: null,
  },

  /**
   * 序号列是否固定
   */
  indexFixed: {
    type: [String, Boolean],
    default: false,
  },

  /**
   * 自定义序号计算
   */
  customIndex: {
    type: Function,
    default: null,
  },

  // ========== 分页相关 ==========

  /**
   * 是否显示分页
   */
  showPagination: {
    type: Boolean,
    default: true,
  },

  /**
   * 总数据量
   */
  total: {
    type: Number,
    default: 0,
  },

  /**
   * 当前页码
   */
  page: {
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
   * 分页布局
   */
  paginationLayout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper',
  },

  /**
   * 分页背景色
   */
  paginationBackground: {
    type: Boolean,
    default: true,
  },

  /**
   * 分页是否使用小型样式
   */
  paginationSmall: {
    type: Boolean,
    default: false,
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
   * 分页是否禁用
   */
  paginationDisabled: {
    type: Boolean,
    default: false,
  },

  /**
   * 单页时是否隐藏分页
   */
  hidePaginationOnSingle: {
    type: Boolean,
    default: false,
  },

  /**
   * 空数据文本
   */
  emptyText: {
    type: String,
    default: '暂无数据',
  },

  // ========== 懒加载相关 ==========

  /**
   * 是否懒加载
   */
  lazy: {
    type: Boolean,
    default: false,
  },

  /**
   * 懒加载回调
   */
  load: {
    type: Function,
    default: null,
  },

  /**
   * 树形数据配置
   */
  treeProps: {
    type: Object,
    default: () => ({ hasChildren: 'hasChildren', children: 'children' }),
  },

  /**
   * 是否可选择
   */
  selectOnIndeterminate: {
    type: Boolean,
    default: true,
  },

  /**
   * 树形缩进
   */
  indent: {
    type: Number,
    default: 16,
  },

  // 行/单元格样式
  rowClassName: Function,
  rowStyle: [Object, Function],
  cellClassName: [String, Function],
  cellStyle: [Object, Function],
  headerRowClassName: [String, Function],
  headerRowStyle: [Object, Function],
  headerCellClassName: [String, Function],
  headerCellStyle: [Object, Function],
  spanMethod: Function,
})

// ============================================================
// Emits
// ============================================================
const emit = defineEmits([
  'update:page',
  'update:pageSize',
  'select',
  'select-all',
  'selection-change',
  'cell-mouse-enter',
  'cell-mouse-leave',
  'cell-click',
  'cell-dblclick',
  'row-click',
  'row-contextmenu',
  'row-dblclick',
  'header-click',
  'header-contextmenu',
  'sort-change',
  'filter-change',
  'current-change',
  'page-change',
  'size-change',
])

// ============================================================
// 组件引用
// ============================================================
const tableRef = ref(null)

// ============================================================
// 状态
// ============================================================
const internalPage = ref(props.page)
const internalPageSize = ref(props.pageSize)

// ============================================================
// 计算属性
// ============================================================

/**
 * 内部列配置（转换 props.columns）
 */
const internalColumns = computed(() => {
  return props.columns.map((col) => ({
    ...col,
    // 默认居中对齐
    align: col.align || 'left',
  }))
})

// ============================================================
// 监听
// ============================================================
watch(() => props.page, (val) => {
  internalPage.value = val
})

watch(() => props.pageSize, (val) => {
  internalPageSize.value = val
})

// ============================================================
// 方法
// ============================================================

function handleSelect(selection, row) {
  emit('select', selection, row)
}

function handleSelectAll(selection) {
  emit('select-all', selection)
}

function handleSelectionChange(selection) {
  emit('selection-change', selection)
}

function handleCellMouseEnter(row, column, cell, event) {
  emit('cell-mouse-enter', row, column, cell, event)
}

function handleCellMouseLeave(row, column, cell, event) {
  emit('cell-mouse-leave', row, column, cell, event)
}

function handleCellClick(row, column, cell, event) {
  emit('cell-click', row, column, cell, event)
}

function handleCellDblClick(row, column, cell, event) {
  emit('cell-dblclick', row, column, cell, event)
}

function handleRowClick(row, column, event) {
  emit('row-click', row, column, event)
}

function handleRowContextmenu(row, column, event) {
  emit('row-contextmenu', row, column, event)
}

function handleRowDblClick(row, column, event) {
  emit('row-dblclick', row, column, event)
}

function handleHeaderClick(column, event) {
  emit('header-click', column, event)
}

function handleHeaderContextmenu(column, event) {
  emit('header-contextmenu', column, event)
}

function handleSortChange({ column, prop, order }) {
  emit('sort-change', { column, prop, order })
}

function handleFilterChange(filters) {
  emit('filter-change', filters)
}

function handleCurrentChange(val) {
  internalPage.value = val
  emit('update:page', val)
  emit('page-change', val)
}

function handleSizeChange(val) {
  internalPageSize.value = val
  internalPage.value = 1
  emit('update:pageSize', val)
  emit('update:page', 1)
  emit('size-change', val)
  emit('page-change', 1)
}

// ============================================================
// 暴露方法
// ============================================================
defineExpose({
  tableRef,
  clearSelection: () => tableRef.value?.clearSelection(),
  toggleRowSelection: (row, selected) => tableRef.value?.toggleRowSelection(row, selected),
  toggleAllSelection: () => tableRef.value?.toggleAllSelection(),
  setCurrentRow: (row) => tableRef.value?.setCurrentRow(row),
  clearSort: () => tableRef.value?.clearSort(),
  clearFilter: (columnKeys) => tableRef.value?.clearFilter(columnKeys),
  doLayout: () => tableRef.value?.doLayout(),
})
</script>

<style lang="scss" scoped>
.bx-table-container {
  width: 100%;

  // 分页
  .bx-table__pagination {
    display: flex;
    justify-content: flex-end;
    padding: $spacing-lg 0 $spacing-sm;
  }

  // 空状态
  .bx-table__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: $spacing-3xl $spacing-lg;

    .bx-table__empty-icon {
      width: 64px;
      height: 64px;
      margin-bottom: $spacing-base;
      opacity: 0.5;
    }

    .bx-table__empty-text {
      font-size: $font-size-sm;
      color: $color-text-secondary;
    }
  }
}
</style>
