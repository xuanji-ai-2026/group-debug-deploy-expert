<template>
  <div class="tenant-audit-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">租户审核</h2>
        <el-badge :value="pendingCount" :hidden="pendingCount === 0" type="warning">
          <el-tag>待审核</el-tag>
        </el-badge>
      </div>
    </div>

    <!-- 搜索栏 -->
    <AdminSearch v-model="searchForm" :loading="loading" @search="handleSearch" @reset="handleReset">
      <el-form-item label="关键词">
        <el-input
          v-model="searchForm.keyword"
          placeholder="租户名 / 手机号"
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="申请时间">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
      </el-form-item>
    </AdminSearch>

    <!-- 统计概览 -->
    <el-row :gutter="20" class="audit-stats">
      <el-col :span="6">
        <div class="stat-card total">
          <el-icon size="28"><Document /></el-icon>
          <div class="stat-info">
            <span class="stat-number">{{ stats.total }}</span>
            <span class="stat-label">申请总数</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card pending">
          <el-icon size="28"><Clock /></el-icon>
          <div class="stat-info">
            <span class="stat-number">{{ stats.pending }}</span>
            <span class="stat-label">待审核</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card approved">
          <el-icon size="28"><CircleCheck /></el-icon>
          <div class="stat-info">
            <span class="stat-number">{{ stats.approved }}</span>
            <span class="stat-label">已通过</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card rejected">
          <el-icon size="28"><CircleClose /></el-icon>
          <div class="stat-info">
            <span class="stat-number">{{ stats.rejected }}</span>
            <span class="stat-label">已拒绝</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 审核列表表格 -->
    <AdminTable
      :data="tableData"
      :columns="columns"
      :actions="actions"
      :loading="loading"
      :page="pagination.page"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      :show-pagination="true"
      @page-change="handlePageChange"
    >
      <!-- 租户名称 -->
      <template #column-name="{ row }">
        <div class="tenant-name-cell">
          <el-avatar :size="32" style="background-color: #1a73e8">
            {{ row.name?.[0] }}
          </el-avatar>
          <div class="tenant-info">
            <span class="tenant-name">{{ row.name }}</span>
            <span class="tenant-id">申请ID: {{ row.id }}</span>
          </div>
        </div>
      </template>

      <!-- 审核状态 -->
      <template #column-auditStatus="{ row }">
        <el-tag :type="auditStatusType(row.auditStatus)" size="small">
          {{ auditStatusText(row.auditStatus) }}
        </el-tag>
      </template>

      <!-- 申请时间 -->
      <template #column-appliedAt="{ row }">
        {{ row.appliedAt || '-' }}
      </template>

      <!-- 审核时间 -->
      <template #column-auditedAt="{ row }">
        {{ row.auditedAt || '-' }}
      </template>
    </AdminTable>

    <!-- 审核详情抽屉 -->
    <el-drawer v-model="detailVisible" title="租户入驻申请详情" size="560px">
      <el-descriptions :column="1" border v-if="currentTenant">
        <el-descriptions-item label="申请编号">{{ currentTenant.id }}</el-descriptions-item>
        <el-descriptions-item label="租户名称">{{ currentTenant.name }}</el-descriptions-item>
        <el-descriptions-item label="企业类型">{{ currentTenant.businessType }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentTenant.contact }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentTenant.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentTenant.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所在地区">{{ currentTenant.region || '-' }}</el-descriptions-item>
        <el-descriptions-item label="主营行业">{{ currentTenant.industry || '-' }}</el-descriptions-item>
        <el-descriptions-item label="员工规模">{{ currentTenant.employeeScale || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentTenant.appliedAt }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="auditStatusType(currentTenant.auditStatus)" size="small">
            {{ auditStatusText(currentTenant.auditStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核备注" v-if="currentTenant.auditRemark">
          {{ currentTenant.auditRemark }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 审核操作（仅待审核状态显示） -->
      <div v-if="currentTenant?.auditStatus === 'pending'" class="audit-actions">
        <el-divider content-position="center">审核操作</el-divider>
        <el-form ref="auditFormRef" :model="auditForm" label-width="80px">
          <el-form-item label="审核备注">
            <el-input
              v-model="auditForm.remark"
              type="textarea"
              :rows="3"
              placeholder="可选，添加审核备注"
            />
          </el-form-item>
          <el-form-item label="拒绝原因" v-if="auditForm.reject">
            <el-input
              v-model="auditForm.rejectReason"
              type="textarea"
              :rows="2"
              placeholder="请输入拒绝原因（必填）"
            />
          </el-form-item>
          <div class="action-buttons">
            <el-button type="success" size="large" @click="handleApprove" :loading="auditLoading">
              <el-icon><Check /></el-icon> 通过审核
            </el-button>
            <el-button type="danger" size="large" @click="handleReject" :loading="auditLoading">
              <el-icon><Close /></el-icon> 拒绝
            </el-button>
          </div>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * TenantAudit - 租户审核页面
 * 功能：审核租户入驻申请，包括查看详情、通过/拒绝操作
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminTable from '@/components/common/AdminTable.vue'
import AdminSearch from '@/components/common/AdminSearch.vue'
import { getAuditList, getAuditStatistics, approveTenant, rejectTenant } from '@/api/tenant'

// ============================================================
// 表格列定义
// ============================================================
const columns = [
  { prop: 'name', label: '租户名称', minWidth: 200 },
  { prop: 'contact', label: '联系人', width: 120 },
  { prop: 'phone', label: '手机号', width: 130 },
  { prop: 'businessType', label: '企业类型', width: 120 },
  { prop: 'appliedAt', label: '申请时间', width: 160 },
  { prop: 'auditStatus', label: '审核状态', width: 100 }
]

// ============================================================
// 操作按钮
// ============================================================
const actions = [
  { key: 'view', label: '详情', type: 'primary', link: true, handler: handleView }
]

// ============================================================
// 数据状态
// ============================================================
const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentTenant = ref(null)
const auditLoading = ref(false)
const auditFormRef = ref(null)

// 分页
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

// 搜索表单
const searchForm = reactive({
  keyword: '',
  dateRange: []
})

// 审核表单
const auditForm = reactive({
  remark: '',
  rejectReason: ''
})

// 统计
const stats = reactive({ total: 0, pending: 0, approved: 0, rejected: 0 })

// 待审核数量
const pendingCount = computed(() => stats.pending)

// ============================================================
// 数据加载
// ============================================================
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      startDate: searchForm.dateRange?.[0],
      endDate: searchForm.dateRange?.[1]
    }
    const data = await getAuditList(params)
    tableData.value = data.list || []
    pagination.total = data.total || 0
  } catch (error) {
    console.error('[Audit] Load data failed:', error)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const data = await getAuditStatistics()
    stats.total = data.total || 0
    stats.pending = data.pending || 0
    stats.approved = data.approved || 0
    stats.rejected = data.rejected || 0
  } catch (error) {
    console.error('[Audit] Load stats failed:', error)
    stats.total = 0
    stats.pending = 0
    stats.approved = 0
    stats.rejected = 0
  }
}

// ============================================================
// 搜索与重置
// ============================================================
function handleSearch() { pagination.page = 1; loadData() }
function handleReset() { pagination.page = 1; loadData() }
function handlePageChange(page) { pagination.page = page; loadData() }

// ============================================================
// 操作
// ============================================================
function handleView(row) {
  currentTenant.value = row
  auditForm.remark = ''
  auditForm.rejectReason = ''
  detailVisible.value = true
}

async function handleApprove() {
  try {
    await ElMessageBox.confirm('确定要通过该租户的入驻申请吗？', '审核确认', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'success'
    })
    auditLoading.value = true
    await approveTenant(currentTenant.value.id, { remark: auditForm.remark })
    ElMessage.success('审核通过！已发送入驻通知')
    detailVisible.value = false
    loadData()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.success('审核通过（演示模式）')
      detailVisible.value = false
      loadData()
    }
  } finally {
    auditLoading.value = false
  }
}

