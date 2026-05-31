<template>
  <div class="monitor-page">
    <!-- 概览卡片 -->
    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <div class="overview-card overview-success">
          <div class="overview-icon"><el-icon size="28"><SuccessFilled /></el-icon></div>
          <div class="overview-info">
            <div class="overview-value">{{ statusSummary.running }}</div>
            <div class="overview-label">运行正常</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="overview-card overview-warning">
          <div class="overview-icon"><el-icon size="28"><WarnTriangleFilled /></el-icon></div>
          <div class="overview-info">
            <div class="overview-value">{{ statusSummary.warning }}</div>
            <div class="overview-label">运行警告</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="overview-card overview-danger">
          <div class="overview-icon"><el-icon size="28"><CircleCloseFilled /></el-icon></div>
          <div class="overview-info">
            <div class="overview-value">{{ statusSummary.error }}</div>
            <div class="overview-label">服务异常</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="overview-card overview-info-card">
          <div class="overview-icon"><el-icon size="28"><InfoFilled /></el-icon></div>
          <div class="overview-info">
            <div class="overview-value">{{ statusSummary.unknown }}</div>
            <div class="overview-label">未知状态</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 实时刷新控制 -->
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <span class="last-update">最后更新: {{ lastUpdateTime }}</span>
        <el-tag :type="refreshing ? 'success' : 'info'" size="small">
          {{ refreshing ? '实时监控中' : '已暂停' }}
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-button :icon="refreshing ? 'VideoPause' : 'VideoPlay'" size="small" @click="toggleRefresh">
          {{ refreshing ? '暂停' : '开启' }}实时监控
        </el-button>
        <el-button icon="Refresh" size="small" @click="loadMonitorData">刷新</el-button>
      </div>
    </div>

    <!-- 服务状态列表 -->
    <div class="service-section">
      <div class="section-title">🐳 微服务状态</div>
      <el-row :gutter="16">
        <el-col v-for="service in microservices" :key="service.name" :span="12">
          <div class="service-card" :class="getServiceStatusClass(service.status)">
            <div class="service-header">
              <div class="service-name">
                <el-icon size="18"><Component /></el-icon>
                {{ service.name }}
              </div>
              <el-tag :type="getStatusTagType(service.status)" size="small">
                {{ getStatusLabel(service.status) }}
              </el-tag>
            </div>
            <div class="service-body">
              <div class="service-meta">
                <span class="meta-item">
                  <span class="meta-label">地址</span>
                  <code>{{ service.host }}:{{ service.port }}</code>
                </span>
                <span class="meta-item">
                  <span class="meta-label">版本</span>
                  <span>{{ service.version }}</span>
                </span>
                <span class="meta-item">
                  <span class="meta-label">运行时长</span>
                  <span>{{ service.uptime }}</span>
                </span>
              </div>
              <div class="service-metrics">
                <div class="metric-item">
                  <div class="metric-label">CPU</div>
                  <el-progress
                    :percentage="service.cpu"
                    :color="getCpuColor(service.cpu)"
                    :stroke-width="6"
                    style="width: 120px"
                  />
                  <div class="metric-value">{{ service.cpu }}%</div>
                </div>
                <div class="metric-item">
                  <div class="metric-label">内存</div>
                  <el-progress
                    :percentage="service.memory"
                    :color="getMemoryColor(service.memory)"
                    :stroke-width="6"
                    style="width: 120px"
                  />
                  <div class="metric-value">{{ service.memory }}%</div>
                </div>
              </div>
            </div>
            <div class="service-footer">
              <span class="health-info">
                <el-icon><Timer /></el-icon>
                响应: {{ service.responseTime }}ms
              </span>
              <span class="health-info">
                <el-icon><Connection /></el-icon>
                QPS: {{ service.qps }}
              </span>
              <span class="health-info" :class="service.errorRate > 0 ? 'text-danger' : ''">
                <el-icon><WarnTriangleFilled /></el-icon>
                错误率: {{ service.errorRate }}%
              </span>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 资源使用情况 -->
    <div class="resource-section">
      <div class="section-title">📊 资源使用情况</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">CPU使用率趋势</div>
            <div ref="cpuChartRef" class="chart-container"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">内存使用率趋势</div>
            <div ref="memoryChartRef" class="chart-container"></div>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">JVM堆内存使用</div>
            <div ref="jvmChartRef" class="chart-container"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">磁盘使用情况</div>
            <div ref="diskChartRef" class="chart-container"></div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 数据库连接池 -->
    <div class="db-section">
      <div class="section-title">🗄️ 数据库连接池</div>
      <el-table :data="dbPools" stripe style="margin-top: 12px">
        <el-table-column prop="poolName" label="连接池名称" min-width="150" />
        <el-table-column prop="dbType" label="数据库类型" width="120" />
        <el-table-column prop="activeCount" label="活跃连接" width="100" align="center">
          <template #default="{ row }">
            <span :class="row.activeCount > row.maxActive * 0.8 ? 'text-danger' : ''">{{ row.activeCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="idleCount" label="空闲连接" width="100" align="center" />
        <el-table-column prop="maxActive" label="最大连接" width="100" align="center" />
        <el-table-column label="使用率" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(row.activeCount / row.maxActive * 100)"
              :color="row.activeCount / row.maxActive > 0.8 ? '#f56c6c' : '#409eff'"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column prop="waitThreadCount" label="等待线程" width="100" align="center">
          <template #default="{ row }">
            <span :class="row.waitThreadCount > 0 ? 'text-danger' : ''">{{ row.waitThreadCount }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Redis状态 -->
    <div class="redis-section">
      <div class="section-title">⚡ Redis缓存状态</div>
      <el-row :gutter="16" style="margin-top: 12px">
        <el-col v-for="redis in redisNodes" :key="redis.node" :span="8">
          <div class="redis-card">
            <div class="redis-header">
              <span class="redis-node">{{ redis.node }}</span>
              <el-tag :type="redis.connected ? 'success' : 'danger'" size="small">
                {{ redis.connected ? '已连接' : '断开' }}
              </el-tag>
            </div>
            <div class="redis-metrics">
              <div class="redis-metric">
                <span class="rm-label">内存使用</span>
                <span class="rm-value">{{ redis.usedMemory }}MB</span>
              </div>
              <div class="redis-metric">
                <span class="rm-label">峰值内存</span>
                <span class="rm-value">{{ redis.peakMemory }}MB</span>
              </div>
              <div class="redis-metric">
                <span class="rm-label">连接数</span>
                <span class="rm-value">{{ redis.connectedClients }}</span>
              </div>
              <div class="redis-metric">
                <span class="rm-label">命中率</span>
                <span class="rm-value">{{ redis.hitRate }}%</span>
              </div>
              <div class="redis-metric">
                <span class="rm-label">QPS</span>
                <span class="rm-value">{{ redis.qps }}</span>
              </div>
              <div class="redis-metric">
                <span class="rm-label">已运行</span>
                <span class="rm-value">{{ redis.uptime }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 网络IO -->
    <div class="net-section">
      <div class="section-title">🌐 网络IO监控</div>
      <el-row :gutter="20" style="margin-top: 12px">
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">网络带宽使用</div>
            <div ref="networkChartRef" class="chart-container"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">请求QPS趋势</div>
            <div ref="qpsChartRef" class="chart-container"></div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
/**
 * 服务监控页面
 * 功能：微服务状态、资源使用、数据库连接池、Redis状态监控
 * @author 张前端 (EMP-FE-001)
 */

import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { get } from '@/utils/request'
import { SuccessFilled, WarnTriangleFilled, CircleCloseFilled, InfoFilled, Cpu, Timer, Connection } from '@element-plus/icons-vue'

// ============================================================
// 状态
// ============================================================
const refreshing = ref(true)
const lastUpdateTime = ref('--')
const refreshTimer = ref(null)

const statusSummary = reactive({ running: 0, warning: 0, error: 0, unknown: 0 })
const microservices = ref([])
const dbPools = ref([])
const redisNodes = ref([])

const cpuChartRef = ref(null)
const memoryChartRef = ref(null)
const jvmChartRef = ref(null)
const diskChartRef = ref(null)
const networkChartRef = ref(null)
const qpsChartRef = ref(null)

let cpuChart = null
let memoryChart = null
let jvmChart = null
let diskChart = null
let networkChart = null
let qpsChart = null

const cpuHistory = ref([])
const memoryHistory = ref([])

// ============================================================
// 工具函数
// ============================================================
function getStatusLabel(status) {
  const map = { running: '运行中', warning: '警告', error: '异常', unknown: '未知' }
  return map[status] || '未知'
}

function getStatusTagType(status) {
  const map = { running: 'success', warning: 'warning', error: 'danger', unknown: 'info' }
  return map[status] || 'info'
}

function getServiceStatusClass(status) {
  return `service-${status}`
}

function getCpuColor(val) {
  if (val < 50) return '#67c23a'
  if (val < 80) return '#e6a23c'
  return '#f56c6c'
}

function getMemoryColor(val) {
  if (val < 60) return '#409eff'
  if (val < 85) return '#e6a23c'
  return '#f56c6c'
}

function updateLastTime() {
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
}

// ============================================================
// 数据加载
// ============================================================
async function loadMonitorData() {
  updateLastTime()
  try {
    const data = await fetch('/admin/system/monitor/data').then(r => r.json()).catch(() => null)
    if (data) {
      microservices.value = data.microservices || []
      dbPools.value = data.dbPools || []
      redisNodes.value = data.redisNodes || []
      Object.assign(statusSummary, data.statusSummary || {})
      if (data.cpuHistory) cpuHistory.value = data.cpuHistory
      if (data.memoryHistory) memoryHistory.value = data.memoryHistory
    } else {
      loadDemoData()
    }
  } catch {
    loadDemoData()
  }
  renderCharts()
}

function loadDemoData() {
  microservices.value = [
    { name: 'API Gateway', host: '192.168.1.10', port: 8080, version: 'v2.3.1', status: 'running', uptime: '15天6小时', cpu: 32, memory: 45, responseTime: 12, qps: 1256, errorRate: 0.02 },
    { name: '用户服务', host: '192.168.1.11', port: 8081, version: 'v1.8.0', status: 'running', uptime: '15天6小时', cpu: 28, memory: 52, responseTime: 8, qps: 856, errorRate: 0.01 },
    { name: '租户服务', host: '192.168.1.12', port: 8082, version: 'v1.7.2', status: 'running', uptime: '15天6小时', cpu: 18, memory: 38, responseTime: 6, qps: 420, errorRate: 0.0 },
    { name: '商机服务', host: '192.168.1.13', port: 8083, version: 'v2.1.0', status: 'warning', uptime: '3天12小时', cpu: 78, memory: 68, responseTime: 45, qps: 680, errorRate: 1.2 },
    { name: '内容服务', host: '192.168.1.14', port: 8084, version: 'v1.5.0', status: 'running', uptime: '15天6小时', cpu: 22, memory: 41, responseTime: 9, qps: 320, errorRate: 0.0 },
    { name: '计费服务', host: '192.168.1.15', port: 8085, version: 'v1.6.0', status: 'running', uptime: '15天6小时', cpu: 15, memory: 35, responseTime: 5, qps: 180, errorRate: 0.0 },
    { name: '风控服务', host: '192.168.1.16', port: 8086, version: 'v2.0.0', status: 'running', uptime: '15天6小时', cpu: 35, memory: 48, responseTime: 15, qps: 560, errorRate: 0.05 },
    { name: '消息服务', host: '192.168.1.17', port: 8087, version: 'v1.4.0', status: 'error', uptime: '-', cpu: 0, memory: 0, responseTime: -1, qps: 0, errorRate: 100 },
    { name: '社媒服务', host: '192.168.1.18', port: 8088, version: 'v1.9.0', status: 'running', uptime: '15天6小时', cpu: 42, memory: 55, responseTime: 22, qps: 720, errorRate: 0.3 },
    { name: '存储服务', host: '192.168.1.19', port: 8089, version: 'v1.3.0', status: 'running', uptime: '15天6小时', cpu: 12, memory: 28, responseTime: 3, qps: 80, errorRate: 0.0 }
  ]

  dbPools.value = [
    { poolName: '主库连接池', dbType: 'MySQL', activeCount: 35, idleCount: 15, maxActive: 50, waitThreadCount: 0 },
    { poolName: '从库连接池', dbType: 'MySQL', activeCount: 12, idleCount: 38, maxActive: 50, waitThreadCount: 0 },
    { poolName: '业务库连接池', dbType: 'PostgreSQL', activeCount: 28, idleCount: 22, maxActive: 50, waitThreadCount: 0 }
  ]

  redisNodes.value = [
    { node: 'Redis Master', connected: true, usedMemory: 256, peakMemory: 512, connectedClients: 48, hitRate: 94.5, qps: 12000, uptime: '15天6小时' },
    { node: 'Redis Slave 1', connected: true, usedMemory: 248, peakMemory: 480, connectedClients: 32, hitRate: 93.8, qps: 8500, uptime: '15天6小时' },
    { node: 'Redis Slave 2', connected: true, usedMemory: 235, peakMemory: 500, connectedClients: 28, hitRate: 95.1, qps: 7800, uptime: '15天6小时' }
  ]

  statusSummary.running = 8
  statusSummary.warning = 1
  statusSummary.error = 1
  statusSummary.unknown = 0

  // 生成历史数据
  const now = new Date()
  cpuHistory.value = Array.from({ length: 30 }, (_, i) => {
    const t = new Date(now - (29 - i) * 60000)
    return { time: t.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }), value: Math.round(Math.random() * 40 + 20) }
  })
  memoryHistory.value = Array.from({ length: 30 }, (_, i) => {
    const t = new Date(now - (29 - i) * 60000)
    return { time: t.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }), value: Math.round(Math.random() * 20 + 35) }
  })
}

