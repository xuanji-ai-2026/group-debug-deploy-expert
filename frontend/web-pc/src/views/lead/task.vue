<template>
  <bx-content
    title="获客任务"
    description="管理截客和主动获客任务"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <template #actions>
      <el-button type="primary" @click="handleCreate">
        <i class="el-icon-plus" />
        新建任务
      </el-button>
    </template>

    <!-- 任务类型标签 -->
    <el-tabs v-model="activeTab" class="lead-task__tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部任务" name="all" />
      <el-tab-pane label="截客任务" name="intercept" />
      <el-tab-pane label="获客任务" name="prospect" />
    </el-tabs>

    <!-- 筛选栏 -->
    <div class="lead-task__toolbar">
      <div class="lead-task__toolbar-left">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索任务名称"
          prefix-icon="el-icon-search"
          clearable
          style="width: 220px"
          @change="handleSearch"
        />
        <el-select
          v-model="searchForm.status"
          placeholder="任务状态"
          clearable
          style="width: 130px"
          @change="handleFilterChange"
        >
          <el-option label="待开始" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已暂停" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
      </div>
    </div>

    <!-- 任务卡片列表 -->
    <div v-loading="loading" class="lead-task__list">
      <el-row :gutter="20">
        <el-col v-for="task in taskList" :key="task.id" :xs="24" :sm="12" :lg="8" :xl="6">
          <el-card class="lead-task__card" :class="`lead-task__card--${task.status}`">
            <div class="lead-task__card-header">
              <div class="lead-task__card-title">
                <el-tag :type="getTaskTypeType(task.taskType)" size="small">
                  {{ getTaskTypeLabel(task.taskType) }}
                </el-tag>
                <span class="lead-task__task-name">{{ task.taskName }}</span>
              </div>
              <el-dropdown @command="(cmd) => handleCommand(cmd, task)">
                <el-button type="text" size="small">
                  <i class="el-icon-more" />
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="view">查看详情</el-dropdown-item>
                    <el-dropdown-item v-if="task.status === 0" command="start">启动</el-dropdown-item>
                    <el-dropdown-item v-if="task.status === 1" command="pause">暂停</el-dropdown-item>
                    <el-dropdown-item v-if="task.status === 2" command="resume">恢复</el-dropdown-item>
                    <el-dropdown-item divided command="delete">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <div class="lead-task__card-body">
              <div class="lead-task__stat-row">
                <div class="lead-task__stat-item">
                  <span class="lead-task__stat-value">{{ task.leadCount || 0 }}</span>
                  <span class="lead-task__stat-label">获取商机</span>
                </div>
                <div class="lead-task__stat-item">
                  <span class="lead-task__stat-value">{{ task.messageCount || 0 }}</span>
                  <span class="lead-task__stat-label">发送私信</span>
                </div>
                <div class="lead-task__stat-item">
                  <span class="lead-task__stat-value">{{ task.replyCount || 0 }}</span>
                  <span class="lead-task__stat-label">收到回复</span>
                </div>
              </div>

              <el-divider />

              <div class="lead-task__info-row">
                <div class="lead-task__info-item">
                  <i class="el-icon-platform-eleme" />
                  <span>{{ task.platformName || '-' }}</span>
                </div>
                <div class="lead-task__info-item">
                  <i class="el-icon-user" />
                  <span>{{ task.accountName || '-' }}</span>
                </div>
              </div>

              <div class="lead-task__progress">
                <div class="lead-task__progress-header">
                  <span>任务进度</span>
                  <span>{{ task.progress || 0 }}%</span>
                </div>
                <el-progress
                  :percentage="task.progress || 0"
                  :status="getProgressStatus(task.status)"
                  :stroke-width="8"
                />
              </div>

              <div class="lead-task__status-row">
                <el-tag :type="getStatusType(task.status)" size="small">
                  {{ getStatusLabel(task.status) }}
                </el-tag>
                <span class="lead-task__time">{{ task.createTime }}</span>
              </div>
            </div>

            <div class="lead-task__card-footer">
              <el-button
                v-if="task.status === 0"
                type="primary"
                size="small"
                @click="handleControl(task, 'start')"
              >
                启动任务
              </el-button>
              <el-button
                v-else-if="task.status === 1"
                type="warning"
                size="small"
                @click="handleControl(task, 'pause')"
              >
                暂停任务
              </el-button>
              <el-button
                v-else-if="task.status === 2"
                type="success"
                size="small"
                @click="handleControl(task, 'resume')"
              >
                恢复任务
              </el-button>
              <el-button v-else size="small" disabled>
                {{ getStatusLabel(task.status) }}
              </el-button>
              <el-button size="small" @click="handleView(task)">
                查看详情
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!taskList.length && !loading" description="暂无任务" />
    </div>

    <!-- 分页 -->
    <div class="lead-task__pagination">
      <el-pagination
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 创建任务弹窗 -->
    <el-dialog
      v-model="createDialogVisible"
      title="新建任务"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
        <el-form-item label="任务类型" prop="taskType">
          <el-radio-group v-model="createForm.taskType">
            <el-radio label="intercept">同业截客</el-radio>
            <el-radio label="prospect">主动获客</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="createForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>

        <el-form-item label="执行账号" prop="accountId">
          <el-select v-model="createForm.accountId" placeholder="请选择执行账号" style="width: 100%">
            <el-option
              v-for="account in accountList"
              :key="account.id"
              :label="`${account.platformName} - ${account.accountName}`"
              :value="account.id"
            />
          </el-select>
        </el-form-item>

        <!-- 截客任务配置 -->
        <template v-if="createForm.taskType === 'intercept'">
          <el-divider content-position="left">截客配置</el-divider>
          <el-form-item label="关键词" prop="keywords">
            <el-select
              v-model="createForm.keywords"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="请输入关键词"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="目标账号">
            <el-select
              v-model="createForm.targetAccounts"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入目标账号（可选）"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="意向关键词">
            <el-select
              v-model="createForm.intentKeywords"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入意向关键词（可选）"
              style="width: 100%"
            />
          </el-form-item>
        </template>

        <!-- 主动获客任务配置 -->
        <template v-else>
          <el-divider content-position="left">获客配置</el-divider>
          <el-form-item label="目标行业">
            <el-input v-model="createForm.industry" placeholder="请输入目标行业" />
          </el-form-item>
          <el-form-item label="关键词" prop="keywords">
            <el-select
              v-model="createForm.keywords"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="请输入关键词"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="地域">
            <el-cascader
              v-model="createForm.region"
              :options="regionOptions"
              placeholder="选择地域（可选）"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="年龄范围">
            <el-select v-model="createForm.ageRange" placeholder="选择年龄范围" style="width: 100%">
              <el-option label="18-24岁" value="18-24" />
              <el-option label="25-34岁" value="25-34" />
              <el-option label="35-44岁" value="35-44" />
              <el-option label="45岁以上" value="45+" />
            </el-select>
          </el-form-item>
          <el-form-item label="兴趣标签">
            <el-select
              v-model="createForm.interestTags"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入兴趣标签（可选）"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="每日上限">
            <el-input-number v-model="createForm.dailyLimit" :min="10" :max="500" :step="10" />
          </el-form-item>
        </template>

        <el-divider content-position="left">私信配置</el-divider>
        <el-form-item label="私信模板">
          <el-select v-model="createForm.messageTemplateId" placeholder="选择私信模板（可选）" style="width: 100%">
            <el-option
              v-for="template in templateList"
              :key="template.id"
              :label="template.name"
              :value="template.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateSubmit">
          创建任务
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑任务弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑任务"
      width="560px"
    >
      <el-form label-width="100px">
        <el-form-item label="任务名称">
          <el-input v-model="editForm.taskName" />
        </el-form-item>
        <el-form-item label="每日上限">
          <el-input-number v-model="editForm.dailyLimit" :min="10" :max="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview LeadTask 商机任务页
 * @description 管理截客和主动获客任务
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as leadApi from '@/api/lead.js'
import * as tenantApi from '@/api/tenant.js'

