<template>
  <bx-content
    title="商机列表"
    description="管理和跟进潜在客户商机"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 搜索筛选栏 -->
    <div class="lead-list__toolbar">
      <div class="lead-list__toolbar-left">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索客户名称/手机号"
          prefix-icon="el-icon-search"
          clearable
          style="width: 220px"
          @change="handleSearch"
        />
        <el-select
          v-model="searchForm.leadSource"
          placeholder="来源"
          clearable
          style="width: 130px"
          @change="handleFilterChange"
        >
          <el-option label="同业截客" :value="1" />
          <el-option label="主动获客" :value="2" />
          <el-option label="手动导入" :value="3" />
        </el-select>
        <el-select
          v-model="searchForm.followStatus"
          placeholder="跟进状态"
          clearable
          style="width: 130px"
          @change="handleFilterChange"
        >
          <el-option label="未跟进" :value="0" />
          <el-option label="跟进中" :value="1" />
          <el-option label="已成交" :value="2" />
          <el-option label="已流失" :value="3" />
        </el-select>
        <el-select
          v-model="searchForm.intentLevel"
          placeholder="意向等级"
          clearable
          style="width: 120px"
          @change="handleFilterChange"
        >
          <el-option label="高意向" :value="1" />
          <el-option label="中意向" :value="2" />
          <el-option label="低意向" :value="3" />
        </el-select>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="handleFilterChange"
        />
      </div>
      <div class="lead-list__toolbar-right">
        <el-button @click="handleExport">
          <i class="el-icon-download" />
          导出
        </el-button>
        <el-button type="primary" @click="handleImport">
          <i class="el-icon-upload" />
          导入
        </el-button>
        <el-button type="primary" @click="handleBatchAssign" :disabled="!selectedRows.length">
          <i class="el-icon-user" />
          批量分配
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="lead-list__stats">
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="lead-list__stat-item">
          <div class="lead-list__stat-value">{{ stats.total }}</div>
          <div class="lead-list__stat-label">全部商机</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="lead-list__stat-item lead-list__stat-item--warning">
          <div class="lead-list__stat-value">{{ stats.unfollowed }}</div>
          <div class="lead-list__stat-label">未跟进</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="lead-list__stat-item lead-list__stat-item--primary">
          <div class="lead-list__stat-value">{{ stats.following }}</div>
          <div class="lead-list__stat-label">跟进中</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="lead-list__stat-item lead-list__stat-item--success">
          <div class="lead-list__stat-value">{{ stats.deal }}</div>
          <div class="lead-list__stat-label">已成交</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="lead-list__stat-item lead-list__stat-item--danger">
          <div class="lead-list__stat-value">{{ stats.lost }}</div>
          <div class="lead-list__stat-label">已流失</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="lead-list__stat-item lead-list__stat-item--info">
          <div class="lead-list__stat-value">{{ stats.today }}</div>
          <div class="lead-list__stat-label">今日新增</div>
        </div>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <div class="lead-list__table">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="客户信息" min-width="200">
          <template #default="{ row }">
            <div class="lead-list__user-cell">
              <el-avatar :size="40" :src="row.userAvatar">
                {{ row.userNickname?.charAt(0) || '?' }}
              </el-avatar>
              <div class="lead-list__user-info">
                <span class="lead-list__user-name">{{ row.userNickname || '-' }}</span>
                <span class="lead-list__user-phone">{{ row.contactPhone || row.userId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="来源平台" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getSourceType(row.leadSource)">
              {{ getSourceLabel(row.leadSource) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="意向等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getIntentType(row.intentLevel)" size="small" effect="dark">
              {{ getIntentLabel(row.intentLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="跟进状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.followStatus)" size="small">
              {{ getStatusLabel(row.followStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="跟进人" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.assigneeName">{{ row.assigneeName }}</span>
            <el-tag v-else size="small" type="info">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近跟进" width="160" align="center">
          <template #default="{ row }">
            <span :class="{ 'lead-list__overdue': isOverdue(row.lastFollowTime) }">
              {{ row.lastFollowTime || '从未跟进' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">
              详情
            </el-button>
            <el-button type="primary" link size="small" @click="handleFollow(row)">
              跟进
            </el-button>
            <el-button type="primary" link size="small" @click="handleAssign(row)">
              {{ row.assigneeId ? '转交' : '分配' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="lead-list__pagination">
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

    <!-- 分配弹窗 -->
    <el-dialog
      v-model="assignDialogVisible"
      :title="assignTitle"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="选择人员">
          <el-select v-model="assignForm.assigneeId" placeholder="请选择跟进人" style="width: 100%">
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.realName"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 跟进弹窗 -->
    <el-dialog
      v-model="followDialogVisible"
      title="添加跟进记录"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="followFormRef" :model="followForm" :rules="followRules" label-width="100px">
        <el-form-item label="跟进方式" prop="followType">
          <el-radio-group v-model="followForm.followType">
            <el-radio :label="1">电话</el-radio>
            <el-radio :label="2">微信</el-radio>
            <el-radio :label="3">私信</el-radio>
            <el-radio :label="4">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="跟进内容" prop="followContent">
          <el-input
            v-model="followForm.followContent"
            type="textarea"
            :rows="4"
            placeholder="请输入跟进内容"
          />
        </el-form-item>
        <el-form-item label="跟进结果">
          <el-input v-model="followForm.followResult" placeholder="请输入跟进结果" />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker
            v-model="followForm.nextFollowTime"
            type="datetime"
            placeholder="选择下次跟进时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="followLoading" @click="handleFollowSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入商机"
      width="480px"
    >
      <div class="lead-list__import-content">
        <el-upload
          drag
          action="/api/v1/leads/import"
          :on-success="handleImportSuccess"
          :on-error="handleImportError"
          accept=".xlsx,.xls,.csv"
        >
          <i class="el-icon-upload" />
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 .xlsx, .xls, .csv 格式，文件大小不超过 10MB
            </div>
          </template>
        </el-upload>
        <div class="lead-list__import-template">
          <el-button link type="primary" @click="handleDownloadTemplate">
            <i class="el-icon-document" />
            下载导入模板
          </el-button>
        </div>
      </div>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview LeadList 商机列表页
 * @description 商机管理，支持搜索、筛选、分配、跟进等操作
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as leadApi from '@/api/lead.js'
import * as userApi from '@/api/user.js'

// ============================================================
// Router
// ============================================================
const router = useRouter()

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const tableData = ref([])
const selectedRows = ref([])

// 搜索表单
const searchForm = reactive({
  keyword: '',
  leadSource: '',
  followStatus: '',
  intentLevel: '',
  dateRange: [],
})

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// 统计数据
const stats = reactive({
  total: 0,
  unfollowed: 0,
  following: 0,
  deal: 0,
  lost: 0,
  today: 0,
})

// 分配弹窗
const assignDialogVisible = ref(false)
const assignLoading = ref(false)
const assignTitle = ref('分配商机')
const assignForm = reactive({
  leadIds: [],
  assigneeId: null,
})
const userList = ref([])

// 跟进弹窗
const followDialogVisible = ref(false)
const followLoading = ref(false)
const followFormRef = ref(null)
const currentLeadId = ref(null)
const followForm = reactive({
  followType: 1,
  followContent: '',
  followResult: '',
  nextFollowTime: '',
})
const followRules = {
  followType: [{ required: true, message: '请选择跟进方式', trigger: 'change' }],
  followContent: [{ required: true, message: '请输入跟进内容', trigger: 'blur' }],
}

// 导入弹窗
const importDialogVisible = ref(false)

// 映射
const sourceMap = {
  1: { label: '同业截客', type: 'primary' },
  2: { label: '主动获客', type: 'success' },
  3: { label: '手动导入', type: 'info' },
}

const intentMap = {
  1: { label: '高', type: 'danger' },
  2: { label: '中', type: 'warning' },
  3: { label: '低', type: 'info' },
}

const statusMap = {
  0: { label: '未跟进', type: 'info' },
  1: { label: '跟进中', type: 'primary' },
  2: { label: '已成交', type: 'success' },
  3: { label: '已流失', type: 'danger' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载商机列表
 */
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
    }

    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.leadSource) params.leadSource = searchForm.leadSource
    if (searchForm.followStatus !== '') params.followStatus = searchForm.followStatus
    if (searchForm.intentLevel) params.intentLevel = searchForm.intentLevel
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }

    const response = await leadApi.getLeadList(params)
    tableData.value = response.data?.list || []
    pagination.total = response.data?.pagination?.total || 0
  } catch (error) {
    console.error('[LeadList] Load data failed:', error)
    tableData.value = []
    pagination.total = 0
    ElMessage.error('加载商机数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 加载统计数据
 */
async function loadStats() {
  try {
    const response = await leadApi.getLeadStatistics()
    Object.assign(stats, response.data || {})
  } catch (error) {
    console.error('[LeadList] Load stats failed:', error)
    Object.assign(stats, {
      total: 0,
      unfollowed: 0,
      following: 0,
      deal: 0,
      lost: 0,
      today: 0,
    })
  }
}

/**
 * 加载用户列表
 */
async function loadUserList() {
  try {
    const response = await userApi.getUserList({ size: 100 })
    userList.value = response.data?.list || []
  } catch (error) {
    console.error('[LeadList] Load users failed:', error)
    userList.value = []
  }
}

/**
 * 获取来源标签
 */
function getSourceLabel(source) {
  return sourceMap[source]?.label || '-'
}

/**
 * 获取来源类型
 */
function getSourceType(source) {
  return sourceMap[source]?.type || 'info'
}

/**
 * 获取意向标签
 */
function getIntentLabel(level) {
  return intentMap[level]?.label || '-'
}

/**
 * 获取意向类型
 */
function getIntentType(level) {
  return intentMap[level]?.type || 'info'
}

/**
 * 获取状态标签
 */
function getStatusLabel(status) {
  return statusMap[status]?.label || '-'
}

/**
 * 获取状态类型
 */
function getStatusType(status) {
  return statusMap[status]?.type || 'info'
}

/**
 * 判断是否逾期
 */
function isOverdue(lastFollowTime) {
  if (!lastFollowTime) return false
  const days = Math.floor((Date.now() - new Date(lastFollowTime).getTime()) / (1000 * 60 * 60 * 24))
  return days > 7
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
 * 选中行变化
 */
function handleSelectionChange(selection) {
  selectedRows.value = selection
}

/**
 * 查看详情
 */
function handleView(row) {
  router.push(`/lead/detail/${row.id}`)
}

/**
 * 打开分配弹窗
 */
function handleAssign(row) {
  assignTitle.value = row.assigneeId ? '转交商机' : '分配商机'
  assignForm.leadIds = [row.id]
  assignForm.assigneeId = row.assigneeId || null
  assignDialogVisible.value = true
  loadUserList()
}

/**
 * 批量分配
 */
function handleBatchAssign() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择要分配的商机')
    return
  }
  assignTitle.value = '批量分配商机'
  assignForm.leadIds = selectedRows.value.map(row => row.id)
  assignForm.assigneeId = null
  assignDialogVisible.value = true
  loadUserList()
}

/**
 * 提交分配
 */
async function handleAssignSubmit() {
  if (!assignForm.assigneeId) {
    ElMessage.warning('请选择跟进人')
    return
  }
  
  assignLoading.value = true
  try {
    if (assignForm.leadIds.length === 1) {
      await leadApi.assignLead(assignForm.leadIds[0], assignForm.assigneeId)
    } else {
      await leadApi.batchAssignLeads(assignForm.leadIds, assignForm.assigneeId)
    }
    ElMessage.success('分配成功')
    assignDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[LeadList] Assign failed:', error)
    ElMessage.error('分配失败，请重试')
  } finally {
    assignLoading.value = false
  }
}

/**
 * 打开跟进弹窗
 */
function handleFollow(row) {
  currentLeadId.value = row.id
  followForm.followType = 1
  followForm.followContent = ''
  followForm.followResult = ''
  followForm.nextFollowTime = ''
  followDialogVisible.value = true
}

/**
 * 提交跟进
 */
async function handleFollowSubmit() {
  if (!followFormRef.value) return

  const valid = await followFormRef.value.validate().catch(() => false)
  if (!valid) return

  followLoading.value = true
  try {
    await leadApi.addLeadFollow(currentLeadId.value, followForm)
    ElMessage.success('跟进记录添加成功')
    followDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[LeadList] Follow failed:', error)
    ElMessage.error('添加失败，请重试')
  } finally {
    followLoading.value = false
  }
}

/**
 * 导出
 */
async function handleExport() {
  try {
    const params = {}
    if (selectedRows.value.length > 0) {
      params.ids = selectedRows.value.map(row => row.id)
    }
    await leadApi.exportLeads(params)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('[LeadList] Export failed:', error)
    ElMessage.success('导出请求已发送')
  }
}

/**
 * 导入
 */
function handleImport() {
  importDialogVisible.value = true
}

/**
 * 导入成功
 */
function handleImportSuccess(response) {
  ElMessage.success(`导入成功：${response.data?.success || 0} 条`)
  importDialogVisible.value = false
  loadData()
}

/**
 * 导入失败
 */
function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

/**
 * 下载模板
 */
async function handleDownloadTemplate() {
  try {
    const url = await leadApi.downloadLeadTemplate()
    window.open(url, '_blank')
  } catch (error) {
    ElMessage.error('模板下载失败')
  }
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style lang="scss" scoped>
.lead-list {
  &__toolbar {
    @include flex-between;
    margin-bottom: $spacing-lg;
    flex-wrap: wrap;
    gap: $spacing-sm;

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

  &__stats {
    margin-bottom: $spacing-lg;
  }

  &__stat-item {
    padding: $spacing-md;
    background: $color-white;
    border-radius: $radius-lg;
    text-align: center;
    box-shadow: $shadow-sm;
    border-left: 4px solid $color-gray-300;

    &--primary {
      border-left-color: $color-primary;
      .lead-list__stat-value {
        color: $color-primary;
      }
    }

    &--success {
      border-left-color: $color-success;
      .lead-list__stat-value {
        color: $color-success;
      }
    }

    &--warning {
      border-left-color: $color-warning;
      .lead-list__stat-value {
        color: $color-warning;
      }
    }

    &--danger {
      border-left-color: $color-danger;
      .lead-list__stat-value {
        color: $color-danger;
      }
    }

    &--info {
      border-left-color: $color-info;
      .lead-list__stat-value {
        color: $color-info;
      }
    }
  }

  &__stat-value {
    font-size: $font-size-2xl;
    font-weight: $font-weight-bold;
    color: $color-text-primary;
    margin-bottom: $spacing-xs;
  }

  &__stat-label {
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__table {
    background: $color-white;
    border-radius: $radius-lg;
    padding: $spacing-lg;
  }

  &__user-cell {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  &__user-info {
    display: flex;
    flex-direction: column;
  }

  &__user-name {
    font-weight: $font-weight-medium;
    color: $color-text-primary;
  }

  &__user-phone {
    font-size: $font-size-xs;
    color: $color-text-secondary;
  }

  &__overdue {
    color: $color-danger;
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: $spacing-lg;
  }

  &__import-content {
    text-align: center;
  }

  &__import-template {
    margin-top: $spacing-lg;
  }
}
</style>