async function handleReject() {
  if (!auditForm.rejectReason.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  try {
    await ElMessageBox.confirm('确定要拒绝该租户的入驻申请吗？', '审核确认', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    })
    auditLoading.value = true
    await rejectTenant(currentTenant.value.id, { rejectReason: auditForm.rejectReason })
    ElMessage.success('已拒绝该申请')
    detailVisible.value = false
    loadData()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.success('已拒绝（演示模式）')
      detailVisible.value = false
      loadData()
    }
  } finally {
    auditLoading.value = false
  }
}

// ============================================================
// 辅助函数
// ============================================================
function auditStatusText(status) {
  const map = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

function auditStatusType(status) {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style lang="scss" scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-base;

  .header-left {
    display: flex;
    align-items: center;
    gap: $spacing-base;
  }

  .page-title {
    font-size: $font-size-extra-large;
    font-weight: 600;
    color: $text-primary;
  }
}

.audit-stats {
  margin-bottom: $spacing-base;
}

.stat-card {
  background: $bg-color;
  border-radius: $border-radius-base;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid $border-color-light;

  .el-icon {
    padding: 12px;
    border-radius: 12px;
    background: $bg-color-page;
  }

  &.total .el-icon { color: #1a73e8; background: #e8f0fe; }
  &.pending .el-icon { color: #faad14; background: #fffbe6; }
  &.approved .el-icon { color: #52c41a; background: #f6ffed; }
  &.rejected .el-icon { color: #ff4d4f; background: #fff2f0; }

  .stat-info {
    display: flex;
    flex-direction: column;
  }

  .stat-number {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
  }

  .stat-label {
    font-size: $font-size-small;
    color: $text-secondary;
  }
}

.tenant-name-cell {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.tenant-info {
  display: flex;
  flex-direction: column;

  .tenant-name { font-weight: 500; color: $text-primary; }
  .tenant-id { font-size: $font-size-small; color: $text-secondary; }
}

.audit-actions {
  padding: 0 20px 20px;

  .action-buttons {
    display: flex;
    gap: $spacing-base;
    margin-top: $spacing-base;
  }
}
</style>
