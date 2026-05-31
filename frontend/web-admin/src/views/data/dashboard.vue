<template>
  <div class="data-dashboard">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card stat-primary">
          <div class="stat-icon">
            <el-icon size="28"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(overview.totalLeads) }}</div>
            <div class="stat-label">商机总数</div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon>
              <span>+{{ overview.leadGrowth || 0 }}%</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-success">
          <div class="stat-icon">
            <el-icon size="28"><SuccessFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(overview.convertedLeads) }}</div>
            <div class="stat-label">已成交商机</div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon>
              <span>+{{ overview.convertGrowth || 0 }}%</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-warning">
          <div class="stat-icon">
            <el-icon size="28"><Coin /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">¥{{ formatNumber(overview.totalConsume) }}</div>
            <div class="stat-label">消费总额</div>
            <div class="stat-trend down">
              <el-icon><Bottom /></el-icon>
              <span>-{{ overview.consumeGrowth || 0 }}%</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-danger">
          <div class="stat-icon">
            <el-icon size="28"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(overview.activeAccounts) }}</div>
            <div class="stat-label">活跃账号</div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon>
              <span>+{{ overview.accountGrowth || 0 }}%</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" class="dashboard-tabs" @tab-change="handleTabChange">
      <!-- 运营数据看板 -->
      <el-tab-pane label="运营数据" name="operation">
        <el-row :gutter="20" class="chart-row">
          <el-col :span="16">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">获客趋势</span>
                <div class="chart-actions">
                  <el-radio-group v-model="operationTimeRange" size="small" @change="loadOperationData">
                    <el-radio-button value="7">近7天</el-radio-button>
                    <el-radio-button value="30">近30天</el-radio-button>
                    <el-radio-button value="90">近90天</el-radio-button>
                  </el-radio-group>
                </div>
              </div>
              <div ref="operationChartRef" class="chart-container"></div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">商机来源分布</span>
              </div>
              <div ref="sourcePieChartRef" class="chart-container"></div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="chart-row">
          <el-col :span="12">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">意向等级分布</span>
              </div>
              <div ref="intentChartRef" class="chart-container"></div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">转化漏斗</span>
              </div>
              <div ref="funnelChartRef" class="chart-container"></div>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 获客数据看板 -->
      <el-tab-pane label="获客数据" name="lead">
        <el-row :gutter="20" class="chart-row">
          <el-col :span="16">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">获客量趋势</span>
                <div class="chart-actions">
                  <el-select v-model="leadPlatform" size="small" placeholder="全部平台" clearable @change="loadLeadData">
                    <el-option label="全部平台" :value="undefined" />
                    <el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" />
                  </el-select>
                  <el-radio-group v-model="leadTimeRange" size="small" @change="loadLeadData">
                    <el-radio-button value="7">近7天</el-radio-button>
                    <el-radio-button value="30">近30天</el-radio-button>
                  </el-radio-group>
                </div>
              </div>
              <div ref="leadChartRef" class="chart-container"></div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">来源占比</span>
              </div>
              <div ref="leadSourceChartRef" class="chart-container"></div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="chart-row">
          <el-col :span="24">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">关键词效果 TOP10</span>
              </div>
              <el-table :data="keywordData" stripe style="width: 100%">
                <el-table-column prop="keyword" label="关键词" min-width="150" />
                <el-table-column prop="platformName" label="平台" width="120" />
                <el-table-column prop="exposure" label="曝光量" width="120" sortable>
                  <template #default="{ row }">{{ formatNumber(row.exposure) }}</template>
                </el-table-column>
                <el-table-column prop="click" label="点击量" width="120" sortable>
                  <template #default="{ row }">{{ formatNumber(row.click) }}</template>
                </el-table-column>
                <el-table-column prop="leadCount" label="获客量" width="120" sortable>
                  <template #default="{ row }">{{ formatNumber(row.leadCount) }}</template>
                </el-table-column>
                <el-table-column prop="convertRate" label="转化率" width="100" sortable>
                  <template #default="{ row }">{{ row.convertRate }}%</template>
                </el-table-column>
                <el-table-column prop="cost" label="消耗(元)" width="100" sortable>
                  <template #default="{ row }">¥{{ row.cost }}</template>
                </el-table-column>
                <el-table-column prop="cpa" label="CPA(元)" width="100" sortable>
                  <template #default="{ row }">¥{{ row.cpa }}</template>
                </el-table-column>
              </el-table>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 账号数据看板 -->
      <el-tab-pane label="账号数据" name="account">
        <el-row :gutter="20" class="chart-row">
          <el-col :span="16">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">账号消息量趋势</span>
                <div class="chart-actions">
                  <el-select v-model="selectedAccount" size="small" placeholder="全部账号" clearable @change="loadAccountData">
                    <el-option label="全部账号" :value="undefined" />
                    <el-option v-for="a in accounts" :key="a.id" :label="a.name" :value="a.id" />
                  </el-select>
                  <el-radio-group v-model="accountTimeRange" size="small" @change="loadAccountData">
                    <el-radio-button value="7">近7天</el-radio-button>
                    <el-radio-button value="30">近30天</el-radio-button>
                  </el-radio-group>
                </div>
              </div>
              <div ref="accountChartRef" class="chart-container"></div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">账号消息量排行</span>
              </div>
              <div ref="accountRankChartRef" class="chart-container"></div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="chart-row">
          <el-col :span="24">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">账号数据明细</span>
                <el-button type="primary" size="small" @click="exportAccountReport">导出报表</el-button>
              </div>
              <el-table :data="accountTableData" stripe style="width: 100%">
                <el-table-column prop="accountName" label="账号名称" min-width="150" />
                <el-table-column prop="platformName" label="平台" width="120" />
                <el-table-column prop="sendCount" label="发送消息" width="100" sortable>
                  <template #default="{ row }">{{ formatNumber(row.sendCount) }}</template>
                </el-table-column>
                <el-table-column prop="receiveCount" label="接收消息" width="100" sortable>
                  <template #default="{ row }">{{ formatNumber(row.receiveCount) }}</template>
                </el-table-column>
                <el-table-column prop="leadCount" label="获客数" width="100" sortable>
                  <template #default="{ row }">{{ formatNumber(row.leadCount) }}</template>
                </el-table-column>
                <el-table-column prop="consume" label="消耗(元)" width="100" sortable>
                  <template #default="{ row }">¥{{ row.consume }}</template>
                </el-table-column>
                <el-table-column prop="costPerLead" label="CPA(元)" width="100" sortable>
                  <template #default="{ row }">¥{{ row.costPerLead || '-' }}</template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                      {{ row.status === 1 ? '正常' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <div class="pagination-wrapper">
                <el-pagination
                  v-model:current-page="accountPage"
                  v-model:page-size="accountPageSize"
                  :total="accountTotal"
                  :page-sizes="[10, 20, 50]"
                  layout="total, sizes, prev, pager, next"
                  @size-change="loadAccountData"
                  @current-change="loadAccountData"
                />
              </div>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 消费数据看板 -->
      <el-tab-pane label="消费数据" name="billing">
        <el-row :gutter="20" class="chart-row">
          <el-col :span="16">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">消费趋势</span>
                <div class="chart-actions">
                  <el-radio-group v-model="billingTimeRange" size="small" @change="loadBillingData">
                    <el-radio-button value="7">近7天</el-radio-button>
                    <el-radio-button value="30">近30天</el-radio-button>
                    <el-radio-button value="90">近90天</el-radio-button>
                  </el-radio-group>
                </div>
              </div>
              <div ref="billingChartRef" class="chart-container"></div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">消费构成</span>
              </div>
              <div ref="billingPieChartRef" class="chart-container"></div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="chart-row">
          <el-col :span="24">
            <div class="chart-card">
              <div class="chart-header">
                <span class="chart-title">消费明细</span>
                <el-button type="primary" size="small" @click="exportBillingReport">导出报表</el-button>
              </div>
              <el-table :data="billingTableData" stripe style="width: 100%">
                <el-table-column prop="tenantName" label="租户" min-width="150" />
                <el-table-column prop="recharge" label="充值(元)" width="120" sortable>
                  <template #default="{ row }">¥{{ row.recharge }}</template>
                </el-table-column>
                <el-table-column prop="consume" label="消费(元)" width="120" sortable>
                  <template #default="{ row }">¥{{ row.consume }}</template>
                </el-table-column>
                <el-table-column prop="balance" label="余额(元)" width="120" sortable>
                  <template #default="{ row }">¥{{ row.balance }}</template>
                </el-table-column>
                <el-table-column prop="leadCount" label="获客数" width="100" sortable>
                  <template #default="{ row }">{{ formatNumber(row.leadCount) }}</template>
                </el-table-column>
                <el-table-column prop="costPerLead" label="CPA(元)" width="100" sortable>
                  <template #default="{ row }">¥{{ row.costPerLead || '-' }}</template>
                </el-table-column>
                <el-table-column prop="updateTime" label="更新时间" width="180" />
              </el-table>
              <div class="pagination-wrapper">
                <el-pagination
                  v-model:current-page="billingPage"
                  v-model:page-size="billingPageSize"
                  :total="billingTotal"
                  :page-sizes="[10, 20, 50]"
                  layout="total, sizes, prev, pager, next"
                  @size-change="loadBillingData"
                  @current-change="loadBillingData"
                />
              </div>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