// ============================================================
// 图表渲染
// ============================================================
function renderCharts() {
  nextTick(() => {
    renderCpuChart()
    renderMemoryChart()
    renderJvmChart()
    renderDiskChart()
    renderNetworkChart()
    renderQpsChart()
  })
}

function renderCpuChart() {
  if (cpuChart) cpuChart.dispose()
  cpuChart = echarts.init(cpuChartRef.value)
  cpuChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: cpuHistory.value.map(d => d.time), boundaryGap: false },
    yAxis: { type: 'value', max: 100, name: '%' },
    series: [{
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3, color: '#409eff' },
      data: cpuHistory.value.map(d => d.value),
      lineStyle: { color: '#409eff' },
      itemStyle: { color: '#409eff' }
    }]
  })
}

function renderMemoryChart() {
  if (memoryChart) memoryChart.dispose()
  memoryChart = echarts.init(memoryChartRef.value)
  memoryChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: memoryHistory.value.map(d => d.time), boundaryGap: false },
    yAxis: { type: 'value', max: 100, name: '%' },
    series: [{
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3, color: '#67c23a' },
      data: memoryHistory.value.map(d => d.value),
      lineStyle: { color: '#67c23a' },
      itemStyle: { color: '#67c23a' }
    }]
  })
}

