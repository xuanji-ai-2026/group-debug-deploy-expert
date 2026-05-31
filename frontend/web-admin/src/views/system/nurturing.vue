<template>
  <div class="nurturing-admin">
    <!-- 操作栏 -->
    <div class="nurturing-admin__toolbar">
      <div class="nurturing-admin__toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索策略/账号"
          prefix-icon="Search"
          clearable
          style="width: 240px"
          @change="handleSearch"
        />
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 140px" @change="handleFilterChange">
          <el-option label="运行中" :value="1" />
          <el-option label="已停止" :value="0" />
        </el-select>
      </div>
      <div class="nurturing-admin__toolbar-right">
        <el-button type="primary" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="nurturing-admin__stats">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__content">
            <div class="stat-card__icon stat-card__icon--blue">
              <el-icon><List /></el-icon>
            </div>
            <div class="stat-card__info">
              <h3>{{ stats.total }}</h3>
              <p>总策略数</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__content">
            <div class="stat-card__icon stat-card__icon--green">
              <el-icon><VideoPlay /></el-icon>
            </div>
            <div class="stat-card__info">
              <h3>{{ stats.running }}</h3>
              <p>运行中</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__content">
            <div class="stat-card__icon stat-card__icon--orange">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-card__info">
              <h3>{{ stats.warning }}</h3>
              <p>异常警告</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__content">
            <div class="stat-card__icon stat-card__icon--red">
              <el-icon><CircleClose /></el-icon>
            </div>
            <div class="stat-card__info">
              <h3>{{ stats.stopped }}</h3>
              <p>已停止</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 策略列表 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="strategyList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        
        <el-table-column label="策略信息" min-width="200">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px">
              <el-avatar :size="32" :src="row.accountAvatar">{{ (row.accountName || '账').charAt(0) }}</el-avatar>
              <div>
                <div style="font-weight: 600">{{ row.strategyName || '未命名' }}</div>
                <div style="font-size: 12px; color: #909399">{{ row.accountName }} ({{ getPlatformName(row.platform) }})</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="每日目标" width="200">
          <template #default="{ row }">
            <el-tag size="small" style="margin: 2px">👍 {{ row.dailyTargets?.likeCount || 0 }}</el-tag>
            <el-tag size="small" type="success" style="margin: 2px">💬 {{ row.dailyTargets?.commentCount || 0 }}</el-tag>
            <el-tag size="small" type="warning" style="margin: 2px">↗️ {{ row.dailyTargets?.shareCount || 0 }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="风险等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getRiskType(row.riskLevel)" size="small">
              {{ getRiskLabel(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="周期" width="80" align="center">
          <template #default="{ row }">
            {{ row.durationDays || 7 }}天
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              :loading="row._toggling"
              @change="(val) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <div v-if="progressMap[row.id]" style="display: flex; align-items: center; gap: 8px">
              <el-progress
                :percentage="progressMap[row.id].progressPercentage || 0"
                :stroke-width="10"
                style="flex: 1"
              />
              <span style="font-size: 12px; color: #909399; min-width: 40px">
                {{ progressMap[row.id].progressPercentage?.toFixed(0) }}%
              </span>
            </div>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewProgress(row)">
              进度详情
            </el-button>
            <el-button
              v-if="row.enabled === 1"
              link
              type="danger"
              size="small"
              @click="handleStop(row)"
            >
              停止
            </el-button>
            <el-button
              v-else
              link
              type="success"
              size="small"
              @click="handleStart(row)"
            >
              启动
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 进度详情对话框 -->
    <el-dialog v-model="progressDialogVisible" title="执行进度详情" width="650px">
      <div v-if="currentProgress" class="progress-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="策略名称">{{ currentStrategy?.strategyName }}</el-descriptions-item>
          <el-descriptions-item label="执行状态">
            <el-tag :type="getStatusType(currentProgress.status)">{{ currentProgress.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="完成度">
            <el-progress
              :percentage="currentProgress.progressPercentage || 0"
              :status="getProgressStatus(currentProgress.status)"
              style="width: 200px"
            />
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ currentProgress.startTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">任务明细</el-divider>
        
        <div v-if="currentProgress.dailyTargets" class="task-list">
          <div
            v-for="(target, key) in currentProgress.dailyTargets"
            :key="key"
            class="task-item"
          >
            <span class="task-label">{{ getTaskLabel(key) }}</span>
            <el-progress
              :percentage="calculateTaskProgress(target, currentProgress.completedCounts?.[key])"
              :stroke-width="18"
              :text-inside="true"
              style="flex: 1; margin: 0 16px"
            />
            <span class="task-value">
              {{ currentProgress.completedCounts?.[key] || 0 }} / {{ target }}
            </span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  List,
  VideoPlay,
  Warning,
  CircleClose
} from '@element-plus/icons-vue'
import {
  getNurturingStrategyList,
  toggleNurturingStatus,
  startNurturingStrategy,
  stopNurturingStrategy,
  getNurturingProgress
} from '@/api/nurturing.js'

const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref(null)
const strategyList = ref([])
const progressMap = ref({})
const progressDialogVisible = ref(false)
const currentStrategy = ref(null)
const currentProgress = ref(null)

const stats = reactive({
  total: 0,
  running: 0,
  warning: 0,
  stopped: 0
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (filterStatus.value !== null) params.enabled = filterStatus.value
    
    const res = await getNurturingStrategyList(params)
    const list = res.data || []
    
    strategyList.value = list.slice(
      (pagination.page - 1) * pagination.size,
      pagination.page * pagination.size
    )
    pagination.total = list.length

    stats.total = list.length
    stats.running = list.filter(s => s.enabled === 1).length
    stats.stopped = list.filter(s => s.enabled !== 1).length
    stats.warning = list.filter(s => s.riskLevel === 'HIGH' && s.enabled === 1).length

    for (const strategy of strategyList.value) {
      if (strategy.enabled === 1) {
        try {
          const progressRes = await getNurturingProgress(strategy.id)
          progressMap.value[strategy.id] = progressRes.data
        } catch (e) {
          console.warn('获取进度失败:', e)
        }
      }
    }
  } catch (e) {
    console.error('加载数据失败:', e)
    ElMessage.error('加载策略数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleFilterChange() {
  pagination.page = 1
  loadData()
}

function handleRefresh() {
  loadData()
  ElMessage.success('数据已刷新')
}

async function handleToggleStatus(strategy, enabled) {
  strategy._toggling = true
  try {
    await toggleNurturingStatus(strategy.id, enabled ? 1 : 0)
    ElMessage.success(enabled ? '策略已启用' : '策略已禁用')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    strategy._toggling = false
  }
}

async function handleStart(strategy) {
  try {
    await startNurturingStrategy(strategy.id)
    ElMessage.success('策略已启动')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '启动失败')
  }
}

async function handleStop(strategy) {
  try {
    await stopNurturingStrategy(strategy.id)
    ElMessage.success('策略已停止')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '停止失败')
  }
}

async function handleViewProgress(strategy) {
  currentStrategy.value = strategy
  progressDialogVisible.value = true
  try {
    const res = await getNurturingProgress(strategy.id)
    currentProgress.value = res.data
  } catch (e) {
    ElMessage.error('获取进度失败')
  }
}

function getPlatformName(platform) {
  const map = { douyin: '抖音', xiaohongshu: '小红书', shipinhao: '视频号', weibo: '微博', bilibili: 'B站' }
  return map[platform] || platform
}

function getRiskType(level) {
  const map = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }
  return map[level] || 'info'
}

function getRiskLabel(level) {
  const map = { LOW: '低风险', MEDIUM: '中等', HIGH: '高风险' }
  return map[level] || level
}

function getStatusType(status) {
  const map = { RUNNING: '', COMPLETED: 'success', STOPPED: 'info', NOT_RUNNING: 'info' }
  return map[status] || 'info'
}

function getProgressStatus(status) {
  if (status === 'COMPLETED') return 'success'
  return undefined
}

function getTaskLabel(key) {
  const map = { likeCount: '点赞', commentCount: '评论', shareCount: '分享', followCount: '关注', browseDuration: '浏览' }
  return map[key] || key
}

function calculateTaskProgress(target, completed) {
  if (!target || target === 0) return 0
  return Math.min(((completed || 0) / target) * 100, 100)
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.nurturing-admin {
  padding: 20px;

  &__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    &-left {
      display: flex;
      gap: 12px;
    }
  }

  &__stats {
    margin-bottom: 20px;
  }
}

.stat-card {
  &__content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: white;

    &--blue { background: linear-gradient(135deg, #409eff, #66b1ff); }
    &--green { background: linear-gradient(135deg, #67c23a, #85ce61); }
    &--orange { background: linear-gradient(135deg, #e6a23c, #ebb563); }
    &--red { background: linear-gradient(135deg, #f56c6c, #f78989); }
  }

  &__info {
    h3 {
      margin: 0;
      font-size: 28px;
      font-weight: 700;
      color: #303133;
    }

    p {
      margin: 4px 0 0;
      font-size: 14px;
      color: #909399;
    }
  }
}

.progress-detail {
  padding: 16px 0;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-item {
  display: flex;
  align-items: center;

  .task-label {
    width: 60px;
    font-size: 14px;
    color: #606266;
  }

  .task-value {
    width: 80px;
    text-align: right;
    font-size: 13px;
    color: #909399;
  }
}
</style>