/**
 * 数据看板页面
 * 功能：运营数据、获客数据、账号数据、消费数据的统一展示
 * @author 张前端 (EMP-FE-001)
 */

import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { request } from '@/api'

// ============================================================
// 状态定义
// ============================================================
const activeTab = ref('operation')
const operationTimeRange = ref('30')
const leadTimeRange = ref('30')
const accountTimeRange = ref('30')
const billingTimeRange = ref('30')
const leadPlatform = ref(undefined)
const selectedAccount = ref(undefined)

const overview = ref({
  totalLeads: 0,
  convertedLeads: 0,
  totalConsume: 0,
  activeAccounts: 0,
  leadGrowth: 0,
  convertGrowth: 0,
  consumeGrowth: 0,
  accountGrowth: 0
})

const platforms = ref([])
const accounts = ref([])
const keywordData = ref([])

// 账号数据分页
const accountPage = ref(1)
const accountPageSize = ref(10)
const accountTotal = ref(0)
const accountTableData = ref([])

// 消费数据分页
const billingPage = ref(1)
const billingPageSize = ref(10)
const billingTotal = ref(0)
const billingTableData = ref([])

// 图表实例
let operationChart = null
let sourcePieChart = null
let intentChart = null
let funnelChart = null
let leadChart = null
let leadSourceChart = null
let accountChart = null
let accountRankChart = null
let billingChart = null
let billingPieChart = null

