/**
 * @fileoverview 表格数据组合式函数
 * @description 封装表格数据加载、分页、筛选、排序等常见逻辑
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, computed, watch } from 'vue'

/**
 * 表格数据组合式函数
 * @param {object} options - 配置选项
 * @param {Function} options.fetchFn - 数据获取函数，接收 { page, size, ...filters } 参数
 * @param {object} options.defaultFilters - 默认筛选条件
 * @param {number} options.defaultPageSize - 默认每页数量
 * @returns {object} 表格相关状态和方法
 */
export function useTable(options = {}) {
  const { fetchFn, defaultFilters = {}, defaultPageSize = 10 } = options

  // ============================================================
  // 状态
  // ============================================================

  // 加载状态
  const loading = ref(false)
  // 表格数据
  const tableData = ref([])
  // 总数据量
  const total = ref(0)

  // 分页状态
  const pagination = reactive({
    page: 1, // 当前页码
    size: defaultPageSize, // 每页数量
  })

  // 筛选条件
  const filters = reactive({ ...defaultFilters })

  // 排序状态
  const sortState = reactive({
    prop: '', // 排序列
    order: '', // 排序方向：ascending / descending
  })

  // 选中行
  const selectedRows = ref([])

  // ============================================================
  // 计算属性
  // ============================================================

  /** 是否为空 */
  const isEmpty = computed(() => tableData.value.length === 0 && !loading.value)

  /** 是否有数据 */
  const hasData = computed(() => tableData.value.length > 0)

  /** 是否有多选 */
  const isMultiSelect = computed(() => selectedRows.value.length > 1)

  /** 选中的 ID 列表 */
  const selectedIds = computed(() => selectedRows.value.map((row) => row.id))

  /** 当前页数据量 */
  const currentPageDataCount = computed(() => tableData.value.length)

  /** 总页数 */
  const totalPages = computed(() => Math.ceil(total.value / pagination.size))

  // ============================================================
  // 方法
  // ============================================================

  /**
   * 加载数据（从第一页开始）
   * @returns {Promise}
   */
  async function loadData() {
    if (!fetchFn) {
      console.warn('[useTable] fetchFn is not provided')
      return
    }

    loading.value = true
    try {
      const params = {
        page: pagination.page,
        size: pagination.size,
        ...filters,
      }

      // 添加排序参数
      if (sortState.prop && sortState.order) {
        params.sortBy = sortState.prop
        params.sortOrder = sortState.order === 'ascending' ? 'asc' : 'desc'
      }

      // 调用数据获取函数
      const response = await fetchFn(params)

      // 处理响应数据
      if (response) {
        // 兼容两种数据格式
        if (response.data) {
          // { data: { list, pagination } }
          tableData.value = response.data.list || response.data || []
          if (response.data.pagination) {
            total.value = response.data.pagination.total || 0
            pagination.page = response.data.pagination.page || 1
            pagination.size = response.data.pagination.size || defaultPageSize
          }
        } else if (Array.isArray(response)) {
          // 直接是数组
          tableData.value = response
          total.value = response.length
        }
      }
    } catch (error) {
      console.error('[useTable] Load data failed:', error)
      tableData.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新数据（保持当前页）
   */
  async function refresh() {
    await loadData()
  }

  /**
   * 重置并重新加载
   */
  async function reset() {
    // 重置分页
    pagination.page = 1
    pagination.size = defaultPageSize

    // 重置筛选条件
    Object.keys(filters).forEach((key) => {
      filters[key] = defaultFilters[key] ?? null
    })

    // 重置排序
    sortState.prop = ''
    sortState.order = ''

    // 重置选中
    selectedRows.value = []

    // 重新加载
    await loadData()
  }

  /**
   * 切换页码
   * @param {number} page - 页码
   */
  function onPageChange(page) {
    pagination.page = page
    loadData()
  }

  /**
   * 切换每页数量
   * @param {number} size - 每页数量
   */
  function onSizeChange(size) {
    pagination.size = size
    pagination.page = 1 // 切换每页数量时重置到第一页
    loadData()
  }

  /**
   * 排序变化
   * @param {object} { prop, order } - 排序列和排序方向
   */
  function onSortChange({ prop, order }) {
    sortState.prop = prop
    sortState.order = order
    loadData()
  }

  /**
   * 选择变化
   * @param {array} selection - 选中的行
   */
  function onSelectionChange(selection) {
    selectedRows.value = selection
  }

  /**
   * 清空选择
   */
  function clearSelection() {
    selectedRows.value = []
  }

  /**
   * 设置筛选条件
   * @param {object} newFilters - 新的筛选条件
   * @param {boolean} [reload=true] - 是否立即重新加载
   */
  function setFilters(newFilters, reload = true) {
    Object.assign(filters, newFilters)
    if (reload) {
      pagination.page = 1
      loadData()
    }
  }

  /**
   * 清除单个筛选条件
   * @param {string} key - 筛选字段
   * @param {boolean} [reload=true] - 是否立即重新加载
   */
  function clearFilter(key, reload = true) {
    filters[key] = defaultFilters[key] ?? null
    if (reload) {
      pagination.page = 1
      loadData()
    }
  }

  /**
   * 设置分页
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   */
  function setPage(page, size) {
    if (page !== undefined) pagination.page = page
    if (size !== undefined) pagination.size = size
    loadData()
  }

  // ============================================================
  // 监听
  // ============================================================

  // 当筛选条件变化时，自动重新加载
  watch(
    filters,
    () => {
      // 如果是深度变化，需要手动触发
    },
    { deep: true }
  )

  // ============================================================
  // 返回
  // ============================================================
  return {
    // 状态
    loading,
    tableData,
    total,
    pagination,
    filters,
    sortState,
    selectedRows,

    // 计算属性
    isEmpty,
    hasData,
    isMultiSelect,
    selectedIds,
    currentPageDataCount,
    totalPages,

    // 方法
    loadData,
    refresh,
    reset,
    onPageChange,
    onSizeChange,
    onSortChange,
    onSelectionChange,
    clearSelection,
    setFilters,
    clearFilter,
    setPage,
  }
}

/**
 * 分页配置默认值
 */
export const defaultPaginationConfig = {
  // 默认每页选项
  pageSizes: [10, 20, 50, 100],
  // 默认每页数量
  defaultPageSize: 10,
  // 布局
  layout: 'total, sizes, prev, pager, next, jumper',
}

export default useTable
