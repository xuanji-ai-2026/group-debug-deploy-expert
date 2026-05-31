<template>
  <bx-content
    title="养号策略管理"
    description="智能养号策略配置与执行监控，提升账号权重和活跃度"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <div class="nurturing-management">
      <!-- 操作栏 -->
      <div class="nurturing-management__toolbar">
        <div class="nurturing-management__toolbar-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索策略名称"
            clearable
            style="width: 240px"
            @change="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 140px" @change="handleFilterChange">
            <el-option label="启用中" :value="1">
              <el-tag type="success" size="small">启用</el-tag>
            </el-option>
            <el-option label="已禁用" :value="0">
              <el-tag type="info" size="small">禁用</el-tag>
            </el-option>
          </el-select>
        </div>
        <div class="nurturing-management__toolbar-right">
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新建策略
          </el-button>
        </div>
      </div>

      <!-- 策略列表 -->
      <div v-loading="loading" class="nurturing-management__list">
        <el-row :gutter="20">
          <el-col v-for="strategy in strategyList" :key="strategy.id" :xs="24" :sm="12" :md="8" :lg="8" :xl="6">
            <el-card class="nurturing-card" shadow="hover" :class="{ 'nurturing-card--disabled': strategy.enabled !== 1 }">
              <!-- 卡片头部 -->
              <div class="nurturing-card__header">
                <div class="nurturing-card__title-section">
                  <h4 class="nurturing-card__title">{{ strategy.strategyName || '未命名策略' }}</h4>
                  <el-tag
                    :type="strategy.enabled === 1 ? 'success' : 'info'"
                    size="small"
                    effect="dark"
                  >
                    {{ strategy.enabled === 1 ? '运行中' : '已停止' }}
                  </el-tag>
                </div>
                <el-dropdown trigger="click" @command="handleCommand($event, strategy)">
                  <el-button type="info" link>
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="edit">编辑策略</el-dropdown-item>
                      <el-dropdown-item command="progress">查看进度</el-dropdown-item>
                      <el-dropdown-item v-if="strategy.enabled === 1" command="stop">停止执行</el-dropdown-item>
                      <el-dropdown-item v-else command="start">启动执行</el-dropdown-item>
                      <el-dropdown-item divided command="delete" style="color: #f56c6c">删除策略</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <!-- 账号信息 -->
              <div class="nurturing-card__account">
                <el-avatar :size="32" :src="strategy.accountAvatar">
                  {{ (strategy.accountName || '账').charAt(0) }}
                </el-avatar>
                <span class="nurturing-card__account-name">{{ strategy.accountName || '未知账号' }}</span>
              </div>

              <!-- 风险等级 -->
              <div class="nurturing-card__risk">
                <span class="nurturing-card__risk-label">风险等级：</span>
                <el-tag
                  :type="getRiskType(strategy.riskLevel)"
                  size="small"
                >
                  {{ getRiskLabel(strategy.riskLevel) }}
                </el-tag>
              </div>

              <!-- 进度条 -->
              <div v-if="progressMap[strategy.id]" class="nurturing-card__progress">
                <div class="nurturing-card__progress-header">
                  <span>今日进度</span>
                  <span>{{ progressMap[strategy.id].progressPercentage?.toFixed(1) }}%</span>
                </div>
                <el-progress
                  :percentage="progressMap[strategy.id].progressPercentage || 0"
                  :status="getProgressStatus(progressMap[strategy.id]?.status)"
                  :stroke-width="10"
                />
              </div>

              <!-- 每日目标摘要 -->
              <div class="nurturing-card__targets">
                <div class="nurturing-card__target-item">
                  <el-icon><Star /></el-icon>
                  <span>点赞 {{ strategy.dailyTargets?.likeCount || 0 }}</span>
                </div>
                <div class="nurturing-card__target-item">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>评论 {{ strategy.dailyTargets?.commentCount || 0 }}</span>
                </div>
                <div class="nurturing-card__target-item">
                  <el-icon><Share /></el-icon>
                  <span>分享 {{ strategy.dailyTargets?.shareCount || 0 }}</span>
                </div>
              </div>

              <!-- 周期信息 -->
              <div class="nurturing-card__footer">
                <span class="nurturing-card__duration">
                  <el-icon><Timer /></el-icon>
                  周期 {{ strategy.durationDays || 7 }} 天
                </span>
                <span class="nurturing-card__time">{{ formatTime(strategy.createTime) }}</span>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 空状态 -->
        <el-empty v-if="strategyList.length === 0 && !loading" description="暂无养号策略">
          <el-button type="primary" @click="handleCreate">创建第一个策略</el-button>
        </el-empty>
      </div>

      <!-- 创建/编辑对话框 -->
      <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑养号策略' : '新建养号策略'"
        width="700px"
        destroy-on-close
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
          label-position="top"
        >
          <!-- 基本信息 -->
          <el-divider content-position="left">基本信息</el-divider>
          
          <el-form-item label="策略名称" prop="strategyName">
            <el-input v-model="form.strategyName" placeholder="输入策略名称，如：新手养号-抖音账号" maxlength="50" />
          </el-form-item>

          <el-form-item label="关联账号" prop="accountId">
            <el-select
              v-model="form.accountId"
              placeholder="选择要养号的社媒账号"
              style="width: 100%"
              filterable
            >
              <el-option
                v-for="account in accountList"
                :key="account.id"
                :label="`${account.accountName} (${getPlatformName(account.platform)})`"
                :value="account.id"
              >
                <div style="display: flex; align-items: center; gap: 8px">
                  <el-avatar :size="24" :src="account.avatar">{{ account.accountName?.charAt(0) }}</el-avatar>
                  <span>{{ account.accountName }}</span>
                  <el-tag size="small">{{ getPlatformName(account.platform) }}</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <!-- 模板选择 -->
          <el-divider content-position="left">快速配置（可选）</el-divider>
          
          <el-form-item label="使用模板">
            <el-select
              v-model="selectedTemplate"
              placeholder="选择预定义模板快速填充"
              clearable
              style="width: 100%"
              @change="handleTemplateChange"
            >
              <el-option
                v-for="tpl in templateList"
                :key="tpl.name"
                :label="`${tpl.name} - ${tpl.description}`"
                :value="tpl.name"
              >
                <div>
                  <strong>{{ tpl.name }}</strong>
                  <p style="color: #909399; margin: 0; font-size: 12px">{{ tpl.description }}</p>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <!-- 每日目标设置 -->
          <el-divider content-position="left">每日目标</el-divider>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="每日点赞数">
                <el-input-number v-model="form.dailyTargets.likeCount" :min="0" :max="100" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="每日评论数">
                <el-input-number v-model="form.dailyTargets.commentCount" :min="0" :max="50" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="每日分享数">
                <el-input-number v-model="form.dailyTargets.shareCount" :min="0" :max="30" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="每日关注数">
                <el-input-number v-model="form.dailyTargets.followCount" :min="0" :max="50" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="浏览时长（分钟）">
            <el-input-number v-model="form.dailyTargets.browseDuration" :min="0" :max="180" :step="5" style="width: 100%" />
          </el-form-item>

          <!-- 高级设置 -->
          <el-divider content-position="left">高级设置</el-divider>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="养号周期（天）">
                <el-input-number v-model="form.durationDays" :min="1" :max="90" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="风险等级">
                <el-radio-group v-model="form.riskLevel">
                  <el-radio label="LOW">低风险</el-radio>
                  <el-radio label="MEDIUM">中等</el-radio>
                  <el-radio label="HIGH">高风险</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '创建策略' }}
          </el-button>
        </template>
      </el-dialog>

      <!-- 进度详情对话框 -->
      <el-dialog v-model="progressDialogVisible" title="执行进度详情" width="600px">
        <div v-if="currentProgress" class="progress-detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="执行状态">
              <el-tag :type="getStatusType(currentProgress.status)">{{ currentProgress.status }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="完成进度">
              <el-progress
                :percentage="currentProgress.progressPercentage || 0"
                :status="getProgressStatus(currentProgress.status)"
                style="width: 200px"
              />
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">
              {{ currentProgress.startTime || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">任务完成情况</el-divider>
          
          <div v-if="currentProgress.dailyTargets" class="progress-tasks">
            <div
              v-for="(target, key) in currentProgress.dailyTargets"
              :key="key"
              class="progress-task-item"
            >
              <span class="task-label">{{ getTaskLabel(key) }}</span>
              <el-progress
                :percentage="calculateTaskProgress(target, currentProgress.completedCounts?.[key])"
                :stroke-width="16"
                style="flex: 1; margin-left: 12px"
              />
              <span class="task-count">
                {{ currentProgress.completedCounts?.[key] || 0 }} / {{ target }}
              </span>
            </div>
          </div>
        </div>
      </el-dialog>
    </div>
  </bx-content>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  MoreFilled,
  Star,
  ChatDotRound,
  Share,
  Timer
} from '@element-plus/icons-vue'
import {
  getNurturingStrategyList,
  createNurturingStrategy,
  updateNurturingStrategy,
  deleteNurturingStrategy,
  toggleNurturingStatus,
  startNurturingStrategy,
  stopNurturingStrategy,
  getNurturingProgress,
  getNurturingTemplates
} from '@/api/nurturing.js'
import { getAccountList } from '@/api/account.js'

const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const filterStatus = ref(null)
const strategyList = ref([])
const accountList = ref([])
const templateList = ref([])
const progressMap = ref({})
const dialogVisible = ref(false)
const progressDialogVisible = ref(false)
const isEdit = ref(false)
const currentStrategy = ref(null)
const currentProgress = ref(null)
const selectedTemplate = ref('')
const formRef = ref(null)

const form = reactive({
  strategyName: '',
  accountId: null,
  dailyTargets: {
    likeCount: 10,
    commentCount: 5,
    shareCount: 2,
    followCount: 3,
    browseDuration: 30
  },
  durationDays: 7,
  riskLevel: 'LOW'
})

const rules = {
  strategyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  accountId: [{ required: true, message: '请选择关联账号', trigger: 'change' }]
}

onMounted(() => {
  loadStrategies()
  loadAccounts()
  loadTemplates()
})

async function loadStrategies() {
  loading.value = true
  try {
    const params = {}
    if (filterStatus.value !== null) params.enabled = filterStatus.value
    const res = await getNurturingStrategyList(params)
    strategyList.value = res.data || []
    
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
    console.error('加载策略失败:', e)
    ElMessage.error('加载策略列表失败')
  } finally {
    loading.value = false
  }
}

async function loadAccounts() {
  try {
    const res = await getAccountList({ pageSize: 100 })
    accountList.value = res.data?.records || []
  } catch (e) {
    console.error('加载账号失败:', e)
  }
}

async function loadTemplates() {
  try {
    const res = await getNurturingTemplates()
    templateList.value = res.data || []
  } catch (e) {
    console.error('加载模板失败:', e)
  }
}

function handleSearch() {
  loadStrategies()
}

function handleFilterChange() {
  loadStrategies()
}

function handleCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function resetForm() {
  form.strategyName = ''
  form.accountId = null
  form.dailyTargets = {
    likeCount: 10,
    commentCount: 5,
    shareCount: 2,
    followCount: 3,
    browseDuration: 30
  }
  form.durationDays = 7
  form.riskLevel = 'LOW'
  selectedTemplate.value = ''
}

function handleTemplateChange(templateName) {
  const template = templateList.value.find(t => t.name === templateName)
  if (template && template.dailyTargets) {
    Object.assign(form.dailyTargets, template.dailyTargets)
    form.durationDays = template.durationDays || 7
    form.riskLevel = template.riskLevel || 'LOW'
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    saving.value = true
    try {
      if (isEdit.value) {
        await updateNurturingStrategy(currentStrategy.value.id, form)
        ElMessage.success('策略更新成功')
      } else {
        await createNurturingStrategy(form)
        ElMessage.success('策略创建成功')
      }
      dialogVisible.value = false
      loadStrategies()
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      saving.value = false
    }
  })
}

async function handleCommand(command, strategy) {
  switch (command) {
    case 'edit':
      isEdit.value = true
      currentStrategy.value = strategy
      Object.assign(form, {
        strategyName: strategy.strategyName,
        accountId: strategy.accountId,
        dailyTargets: strategy.dailyTargets || {},
        durationDays: strategy.durationDays,
        riskLevel: strategy.riskLevel
      })
      dialogVisible.value = true
      break
      
    case 'progress':
      currentStrategy.value = strategy
      progressDialogVisible.value = true
      try {
        const res = await getNurturingProgress(strategy.id)
        currentProgress.value = res.data
      } catch (e) {
        ElMessage.error('获取进度失败')
      }
      break
      
    case 'start':
      try {
        await startNurturingStrategy(strategy.id)
        ElMessage.success('策略已启动')
        loadStrategies()
      } catch (e) {
        ElMessage.error(e.message || '启动失败')
      }
      break
      
    case 'stop':
      try {
        await stopNurturingStrategy(strategy.id)
        ElMessage.success('策略已停止')
        loadStrategies()
      } catch (e) {
        ElMessage.error(e.message || '停止失败')
      }
      break
      
    case 'delete':
      ElMessageBox.confirm(
        `确定要删除策略「${strategy.strategyName}」吗？此操作不可恢复。`,
        '确认删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
      ).then(async () => {
        try {
          await deleteNurturingStrategy(strategy.id)
          ElMessage.success('删除成功')
          loadStrategies()
        } catch (e) {
          ElMessage.error(e.message || '删除失败')
        }
      }).catch(() => {})
      break
  }
}

function getPlatformName(platform) {
  const map = {
    douyin: '抖音',
    xiaohongshu: '小红书',
    shipinhao: '视频号',
    weibo: '微博',
    bilibili: 'B站',
    kuaishou: '快手'
  }
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
  const map = {
    RUNNING: '',
    COMPLETED: 'success',
    STOPPED: 'info',
    NOT_RUNNING: 'info',
    PAUSED: 'warning'
  }
  return map[status] || 'info'
}

function getProgressStatus(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'STOPPED' || status === 'NOT_RUNNING') return undefined
  return undefined
}