// ============================================================
// Router
// ============================================================
const router = useRouter()

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const activeTab = ref('all')
const taskList = ref([])

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: '',
})

// 分页
const pagination = reactive({
  page: 1,
  size: 12,
  total: 0,
})

// 账号列表
const accountList = ref([])
const templateList = ref([])
const regionOptions = ref([])

// 创建弹窗
const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  taskType: 'intercept',
  taskName: '',
  accountId: null,
  keywords: [],
  targetAccounts: [],
  intentKeywords: [],
  industry: '',
  region: [],
  ageRange: '',
  interestTags: [],
  dailyLimit: 100,
  messageTemplateId: null,
})
const createRules = {
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  accountId: [{ required: true, message: '请选择执行账号', trigger: 'change' }],
  keywords: [{ required: true, message: '请输入关键词', trigger: 'change' }],
}

// 编辑弹窗
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: null,
  taskName: '',
  dailyLimit: 100,
})

// 映射
const taskTypeMap = {
  intercept: { label: '截客', type: 'primary' },
  prospect: { label: '获客', type: 'success' },
}

const statusMap = {
  0: { label: '待开始', type: 'info' },
  1: { label: '进行中', type: 'success' },
  2: { label: '已暂停', type: 'warning' },
  3: { label: '已完成', type: 'primary' },
  4: { label: '已取消', type: 'danger' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载任务列表
 */
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
    }

    if (activeTab.value !== 'all') {
      params.taskType = activeTab.value
    }
    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }
    if (searchForm.status !== '') {
      params.status = searchForm.status
    }

    const response = await leadApi.getTaskList(params)
    taskList.value = response.data?.list || []
    pagination.total = response.data?.pagination?.total || 0
  } catch (error) {
    console.error('[LeadTask] Load data failed:', error)
    taskList.value = []
    pagination.total = 0
    ElMessage.error('加载任务数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 加载账号列表
 */
async function loadAccounts() {
  try {
    const response = await tenantApi.getSocialAccounts({ size: 100 })
    accountList.value = response.data?.list || []
  } catch (error) {
    console.error('[LeadTask] Load accounts failed:', error)
    accountList.value = []
  }
}

// 获取显示方法
function getTaskTypeLabel(type) { return taskTypeMap[type]?.label || '-' }
function getTaskTypeType(type) { return taskTypeMap[type]?.type || 'info' }
function getStatusLabel(status) { return statusMap[status]?.label || '-' }
function getStatusType(status) { return statusMap[status]?.type || 'info' }

function getProgressStatus(status) {
  if (status === 3) return 'success'
  if (status === 4) return 'exception'
  return ''
}

/**
 * 标签切换
 */
function handleTabChange() {
  pagination.page = 1
  loadData()
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
function handlePageChange(page) {
  pagination.page = page
  loadData()
}

/**
 * 每页数量变化
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadData()
}

/**
 * 打开创建弹窗
 */
function handleCreate() {
  createForm.taskType = 'intercept'
  createForm.taskName = ''
  createForm.accountId = null
  createForm.keywords = []
  createForm.targetAccounts = []
  createForm.intentKeywords = []
  createForm.industry = ''
  createForm.region = []
  createForm.ageRange = ''
  createForm.interestTags = []
  createForm.dailyLimit = 100
  createForm.messageTemplateId = null
  createDialogVisible.value = true
  loadAccounts()
}

/**
 * 创建任务
 */
async function handleCreateSubmit() {
  if (!createFormRef.value) return

  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  createLoading.value = true
  try {
    if (createForm.taskType === 'intercept') {
      await leadApi.createInterceptTask(createForm)
    } else {
      await leadApi.createProspectTask(createForm)
    }
    ElMessage.success('任务创建成功')
    createDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[LeadTask] Create failed:', error)
    ElMessage.error('创建失败，请重试')
  } finally {
    createLoading.value = false
  }
}

/**
 * 操作命令
 */
function handleCommand(command, task) {
  switch (command) {
    case 'edit':
      handleEdit(task)
      break
    case 'view':
      handleView(task)
      break
    case 'start':
      handleControl(task, 'start')
      break
    case 'pause':
      handleControl(task, 'pause')
      break
    case 'resume':
      handleControl(task, 'resume')
      break
    case 'delete':
      handleDelete(task)
      break
  }
}

/**
 * 编辑任务
 */
function handleEdit(task) {
  editForm.id = task.id
  editForm.taskName = task.taskName
  editForm.dailyLimit = task.dailyLimit || 100
  editDialogVisible.value = true
}

/**
 * 保存编辑
 */
async function handleEditSubmit() {
  editLoading.value = true
  try {
    console.log('[LeadTask] 编辑提交(预留):', editForm.value)
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[LeadTask] Edit failed:', error)
    ElMessage.error('保存失败')
  } finally {
    editLoading.value = false
  }
}

/**
 * 查看任务详情
 */
function handleView(task) {
  console.log('[LeadTask] 查看任务详情:', task.id)
  ElMessage.info('查看任务详情')
}

/**
 * 控制任务状态
 */
async function handleControl(task, action) {
  const actionMap = {
    start: '启动',
    pause: '暂停',
    resume: '恢复',
    cancel: '取消',
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要${actionMap[action]}任务「${task.taskName}」吗？`,
      `确认${actionMap[action]}`,
      { type: 'warning' }
    )

    await leadApi.controlTask(task.id, action)
    ElMessage.success(`${actionMap[action]}成功`)
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[LeadTask] Control failed:', error)
      ElMessage.error('操作失败')
    }
  }
}

/**
 * 删除任务
 */
async function handleDelete(task) {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务「${task.taskName}」吗？删除后不可恢复。`,
      '确认删除',
      { type: 'danger' }
    )

    await leadApi.deleteTask(task.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[LeadTask] Delete failed:', error)
      ElMessage.error('删除失败')
    }
  }
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.lead-task {
  &__tabs {
    margin-bottom: $spacing-lg;
  }

  &__toolbar {
    margin-bottom: $spacing-lg;

    &-left {
      display: flex;
      gap: $spacing-sm;
    }
  }

  &__list {
    min-height: 400px;
  }

  &__card {
    margin-bottom: $spacing-lg;
    transition: all $transition-duration-base;

    &:hover {
      transform: translateY(-4px);
      box-shadow: $shadow-md;
    }

    &--1 {
      border-top: 3px solid $color-success;
    }

    &--2 {
      border-top: 3px solid $color-warning;
    }

    &--4 {
      opacity: 0.7;
    }
  }

  &__card-header {
    @include flex-between;
    margin-bottom: $spacing-md;
  }

  &__card-title {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  &__task-name {
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
    @include text-ellipsis(1);
  }

  &__card-body {
    .el-divider {
      margin: $spacing-md 0;
    }
  }

  &__stat-row {
    display: flex;
    justify-content: space-around;
    text-align: center;
  }

  &__stat-item {
    display: flex;
    flex-direction: column;
  }

  &__stat-value {
    font-size: $font-size-xl;
    font-weight: $font-weight-bold;
    color: $color-primary;
  }

  &__stat-label {
    font-size: $font-size-xs;
    color: $color-text-secondary;
    margin-top: $spacing-xs;
  }

  &__info-row {
    display: flex;
    gap: $spacing-lg;
    margin-bottom: $spacing-md;
  }

  &__info-item {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    font-size: $font-size-sm;
    color: $color-text-secondary;

    i {
      color: $color-primary;
    }
  }

  &__progress {
    margin-bottom: $spacing-md;
  }

  &__progress-header {
    @include flex-between;
    margin-bottom: $spacing-xs;
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__status-row {
    @include flex-between;
  }

  &__time {
    font-size: $font-size-xs;
    color: $color-text-secondary;
  }

  &__card-footer {
    display: flex;
    gap: $spacing-sm;
    margin-top: $spacing-md;
    padding-top: $spacing-md;
    border-top: 1px solid $color-border-light;

    .el-button {
      flex: 1;
    }
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: $spacing-lg;
  }
}
</style>