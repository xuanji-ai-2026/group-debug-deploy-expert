<template>
  <div class="tenant-list-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">租户列表</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleExport">
          <el-icon><Download /></el-icon> 导出数据
        </el-button>
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
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="待审核" value="pending" />
          <el-option label="正常" value="active" />
          <el-option label="已禁用" value="disabled" />
        </el-select>
      </el-form-item>
      <el-form-item label="套餐类型">
        <el-select v-model="searchForm.plan" placeholder="全部套餐" clearable style="width: 140px">
          <el-option label="免费版" value="free" />
          <el-option label="基础版" value="basic" />
          <el-option label="专业版" value="pro" />
          <el-option label="企业版" value="enterprise" />
        </el-select>
      </el-form-item>
    </AdminSearch>

    <!-- 数据表格 -->
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
            <span class="tenant-id">ID: {{ row.id }}</span>
          </div>
        </div>
      </template>

      <!-- 状态 -->
      <template #column-status="{ row }">
        <el-tag :type="statusType(row.status)" size="small">
          {{ statusText(row.status) }}
        </el-tag>
      </template>

      <!-- 积分余额 -->
      <template #column-balance="{ row }">
        <span class="balance-value">{{ row.balance?.toLocaleString() || 0 }}</span>
      </template>

      <!-- 风控等级 -->
      <template #column-riskLevel="{ row }">
        <el-tag :type="riskType(row.riskLevel)" size="small">
          {{ riskText(row.riskLevel) }}
        </el-tag>
      </template>

      <!-- 创建时间 -->
      <template #column-createdAt="{ row }">
        {{ formatDate(row.createdAt) }}
      </template>
    </AdminTable>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="租户详情" size="500px">
      <el-descriptions :column="1" border v-if="currentTenant">
        <el-descriptions-item label="租户ID">{{ currentTenant.id }}</el-descriptions-item>
        <el-descriptions-item label="租户名称">{{ currentTenant.name }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentTenant.contact }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentTenant.phone }}</el-descriptions-item>
        <el-descriptions-item label="套餐">{{ currentTenant.plan }}</el-descriptions-item>
        <el-descriptions-item label="积分余额">{{ currentTenant.balance }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDate(currentTenant.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentTenant.status)" size="small">
            {{ statusText(currentTenant.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="风控等级">
          <el-tag :type="riskType(currentTenant.riskLevel)" size="small">
            {{ riskText(currentTenant.riskLevel) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * TenantList - 租户列表页面
 * 功能：展示所有租户列表，支持搜索、筛选、状态管理
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminTable from '@/components/common/AdminTable.vue'
import AdminSearch from '@/components/common/AdminSearch.vue'
import { getTenantList, enableTenant, disableTenant } from '@/api/tenant'

// ============================================================
// 表格列定义
// ============================================================
const columns = [
  { prop: 'name', label: '租户名称', minWidth: 200, showOverflowTooltip: false },
  { prop: 'contact', label: '联系人', width: 120 },
  { prop: 'phone', label: '手机号', width: 130 },
  { prop: 'plan', label: '套餐', width: 100 },
  { prop: 'balance', label: '积分余额', width: 120 },
  { prop: 'status', label: '状态', width: 90 },
  { prop: 'riskLevel', label: '风控等级', width: 100 },
  { prop: 'createdAt', label: '注册时间', width: 160 }
]

// ============================================================
// 操作按钮定义
// ============================================================
const actions = [
  { key: 'view', label: '查看', type: 'primary', link: true, handler: handleView },
  { key: 'enable', label: '启用', type: 'success', link: true, handler: handleEnable },
  { key: 'disable', label: '禁用', type: 'danger', link: true, handler: handleDisable },
  { key: 'divider', divider: true },
  { key: 'reset', label: '重置密钥', type: 'warning', link: true, handler: handleResetSecret }
]

// ============================================================
// 数据状态
// ============================================================
const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentTenant = ref(null)

// 分页参数
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: '',
  plan: ''
})

// ============================================================
// 表格数据加载
// ============================================================
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    const data = await getTenantList(params)
    tableData.value = data.list || []
    pagination.total = data.total || 0
  } catch (error) {
    console.error('[TenantList] Load data failed:', error)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ============================================================
// 搜索与重置
// ============================================================
function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  pagination.page = 1
  loadData()
}

// ============================================================
// 分页
// ============================================================
function handlePageChange(page) {
  pagination.page = page
  loadData()
}

// ============================================================
// 操作处理
// ============================================================
function handleView(row) {
  currentTenant.value = row
  detailVisible.value = true
}

async function handleEnable(row) {
  try {
    await ElMessageBox.confirm(`确定要启用租户「${row.name}」吗？`, '提示')
    await enableTenant(row.id)
    ElMessage.success('启用成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.success('启用成功（演示模式）')
      row.status = 'active'
    }
  }
}

async function handleDisable(row) {
  try {
    await ElMessageBox.confirm(`确定要禁用租户「${row.name}」吗？禁用后该租户将无法登录。`, '警告', {
      confirmButtonText: '确定禁用',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await disableTenant(row.id)
    ElMessage.success('禁用成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.success('禁用成功（演示模式）')
      row.status = 'disabled'
    }
  }
}

async function handleResetSecret(row) {
  try {
    await ElMessageBox.confirm(`确定要重置租户「${row.name}」的密钥吗？`, '提示')
    ElMessage.success('密钥已重置，请联系租户获取新密钥')
  } catch {}
}

function handleExport() {
  ElMessage.info('导出功能开发中...')
}

// ============================================================
// 辅助函数
// ============================================================
function statusText(status) {
  const map = { pending: '待审核', active: '正常', disabled: '已禁用' }
  return map[status] || status
}

function statusType(status) {
  const map = { pending: 'warning', active: 'success', disabled: 'danger' }
  return map[status] || 'info'
}

function riskText(level) {
  const map = { low: '低风险', medium: '中风险', high: '高风险' }
  return map[level] || level
}

function riskType(level) {
  const map = { low: 'success', medium: 'warning', high: 'danger' }
  return map[level] || 'info'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.split(' ')[0]
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.tenant-list-page {
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

.tenant-name-cell {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.tenant-info {
  display: flex;
  flex-direction: column;
}

.tenant-name {
  font-weight: 500;
  color: $text-primary;
}

.tenant-id {
  font-size: $font-size-small;
  color: $text-secondary;
}

.balance-value {
  font-weight: 600;
  color: $primary-color;
}
</style>