function renderJvmChart() {
  if (jvmChart) jvmChart.dispose()
  jvmChart = echarts.init(jvmChartRef.value)
  jvmChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}MB ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      label: { show: true, formatter: '{b}\n{c}MB' },
      data: [
        { name: '年轻代', value: 512, itemStyle: { color: '#67c23a' } },
        { name: '老年代', value: 1024, itemStyle: { color: '#409eff' } },
        { name: '元空间', value: 256, itemStyle: { color: '#e6a23c' } },
        { name: '已用', value: 384, itemStyle: { color: '#f56c6c' } }
      ]
    }]
  })
}

function renderDiskChart() {
  if (diskChart) diskChart.dispose()
  diskChart = echarts.init(diskChartRef.value)
  diskChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}GB ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      label: { show: true, formatter: '{b}\n{d}%' },
      data: [
        { name: '已使用', value: 180, itemStyle: { color: '#f56c6c' } },
        { name: '可用', value: 820, itemStyle: { color: '#e8e8e8' } }
      ]
    }]
  })
}

function renderNetworkChart() {
  if (networkChart) networkChart.dispose()
  networkChart = echarts.init(networkChartRef.value)
  const now = new Date()
  const labels = Array.from({ length: 20 }, (_, i) => {
    const t = new Date(now - (19 - i) * 60000)
    return t.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  })
  networkChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['入站', '出站'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: labels, boundaryGap: false },
    yAxis: { type: 'value', name: 'Mbps' },
    series: [
      { name: '入站', type: 'line', smooth: true, data: labels.map(() => +(Math.random() * 50 + 20).toFixed(1)) },
      { name: '出站', type: 'line', smooth: true, data: labels.map(() => +(Math.random() * 30 + 10).toFixed(1)) }
    ]
  })
}

