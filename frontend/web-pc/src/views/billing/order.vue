<template>
  <bx-content
    title="订单记录"
    description="查看充值和套餐购买订单，管理您的交易记录"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 筛选区域 -->
    <div class="order__filter">
      <div class="order__filter-left">
        <el-select
          v-model="filterOrderType"
          placeholder="订单类型"
          clearable
          style="width: 140px"
          @change="handleFilterChange"
        >
          <el-option label="全部" value="" />
          <el-option label="充值订单" :value="1" />
          <el-option label="套餐购买" :value="2" />
        </el-select>
        <el-select
          v-model="filterPayStatus"
          placeholder="支付状态"
          clearable
          style="width: 140px"
          @change="handleFilterChange"
        >
          <el-option label="全部" value="" />
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已退款" :value="2" />
          <el-option label="已取消" :value="3" />
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
      <div class="order__filter-right">
        <el-input
          v-model="searchOrderNo"
          placeholder="搜索订单号"
          prefix-icon="el-icon-search"
          clearable
          style="width: 220px"
          @change="handleFilterChange"
        />
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="order__table">
      <el-table v-loading="loading" :data="orderList" stripe>
        <el-table-column label="订单号" prop="orderNo" width="180" align="center">
          <template #default="{ row }">
            <span class="order__no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.orderType === 1 ? 'primary' : 'success'" size="small">
              {{ row.orderType === 1 ? '充值' : '套餐购买' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="商品信息" min-width="200">
          <template #default="{ row }">
            <div class="order__product">
              <span class="order__product-name">{{ row.productName }}</span>
              <span v-if="row.giftPoints > 0" class="order__product-gift">
                赠 {{ row.giftPoints }} 积分
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="支付金额" width="120" align="center">
          <template #default="{ row }">
            <span class="order__amount">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="获得积分" width="120" align="center">
          <template #default="{ row }">
            <span class="order__points">+{{ row.points }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="100" align="center">
          <template #default="{ row }">
            <div class="order__pay-channel">
              <i :class="getPayChannelIcon(row.payChannel)" />
              <span>{{ getPayChannelLabel(row.payChannel) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              link
              size="small"
              @click="handlePay(row)"
            >
              去支付
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              link
              size="small"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="order__pagination">
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

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="560px">
      <div v-if="currentOrder" class="order__detail">
        <div class="order__detail-section">
          <h4 class="order__detail-title">订单信息</h4>
          <div class="order__detail-item">
            <span class="order__detail-label">订单号</span>
            <span class="order__detail-value">{{ currentOrder.orderNo }}</span>
          </div>
          <div class="order__detail-item">
            <span class="order__detail-label">订单类型</span>
            <el-tag :type="currentOrder.orderType === 1 ? 'primary' : 'success'" size="small">
              {{ currentOrder.orderType === 1 ? '充值订单' : '套餐购买' }}
            </el-tag>
          </div>
          <div class="order__detail-item">
            <span class="order__detail-label">订单状态</span>
            <el-tag :type="getStatusType(currentOrder.status)" size="small">
              {{ getStatusLabel(currentOrder.status) }}
            </el-tag>
          </div>
          <div class="order__detail-item">
            <span class="order__detail-label">创建时间</span>
            <span class="order__detail-value">{{ currentOrder.createTime }}</span>
          </div>
        </div>

        <div class="order__detail-section">
          <h4 class="order__detail-title">商品信息</h4>
          <div class="order__detail-item">
            <span class="order__detail-label">商品名称</span>
            <span class="order__detail-value">{{ currentOrder.productName }}</span>
          </div>
          <div class="order__detail-item">
            <span class="order__detail-label">支付金额</span>
            <span class="order__detail-value order__detail-value--amount">
              ¥{{ currentOrder.amount }}
            </span>
          </div>
          <div class="order__detail-item">
            <span class="order__detail-label">获得积分</span>
            <span class="order__detail-value order__detail-value--points">
              +{{ currentOrder.points }}
            </span>
          </div>
          <div v-if="currentOrder.giftPoints > 0" class="order__detail-item">
            <span class="order__detail-label">赠送积分</span>
            <span class="order__detail-value order__detail-value--gift">
              +{{ currentOrder.giftPoints }}
            </span>
          </div>
        </div>

        <div class="order__detail-section">
          <h4 class="order__detail-title">支付信息</h4>
          <div class="order__detail-item">
            <span class="order__detail-label">支付方式</span>
            <span class="order__detail-value">{{ getPayChannelLabel(currentOrder.payChannel) }}</span>
          </div>
          <div v-if="currentOrder.payTime" class="order__detail-item">
            <span class="order__detail-label">支付时间</span>
            <span class="order__detail-value">{{ currentOrder.payTime }}</span>
          </div>
          <div v-if="currentOrder.tradeNo" class="order__detail-item">
            <span class="order__detail-label">交易流水号</span>
            <span class="order__detail-value">{{ currentOrder.tradeNo }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 支付二维码弹窗 -->
    <el-dialog
      v-model="qrDialogVisible"
      title="扫码支付"
      width="400px"
      :close-on-click-modal="false"
      @close="stopPolling"
    >
      <div class="order__qr-dialog">
        <div class="order__qr-amount">
          <span class="order__qr-label">支付金额</span>
          <span class="order__qr-price">¥{{ currentOrder?.amount || 0 }}</span>
        </div>
        <div class="order__qr-container">
          <div v-if="qrCodeUrl" class="order__qr-image">
            <img :src="qrCodeUrl" alt="支付二维码" />
          </div>
          <div v-else class="order__qr-loading">
            <el-loading />
            <p>正在生成支付二维码...</p>
          </div>
        </div>
        <div class="order__qr-tips">
          <p>请使用{{ currentOrder?.payChannel === 'wechat' ? '微信' : '支付宝' }}扫一扫</p>
          <p>完成支付后自动到账</p>
        </div>
      </div>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview Order 订单记录页
 * @description 查看和管理充值、套餐购买订单
 * @author EMP-FE-003 李明
 */
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as billingApi from '@/api/billing.js'

// ============================================================
// 状态
// ============================================================
const loading = ref(false)

// 订单列表
const orderList = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// 筛选条件
const filterOrderType = ref('')
const filterPayStatus = ref('')
const dateRange = ref([])
const searchOrderNo = ref('')

// 详情弹窗
const detailDialogVisible = ref(false)
const currentOrder = ref(null)

// 支付弹窗
const qrDialogVisible = ref(false)
const qrCodeUrl = ref('')
let pollingTimer = null

// ============================================================
// 状态映射
// ============================================================
const statusMap = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已退款', type: 'info' },
  3: { label: '已取消', type: 'danger' },
}

const payChannelMap = {
  wechat: { label: '微信支付', icon: 'el-icon-chat-dot-round' },
  alipay: { label: '支付宝', icon: 'el-icon-wallet' },
  unionpay: { label: '对公转账', icon: 'el-icon-bank-card' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载订单列表
 */
async function loadOrders() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
    }

    if (filterOrderType.value) {
      params.orderType = filterOrderType.value
    }
    if (filterPayStatus.value !== '') {
      params.payStatus = filterPayStatus.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (searchOrderNo.value) {
      params.orderNo = searchOrderNo.value
    }

    const response = await billingApi.getOrderList(params)
    orderList.value = response.data?.list || []
    pagination.total = response.data?.pagination?.total || 0
  } catch (error) {
    console.error('[Order] Load orders failed:', error)
    orderList.value = []
    pagination.total = 0
    ElMessage.error('加载订单数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 获取状态标签
 */
function getStatusLabel(status) {
  return statusMap[status]?.label || '-'
}

/**
 * 获取状态样式
 */
function getStatusType(status) {
  return statusMap[status]?.type || 'info'
}

/**
 * 获取支付渠道标签
 */
function getPayChannelLabel(channel) {
  return payChannelMap[channel]?.label || channel
}

/**
 * 获取支付渠道图标
 */
function getPayChannelIcon(channel) {
  return payChannelMap[channel]?.icon || 'el-icon-money'
}

/**
 * 筛选变化
 */
function handleFilterChange() {
  pagination.page = 1
  loadOrders()
}

/**
 * 分页变化
 */
function handlePageChange(page) {
  pagination.page = page
  loadOrders()
}

/**
 * 每页数量变化
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadOrders()
}

/**
 * 查看详情
 */
function handleView(row) {
  currentOrder.value = row
  detailDialogVisible.value = true
}

/**
 * 去支付
 */
function handlePay(row) {
  currentOrder.value = row
  qrCodeUrl.value = '' // 这里应该调用API获取支付二维码
  qrDialogVisible.value = true
  startPolling()
}

/**
 * 开始轮询订单状态
 */
function startPolling() {
  stopPolling()
  pollingTimer = setInterval(async () => {
    if (!currentOrder.value?.orderNo) return
    try {
      const response = await billingApi.getOrderStatus(currentOrder.value.orderNo)
      if (response.data?.status === 1) {
        ElMessage.success('支付成功！')
        stopPolling()
        qrDialogVisible.value = false
        loadOrders()
      }
    } catch (error) {
      console.error('[Order] Poll status failed:', error)
    }
  }, 3000)
}

/**
 * 停止轮询
 */
function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

/**
 * 取消订单
 */
async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
    await billingApi.cancelOrder(row.id)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[Order] Cancel failed:', error)
      ElMessage.error('取消失败')
    }
  }
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadOrders()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style lang="scss" scoped>
// 筛选区域
.order__filter {
  @include flex-between;
  margin-bottom: $spacing-lg;

  &-left {
    display: flex;
    gap: $spacing-sm;
  }
}

// 表格
.order__table {
  background: $color-white;
  border-radius: $radius-lg;
  padding: $spacing-lg;
}

.order__no {
  font-family: $font-family-code;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.order__product {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.order__product-name {
  font-weight: $font-weight-medium;
  color: $color-text-primary;
}

.order__product-gift {
  font-size: $font-size-xs;
  color: $color-success;
}

.order__amount {
  font-weight: $font-weight-semibold;
  color: $color-danger;
}

.order__points {
  color: $color-success;
  font-weight: $font-weight-semibold;
}

.order__pay-channel {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $color-text-secondary;

  i {
    font-size: 16px;
  }
}

// 分页
.order__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-lg;
}

// 详情
.order__detail {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.order__detail-section {
  padding: $spacing-lg;
  background: $color-gray-50;
  border-radius: $radius-lg;
}

.order__detail-title {
  margin: 0 0 $spacing-base 0;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  padding-bottom: $spacing-sm;
  border-bottom: 1px solid $color-border-light;
}

.order__detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;

  &:last-child {
    margin-bottom: 0;
  }
}

.order__detail-label {
  color: $color-text-secondary;
  font-size: $font-size-sm;
}

.order__detail-value {
  color: $color-text-primary;
  font-weight: $font-weight-medium;

  &--amount {
    color: $color-danger;
    font-size: $font-size-lg;
  }

  &--points {
    color: $color-success;
    font-weight: $font-weight-bold;
  }

  &--gift {
    color: $color-warning;
  }
}

// 支付弹窗
.order__qr-dialog {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-lg;
}

.order__qr-amount {
  text-align: center;
  margin-bottom: $spacing-lg;
}

.order__qr-label {
  display: block;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-bottom: $spacing-xs;
}

.order__qr-price {
  display: block;
  font-size: 32px;
  font-weight: $font-weight-bold;
  color: $color-danger;
}

.order__qr-container {
  width: 200px;
  height: 200px;
  margin-bottom: $spacing-lg;
  border: 1px solid $color-border-base;
  border-radius: $radius-md;
  overflow: hidden;
}

.order__qr-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 180px;
    height: 180px;
  }
}

.order__qr-loading {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: $color-text-secondary;
}

.order__qr-tips {
  text-align: center;
  color: $color-text-secondary;
  font-size: $font-size-sm;

  p {
    margin: $spacing-xs 0;
  }
}
</style>
