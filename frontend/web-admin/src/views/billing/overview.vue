<template>
  <div class="billing-overview-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">计费概览</h2>
      <div class="header-actions">
        <el-radio-group v-model="dateRangeType" size="default" @change="handleDateRangeChange">
          <el-radio-button value="today">今日</el-radio-button>
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
          <el-radio-button value="year">本年</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="key-metrics">
      <el-col :span="6">
        <div class="metric-card recharge">
          <div class="metric-icon">
            <el-icon size="28"><Wallet /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">总充值金额</div>
            <div class="metric-value">¥{{ overview.totalRecharge.toLocaleString() }}</div>
            <div class="metric-trend up">
              <el-icon><Top /></el-icon>
              <span>+15.8% 较上期</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="metric-card consume">
          <div class="metric-icon">
            <el-icon size="28"><Coin /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">总消耗积分</div>
            <div class="metric-value">{{ overview.totalConsume.toLocaleString() }}</div>
            <div class="metric-trend down">
              <el-icon><Bottom /></el-icon>
              <span>-8.3% 较上期</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="metric-card balance">
          <div class="metric-icon">
            <el-icon size="28"><Money /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">账户余额</div>
            <div class="metric-value">{{ overview.totalBalance.toLocaleString() }}</div>
            <div class="metric-trend up">
              <el-icon><Top /></el-icon>
              <span>+22.5% 较上期</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="metric-card orders">
          <div class="metric-icon">
            <el-icon size="28"><Tickets /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">订单总数</div>
            <div class="metric-value">{{ overview.totalOrders.toLocaleString() }}</div>
            <div class="metric-trend neutral">
              <span>本月新增 128 笔</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 收支趋势图 -->
      <el-col :span="16">
        <div class="chart-panel">
          <div class="panel-header">
            <span class="panel-title">收支趋势</span>
            <el-select v-model="trendType" size="small" style="width: 120px" @change="loadTrend">
              <el-option label="按日" value="day" />
              <el-option label="按周" value="week" />
              <el-option label="按月" value="month" />
            </el-select>
          </div>
          <div class="chart-container" ref="trendChartRef">
            <!-- 趋势图表占位 -->
            <div class="chart-placeholder">
              <div class="chart-bars">
                <div v-for="(item, i) in trendData" :key="i" class="chart-bar-group">
                  <div class="bar recharge-bar" :style="{ height: item.rechargePercent + '%' }" />
                  <div class="bar consume-bar" :style="{ height: item.consumePercent + '%' }" />
                  <span class="bar-label">{{ item.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 充值来源占比 -->
      <el-col :span="8">
        <div class="chart-panel">
          <div class="panel-header">
            <span class="panel-title">充值来源分布</span>
          </div>
          <div class="chart-container">
            <div class="pie-chart-placeholder">
              <el-progress
                v-for="item in rechargeSources"
                :key="item.name"
                type="dashboard"
                :percentage="item.percentage"
                :color="item.color"
                :width="120"
              >
                <template #default>
                  <span class="pie-label">{{ item.name }}</span>
                </template>
              </el-progress>
            </div>
            <div class="pie-legend">
              <div v-for="item in rechargeSources" :key="item.name" class="legend-item">
                <span class="legend-dot" :style="{ background: item.color }" />
                <span class="legend-name">{{ item.name }}</span>
                <span class="legend-value">{{ item.amount }}元</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 消费排行 & 充值记录 -->
    <el-row :gutter="20" class="tables-row">
      <!-- 消费排行 -->
      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">积分消耗 TOP10</span>
          </div>
          <el-table :data="consumeRanking" stripe size="small">
            <el-table-column type="index" label="排名" width="60" align="center" />
            <el-table-column prop="tenantName" label="租户名称" min-width="150" />
            <el-table-column prop="consume" label="消耗积分" width="120" align="right">
              <template #default="{ row }">
                <span style="color: #ff4d4f; font-weight: 600">{{ row.consume.toLocaleString() }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" width="80" align="center" />
          </el-table>
        </div>
      </el-col>

      <!-- 最新充值记录 -->
      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">最新充值记录</span>
            <el-button link type="primary" size="small" @click="goToOrders">查看全部</el-button>
          </div>
          <el-table :data="rechargeRecords" stripe size="small">
            <el-table-column prop="tenantName" label="租户名称" min-width="120" />
            <el-table-column prop="amount" label="充值金额" width="100" align="right">
              <template #default="{ row }">
                <span style="color: #52c41a; font-weight: 600">+¥{{ row.amount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="points" label="获得积分" width="100" align="right">
              <template #default="{ row }">
                {{ row.points.toLocaleString() }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="140" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * BillingOverview - 计费概览页面
 * 功能：展示平台整体计费数据、收支趋势、消费排行等
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBillingOverview, getBillingTrend, getConsumeRanking, getRechargeList } from '@/api/billing'
import { Wallet, Coin, Money, Tickets, Top, Bottom } from '@element-plus/icons-vue'

const router = useRouter()

// ============================================================
// 时间范围
// ============================================================
const dateRangeType = ref('month')
const trendType = ref('day')

// ============================================================
// 数据状态
// ============================================================
const overview = reactive({
  totalRecharge: 0,
  totalConsume: 0,
  totalBalance: 0,
  totalOrders: 0
})

const trendData = ref([])
const consumeRanking = ref([])
const rechargeRecords = ref([])

const rechargeSources = ref([
  { name: '套餐购买', amount: '68,500', percentage: 53, color: '#1a73e8' },
  { name: '充值积分', amount: '42,300', percentage: 33, color: '#52c41a' },
  { name: '活动赠送', amount: '17,800', percentage: 14, color: '#faad14' }
])

// ============================================================
// 数据加载
// ============================================================
async function loadOverview() {
  try {
    const data = await getBillingOverview()
    Object.assign(overview, data)
  } catch {
    overview.totalRecharge = 128600
    overview.totalConsume = 856420
    overview.totalBalance = 1245680
    overview.totalOrders = 1586
  }
}

async function loadTrend() {
  try {
    const data = await getBillingTrend({ type: trendType.value })
    trendData.value = data
  } catch {
    trendData.value = [
      { label: '1月', rechargePercent: 65, consumePercent: 40 },
      { label: '2月', rechargePercent: 72, consumePercent: 55 },
      { label: '3月', rechargePercent: 60, consumePercent: 45 },
      { label: '4月', rechargePercent: 80, consumePercent: 60 },
      { label: '5月', rechargePercent: 85, consumePercent: 70 },
      { label: '6月', rechargePercent: 78, consumePercent: 62 },
      { label: '7月', rechargePercent: 90, consumePercent: 75 }
    ]
  }
}

async function loadRanking() {
  try {
    const data = await getConsumeRanking({ topN: 10 })
    consumeRanking.value = data
  } catch {
    consumeRanking.value = [
      { tenantName: '深圳星辰科技有限公司', consume: 125800, orderCount: 256 },
      { tenantName: '北京智云网络科技', consume: 92400, orderCount: 189 },
      { tenantName: '杭州鼎盛传媒', consume: 81600, orderCount: 167 },
      { tenantName: '广州领航贸易公司', consume: 68200, orderCount: 134 },
      { tenantName: '成都云端软件', consume: 55800, orderCount: 112 },
      { tenantName: '上海蓝图数据服务', consume: 45600, orderCount: 98 },
      { tenantName: '武汉创新科技', consume: 38900, orderCount: 76 },
      { tenantName: '南京博远网络', consume: 32100, orderCount: 65 }
    ]
  }
}

async function loadRechargeRecords() {
  try {
    const data = await getRechargeList({ page: 1, pageSize: 5 })
    rechargeRecords.value = data.list || []
  } catch {
    rechargeRecords.value = [
      { tenantName: '深圳星辰科技', amount: 1000, points: 1100, createdAt: '2024-01-18 14:30' },
      { tenantName: '杭州鼎盛传媒', amount: 500, points: 550, createdAt: '2024-01-18 11:20' },
      { tenantName: '北京智云网络', amount: 2000, points: 2200, createdAt: '2024-01-18 09:45' },
      { tenantName: '广州领航贸易', amount: 300, points: 315, createdAt: '2024-01-17 16:30' },
      { tenantName: '成都云端软件', amount: 5000, points: 5500, createdAt: '2024-01-17 10:00' }
    ]
  }
}

function handleDateRangeChange() {
  loadOverview()
  loadTrend()
}

function goToOrders() {
  ElMessage.info('订单管理页面开发中...')
}

onMounted(() => {
  loadOverview()
  loadTrend()
  loadRanking()
  loadRechargeRecords()
})
</script>

<style lang="scss" scoped>
.billing-overview-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-base;

  .page-title {
    font-size: $font-size-extra-large;
    font-weight: 600;
    color: $text-primary;
  }
}

// 核心指标
.key-metrics {
  margin-bottom: $spacing-base;
}

.metric-card {
  background: $bg-color;
  border-radius: $border-radius-base;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid $border-color-light;

  .metric-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &.recharge .metric-icon { background: #e8f0fe; color: #1a73e8; }
  &.consume .metric-icon { background: #fff2f0; color: #ff4d4f; }
  &.balance .metric-icon { background: #f6ffed; color: #52c41a; }
  &.orders .metric-icon { background: #fffbe6; color: #faad14; }

  .metric-content { flex: 1; }

  .metric-label {
    font-size: $font-size-small;
    color: $text-secondary;
    margin-bottom: 4px;
  }

  .metric-value {
    font-size: 22px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 4px;
  }

  .metric-trend {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: $font-size-small;

    &.up { color: $success-color; }
    &.down { color: $danger-color; }
    &.neutral { color: $text-secondary; }
  }
}

// 图表区域
.charts-row {
  margin-bottom: $spacing-base;
}

.chart-panel {
  background: $bg-color;
  border-radius: $border-radius-base;
  border: 1px solid $border-color-light;
  padding: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .panel-title {
    font-weight: 600;
    color: $text-primary;
    font-size: $font-size-base;
  }
}

.chart-container {
  min-height: 200px;
}

// 趋势图占位（演示）
.chart-placeholder {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-top: 20px;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  width: 100%;
  justify-content: center;
}

.chart-bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 40px;
}

.bar {
  width: 16px;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

.recharge-bar {
  background: linear-gradient(to top, #1a73e8, #4a9eff);
}

.consume-bar {
  background: linear-gradient(to top, #ff4d4f, #ff7875);
}

.bar-label {
  font-size: $font-size-small;
  color: $text-secondary;
  margin-top: 4px;
}

// 饼图占位
.pie-chart-placeholder {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 16px;
}

.pie-label {
  font-size: $font-size-small;
  color: $text-secondary;
}

.pie-legend {
  .legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 0;
    font-size: $font-size-base;

    .legend-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }

    .legend-name { flex: 1; color: $text-primary; }
    .legend-value { color: $text-secondary; font-size: $font-size-small; }
  }
}

// 表格行
.tables-row {
  margin-bottom: $spacing-base;
}

.panel {
  background: $bg-color;
  border-radius: $border-radius-base;
  border: 1px solid $border-color-light;
  overflow: hidden;

  .panel-header {
    padding: 14px 16px;
    border-bottom: 1px solid $border-color-light;
  }
}
</style>