const operationChartRef = ref(null)
const sourcePieChartRef = ref(null)
const intentChartRef = ref(null)
const funnelChartRef = ref(null)
const leadChartRef = ref(null)
const leadSourceChartRef = ref(null)
const accountChartRef = ref(null)
const accountRankChartRef = ref(null)
const billingChartRef = ref(null)
const billingPieChartRef = ref(null)

// 定时器
let realtimeTimer = null

// ============================================================
// 工具函数
// ============================================================
function formatNumber(num) {
  if (num == null) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num.toLocaleString()
}

function getDaysAgo(days) {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().split('T')[0]
}

// ============================================================
// 数据加载
// ============================================================
async function loadOverview() {
  try {
    const data = await request.get('/admin/data/dashboard/overview')
    overview.value = data
  } catch (error) {
    console.error('[Dashboard] Load overview failed:', error)
    overview.value = {
      totalLeads: 0,
      convertedLeads: 0,
      totalConsume: 0,
      activeAccounts: 0,
      leadGrowth: 0,
      convertGrowth: 0,
      consumeGrowth: 0,
      accountGrowth: 0
    }
  }
}

async function loadPlatforms() {
  try {
    const data = await request.get('/admin/platforms')
    platforms.value = data || []
  } catch {
    platforms.value = []
  }
}