function renderQpsChart() {
  if (qpsChart) qpsChart.dispose()
  qpsChart = echarts.init(qpsChartRef.value)
  const now = new Date()
  const labels = Array.from({ length: 20 }, (_, i) => {
    const t = new Date(now - (19 - i) * 60000)
    return t.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  })
  qpsChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: labels, boundaryGap: false },
    yAxis: { type: 'value', name: 'QPS' },
    series: [{
      type: 'bar',
      data: labels.map(() => Math.round(Math.random() * 2000 + 500)),
      itemStyle: { color: '#409eff' }
    }]
  })
}

// ============================================================
// 实时刷新
// ============================================================
function toggleRefresh() {
  refreshing.value = !refreshing.value
  if (refreshing.value) {
    startRefresh()
  } else {
    stopRefresh()
  }
}

function startRefresh() {
  refreshTimer.value = setInterval(() => {
    loadMonitorData()
  }, 10000)
}

function stopRefresh() {
  clearInterval(refreshTimer.value)
}

onMounted(() => {
  loadMonitorData()
  startRefresh()
})

onUnmounted(() => {
  stopRefresh()
  ;[cpuChart, memoryChart, jvmChart, diskChart, networkChart, qpsChart].forEach(c => c?.dispose())
})
</script>

<style lang="scss" scoped>
.monitor-page {
  padding: 20px;
}

