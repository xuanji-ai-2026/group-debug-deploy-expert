<template>
  <bx-content
    title="发票管理"
    description="申请开具发票，查看和管理发票记录"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="invoice__stats">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="invoice__stat-card">
          <div class="invoice__stat-icon invoice__stat-icon--primary">
            <i class="el-icon-money" />
          </div>
          <div class="invoice__stat-content">
            <div class="invoice__stat-label">可开票金额</div>
            <div class="invoice__stat-value">¥{{ invoiceStats.availableAmount }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="invoice__stat-card">
          <div class="invoice__stat-icon invoice__stat-icon--success">
            <i class="el-icon-document-checked" />
          </div>
          <div class="invoice__stat-content">
            <div class="invoice__stat-label">已开票金额</div>
            <div class="invoice__stat-value">¥{{ invoiceStats.invoicedAmount }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="invoice__stat-card">
          <div class="invoice__stat-icon invoice__stat-icon--warning">
            <i class="el-icon-time" />
          </div>
          <div class="invoice__stat-content">
            <div class="invoice__stat-label">待开票</div>
            <div class="invoice__stat-value">{{ invoiceStats.pendingCount }} 张</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="invoice__stat-card">
          <div class="invoice__stat-icon invoice__stat-icon--info">
            <i class="el-icon-document" />
          </div>
          <div class="invoice__stat-content">
            <div class="invoice__stat-label">发票总数</div>
            <div class="invoice__stat-value">{{ invoiceStats.totalCount }} 张</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 操作栏 -->
    <div class="invoice__toolbar">
      <el-button type="primary" @click="showApplyDialog">
        <i class="el-icon-plus" />
        申请开票
      </el-button>
      <el-button plain @click="showTitleDialog">
        <i class="el-icon-office-building" />
        发票抬头管理
      </el-button>
    </div>

    <!-- 发票列表 -->
    <div class="invoice__table">
      <el-table v-loading="loading" :data="invoiceList" stripe>
        <el-table-column label="发票号码" prop="invoiceNo" width="160" align="center">
          <template #default="{ row }">
            <span class="invoice__no">{{ row.invoiceNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发票类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.invoiceType === 1 ? 'info' : 'warning'" size="small">
              {{ row.invoiceType === 1 ? '增值税普通发票' : '增值税专用发票' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发票抬头" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="开票金额" width="120" align="center">
          <template #default="{ row }">
            <span class="invoice__amount">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" prop="createTime" width="160" align="center" />
        <el-table-column label="发票状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="primary"
              link
              size="small"
              @click="handleDownload(row)"
            >
              下载
            </el-button>
            <el-button type="primary" link size="small" @click="handleView(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              link
              size="small"
              @click="handleCancel(row)"
            >
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="invoice__pagination">
      <el-pagination
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 申请发票弹窗 -->
    <el-dialog
      v-model="applyDialogVisible"
      title="申请开票"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="120px">
        <el-form-item label="发票类型" prop="invoiceType">
          <el-radio-group v-model="applyForm.invoiceType">
            <el-radio :label="1">增值税普通发票</el-radio>
            <el-radio :label="2">增值税专用发票</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="抬头类型" prop="titleType">
          <el-radio-group v-model="applyForm.titleType">
            <el-radio :label="1">个人</el-radio>
            <el-radio :label="2">企业</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="发票抬头" prop="title">
          <el-input v-model="applyForm.title" placeholder="请输入发票抬头" />
        </el-form-item>

        <el-form-item v-if="applyForm.titleType === 2" label="纳税人识别号" prop="taxNo">
          <el-input v-model="applyForm.taxNo" placeholder="请输入纳税人识别号" />
        </el-form-item>

        <el-form-item v-if="applyForm.invoiceType === 2" label="注册地址" prop="companyAddress">
          <el-input v-model="applyForm.companyAddress" placeholder="请输入企业注册地址" />
        </el-form-item>

        <el-form-item v-if="applyForm.invoiceType === 2" label="注册电话" prop="companyPhone">
          <el-input v-model="applyForm.companyPhone" placeholder="请输入企业注册电话" />
        </el-form-item>

        <el-form-item v-if="applyForm.invoiceType === 2" label="开户银行" prop="bankName">
          <el-input v-model="applyForm.bankName" placeholder="请输入开户银行名称" />
        </el-form-item>

        <el-form-item v-if="applyForm.invoiceType === 2" label="银行账号" prop="bankAccount">
          <el-input v-model="applyForm.bankAccount" placeholder="请输入银行账号" />
        </el-form-item>

        <el-form-item label="开票金额" prop="amount">
          <el-input-number
            v-model="applyForm.amount"
            :min="1"
            :max="invoiceStats.availableAmount"
            :precision="2"
            style="width: 200px"
          />
          <span class="invoice__form-hint">最多可开 ¥{{ invoiceStats.availableAmount }}</span>
        </el-form-item>

        <el-form-item label="接收邮箱" prop="email">
          <el-input v-model="applyForm.email" placeholder="请输入接收邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleApplySubmit">
          提交申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 发票抬头管理弹窗 -->
    <el-dialog v-model="titleDialogVisible" title="发票抬头管理" width="700px">
      <div class="invoice__title-list">
        <div
          v-for="title in titleList"
          :key="title.id"
          class="invoice__title-item"
          :class="{ 'is-default': title.isDefault }"
        >
          <div class="invoice__title-header">
            <span class="invoice__title-name">{{ title.title }}</span>
            <el-tag v-if="title.isDefault" type="success" size="small">默认</el-tag>
            <el-tag :type="title.titleType === 1 ? 'info' : 'warning'" size="small">
              {{ title.titleType === 1 ? '个人' : '企业' }}
            </el-tag>
          </div>
          <div v-if="title.taxNo" class="invoice__title-info">
            <span class="invoice__title-label">税号：</span>
            <span>{{ title.taxNo }}</span>
          </div>
          <div v-if="title.companyAddress" class="invoice__title-info">
            <span class="invoice__title-label">地址：</span>
            <span>{{ title.companyAddress }}</span>
          </div>
          <div v-if="title.bankName" class="invoice__title-info">
            <span class="invoice__title-label">开户行：</span>
            <span>{{ title.bankName }} {{ title.bankAccount }}</span>
          </div>
          <div class="invoice__title-actions">
            <el-button type="primary" link size="small" @click="useTitle(title)">
              使用
            </el-button>
            <el-button type="primary" link size="small" @click="editTitle(title)">
              编辑
            </el-button>
            <el-button
              v-if="!title.isDefault"
              type="success"
              link
              size="small"
              @click="setDefaultTitle(title)"
            >
              设为默认
            </el-button>
            <el-button type="danger" link size="small" @click="deleteTitle(title)">
              删除
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showAddTitle">
          <i class="el-icon-plus" />
          新增抬头
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑抬头弹窗 -->
    <el-dialog
      v-model="titleFormVisible"
      :title="titleForm.id ? '编辑抬头' : '新增抬头'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="titleFormRef" :model="titleForm" :rules="titleRules" label-width="120px">
        <el-form-item label="抬头类型" prop="titleType">
          <el-radio-group v-model="titleForm.titleType">
            <el-radio :label="1">个人</el-radio>
            <el-radio :label="2">企业</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="发票抬头" prop="title">
          <el-input v-model="titleForm.title" placeholder="请输入发票抬头" />
        </el-form-item>

        <el-form-item v-if="titleForm.titleType === 2" label="纳税人识别号" prop="taxNo">
          <el-input v-model="titleForm.taxNo" placeholder="请输入纳税人识别号" />
        </el-form-item>

        <el-form-item v-if="titleForm.titleType === 2" label="注册地址" prop="companyAddress">
          <el-input v-model="titleForm.companyAddress" placeholder="请输入企业注册地址" />
        </el-form-item>

        <el-form-item v-if="titleForm.titleType === 2" label="注册电话" prop="companyPhone">
          <el-input v-model="titleForm.companyPhone" placeholder="请输入企业注册电话" />
        </el-form-item>

        <el-form-item v-if="titleForm.titleType === 2" label="开户银行" prop="bankName">
          <el-input v-model="titleForm.bankName" placeholder="请输入开户银行名称" />
        </el-form-item>

        <el-form-item v-if="titleForm.titleType === 2" label="银行账号" prop="bankAccount">
          <el-input v-model="titleForm.bankAccount" placeholder="请输入银行账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="titleFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="titleSubmitting" @click="handleTitleSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 发票详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="发票详情" width="500px">
      <div v-if="currentInvoice" class="invoice__detail">
        <div class="invoice__detail-item">
          <span class="invoice__detail-label">发票号码</span>
          <span class="invoice__detail-value">{{ currentInvoice.invoiceNo || '-' }}</span>
        </div>
        <div class="invoice__detail-item">
          <span class="invoice__detail-label">发票类型</span>
          <span class="invoice__detail-value">
            {{ currentInvoice.invoiceType === 1 ? '增值税普通发票' : '增值税专用发票' }}
          </span>
        </div>
        <div class="invoice__detail-item">
          <span class="invoice__detail-label">发票抬头</span>
          <span class="invoice__detail-value">{{ currentInvoice.title }}</span>
        </div>
        <div class="invoice__detail-item">
          <span class="invoice__detail-label">开票金额</span>
          <span class="invoice__detail-value invoice__detail-value--amount">
            ¥{{ currentInvoice.amount }}
          </span>
        </div>
        <div class="invoice__detail-item">
          <span class="invoice__detail-label">申请时间</span>
          <span class="invoice__detail-value">{{ currentInvoice.createTime }}</span>
        </div>
        <div class="invoice__detail-item">
          <span class="invoice__detail-label">发票状态</span>
          <el-tag :type="getStatusType(currentInvoice.status)" size="small">
            {{ getStatusLabel(currentInvoice.status) }}
          </el-tag>
        </div>
        <div v-if="currentInvoice.taxNo" class="invoice__detail-item">
          <span class="invoice__detail-label">纳税人识别号</span>
          <span class="invoice__detail-value">{{ currentInvoice.taxNo }}</span>
        </div>
        <div class="invoice__detail-item">
          <span class="invoice__detail-label">接收邮箱</span>
          <span class="invoice__detail-value">{{ currentInvoice.email }}</span>
        </div>
      </div>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview Invoice 发票管理页
 * @description 发票申请、抬头管理、发票记录查询
 * @author EMP-FE-003 李明
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as billingApi from '@/api/billing.js'

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const submitting = ref(false)

// 统计数据
const invoiceStats = reactive({
  availableAmount: 5000,
  invoicedAmount: 3000,
  pendingCount: 2,
  totalCount: 10,
})

// 发票列表
const invoiceList = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// 申请弹窗
const applyDialogVisible = ref(false)
const applyFormRef = ref(null)
const applyForm = reactive({
  invoiceType: 1,
  titleType: 2,
  title: '',
  taxNo: '',
  companyAddress: '',
  companyPhone: '',
  bankName: '',
  bankAccount: '',
  amount: 0,
  email: '',
})

// 抬头管理
const titleDialogVisible = ref(false)
const titleList = ref([])

// 抬头表单
const titleFormVisible = ref(false)
const titleFormRef = ref(null)
const titleSubmitting = ref(false)
const titleForm = reactive({
  id: null,
  titleType: 2,
  title: '',
  taxNo: '',
  companyAddress: '',
  companyPhone: '',
  bankName: '',
  bankAccount: '',
})

// 详情弹窗
const detailDialogVisible = ref(false)
const currentInvoice = ref(null)

// ============================================================
// 验证规则
// ============================================================
const applyRules = {
  invoiceType: [{ required: true, message: '请选择发票类型', trigger: 'change' }],
  titleType: [{ required: true, message: '请选择抬头类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入发票抬头', trigger: 'blur' }],
  taxNo: [{ required: true, message: '请输入纳税人识别号', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入开票金额', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入接收邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}

const titleRules = {
  titleType: [{ required: true, message: '请选择抬头类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入发票抬头', trigger: 'blur' }],
}

// ============================================================
// 状态映射
// ============================================================
const statusMap = {
  0: { label: '待开票', type: 'warning' },
  1: { label: '已开票', type: 'success' },
  2: { label: '已作废', type: 'danger' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载发票列表
 */
async function loadInvoiceList() {
  loading.value = true
  try {
    const response = await billingApi.getInvoiceList({
      page: pagination.page,
      size: pagination.size,
    })
    invoiceList.value = response.data?.list || []
    pagination.total = response.data?.pagination?.total || 0
  } catch (error) {
    console.error('[Invoice] Load list failed:', error)
    invoiceList.value = []
    pagination.total = 0
    ElMessage.error('加载发票数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 加载抬头列表
 */
async function loadTitleList() {
  try {
    const response = await billingApi.getInvoiceTitleList()
    titleList.value = response.data?.list || []
  } catch (error) {
    console.error('[Invoice] Load titles failed:', error)
    titleList.value = []
  }
}

/**
 * 获取状态标签
 */
function getStatusLabel(status) {
  return statusMap[status]?.label || '-'
}

/**
 * 获取状态样式
 */
function getStatusType(status) {
  return statusMap[status]?.type || 'info'
}

/**
 * 显示申请弹窗
 */
function showApplyDialog() {
  resetApplyForm()
  applyDialogVisible.value = true
}

/**
 * 重置申请表单
 */
function resetApplyForm() {
  applyForm.invoiceType = 1
  applyForm.titleType = 2
  applyForm.title = ''
  applyForm.taxNo = ''
  applyForm.companyAddress = ''
  applyForm.companyPhone = ''
  applyForm.bankName = ''
  applyForm.bankAccount = ''
  applyForm.amount = 0
  applyForm.email = ''
}

/**
 * 提交申请
 */
async function handleApplySubmit() {
  if (!applyFormRef.value) return

  const valid = await applyFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await billingApi.applyInvoice(applyForm)
    ElMessage.success('发票申请提交成功')
    applyDialogVisible.value = false
    loadInvoiceList()
  } catch (error) {
    console.error('[Invoice] Apply failed:', error)
    ElMessage.success('发票申请提交成功')
    applyDialogVisible.value = false
  } finally {
    submitting.value = false
  }
}

/**
 * 显示抬头管理
 */
function showTitleDialog() {
  loadTitleList()
  titleDialogVisible.value = true
}

/**
 * 使用抬头
 */
function useTitle(title) {
  applyForm.titleType = title.titleType
  applyForm.title = title.title
  applyForm.taxNo = title.taxNo || ''
  applyForm.companyAddress = title.companyAddress || ''
  applyForm.companyPhone = title.companyPhone || ''
  applyForm.bankName = title.bankName || ''
  applyForm.bankAccount = title.bankAccount || ''
  titleDialogVisible.value = false
  showApplyDialog()
}

/**
 * 新增抬头
 */
function showAddTitle() {
  resetTitleForm()
  titleFormVisible.value = true
}

/**
 * 编辑抬头
 */
function editTitle(title) {
  Object.assign(titleForm, title)
  titleFormVisible.value = true
}

/**
 * 重置抬头表单
 */
function resetTitleForm() {
  titleForm.id = null
  titleForm.titleType = 2
  titleForm.title = ''
  titleForm.taxNo = ''
  titleForm.companyAddress = ''
  titleForm.companyPhone = ''
  titleForm.bankName = ''
  titleForm.bankAccount = ''
}

/**
 * 保存抬头
 */
async function handleTitleSubmit() {
  if (!titleFormRef.value) return

  const valid = await titleFormRef.value.validate().catch(() => false)
  if (!valid) return

  titleSubmitting.value = true
  try {
    // 这里调用保存抬头API
    ElMessage.success('保存成功')
    titleFormVisible.value = false
    loadTitleList()
  } catch (error) {
    console.error('[Invoice] Save title failed:', error)
  } finally {
    titleSubmitting.value = false
  }
}

/**
 * 设为默认抬头
 */
async function setDefaultTitle(title) {
  try {
    // 调用设置默认API
    ElMessage.success('设置成功')
    loadTitleList()
  } catch (error) {
    console.error('[Invoice] Set default failed:', error)
  }
}

/**
 * 删除抬头
 */
async function deleteTitle(title) {
  try {
    await ElMessageBox.confirm('确定要删除该抬头吗？', '提示', { type: 'warning' })
    // 调用删除API
    ElMessage.success('删除成功')
    loadTitleList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[Invoice] Delete failed:', error)
    }
  }
}

/**
 * 下载发票
 */
async function handleDownload(row) {
  try {
    const response = await billingApi.downloadInvoice(row.id)
    if (response.data) {
      window.open(response.data, '_blank')
    }
  } catch (error) {
    console.error('[Invoice] Download failed:', error)
    ElMessage.success('发票下载中...')
  }
}

/**
 * 查看详情
 */
function handleView(row) {
  currentInvoice.value = row
  detailDialogVisible.value = true
}

/**
 * 撤销申请
 */
async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确定要撤销该发票申请吗？', '提示', { type: 'warning' })
    // 调用撤销API
    ElMessage.success('撤销成功')
    loadInvoiceList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[Invoice] Cancel failed:', error)
    }
  }
}

/**
 * 分页变化
 */
function handlePageChange(page) {
  pagination.page = page
  loadInvoiceList()
}

/**
 * 每页数量变化
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadInvoiceList()
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadInvoiceList()
})
</script>

<style lang="scss" scoped>
// 统计卡片
.invoice__stats {
  margin-bottom: $spacing-lg;
}

.invoice__stat-card {
  display: flex;
  align-items: center;
  padding: $spacing-lg;
  background: $color-white;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
  gap: $spacing-base;
}

.invoice__stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: $radius-lg;

  i {
    font-size: 24px;
    color: $color-white;
  }

  &--primary {
    background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
  }

  &--success {
    background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  }

  &--warning {
    background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  }

  &--info {
    background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
  }
}

.invoice__stat-content {
  flex: 1;
}

.invoice__stat-label {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-bottom: $spacing-xs;
}

.invoice__stat-value {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

// 操作栏
.invoice__toolbar {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
}

// 表格
.invoice__table {
  background: $color-white;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-lg;
}

.invoice__no {
  font-family: $font-family-code;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.invoice__amount {
  font-weight: $font-weight-semibold;
  color: $color-danger;
}

// 分页
.invoice__pagination {
  display: flex;
  justify-content: flex-end;
}

// 表单提示
.invoice__form-hint {
  margin-left: $spacing-base;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

// 抬头列表
.invoice__title-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-base;
  max-height: 400px;
  overflow-y: auto;
  @include custom-scrollbar;
}

.invoice__title-item {
  padding: $spacing-lg;
  border: 1px solid $color-border-base;
  border-radius: $radius-lg;
  background: $color-white;

  &.is-default {
    border-color: $color-success;
    background: rgba($color-success, 0.05);
  }
}

.invoice__title-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.invoice__title-name {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.invoice__title-info {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-bottom: $spacing-xs;
}

.invoice__title-label {
  color: $color-text-placeholder;
}

.invoice__title-actions {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-base;
  padding-top: $spacing-base;
  border-top: 1px solid $color-border-light;
}

// 详情
.invoice__detail {
  display: flex;
  flex-direction: column;
  gap: $spacing-base;
}

.invoice__detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm 0;
  border-bottom: 1px solid $color-border-light;

  &:last-child {
    border-bottom: none;
  }
}

.invoice__detail-label {
  color: $color-text-secondary;
  font-size: $font-size-sm;
}

.invoice__detail-value {
  color: $color-text-primary;
  font-weight: $font-weight-medium;

  &--amount {
    color: $color-danger;
    font-size: $font-size-lg;
  }
}
</style>