async function loadAccounts() {
  try {
    const data = await request.get('/admin/accounts/list')
    accounts.value = data?.list || data || []
  } catch {
    accounts.value = []
  }
}

async function loadOperationData() {
  const params = {
    startDate: getDaysAgo(parseInt(operationTimeRange.value)),
    endDate: new Date().toISOString().split('T')[0]
  }
  try {
    const [trendData, sourceData, intentData, funnelData] = await Promise.all([
      get('/admin/data/analysis/lead-trend', params),
      get('/admin/data/analysis/source', params),
      get('/admin/data/analysis/intent-distribution', params),
      get('/admin/data/analysis/funnel', params)
    ])
    renderOperationCharts(trendData, sourceData, intentData, funnelData)
  } catch (error) {
    console.error('[Dashboard] Load operation data failed:', error)
    renderOperationCharts([], [], [], [])
  }
}

async function loadLeadData() {
  const params = {
    startDate: getDaysAgo(parseInt(leadTimeRange.value)),
    endDate: new Date().toISOString().split('T')[0],
    platformId: leadPlatform.value
  }
  try {
    const [trendData, sourceData, keywordList] = await Promise.all([
      request.get('/admin/data/dashboard/lead/trend', params),
      request.get('/admin/data/analysis/source', params),
      request.get('/admin/data/analysis/keyword-performance', { ...params, topN: 10 })
    ])
    renderLeadCharts(trendData, sourceData)
    keywordData.value = keywordList || getDemoKeywordData()
  } catch {
    renderLeadCharts(getDemoTrendData(), getDemoSourceData())
    keywordData.value = getDemoKeywordData()
  }
}

async function loadAccountData() {
  const params = {
    page: accountPage.value,
    pageSize: accountPageSize.value,
    startDate: getDaysAgo(parseInt(accountTimeRange.value)),
    endDate: new Date().toISOString().split('T')[0],
    accountId: selectedAccount.value
  }
  try {
    const [trendData, rankData, tableData] = await Promise.all([
      request.get('/admin/data/dashboard/account/trend', params),
      request.get('/admin/data/dashboard/account/ranking', params),
      request.get('/admin/data/dashboard/account/table', params)
    ])
    renderAccountCharts(trendData, rankData)
    accountTableData.value = tableData?.list || []
    accountTotal.value = tableData?.pagination?.total || 0
  } catch {
    renderAccountCharts(getDemoAccountTrendData(), getDemoAccountRankData())
    accountTableData.value = getDemoAccountTableData()
    accountTotal.value = 5
  }
}

async function loadBillingData() {
  const params = {
    page: billingPage.value,
    pageSize: billingPageSize.value,
    startDate: getDaysAgo(parseInt(billingTimeRange.value)),
    endDate: new Date().toISOString().split('T')[0]
  }
  try {
    const [trendData, pieData, tableData] = await Promise.all([
      request.get('/admin/data/dashboard/billing/trend', params),
      request.get('/admin/data/dashboard/billing/composition', params),
      request.get('/admin/data/dashboard/billing/table', params)
    ])
    renderBillingCharts(trendData, pieData)
    billingTableData.value = tableData?.list || []
    billingTotal.value = tableData?.pagination?.total || 0
  } catch {
    renderBillingCharts(getDemoBillingTrendData(), getDemoBillingPieData())
    billingTableData.value = getDemoBillingTableData()
    billingTotal.value = 5
  }
}

