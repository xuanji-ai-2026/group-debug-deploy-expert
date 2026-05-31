<template>
  <bx-content
    title="充值中心"
    description="选择充值套餐或自定义金额进行账户充值"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 当前余额展示 -->
    <div class="recharge__balance">
      <div class="recharge__balance-label">当前账户余额</div>
      <div class="recharge__balance-value">
        <span class="recharge__balance-num">{{ formatNumber(currentBalance) }}</span>
        <span class="recharge__balance-unit">积分</span>
      </div>
    </div>

    <!-- 充值套餐选择 -->
    <div class="recharge__section">
      <h3 class="recharge__section-title">
        <i class="el-icon-collection-tag" />
        选择充值套餐
      </h3>
      <el-row :gutter="20" class="recharge__packages">
        <el-col
          v-for="pkg in packageList"
          :key="pkg.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <div
            class="recharge__package-card"
            :class="{
              'is-selected': selectedPackage?.id === pkg.id,
              'is-recommended': pkg.isRecommended,
            }"
            @click="selectPackage(pkg)"
          >
            <div v-if="pkg.isRecommended" class="recharge__package-badge">推荐</div>
            <div class="recharge__package-points">{{ formatNumber(pkg.points) }}</div>
            <div class="recharge__package-unit">积分</div>
            <div class="recharge__package-divider" />
            <div class="recharge__package-price">
              <span class="recharge__package-currency">¥</span>
              <span class="recharge__package-amount">{{ pkg.price }}</span>
            </div>
            <div v-if="pkg.giftPoints > 0" class="recharge__package-gift">
              赠送 {{ formatNumber(pkg.giftPoints) }} 积分
            </div>
            <div class="recharge__package-original" v-if="pkg.originalPrice">
              原价 ¥{{ pkg.originalPrice }}
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 自定义金额 -->
    <div class="recharge__section">
      <h3 class="recharge__section-title">
        <i class="el-icon-edit" />
        自定义充值金额
      </h3>
      <div class="recharge__custom">
        <div class="recharge__custom-input">
          <span class="recharge__custom-currency">¥</span>
          <el-input-number
            v-model="customAmount"
            :min="10"
            :max="100000"
            :step="10"
            :precision="0"
            controls-position="right"
            style="width: 200px"
            @change="handleCustomAmountChange"
          />
          <span class="recharge__custom-hint">元起充，1元 = 10积分</span>
        </div>
        <div v-if="customAmount > 0" class="recharge__custom-preview">
          可获得 <span class="recharge__custom-points">{{ customAmount * 10 }}</span> 积分
        </div>
      </div>
    </div>

    <!-- 支付方式 -->
    <div class="recharge__section">
      <h3 class="recharge__section-title">
        <i class="el-icon-bank-card" />
        选择支付方式
      </h3>
      <div class="recharge__payment-methods">
        <div
          v-for="method in paymentMethods"
          :key="method.value"
          class="recharge__payment-item"
          :class="{ 'is-selected': selectedPayment === method.value }"
          @click="selectPayment(method.value)"
        >
          <div class="recharge__payment-icon">
            <img :src="method.icon" :alt="method.label" />
          </div>
          <div class="recharge__payment-info">
            <div class="recharge__payment-label">{{ method.label }}</div>
            <div class="recharge__payment-desc">{{ method.description }}</div>
          </div>
          <div class="recharge__payment-check">
            <i v-if="selectedPayment === method.value" class="el-icon-check" />
          </div>
        </div>
      </div>
    </div>

    <!-- 订单确认 -->
    <div class="recharge__section">
      <h3 class="recharge__section-title">
        <i class="el-icon-document-checked" />
        订单确认
      </h3>
      <div class="recharge__order">
        <div class="recharge__order-item">
          <span class="recharge__order-label">充值积分</span>
          <span class="recharge__order-value recharge__order-value--primary">
            {{ formatNumber(orderPoints) }} 积分
          </span>
        </div>
        <div class="recharge__order-item">
          <span class="recharge__order-label">支付金额</span>
          <span class="recharge__order-value recharge__order-value--price">
            ¥{{ orderAmount }}
          </span>
        </div>
        <div class="recharge__order-divider" />
        <div class="recharge__order-total">
          <span>实付金额</span>
          <span class="recharge__order-total-price">¥{{ orderAmount }}</span>
        </div>
        <el-button
          type="primary"
          size="large"
          :disabled="!canSubmit"
          :loading="submitting"
          class="recharge__order-submit"
          @click="handleSubmit"
        >
          立即支付
        </el-button>
      </div>
    </div>

    <!-- 充值记录 -->
    <div class="recharge__section">
      <h3 class="recharge__section-title">
        <i class="el-icon-time" />
        最近充值记录
      </h3>
      <el-table v-loading="loading" :data="rechargeRecords" stripe>
        <el-table-column label="订单号" prop="orderNo" width="180" align="center">
          <template #default="{ row }">
            <span class="recharge__record-order">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="充值积分" prop="points" width="120" align="center">
          <template #default="{ row }">
            <span class="recharge__record-points">+{{ formatNumber(row.points) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付金额" prop="amount" width="120" align="center">
          <template #default="{ row }">
            <span>¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付方式" prop="payChannel" width="120" align="center">
          <template #default="{ row }">
            {{ getPaymentLabel(row.payChannel) }}
          </template>
        </el-table-column>
        <el-table-column label="订单状态" prop="status" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180" align="center" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="primary"
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
      <div class="recharge__pagination">
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[5, 10, 20]"
          layout="total, prev, pager, next"
          background
          small
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 支付二维码弹窗 -->
    <el-dialog
      v-model="qrDialogVisible"
      title="扫码支付"
      width="400px"
      :close-on-click-modal="false"
      @close="stopPolling"
    >
      <div class="recharge__qr-dialog">
        <div class="recharge__qr-amount">
          <span class="recharge__qr-label">支付金额</span>
          <span class="recharge__qr-price">¥{{ currentOrder?.amount || 0 }}</span>
        </div>
        <div class="recharge__qr-container">
          <div v-if="qrCodeUrl" class="recharge__qr-image">
            <img :src="qrCodeUrl" alt="支付二维码" />
          </div>
          <div v-else class="recharge__qr-loading">
            <el-loading />
            <p>正在生成支付二维码...</p>
          </div>
        </div>
        <div class="recharge__qr-tips">
          <p>请使用{{ currentOrder?.payChannel === 'wechat' ? '微信' : '支付宝' }}扫一扫</p>
          <p>完成支付后自动到账</p>
        </div>
      </div>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview Recharge 充值中心页
 * @description 支持套餐选择和自定义金额充值，微信支付/支付宝/对公转账
 * @author EMP-FE-003 李明
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as billingApi from '@/api/billing.js'

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const submitting = ref(false)
const currentBalance = ref(5000)

// 套餐列表
const packageList = ref([])
const selectedPackage = ref(null)

// 自定义金额
const customAmount = ref(0)
const isCustom = ref(false)

// 支付方式
const selectedPayment = ref('wechat')
const paymentMethods = [
  {
    value: 'wechat',
    label: '微信支付',
    description: '使用微信扫码支付，即时到账',
    icon: 'https://mp.weixin.qq.com/favicon.ico',
  },
  {
    value: 'alipay',
    label: '支付宝',
    description: '使用支付宝扫码支付，即时到账',
    icon: 'https://www.alipay.com/favicon.ico',
  },
  {
    value: 'unionpay',
    label: '对公转账',
    description: '企业用户专享，支持发票申请',
    icon: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSIjMDA0MDc5Ij48cGF0aCBkPSJNMTIgMkM2LjQ4IDIgMiA2LjQ4IDIgMTJzNC40OCAxMCAxMCAxMCAxMC00LjQ4IDEwLTEwUzE3LjUyIDIgMTIgMnptMCAxOGMtNC40MSAwLTgtMy41OS04LThzMy41OS04IDgtOCA4IDMuNTkgOCA4LTMuNTkgOC04IDh6Ii8+PHBhdGggZD0iTTEyIDZ2MTJsNi02LTYtNnptMCAwbDYgNi02IDZWNnptMCAwbC02IDYgNiA2VjZ6Ii8+PC9zdmc+',
  },
]

// 充值记录
const rechargeRecords = ref([])
const pagination = reactive({
  page: 1,
  size: 5,
  total: 0,
})

// 支付弹窗
const qrDialogVisible = ref(false)
const qrCodeUrl = ref('')
const currentOrder = ref(null)
let pollingTimer = null

// ============================================================
// 计算属性
// ============================================================

// 订单积分
const orderPoints = computed(() => {
  if (selectedPackage.value) {
    return selectedPackage.value.points + (selectedPackage.value.giftPoints || 0)
  }
  if (isCustom.value && customAmount.value > 0) {
    return customAmount.value * 10
  }
  return 0
})

// 订单金额
const orderAmount = computed(() => {
  if (selectedPackage.value) {
    return selectedPackage.value.price
  }
  if (isCustom.value && customAmount.value > 0) {
    return customAmount.value
  }
  return 0
})

// 是否可提交
const canSubmit = computed(() => {
  return orderAmount.value > 0 && selectedPayment.value && !submitting.value
})

// ============================================================
// 状态映射
// ============================================================
const statusMap = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已退款', type: 'info' },
  3: { label: '已取消', type: 'danger' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载套餐列表
 */
async function loadPackages() {
  try {
    const response = await billingApi.getPackageList()
    packageList.value = response.data || []
    
    // 如果没有数据，使用模拟数据
    if (packageList.value.length === 0) {
      packageList.value = [
        { id: 1, points: 1000, price: 100, originalPrice: 120, giftPoints: 100, isRecommended: false },
        { id: 2, points: 3000, price: 280, originalPrice: 360, giftPoints: 400, isRecommended: true },
        { id: 3, points: 5000, price: 450, originalPrice: 600, giftPoints: 800, isRecommended: false },
        { id: 4, points: 10000, price: 850, originalPrice: 1200, giftPoints: 2000, isRecommended: false },
      ]
    }
  } catch (error) {
    console.error('[Recharge] Load packages failed:', error)
    packageList.value = [
      { id: 1, points: 1000, price: 100, originalPrice: 120, giftPoints: 100, isRecommended: false },
      { id: 2, points: 3000, price: 280, originalPrice: 360, giftPoints: 400, isRecommended: true },
      { id: 3, points: 5000, price: 450, originalPrice: 600, giftPoints: 800, isRecommended: false },
      { id: 4, points: 10000, price: 850, originalPrice: 1200, giftPoints: 2000, isRecommended: false },
    ]
  }
}

/**
 * 加载充值记录
 */
async function loadRecords() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      orderType: 1, // 充值订单
    }
    const response = await billingApi.getOrderList(params)
    rechargeRecords.value = response.data?.list || []
    pagination.total = response.data?.pagination?.total || 0
  } catch (error) {
    console.error('[Recharge] Load records failed:', error)
    rechargeRecords.value = []
    pagination.total = 0
    ElMessage.error('加载充值记录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 选择套餐
 */
function selectPackage(pkg) {
  selectedPackage.value = pkg
  isCustom.value = false
  customAmount.value = 0
}

/**
 * 自定义金额变化
 */
function handleCustomAmountChange(val) {
  if (val > 0) {
    isCustom.value = true
    selectedPackage.value = null
  }
}

/**
 * 选择支付方式
 */
function selectPayment(method) {
  selectedPayment.value = method
}

/**
 * 获取支付标签
 */
function getPaymentLabel(channel) {
  const map = { wechat: '微信支付', alipay: '支付宝', unionpay: '对公转账' }
  return map[channel] || channel
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
 * 格式化数字
 */
function formatNumber(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString()
}

/**
 * 提交订单
 */
async function handleSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    const params = {
      amount: orderAmount.value,
      payChannel: selectedPayment.value,
    }

    // 如果是套餐，使用套餐购买接口
    if (selectedPackage.value) {
      params.packageId = selectedPackage.value.id
      const response = await billingApi.purchasePackage(params)
      currentOrder.value = {
        orderNo: response.data?.orderNo,
        amount: orderAmount.value,
        payChannel: selectedPayment.value,
        ...response.data,
      }
    } else {
      // 自定义金额使用充值接口
      const response = await billingApi.createRechargeOrder(params)
      currentOrder.value = {
        orderNo: response.data?.orderNo,
        amount: orderAmount.value,
        payChannel: selectedPayment.value,
        ...response.data,
      }
    }

    // 显示支付弹窗
    if (selectedPayment.value === 'wechat' || selectedPayment.value === 'alipay') {
      qrCodeUrl.value = currentOrder.value.payInfo?.qrCode || ''
      qrDialogVisible.value = true
      startPolling()
    } else {
      // 对公转账显示转账信息
      ElMessage.success('订单创建成功，请查看转账信息')
    }

    loadRecords()
  } catch (error) {
    console.error('[Recharge] Submit failed:', error)
    ElMessage.error('订单创建失败，请重试')
  } finally {
    submitting.value = false
  }
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
        loadRecords()
        loadBalance()
      }
    } catch (error) {
      console.error('[Recharge] Poll status failed:', error)
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
 * 去支付
 */
function handlePay(row) {
  currentOrder.value = row
  qrCodeUrl.value = ''
  qrDialogVisible.value = true
  startPolling()
}

/**
 * 取消订单
 */
async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
    await billingApi.cancelOrder(row.id)
    ElMessage.success('订单已取消')
    loadRecords()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[Recharge] Cancel failed:', error)
      ElMessage.error('取消失败')
    }
  }
}

