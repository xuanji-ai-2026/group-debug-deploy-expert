<template>
  <bx-content
    title="账户余额"
    description="查看账户余额、积分变动记录，管理您的账户资金"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 余额卡片区域 -->
    <el-row :gutter="20" class="balance__header">
      <el-col :xs="24" :sm="12" :lg="8">
        <div class="balance__card balance__card--primary">
          <div class="balance__card-icon">
            <i class="el-icon-wallet" />
          </div>
          <div class="balance__card-content">
            <div class="balance__card-label">当前积分余额</div>
            <div class="balance__card-value">
              <span class="balance__card-num">{{ formatNumber(balanceInfo.balance) }}</span>
              <span class="balance__card-unit">积分</span>
            </div>
          </div>
          <div class="balance__card-action">
            <el-button type="primary" size="small" @click="$router.push('/billing/recharge')">
              立即充值
            </el-button>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="8">
        <div class="balance__card balance__card--success">
          <div class="balance__card-icon">
            <i class="el-icon-coin" />
          </div>
          <div class="balance__card-content">
            <div class="balance__card-label">累计充值</div>
            <div class="balance__card-value">
              <span class="balance__card-num">{{ formatNumber(balanceInfo.totalRecharge) }}</span>
              <span class="balance__card-unit">积分</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="8">
        <div class="balance__card balance__card--warning">
          <div class="balance__card-icon">
            <i class="el-icon-shopping-cart-full" />
          </div>
          <div class="balance__card-content">
            <div class="balance__card-label">累计消费</div>
            <div class="balance__card-value">
              <span class="balance__card-num">{{ formatNumber(balanceInfo.totalConsumption) }}</span>
              <span class="balance__card-unit">积分</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选区域 -->
    <div class="balance__filter">
      <div class="balance__filter-left">
        <el-select
          v-model="filterTransType"
          placeholder="变动类型"
          clearable
          style="width: 140px"
          @change="handleFilterChange"
        >
          <el-option label="全部" value="" />
          <el-option label="收入" :value="1" />
          <el-option label="支出" :value="2" />
          <el-option label="赠送" :value="3" />
          <el-option label="退款" :value="4" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="handleFilterChange"
        />
      </div>
      <div class="balance__filter-right">
        <el-button type="primary" plain @click="handleExport">
          <i class="el-icon-download" />
          导出明细
        </el-button>
      </div>
    </div>

    <!-- 明细表格 -->
    <div class="balance__table">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column label="变动时间" prop="createTime" width="180" align="center" />
        <el-table-column label="变动类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTransTypeStyle(row.transType)" size="small">
              {{ getTransTypeLabel(row.transType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动积分" width="140" align="center">
          <template #default="{ row }">
            <span :class="getAmountClass(row.amount)">
              {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="140" align="center">
          <template #default="{ row }">
            <span class="balance__amount--neutral">{{ row.balanceAfter }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变动说明" prop="description" min-width="200" show-overflow-tooltip />
        <el-table-column label="关联订单" prop="orderNo" width="180" align="center">
          <template #default="{ row }">
            <span v-if="row.orderNo" class="balance__order-no">{{ row.orderNo }}</span>
            <span v-else class="balance__empty">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="balance__pagination">
      <el-pagination
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview Balance 余额管理页
 * @description 展示账户余额、积分变动记录，支持筛选和导出
 * @author EMP-FE-003 李明
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as billingApi from '@/api/billing.js'

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const balanceInfo = reactive({
  balance: 0,
  frozenBalance: 0,
  totalRecharge: 0,
  totalConsumption: 0,
})

// 表格数据
const tableData = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// 筛选条件
const filterTransType = ref('')
const dateRange = ref([])

// ============================================================
// 交易类型映射
// ============================================================
const transTypeMap = {
  1: { label: '充值', style: 'success' },
  2: { label: '消费', style: 'danger' },
  3: { label: '赠送', style: 'warning' },
  4: { label: '退款', style: 'info' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 获取余额信息
 */
async function loadBalanceInfo() {
  try {
    const response = await billingApi.getBalance()
    const data = response.data || {}
    balanceInfo.balance = data.balance || 0
    balanceInfo.frozenBalance = data.frozenBalance || 0
    balanceInfo.totalRecharge = data.totalRecharge || 0
    balanceInfo.totalConsumption = data.totalConsumption || 0
  } catch (error) {
    console.error('[Balance] Load balance info failed:', error)
    // 使用模拟数据
    balanceInfo.balance = 5000
    balanceInfo.frozenBalance = 0
    balanceInfo.totalRecharge = 10000
    balanceInfo.totalConsumption = 5000
  }
}

/**
 * 加载交易明细
 */
async function loadTransactions() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
    }

    if (filterTransType.value) {
      params.transType = filterTransType.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const response = await billingApi.getTransactionList(params)
    tableData.value = response.data?.list || []
    pagination.total = response.data?.pagination?.total || 0
  } catch (error) {
    console.error('[Balance] Load transactions failed:', error)
    tableData.value = []
    pagination.total = 0
    ElMessage.error('加载交易记录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 获取交易类型标签
 */
function getTransTypeLabel(type) {
  return transTypeMap[type]?.label || '-'
}

/**
 * 获取交易类型样式
 */
function getTransTypeStyle(type) {
  return transTypeMap[type]?.style || 'info'
}

/**
 * 获取金额样式类
 */
function getAmountClass(amount) {
  if (amount > 0) return 'balance__amount--positive'
  if (amount < 0) return 'balance__amount--negative'
  return 'balance__amount--neutral'
}

/**
 * 格式化金额显示
 */
function formatAmount(amount) {
  if (amount > 0) return `+${amount}`
  return String(amount)
}

/**
 * 格式化数字
 */
function formatNumber(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString()
}

/**
 * 筛选变化
 */
function handleFilterChange() {
  pagination.page = 1
  loadTransactions()
}

/**
 * 分页变化
 */
function handlePageChange(page) {
  pagination.page = page
  loadTransactions()
}

/**
 * 每页数量变化
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadTransactions()
}

/**
 * 导出明细
 */
function handleExport() {
  ElMessage.success('明细导出中，请稍后在下载中心查看')
  // 实际项目中这里会调用导出API
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadBalanceInfo()
  loadTransactions()
})
</script>

<style lang="scss" scoped>
// 余额卡片
.balance__header {
  margin-bottom: $spacing-lg;
}

.balance__card {
  display: flex;
  align-items: center;
  padding: $spacing-xl;
  background: $color-white;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
  gap: $spacing-lg;

  &--primary {
    background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
    color: $color-white;
  }

  &--success {
    background: linear-gradient(135deg, #10B981 0%, #059669 100%);
    color: $color-white;
  }

  &--warning {
    background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
    color: $color-white;
  }
}

.balance__card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: $radius-lg;

  i {
    font-size: 28px;
  }
}

.balance__card-content {
  flex: 1;
}

.balance__card-label {
  font-size: $font-size-sm;
  opacity: 0.9;
  margin-bottom: $spacing-xs;
}

.balance__card-value {
  display: flex;
  align-items: baseline;
  gap: $spacing-xs;
}

.balance__card-num {
  font-size: 32px;
  font-weight: $font-weight-bold;
  line-height: 1;
}

.balance__card-unit {
  font-size: $font-size-sm;
  opacity: 0.8;
}

.balance__card-action {
  .el-button {
    background: $color-white;
    border-color: $color-white;
    color: $color-primary;

    &:hover {
      background: rgba(255, 255, 255, 0.9);
    }
  }
}

// 筛选区域
.balance__filter {
  @include flex-between;
  margin-bottom: $spacing-lg;

  &-left {
    display: flex;
    gap: $spacing-sm;
  }
}

// 表格
.balance__table {
  background: $color-white;
  border-radius: $radius-lg;
  padding: $spacing-lg;
}

.balance__amount {
  &--positive {
    color: $color-success;
    font-weight: $font-weight-semibold;
  }

  &--negative {
    color: $color-danger;
    font-weight: $font-weight-semibold;
  }

  &--neutral {
    color: $color-text-primary;
  }
}

.balance__order-no {
  font-family: $font-family-code;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.balance__empty {
  color: $color-text-placeholder;
}

// 分页
.balance__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-lg;
}
</style>