// ============================================================
// 图表渲染
// ============================================================
function renderOperationCharts(trendData, sourceData, intentData, funnelData) {
  nextTick(() => {
    // 获客趋势折线图
    if (operationChart) operationChart.dispose()
    operationChart = echarts.init(operationChartRef.value)
    operationChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['新增商机', '成交商机', '总获客'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: trendData.map(d => d.date),
        boundaryGap: false
      },
      yAxis: [{ type: 'value' }, { type: 'value', name: '成交率(%)', max: 100 }],
      series: [
        { name: '新增商机', type: 'line', smooth: true, data: trendData.map(d => d.newLeads) },
        { name: '成交商机', type: 'line', smooth: true, data: trendData.map(d => d.convertedLeads) },
        {
          name: '总获客',
          type: 'line',
          smooth: true,
          yAxisIndex: 1,
          data: trendData.map(d => d.convertRate),
          formatter: '{c}%'
        }
      ]
    })

    // 来源饼图
    if (sourcePieChart) sourcePieChart.dispose()
    sourcePieChart = echarts.init(sourcePieChartRef.value)
    sourcePieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', right: 10, top: 'center' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        label: { show: false },
        emphasis: { label: { show: true, fontWeight: 'bold' } },
        data: sourceData.map(d => ({ name: d.sourceName, value: d.count }))
      }]
    })

    // 意向等级柱状图
    if (intentChart) intentChart.dispose()
    intentChart = echarts.init(intentChartRef.value)
    intentChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: intentData.map(d => d.levelName) },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: intentData.map(d => d.count),
        itemStyle: {
          color(params) {
            const colors = ['#67C23A', '#409EFF', '#E6A23C', '#F56C6C']
            return colors[params.dataIndex % colors.length]
          }
        }
      }]
    })

    // 漏斗图
    if (funnelChart) funnelChart.dispose()
    funnelChart = echarts.init(funnelChartRef.value)
    funnelChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'funnel',
        left: '10%',
        top: 20,
        bottom: 20,
        width: '80%',
        min: 0,
        max: funnelData[0]?.value || 100,
        minSize: '0%',
        maxSize: '100%',
        gap: 2,
        label: { show: true, position: 'inside', formatter: '{b}\n{c}' },
        data: funnelData.map(d => ({ name: d.name, value: d.value }))
      }]
    })
  })
}

function renderLeadCharts(trendData, sourceData) {
  nextTick(() => {
    if (leadChart) leadChart.dispose()
    leadChart = echarts.init(leadChartRef.value)
    leadChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['曝光', '点击', '获客'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: { type: 'category', data: trendData.map(d => d.date), boundaryGap: false },
      yAxis: [{ type: 'value' }],
      series: [
        { name: '曝光', type: 'bar', data: trendData.map(d => d.exposure) },
        { name: '点击', type: 'bar', data: trendData.map(d => d.click) },
        { name: '获客', type: 'line', smooth: true, data: trendData.map(d => d.leadCount) }
      ]
    })

    if (leadSourceChart) leadSourceChart.dispose()
    leadSourceChart = echarts.init(leadSourceChartRef.value)
    leadSourceChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: '70%',
        center: ['50%', '50%'],
        label: { show: true, formatter: '{b}\n{d}%' },
        data: sourceData.map(d => ({ name: d.sourceName, value: d.count }))
      }]
    })
  })
}

function renderAccountCharts(trendData, rankData) {
  nextTick(() => {
    if (accountChart) accountChart.dispose()
    accountChart = echarts.init(accountChartRef.value)
    accountChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['发送', '接收'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: { type: 'category', data: trendData.map(d => d.date), boundaryGap: false },
      yAxis: [{ type: 'value' }],
      series: [
        { name: '发送', type: 'line', smooth: true, data: trendData.map(d => d.sendCount) },
        { name: '接收', type: 'line', smooth: true, data: trendData.map(d => d.receiveCount) }
      ]
    })

    if (accountRankChart) accountRankChart.dispose()
    accountRankChart = echarts.init(accountRankChartRef.value)
    accountRankChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: rankData.map(d => d.accountName).reverse() },
      series: [{
        type: 'bar',
        data: rankData.map(d => d.messageCount).reverse(),
        itemStyle: { color: '#409EFF' }
      }]
    })
  })
}

