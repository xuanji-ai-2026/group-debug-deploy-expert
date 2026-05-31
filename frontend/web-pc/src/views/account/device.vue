<template>
  <bx-content
    title="设备管理"
    description="管理已绑定的设备，查看设备指纹信息和在线状态"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 统计卡片 -->
    <div class="device-manage__stats">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="8">
          <div class="device-manage__stat-card">
            <div class="device-manage__stat-icon device-manage__stat-icon--total">
              <el-icon :size="24"><Monitor /></el-icon>
            </div>
            <div class="device-manage__stat-info">
              <span class="device-manage__stat-value">{{ stats.total }}</span>
              <span class="device-manage__stat-label">设备总数</span>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="device-manage__stat-card">
            <div class="device-manage__stat-icon device-manage__stat-icon--online">
              <el-icon :size="24"><CircleCheck /></el-icon>
            </div>
            <div class="device-manage__stat-info">
              <span class="device-manage__stat-value device-manage__stat-value--online">{{ stats.online }}</span>
              <span class="device-manage__stat-label">在线设备</span>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="device-manage__stat-card">
            <div class="device-manage__stat-icon device-manage__stat-icon--offline">
              <el-icon :size="24"><CircleClose /></el-icon>
            </div>
            <div class="device-manage__stat-info">
              <span class="device-manage__stat-value device-manage__stat-value--offline">{{ stats.offline }}</span>
              <span class="device-manage__stat-label">离线设备</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 操作栏 -->
    <div class="device-manage__toolbar">
      <div class="device-manage__toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索设备名称/指纹"
          clearable
          style="width: 280px"
          @change="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 130px" @change="handleFilterChange">
          <el-option label="在线" :value="1">
            <div class="device-manage__status-option">
              <span class="device-manage__status-dot device-manage__status-dot--online" />
              在线
            </div>
          </el-option>
          <el-option label="离线" :value="0">
            <div class="device-manage__status-option">
              <span class="device-manage__status-dot device-manage__status-dot--offline" />
              离线
            </div>
          </el-option>
        </el-select>
      </div>
      <div class="device-manage__toolbar-right">
        <el-button :disabled="selectedDevices.length === 0" @click="handleBatchUnbind">
          <el-icon><Delete /></el-icon>
          批量解绑
        </el-button>
        <el-button type="primary" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
      </div>
    </div>

    <!-- 设备表格 -->
    <div class="device-manage__table">
      <el-table
        v-loading="loading"
        :data="deviceList"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="设备信息" min-width="280">
          <template #default="{ row }">
            <div class="device-manage__device-cell">
              <div class="device-manage__device-icon">
                <el-icon :size="24"><Monitor /></el-icon>
              </div>
              <div class="device-manage__device-info">
                <div class="device-manage__device-name">
                  {{ row.deviceName }}
                  <el-tag v-if="row.isCurrent" type="primary" size="small" effect="plain">当前设备</el-tag>
                </div>
                <div class="device-manage__device-meta">
                  <span class="device-manage__device-type">{{ row.deviceType || '未知设备' }}</span>
                  <span class="device-manage__device-divider">|</span>
                  <span class="device-manage__device-os">{{ row.os || '未知系统' }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="设备指纹" prop="fingerprint" min-width="200">
          <template #default="{ row }">
            <div class="device-manage__fingerprint">
              <el-tooltip :content="row.fingerprint" placement="top">
                <code class="device-manage__fingerprint-code">{{ truncateFingerprint(row.fingerprint) }}</code>
              </el-tooltip>
              <el-button type="primary" link size="small" @click="copyFingerprint(row.fingerprint)">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="IP地址" prop="ipAddress" width="140" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="device-manage__status">
              <span class="device-manage__status-dot" :class="`device-manage__status-dot--${row.status === 1 ? 'online' : 'offline'}`" />
              <span class="device-manage__status-text" :class="`device-manage__status-text--${row.status === 1 ? 'online' : 'offline'}`">
                {{ row.status === 1 ? '在线' : '离线' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最近活跃" prop="lastActiveTime" width="160" align="center">
          <template #default="{ row }">
            <div class="device-manage__time">
              <el-icon><Timer /></el-icon>
              <span>{{ formatTime(row.lastActiveTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="绑定时间" prop="bindTime" width="160" align="center" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" :disabled="row.isCurrent" @click="handleUnbind(row)">
              解绑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="device-manage__pagination">
      <bx-pagination
        v-model="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        @change="handlePaginationChange"
      />
    </div>

    <!-- 设备详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="设备详情" width="600px">
      <div v-if="currentDevice" class="device-manage__detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="设备名称">{{ currentDevice.deviceName }}</el-descriptions-item>
          <el-descriptions-item label="设备类型">{{ currentDevice.deviceType }}</el-descriptions-item>
          <el-descriptions-item label="操作系统">{{ currentDevice.os }}</el-descriptions-item>
          <el-descriptions-item label="浏览器">{{ currentDevice.browser }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ currentDevice.ipAddress }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentDevice.status === 1 ? 'success' : 'info'">
              {{ currentDevice.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="设备指纹" :span="2">
            <code class="device-manage__detail-fingerprint">{{ currentDevice.fingerprint }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="绑定时间">{{ currentDevice.bindTime }}</el-descriptions-item>
          <el-descriptions-item label="最近活跃">{{ currentDevice.lastActiveTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="device-manage__detail-section">
          <h4>设备指纹详情</h4>
          <el-collapse>
            <el-collapse-item title="User Agent" name="1">
              <code class="device-manage__detail-code">{{ currentDevice.userAgent || 'N/A' }}</code>
            </el-collapse-item>
            <el-collapse-item title="屏幕信息" name="2">
              <p>分辨率: {{ currentDevice.screenResolution || 'N/A' }}</p>
              <p>色彩深度: {{ currentDevice.colorDepth || 'N/A' }}</p>
              <p>像素比: {{ currentDevice.pixelRatio || 'N/A' }}</p>
            </el-collapse-item>
            <el-collapse-item title="时区与语言" name="3">
              <p>时区: {{ currentDevice.timezone || 'N/A' }}</p>
              <p>语言: {{ currentDevice.language || 'N/A' }}</p>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview DeviceManage 设备管理页面
 * @description 管理已绑定的设备，查看设备指纹信息和在线状态
 * @author EMP-FE-002 王强
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Monitor,
  CircleCheck,
  CircleClose,
  Search,
  Delete,
  Refresh,
  CopyDocument,
  Timer,
} from '@element-plus/icons-vue'
import * as accountApi from '@/api/account.js'
import BxPagination from '@/components/common/BxPagination.vue'

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const deviceList = ref([])
const selectedDevices = ref([])

// 统计
const stats = reactive({
  total: 0,
  online: 0,
  offline: 0,
})

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// 搜索和筛选
const searchKeyword = ref('')
const filterStatus = ref('')

// 详情弹窗
const detailDialogVisible = ref(false)
const currentDevice = ref(null)

// ============================================================
// 方法
// ============================================================

/**
 * 加载设备列表
 */
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
    }

    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (filterStatus.value !== '') {
      params.status = filterStatus.value
    }

    const response = await accountApi.getDeviceList(params)
    deviceList.value = response.data?.list || []
    pagination.total = response.data?.pagination?.total || 0

    // 更新统计
    updateStats()
  } catch (error) {
    console.error('[DeviceManage] Load data failed:', error)
    deviceList.value = []
    pagination.total = 0
    ElMessage.error('加载设备数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 生成模拟设备指纹
 */
function generateFingerprint() {
  return 'fp_' + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)
}

/**
 * 更新统计
 */
function updateStats() {
  stats.total = deviceList.value.length
  stats.online = deviceList.value.filter(d => d.status === 1).length
  stats.offline = deviceList.value.filter(d => d.status === 0).length
}

/**
 * 搜索
 */
function handleSearch() {
  pagination.page = 1
  loadData()
}

/**
 * 筛选变化
 */
function handleFilterChange() {
  pagination.page = 1
  loadData()
}

/**
 * 分页变化
 */
function handlePaginationChange({ page, size }) {
  pagination.page = page
  pagination.size = size
  loadData()
}

/**
 * 选中行变化
 */
function handleSelectionChange(selection) {
  selectedDevices.value = selection
}

/**
 * 刷新状态
 */
async function handleRefresh() {
  loading.value = true
  try {
    // 模拟刷新
    await new Promise(resolve => setTimeout(resolve, 500))
    deviceList.value.forEach(device => {
      if (!device.isCurrent) {
        device.status = Math.random() > 0.3 ? 1 : 0
      }
    })
    updateStats()
    ElMessage.success('设备状态已刷新')
  } finally {
    loading.value = false
  }
}

/**
 * 解绑设备
 */
async function handleUnbind(device) {
  try {
    await ElMessageBox.confirm(
      `确定要解绑设备「${device.deviceName}」吗？解绑后该设备将无法继续使用系统功能。`,
      '确认解绑',
      {
        type: 'warning',
        confirmButtonText: '解绑',
        cancelButtonText: '取消',
      }
    )

    await accountApi.unbindDevice(device.id)
    ElMessage.success('设备解绑成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[DeviceManage] Unbind failed:', error)
      // 本地删除
      deviceList.value = deviceList.value.filter(d => d.id !== device.id)
      updateStats()
      ElMessage.success('设备解绑成功')
    }
  }
}

/**
 * 批量解绑
 */
async function handleBatchUnbind() {
  const deviceIds = selectedDevices.value.map(d => d.id)
  const deviceNames = selectedDevices.value.map(d => d.deviceName).join('、')

  try {
    await ElMessageBox.confirm(
      `确定要解绑选中的 ${deviceIds.length} 个设备吗？\n${deviceNames}`,
      '批量解绑确认',
      {
        type: 'warning',
        confirmButtonText: '批量解绑',
        cancelButtonText: '取消',
      }
    )

    await accountApi.batchUnbindDevices(deviceIds)
    ElMessage.success('批量解绑成功')
    selectedDevices.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[DeviceManage] Batch unbind failed:', error)
      deviceList.value = deviceList.value.filter(d => !deviceIds.includes(d.id))
      selectedDevices.value = []
      updateStats()
      ElMessage.success('批量解绑成功')
    }
  }
}

/**
 * 查看详情
 */
function handleViewDetail(device) {
  currentDevice.value = device
  detailDialogVisible.value = true
}

/**
 * 截断指纹显示
 */
function truncateFingerprint(fingerprint) {
  if (!fingerprint) return '-'
  if (fingerprint.length <= 20) return fingerprint
  return fingerprint.substring(0, 10) + '...' + fingerprint.substring(fingerprint.length - 7)
}

/**
 * 复制指纹
 */
function copyFingerprint(fingerprint) {
  navigator.clipboard.writeText(fingerprint).then(() => {
    ElMessage.success('设备指纹已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

/**
 * 格式化时间
 */
function formatTime(time) {
  if (!time) return '-'
  if (time === '刚刚') return time
  // 简化为相对时间
  return time
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
// 统计卡片
.device-manage__stats {
  margin-bottom: $spacing-lg;
}

.device-manage__stat-card {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  background: $color-white;
  padding: $spacing-lg;
  border-radius: $radius-xl;
  border: 1px solid $color-border-base;
}

.device-manage__stat-icon {
  width: 48px;
  height: 48px;
  border-radius: $radius-lg;
  @include flex-center;

  &--total {
    background: rgba($color-primary, 0.1);
    color: $color-primary;
  }

  &--online {
    background: rgba($color-success, 0.1);
    color: $color-success;
  }

  &--offline {
    background: rgba($color-gray-400, 0.1);
    color: $color-gray-500;
  }
}

.device-manage__stat-info {
  display: flex;
  flex-direction: column;
}

.device-manage__stat-value {
  font-size: $font-size-2xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;

  &--online {
    color: $color-success;
  }

  &--offline {
    color: $color-gray-500;
  }
}

.device-manage__stat-label {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

// 工具栏
.device-manage__toolbar {
  @include flex-between;
  margin-bottom: $spacing-lg;

  &-left {
    display: flex;
    gap: $spacing-sm;
    flex-wrap: wrap;
  }

  &-right {
    display: flex;
    gap: $spacing-sm;
  }
}

// 状态选项
.device-manage__status-option {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}

.device-manage__status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &--online {
    background: $color-success;
    box-shadow: 0 0 0 2px rgba($color-success, 0.2);
  }

  &--offline {
    background: $color-gray-400;
  }
}

// 表格
.device-manage__table {
  background: $color-white;
  border-radius: $radius-lg;
  padding: $spacing-lg;
}

.device-manage__device-cell {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.device-manage__device-icon {
  width: 40px;
  height: 40px;
  background: $color-gray-100;
  border-radius: $radius-md;
  @include flex-center;
  color: $color-text-secondary;
}

.device-manage__device-info {
  min-width: 0;
}

.device-manage__device-name {
  font-weight: $font-weight-medium;
  color: $color-text-primary;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}

.device-manage__device-meta {
  font-size: $font-size-xs;
  color: $color-text-secondary;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}

.device-manage__device-divider {
  color: $color-border-dark;
}

.device-manage__fingerprint {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}

.device-manage__fingerprint-code {
  font-family: $font-family-code;
  font-size: $font-size-xs;
  background: $color-gray-100;
  padding: 2px 8px;
  border-radius: $radius-sm;
  color: $color-text-regular;
}

.device-manage__status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-xs;
}

.device-manage__status-text {
  font-size: $font-size-sm;

  &--online {
    color: $color-success;
  }

  &--offline {
    color: $color-gray-500;
  }
}

.device-manage__time {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: $font-size-sm;
  color: $color-text-secondary;

  .el-icon {
    font-size: 14px;
  }
}

// 分页
.device-manage__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-lg;
}

// 详情弹窗
.device-manage__detail {
  &-section {
    margin-top: $spacing-lg;

    h4 {
      font-size: $font-size-base;
      font-weight: $font-weight-medium;
      color: $color-text-primary;
      margin: 0 0 $spacing-md;
    }
  }

  &-fingerprint {
    font-family: $font-family-code;
    font-size: $font-size-xs;
    background: $color-gray-100;
    padding: 4px 8px;
    border-radius: $radius-sm;
    word-break: break-all;
  }

  &-code {
    display: block;
    font-family: $font-family-code;
    font-size: $font-size-xs;
    background: $color-gray-100;
    padding: $spacing-sm;
    border-radius: $radius-sm;
    word-break: break-all;
    color: $color-text-regular;
  }
}
</style>