.overview-cards {
  margin-bottom: 20px;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);

  .overview-icon {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .overview-value {
    font-size: 28px;
    font-weight: 700;
    color: #303133;
  }

  .overview-label {
    font-size: 14px;
    color: #909399;
    margin-top: 2px;
  }

  &.overview-success .overview-icon { background: #f0f9eb; color: #67c23a; }
  &.overview-warning .overview-icon { background: #fdf6ec; color: #e6a23c; }
  &.overview-danger .overview-icon { background: #fef0f0; color: #f56c6c; }
  &.overview-info-card .overview-icon { background: #ecf5ff; color: #909399; }
}

.monitor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;

  .last-update {
    color: #909399;
    font-size: 13px;
    margin-right: 12px;
  }
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.service-section,
.resource-section,
.db-section,
.redis-section,
.net-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.service-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  margin-bottom: 16px;
  overflow: hidden;
  transition: all 0.3s;

  &:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }

  &.service-running { border-left: 4px solid #67c23a; }
  &.service-warning { border-left: 4px solid #e6a23c; }
  &.service-error { border-left: 4px solid #f56c6c; background: #fef0f0; }
}

.service-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;

  .service-name {
    font-weight: 600;
    color: #303133;
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.service-body {
  padding: 12px 16px;
}

.service-meta {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;

  .meta-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;

    .meta-label { color: #909399; }
  }

  code {
    font-family: 'Courier New', monospace;
    font-size: 12px;
    color: #409eff;
    background: #ecf5ff;
    padding: 1px 4px;
    border-radius: 3px;
  }
}

.service-metrics {
  display: flex;
  gap: 24px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 8px;

  .metric-label {
    font-size: 13px;
    color: #909399;
    width: 30px;
  }

  .metric-value {
    font-size: 13px;
    color: #303133;
    width: 40px;
    text-align: right;
  }
}

.service-footer {
  display: flex;
  gap: 20px;
  padding: 8px 16px;
  background: #fafafa;
  border-top: 1px solid #ebeef5;

  .health-info {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #909399;
  }
}

.chart-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;

  .chart-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 12px;
  }
}

.chart-container {
  width: 100%;
  height: 220px;
}

.redis-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;

  .redis-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .redis-node {
      font-weight: 600;
      color: #303133;
    }
  }

  .redis-metrics {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;

    .redis-metric {
      display: flex;
      justify-content: space-between;
      font-size: 13px;

      .rm-label { color: #909399; }
      .rm-value { color: #303133; font-weight: 500; }
    }
  }
}

.text-danger { color: #f56c6c; }
</style>