function renderBillingCharts(trendData, pieData) {
  nextTick(() => {
    if (billingChart) billingChart.dispose()
    billingChart = echarts.init(billingChartRef.value)
    billingChart.setOption({
      tooltip: { trigger: 'axis', formatter: '{b}<br/>消费: ¥{c}' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: trendData.map(d => d.date), boundaryGap: false },
      yAxis: [{ type: 'value', name: '消费(元)' }],
      series: [{
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.3 },
        data: trendData.map(d => d.consume)
      }]
    })

    if (billingPieChart) billingPieChart.dispose()
    billingPieChart = echarts.init(billingPieChartRef.value)
    billingPieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: '70%',
        center: ['50%', '50%'],
        label: { show: true, formatter: '{b}\n¥{c}' },
        data: pieData.map(d => ({ name: d.name, value: d.amount }))
      }]
    })
  })
}

// ============================================================
// 导出功能
// ============================================================
async function exportAccountReport() {
  try {
    await request.post('/admin/data/reports/accounts/export', {
      startDate: getDaysAgo(parseInt(accountTimeRange.value)),
      endDate: new Date().toISOString().split('T')[0],
      accountIds: selectedAccount.value ? [selectedAccount.value] : undefined
    })
    ElMessage.success('导出任务已创建，请在"导出记录"中下载')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

async function exportBillingReport() {
  try {
    await post('/admin/data/reports/billing/export', {
      startDate: getDaysAgo(parseInt(billingTimeRange.value)),
      endDate: new Date().toISOString().split('T')[0]
    })
    ElMessage.success('导出任务已创建，请在"导出记录"中下载')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// ============================================================
// 标签页切换
// ============================================================
function handleTabChange(tab) {
  nextTick(() => {
    window.dispatchEvent(new Event('resize'))
  })
  if (tab === 'operation') loadOperationData()
  else if (tab === 'lead') loadLeadData()
  else if (tab === 'account') loadAccountData()
  else if (tab === 'billing') loadBillingData()
}

// ============================================================
// 生命周期
// ============================================================
onMounted(async () => {
  await loadOverview()
  await loadPlatforms()
  await loadAccounts()
  loadOperationData()

  // 启动实时数据轮询
  realtimeTimer = setInterval(() => {
    if (activeTab.value === 'operation') loadOverview()
  }, 60000)
})

onUnmounted(() => {
  clearInterval(realtimeTimer)
  ;[operationChart, sourcePieChart, intentChart, funnelChart,
    leadChart, leadSourceChart, accountChart, accountRankChart,
    billingChart, billingPieChart].forEach(chart => chart?.dispose())
})
</script>

<style lang="scss" scoped>
.data-dashboard {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .stat-info {
    flex: 1;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 700;
    color: #303133;
    line-height: 1.2;
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
  }

  .stat-trend {
    display: flex;
    align-items: center;
    gap: 2px;
    font-size: 12px;
    margin-top: 4px;

    &.up { color: #67c23a; }
    &.down { color: #f56c6c; }
  }

  &.stat-primary .stat-icon { background: #ecf5ff; color: #409eff; }
  &.stat-success .stat-icon { background: #f0f9eb; color: #67c23a; }
  &.stat-warning .stat-icon { background: #fdf6ec; color: #e6a23c; }
  &.stat-danger .stat-icon { background: #fef0f0; color: #f56c6c; }
}

.dashboard-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.chart-row {
  margin-bottom: 20px;

  &:last-child { margin-bottom: 0; }
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #ebeef5;

  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .chart-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .chart-actions {
    display: flex;
    gap: 8px;
    align-items: center;
  }
}

.chart-container {
  width: 100%;
  height: 300px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