function getTaskLabel(key) {
  const map = {
    likeCount: '点赞',
    commentCount: '评论',
    shareCount: '分享',
    followCount: '关注',
    browseDuration: '浏览'
  }
  return map[key] || key
}

function calculateTaskProgress(target, completed) {
  if (!target || target === 0) return 0
  const c = completed || 0
  return Math.min((c / target) * 100, 100)
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleDateString('zh-CN')
}
</script>

<style scoped lang="scss">
.nurturing-management {
  padding: 20px;

  &__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    &-left {
      display: flex;
      gap: 12px;
    }

    &-right {
      display: flex;
      gap: 12px;
    }
  }

  &__list {
    min-height: 400px;
  }
}

.nurturing-card {
  margin-bottom: 20px;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }

  &--disabled {
    opacity: 0.7;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;
  }

  &__title-section {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
  }

  &__title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  &__account {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    padding: 8px 0;
    border-top: 1px solid #ebeef5;
  }

  &__account-name {
    font-size: 14px;
    color: #606266;
  }

  &__risk {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    &-label {
      font-size: 13px;
      color: #909399;
    }
  }

  &__progress {
    margin-bottom: 16px;

    &-header {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      color: #606266;
      margin-bottom: 6px;
    }
  }

  &__targets {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 12px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 6px;
  }

  &__target-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #606266;

    .el-icon {
      color: #409eff;
    }
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
    font-size: 12px;
    color: #909399;
  }

  &__duration {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.progress-detail {
  padding: 16px 0;
}

.progress-tasks {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.progress-task-item {
  display: flex;
  align-items: center;

  .task-label {
    width: 60px;
    font-size: 14px;
    color: #606266;
  }

  .task-count {
    width: 80px;
    text-align: right;
    font-size: 13px;
    color: #909399;
    margin-left: 12px;
  }
}
</style>