/**
 * 加载余额
 */
async function loadBalance() {
  try {
    const response = await billingApi.getBalance()
    currentBalance.value = response.data?.balance || 0
  } catch (error) {
    console.error('[Recharge] Load balance failed:', error)
  }
}

/**
 * 分页变化
 */
function handlePageChange(page) {
  pagination.page = page
  loadRecords()
}

/**
 * 每页数量变化
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadRecords()
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadPackages()
  loadRecords()
  loadBalance()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style lang="scss" scoped>
// 当前余额
.recharge__balance {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-2xl;
  background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
  border-radius: $radius-xl;
  margin-bottom: $spacing-xl;
  color: $color-white;
}

.recharge__balance-label {
  font-size: $font-size-base;
  opacity: 0.9;
  margin-bottom: $spacing-sm;
}

.recharge__balance-value {
  display: flex;
  align-items: baseline;
  gap: $spacing-sm;
}

.recharge__balance-num {
  font-size: 48px;
  font-weight: $font-weight-bold;
  line-height: 1;
}

.recharge__balance-unit {
  font-size: $font-size-lg;
  opacity: 0.8;
}

// 区块标题
.recharge__section {
  margin-bottom: $spacing-xl;
  background: $color-white;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  box-shadow: $shadow-sm;
}

.recharge__section-title {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin: 0 0 $spacing-lg 0;
  font-size: $font-size-lg;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;

  i {
    color: $color-primary;
  }
}

// 套餐卡片
.recharge__packages {
  .el-col {
    margin-bottom: $spacing-lg;
  }
}

.recharge__package-card {
  position: relative;
  padding: $spacing-lg;
  border: 2px solid $color-border-base;
  border-radius: $radius-lg;
  text-align: center;
  cursor: pointer;
  transition: all $transition-duration-base;

  &:hover {
    border-color: $color-primary-light;
    box-shadow: $shadow-md;
  }

  &.is-selected {
    border-color: $color-primary;
    background: rgba($color-primary, 0.05);
  }

  &.is-recommended {
    border-color: $color-warning;

    &.is-selected {
      background: rgba($color-warning, 0.05);
    }
  }
}

.recharge__package-badge {
  position: absolute;
  top: -1px;
  right: -1px;
  padding: 4px 12px;
  background: $color-warning;
  color: $color-white;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  border-radius: 0 $radius-lg 0 $radius-lg;
}

.recharge__package-points {
  font-size: 32px;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1;
}

.recharge__package-unit {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-top: $spacing-xs;
}

.recharge__package-divider {
  width: 40px;
  height: 2px;
  background: $color-border-base;
  margin: $spacing-base auto;
}

.recharge__package-price {
  color: $color-danger;
}

.recharge__package-currency {
  font-size: $font-size-lg;
}

.recharge__package-amount {
  font-size: 28px;
  font-weight: $font-weight-bold;
}

.recharge__package-gift {
  margin-top: $spacing-xs;
  font-size: $font-size-xs;
  color: $color-success;
}

.recharge__package-original {
  margin-top: $spacing-xs;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
  text-decoration: line-through;
}

// 自定义金额
.recharge__custom {
  display: flex;
  flex-direction: column;
  gap: $spacing-base;
}

.recharge__custom-input {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.recharge__custom-currency {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.recharge__custom-hint {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.recharge__custom-preview {
  font-size: $font-size-base;
  color: $color-text-secondary;
}

.recharge__custom-points {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-success;
}

// 支付方式
.recharge__payment-methods {
  display: flex;
  flex-direction: column;
  gap: $spacing-base;
}

.recharge__payment-item {
  display: flex;
  align-items: center;
  padding: $spacing-lg;
  border: 2px solid $color-border-base;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: all $transition-duration-base;

  &:hover {
    border-color: $color-primary-light;
  }

  &.is-selected {
    border-color: $color-primary;
    background: rgba($color-primary, 0.05);
  }
}

.recharge__payment-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: $spacing-base;

  img {
    width: 32px;
    height: 32px;
    object-fit: contain;
  }
}

.recharge__payment-info {
  flex: 1;
}

.recharge__payment-label {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.recharge__payment-desc {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-top: 2px;
}

.recharge__payment-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: $color-primary;
  color: $color-white;
  display: flex;
  align-items: center;
  justify-content: center;
}

// 订单确认
.recharge__order {
  max-width: 400px;
  margin: 0 auto;
}

.recharge__order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-base;
}

.recharge__order-label {
  font-size: $font-size-base;
  color: $color-text-secondary;
}

.recharge__order-value {
  font-size: $font-size-base;
  font-weight: $font-weight-medium;

  &--primary {
    color: $color-primary;
  }

  &--price {
    color: $color-danger;
    font-weight: $font-weight-bold;
  }
}

.recharge__order-divider {
  height: 1px;
  background: $color-border-base;
  margin: $spacing-lg 0;
}

.recharge__order-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
  font-size: $font-size-lg;
  font-weight: $font-weight-semibold;
}

.recharge__order-total-price {
  font-size: 32px;
  color: $color-danger;
}

.recharge__order-submit {
  width: 100%;
  height: 48px;
  font-size: $font-size-lg;
}

// 充值记录
.recharge__record-order {
  font-family: $font-family-code;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.recharge__record-points {
  color: $color-success;
  font-weight: $font-weight-semibold;
}

.recharge__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-lg;
}

// 支付弹窗
.recharge__qr-dialog {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-lg;
}

.recharge__qr-amount {
  text-align: center;
  margin-bottom: $spacing-lg;
}

.recharge__qr-label {
  display: block;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-bottom: $spacing-xs;
}

.recharge__qr-price {
  display: block;
  font-size: 32px;
  font-weight: $font-weight-bold;
  color: $color-danger;
}

.recharge__qr-container {
  width: 200px;
  height: 200px;
  margin-bottom: $spacing-lg;
  border: 1px solid $color-border-base;
  border-radius: $radius-md;
  overflow: hidden;
}

.recharge__qr-image {
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

.recharge__qr-loading {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: $color-text-secondary;
}

.recharge__qr-tips {
  text-align: center;
  color: $color-text-secondary;
  font-size: $font-size-sm;

  p {
    margin: $spacing-xs 0;
  }
}
</style>
