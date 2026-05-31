<template>
  <bx-content
    title="租户信息"
    description="查看和管理当前租户的基本信息"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 加载状态 -->
    <div v-if="loading" v-loading="loading" class="tenant-info__loading" />
    
    <!-- 租户信息卡片 -->
    <el-row :gutter="20" class="tenant-info__row">
      <!-- 基本信息 -->
      <el-col :xs="24" :lg="16">
        <el-card class="tenant-info__card">
          <template #header>
            <div class="tenant-info__card-header">
              <span class="tenant-info__card-title">基本信息</span>
              <el-button type="primary" link @click="handleEdit">
                <i class="el-icon-edit" />
                编辑
              </el-button>
            </div>
          </template>
          
          <div class="tenant-info__basic">
            <div class="tenant-info__item">
              <span class="tenant-info__label">租户ID</span>
              <span class="tenant-info__value">{{ tenantInfo.id || '-' }}</span>
            </div>
            <div class="tenant-info__item">
              <span class="tenant-info__label">租户名称</span>
              <span class="tenant-info__value">{{ tenantInfo.tenantName || '-' }}</span>
            </div>
            <div class="tenant-info__item">
              <span class="tenant-info__label">所属行业</span>
              <span class="tenant-info__value">{{ tenantInfo.industry || '-' }}</span>
            </div>
            <div class="tenant-info__item">
              <span class="tenant-info__label">租户状态</span>
              <el-tag :type="getStatusType(tenantInfo.status)" size="small">
                {{ getStatusLabel(tenantInfo.status) }}
              </el-tag>
            </div>
            <div class="tenant-info__item">
              <span class="tenant-info__label">创建时间</span>
              <span class="tenant-info__value">{{ tenantInfo.createTime || '-' }}</span>
            </div>
            <div class="tenant-info__item">
              <span class="tenant-info__label">到期时间</span>
              <span class="tenant-info__value">{{ tenantInfo.expireTime || '-' }}</span>
            </div>
            <div class="tenant-info__item">
              <span class="tenant-info__label">联系人</span>
              <span class="tenant-info__value">{{ tenantInfo.contactName || '-' }}</span>
            </div>
            <div class="tenant-info__item">
              <span class="tenant-info__label">联系电话</span>
              <span class="tenant-info__value">{{ tenantInfo.contactPhone || '-' }}</span>
            </div>
            <div class="tenant-info__item">
              <span class="tenant-info__label">联系邮箱</span>
              <span class="tenant-info__value">{{ tenantInfo.contactEmail || '-' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 套餐信息 -->
      <el-col :xs="24" :lg="8">
        <el-card class="tenant-info__card tenant-info__card--highlight">
          <template #header>
            <div class="tenant-info__card-header">
              <span class="tenant-info__card-title">当前套餐</span>
              <el-tag type="success" size="small">{{ packageInfo.packageTypeLabel || '基础版' }}</el-tag>
            </div>
          </template>
          
          <div class="tenant-info__package">
            <div class="tenant-info__package-name">{{ packageInfo.packageName || '基础套餐' }}</div>
            <div class="tenant-info__package-price">
              <span class="tenant-info__price">¥{{ packageInfo.price || 0 }}</span>
              <span class="tenant-info__unit">/{{ packageInfo.billingCycle || '月' }}</span>
            </div>
            
            <el-divider />
            
            <div class="tenant-info__quota-list">
              <div class="tenant-info__quota-item">
                <span class="tenant-info__quota-label">每日截客上限</span>
                <span class="tenant-info__quota-value">{{ packageInfo.dailyInterceptLimit || 0 }} 次</span>
              </div>
              <div class="tenant-info__quota-item">
                <span class="tenant-info__quota-label">每日获客上限</span>
                <span class="tenant-info__quota-value">{{ packageInfo.dailyProspectLimit || 0 }} 次</span>
              </div>
              <div class="tenant-info__quota-item">
                <span class="tenant-info__quota-label">内容发布上限</span>
                <span class="tenant-info__quota-value">{{ packageInfo.contentLimit || 0 }} 篇/月</span>
              </div>
              <div class="tenant-info__quota-item">
                <span class="tenant-info__quota-label">账号绑定上限</span>
                <span class="tenant-info__quota-value">{{ packageInfo.accountLimit || 0 }} 个</span>
              </div>
              <div class="tenant-info__quota-item">
                <span class="tenant-info__quota-label">子账号数量</span>
                <span class="tenant-info__quota-value">{{ packageInfo.subAccountLimit || 0 }} 个</span>
              </div>
            </div>
            
            <el-button type="primary" class="tenant-info__upgrade-btn" @click="handleUpgrade">
              升级套餐
            </el-button>
          </div>
        </el-card>
        
        <!-- 使用量统计 -->
        <el-card class="tenant-info__card tenant-info__usage-card">
          <template #header>
            <div class="tenant-info__card-header">
              <span class="tenant-info__card-title">本月使用</span>
            </div>
          </template>
          
          <div class="tenant-info__usage">
            <div class="tenant-info__usage-item">
              <div class="tenant-info__usage-info">
                <span class="tenant-info__usage-label">截客任务</span>
                <span class="tenant-info__usage-value">{{ usage.interceptUsed }}/{{ usage.interceptTotal }}</span>
              </div>
              <el-progress :percentage="getUsagePercent(usage.interceptUsed, usage.interceptTotal)" :color="progressColors" />
            </div>
            <div class="tenant-info__usage-item">
              <div class="tenant-info__usage-info">
                <span class="tenant-info__usage-label">获客任务</span>
                <span class="tenant-info__usage-value">{{ usage.prospectUsed }}/{{ usage.prospectTotal }}</span>
              </div>
              <el-progress :percentage="getUsagePercent(usage.prospectUsed, usage.prospectTotal)" :color="progressColors" />
            </div>
            <div class="tenant-info__usage-item">
              <div class="tenant-info__usage-info">
                <span class="tenant-info__usage-label">内容发布</span>
                <span class="tenant-info__usage-value">{{ usage.contentUsed }}/{{ usage.contentTotal }}</span>
              </div>
              <el-progress :percentage="getUsagePercent(usage.contentUsed, usage.contentTotal)" :color="progressColors" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑租户信息"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="formData.tenantName" placeholder="请输入租户名称" />
        </el-form-item>
        <el-form-item label="所属行业" prop="industry">
          <el-select v-model="formData.industry" placeholder="请选择所属行业" style="width: 100%">
            <el-option v-for="item in industryList" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系邮箱" prop="contactEmail">
          <el-input v-model="formData.contactEmail" placeholder="请输入联系邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview TenantInfo 租户信息页
 * @description 展示当前租户的基本信息和套餐信息
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as tenantApi from '@/api/tenant.js'

// ============================================================
// Router
// ============================================================
const router = useRouter()

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

// 租户信息
const tenantInfo = reactive({
  id: null,
  tenantName: '',
  industry: '',
  status: 1,
  createTime: '',
  expireTime: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
})

// 套餐信息
const packageInfo = reactive({
  packageType: '',
  packageTypeLabel: '',
  packageName: '',
  price: 0,
  billingCycle: '月',
  dailyInterceptLimit: 0,
  dailyProspectLimit: 0,
  contentLimit: 0,
  accountLimit: 0,
  subAccountLimit: 0,
})

// 使用量
const usage = reactive({
  interceptUsed: 0,
  interceptTotal: 0,
  prospectUsed: 0,
  prospectTotal: 0,
  contentUsed: 0,
  contentTotal: 0,
})

// 表单数据
const formData = reactive({
  tenantName: '',
  industry: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
})

// 表单验证
const formRules = {
  tenantName: [
    { required: true, message: '请输入租户名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
  ],
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  contactEmail: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}

// 行业列表
const industryList = [
  { label: '教育培训', value: 'education' },
  { label: '金融保险', value: 'finance' },
  { label: '医疗健康', value: 'medical' },
  { label: '房地产', value: 'real_estate' },
  { label: '汽车服务', value: 'automotive' },
  { label: '餐饮服务', value: 'catering' },
  { label: '电商零售', value: 'ecommerce' },
  { label: '旅游出行', value: 'travel' },
  { label: '企业服务', value: 'enterprise' },
  { label: '其他', value: 'other' },
]

// 进度条颜色
const progressColors = [
  { color: '#10B981', percentage: 60 },
  { color: '#F59E0B', percentage: 80 },
  { color: '#EF4444', percentage: 100 },
]

// 状态映射
const statusMap = {
  0: { label: '禁用', type: 'danger' },
  1: { label: '正常', type: 'success' },
  2: { label: '待审核', type: 'warning' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载租户信息
 */
async function loadTenantInfo() {
  loading.value = true
  try {
    const response = await tenantApi.getCurrentTenant()
    const data = response.data || {}
    
    Object.assign(tenantInfo, data)
  } catch (error) {
    console.error('[TenantInfo] Load tenant failed:', error)
    // 使用模拟数据
    Object.assign(tenantInfo, {
      id: 'T2024001',
      tenantName: '北极星科技有限公司',
      industry: '企业服务',
      status: 1,
      createTime: '2024-01-15 10:30:00',
      expireTime: '2025-01-15 23:59:59',
      contactName: '张经理',
      contactPhone: '13800138000',
      contactEmail: 'contact@beijixing.com',
    })
  } finally {
    loading.value = false
  }
}

/**
 * 加载套餐信息
 */
async function loadPackageInfo() {
  try {
    const response = await tenantApi.getTenantPackage()
    const data = response.data || {}
    
    Object.assign(packageInfo, data)
  } catch (error) {
    console.error('[TenantInfo] Load package failed:', error)
    // 使用模拟数据
    Object.assign(packageInfo, {
      packageType: 'professional',
      packageTypeLabel: '专业版',
      packageName: '专业版套餐',
      price: 299,
      billingCycle: '月',
      dailyInterceptLimit: 500,
      dailyProspectLimit: 300,
      contentLimit: 100,
      accountLimit: 20,
      subAccountLimit: 10,
    })
  }
}

/**
 * 加载使用量统计
 */
function loadUsageStats() {
  // 模拟数据，实际应从API获取
  Object.assign(usage, {
    interceptUsed: 320,
    interceptTotal: 500,
    prospectUsed: 180,
    prospectTotal: 300,
    contentUsed: 45,
    contentTotal: 100,
  })
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
 * 获取使用百分比
 */
function getUsagePercent(used, total) {
  if (!total) return 0
  return Math.round((used / total) * 100)
}

/**
 * 编辑租户信息
 */
function handleEdit() {
  formData.tenantName = tenantInfo.tenantName
  formData.industry = tenantInfo.industry
  formData.contactName = tenantInfo.contactName
  formData.contactPhone = tenantInfo.contactPhone
  formData.contactEmail = tenantInfo.contactEmail
  dialogVisible.value = true
}

/**
 * 升级套餐
 */
function handleUpgrade() {
  router.push('/billing/recharge')
}

/**
 * 提交表单
 */
async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await tenantApi.updateCurrentTenant(formData)
    ElMessage.success('租户信息更新成功')
    dialogVisible.value = false
    loadTenantInfo()
  } catch (error) {
    console.error('[TenantInfo] Update failed:', error)
    ElMessage.error('更新失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadTenantInfo()
  loadPackageInfo()
  loadUsageStats()
})
</script>

<style lang="scss" scoped>
.tenant-info {
  &__loading {
    min-height: 400px;
  }

  &__row {
    margin-bottom: $spacing-lg;
  }

  &__card {
    margin-bottom: $spacing-lg;

    &--highlight {
      background: linear-gradient(135deg, rgba($color-primary, 0.05) 0%, rgba($color-secondary, 0.05) 100%);
    }
  }

  &__card-header {
    @include flex-between;
  }

  &__card-title {
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
  }

  &__basic {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: $spacing-lg;

    @media (max-width: $breakpoint-md) {
      grid-template-columns: 1fr;
    }
  }

  &__item {
    display: flex;
    align-items: center;
    padding: $spacing-sm 0;
    border-bottom: 1px solid $color-border-light;

    &:last-child {
      border-bottom: none;
    }
  }

  &__label {
    width: 100px;
    flex-shrink: 0;
    color: $color-text-secondary;
    font-size: $font-size-sm;
  }

  &__value {
    flex: 1;
    color: $color-text-primary;
    font-weight: $font-weight-medium;
  }

  &__package {
    text-align: center;
  }

  &__package-name {
    font-size: $font-size-xl;
    font-weight: $font-weight-bold;
    color: $color-text-primary;
    margin-bottom: $spacing-sm;
  }

  &__package-price {
    margin-bottom: $spacing-lg;

    .tenant-info__price {
      font-size: $font-size-3xl;
      font-weight: $font-weight-bold;
      color: $color-primary;
    }

    .tenant-info__unit {
      font-size: $font-size-sm;
      color: $color-text-secondary;
    }
  }

  &__quota-list {
    text-align: left;
    margin-bottom: $spacing-lg;
  }

  &__quota-item {
    @include flex-between;
    padding: $spacing-sm 0;
    border-bottom: 1px dashed $color-border-light;

    &:last-child {
      border-bottom: none;
    }
  }

  &__quota-label {
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__quota-value {
    font-size: $font-size-sm;
    font-weight: $font-weight-medium;
    color: $color-text-primary;
  }

  &__upgrade-btn {
    width: 100%;
  }

  &__usage-card {
    margin-top: $spacing-lg;
  }

  &__usage {
    display: flex;
    flex-direction: column;
    gap: $spacing-lg;
  }

  &__usage-item {
    display: flex;
    flex-direction: column;
    gap: $spacing-sm;
  }

  &__usage-info {
    @include flex-between;
  }

  &__usage-label {
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__usage-value {
    font-size: $font-size-sm;
    font-weight: $font-weight-medium;
    color: $color-text-primary;
  }
}
</style>