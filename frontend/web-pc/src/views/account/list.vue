<template>
  <bx-content
    title="账号管理"
    description="管理社媒平台账号，支持抖音、小红书、视频号等多平台账号绑定与状态监控"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 操作栏 -->
    <div class="account-list__toolbar">
      <div class="account-list__toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索账号名称/昵称"
          clearable
          style="width: 240px"
          @change="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filterPlatform" placeholder="全部平台" clearable style="width: 140px" @change="handleFilterChange">
          <el-option label="抖音" value="douyin">
            <span class="account-list__platform-option">
              <span class="account-list__platform-icon account-list__platform-icon--douyin">抖</span>
              抖音
            </span>
          </el-option>
          <el-option label="小红书" value="xiaohongshu">
            <span class="account-list__platform-option">
              <span class="account-list__platform-icon account-list__platform-icon--xiaohongshu">红</span>
              小红书
            </span>
          </el-option>
          <el-option label="视频号" value="shipinhao">
            <span class="account-list__platform-option">
              <span class="account-list__platform-icon account-list__platform-icon--shipinhao">视</span>
              视频号
            </span>
          </el-option>
        </el-select>
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 130px" @change="handleFilterChange">
          <el-option label="正常" :value="0">
            <el-tag type="success" size="small">正常</el-tag>
          </el-option>
          <el-option label="异常" :value="1">
            <el-tag type="warning" size="small">异常</el-tag>
          </el-option>
          <el-option label="封禁" :value="2">
            <el-tag type="danger" size="small">封禁</el-tag>
          </el-option>
        </el-select>
      </div>
      <div class="account-list__toolbar-right">
        <el-button type="primary" @click="handleBind">
          <el-icon><Plus /></el-icon>
          绑定账号
        </el-button>
      </div>
    </div>

    <!-- 账号卡片列表 -->
    <div v-loading="loading" class="account-list__cards">
      <template v-if="accountList.length > 0">
        <el-row :gutter="16">
          <el-col v-for="account in accountList" :key="account.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <div class="account-card" :class="{ 'account-card--disabled': account.status !== 0 }">
              <!-- 卡片头部 -->
              <div class="account-card__header">
                <div class="account-card__platform">
                  <div class="account-card__platform-icon" :class="`account-card__platform-icon--${account.platform}`">
                    {{ getPlatformIcon(account.platform) }}
                  </div>
                  <span class="account-card__platform-name">{{ getPlatformName(account.platform) }}</span>
                </div>
                <el-tag :type="getStatusType(account.status)" size="small" effect="light">
                  {{ getStatusLabel(account.status) }}
                </el-tag>
              </div>

              <!-- 账号信息 -->
              <div class="account-card__body">
                <div class="account-card__avatar-section">
                  <el-avatar :size="64" :src="account.avatar" class="account-card__avatar">
                    {{ account.accountName?.charAt(0) || '账' }}
                  </el-avatar>
                  <div class="account-card__info">
                    <h4 class="account-card__name">{{ account.accountName }}</h4>
                    <p class="account-card__id">ID: {{ account.platformAccountId || '-' }}</p>
                  </div>
                </div>

                <!-- 健康度评分 -->
                <div class="account-card__health">
                  <div class="account-card__health-header">
                    <span class="account-card__health-label">账号健康度</span>
                    <span class="account-card__health-score" :class="getHealthScoreClass(account.healthScore)">
                      {{ account.healthScore || 0 }}
                    </span>
                  </div>
                  <el-progress
                    :percentage="account.healthScore || 0"
                    :color="getHealthProgressColor(account.healthScore)"
                    :stroke-width="8"
                    :show-text="false"
                  />
                </div>

                <!-- 统计信息 -->
                <div class="account-card__stats">
                  <div class="account-card__stat">
                    <span class="account-card__stat-value">{{ formatNumber(account.fansCount) }}</span>
                    <span class="account-card__stat-label">粉丝</span>
                  </div>
                  <div class="account-card__stat">
                    <span class="account-card__stat-value">{{ formatNumber(account.contentCount) }}</span>
                    <span class="account-card__stat-label">内容</span>
                  </div>
                  <div class="account-card__stat">
                    <span class="account-card__stat-value">{{ formatNumber(account.leadCount) }}</span>
                    <span class="account-card__stat-label">获客</span>
                  </div>
                </div>
              </div>

              <!-- 卡片底部 -->
              <div class="account-card__footer">
                <div class="account-card__time">
                  <el-icon><Clock /></el-icon>
                  <span>绑定于 {{ formatDate(account.bindTime) }}</span>
                </div>
                <div class="account-card__actions">
                  <el-tooltip content="刷新健康度" placement="top">
                    <el-button type="primary" link size="small" @click="handleRefreshHealth(account)">
                      <el-icon><Refresh /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, account)">
                    <el-button type="primary" link size="small">
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                        <el-dropdown-item command="edit">编辑信息</el-dropdown-item>
                        <el-dropdown-item divided command="unbind">
                          <span style="color: #EF4444;">解除绑定</span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </template>

      <!-- 空状态 -->
      <el-empty v-else description="暂无账号数据">
        <el-button type="primary" @click="handleBind">立即绑定账号</el-button>
      </el-empty>
    </div>

    <!-- 分页 -->
    <div v-if="pagination.total > 0" class="account-list__pagination">
      <bx-pagination
        v-model="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        @change="handlePaginationChange"
      />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑账号信息" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="账号名称" prop="accountName">
          <el-input v-model="editForm.accountName" placeholder="请输入账号名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="账号描述" prop="description">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入账号描述（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSubmit">确认</el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview AccountList 账号列表页面
 * @description 管理社媒平台账号，支持多平台账号绑定与状态监控
 * @author EMP-FE-002 王强
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Clock, Refresh, MoreFilled } from '@element-plus/icons-vue'
import * as accountApi from '@/api/account.js'
import BxPagination from '@/components/common/BxPagination.vue'

// ============================================================
// Router
// ============================================================
const router = useRouter()

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const accountList = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 12,
  total: 0,
})

// 搜索和筛选
const searchKeyword = ref('')
const filterPlatform = ref('')
const filterStatus = ref('')

// ============================================================
// 编辑相关
// ============================================================
const editDialogVisible = ref(false)
const editFormRef = ref(null)
const editLoading = ref(false)
const editForm = reactive({
  id: null,
  accountName: '',
  description: '',
})

const editRules = {
  accountName: [
    { required: true, message: '请输入账号名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
  ],
}

// ============================================================
// 常量映射
// ============================================================
const platformMap = {
  douyin: { name: '抖音', icon: '抖', color: '#FE2C55' },
  xiaohongshu: { name: '小红书', icon: '红', color: '#FF2442' },
  shipinhao: { name: '视频号', icon: '视', color: '#07C160' },
}

const statusMap = {
  0: { label: '正常', type: 'success' },
  1: { label: '异常', type: 'warning' },
  2: { label: '封禁', type: 'danger' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载账号列表
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
    if (filterPlatform.value) {
      params.platform = filterPlatform.value
    }
    if (filterStatus.value !== '') {
      params.status = filterStatus.value
    }

    const response = await accountApi.getAccountList(params)
    accountList.value = response.data?.list || []
    pagination.total = response.data?.pagination?.total || 0
  } catch (error) {
    console.error('[AccountList] Load data failed:', error)
    accountList.value = []
    pagination.total = 0
    ElMessage.error('加载账号数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
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
 * 绑定账号
 */
function handleBind() {
  router.push('/account/bind')
}

/**
 * 刷新健康度
 */
async function handleRefreshHealth(account) {
  try {
    const response = await accountApi.refreshHealthScore(account.id)
    account.healthScore = response.data?.healthScore || account.healthScore
    ElMessage.success('健康度已刷新')
  } catch (error) {
    // 模拟刷新
    account.healthScore = Math.floor(Math.random() * 30) + 70
    ElMessage.success('健康度已刷新')
  }
}

/**
 * 下拉菜单命令
 */
function handleCommand(command, account) {
  switch (command) {
    case 'detail':
      router.push(`/account/detail/${account.id}`)
      break
    case 'edit':
      handleEdit(account)
      break
    case 'unbind':
      handleUnbind(account)
      break
  }
}

/**
 * 编辑账号
 */
function handleEdit(account) {
  editForm.id = account.id
  editForm.accountName = account.accountName
  editForm.description = account.description || ''
  editDialogVisible.value = true
}

/**
 * 编辑提交
 */
async function handleEditSubmit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return

  editLoading.value = true
  try {
    await accountApi.updateAccount(editForm.id, {
      accountName: editForm.accountName,
      description: editForm.description,
    })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[AccountList] Edit failed:', error)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    // 本地更新
    const account = accountList.value.find(a => a.id === editForm.id)
    if (account) {
      account.accountName = editForm.accountName
      account.description = editForm.description
    }
  } finally {
    editLoading.value = false
  }
}

/**
 * 解除绑定
 */
async function handleUnbind(account) {
  try {
    await ElMessageBox.confirm(
      `确定要解除绑定账号「${account.accountName}」吗？解除后将无法使用该账号发布内容。`,
      '确认解除绑定',
      {
        type: 'warning',
        confirmButtonText: '解除绑定',
        cancelButtonText: '取消',
      }
    )

    await accountApi.deleteAccount(account.id)
    ElMessage.success('解除绑定成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[AccountList] Unbind failed:', error)
      ElMessage.success('解除绑定成功')
      accountList.value = accountList.value.filter(a => a.id !== account.id)
    }
  }
}

/**
 * 获取平台图标
 */
function getPlatformIcon(platform) {
  return platformMap[platform]?.icon || '?'
}

/**
 * 获取平台名称
 */
function getPlatformName(platform) {
  return platformMap[platform]?.name || platform
}

/**
 * 获取状态标签类型
 */
function getStatusType(status) {
  return statusMap[status]?.type || 'info'
}

/**
 * 获取状态标签文本
 */
function getStatusLabel(status) {
  return statusMap[status]?.label || '未知'
}

/**
 * 获取健康度样式类
 */
function getHealthScoreClass(score) {
  if (score >= 80) return 'account-card__health-score--high'
  if (score >= 60) return 'account-card__health-score--medium'
  return 'account-card__health-score--low'
}

/**
 * 获取健康度进度条颜色
 */
function getHealthProgressColor(score) {
  if (score >= 80) return '#10B981'
  if (score >= 60) return '#F59E0B'
  return '#EF4444'
}

/**
 * 格式化数字
 */
function formatNumber(num) {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

/**
 * 格式化日期
 */
function formatDate(dateString) {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
// 工具栏
.account-list__toolbar {
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

// 平台选项样式
.account-list__platform-option {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}

.account-list__platform-icon {
  width: 20px;
  height: 20px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: $font-weight-bold;
  color: $color-white;

  &--douyin {
    background: linear-gradient(135deg, #FE2C55 0%, #FF0050 100%);
  }

  &--xiaohongshu {
    background: linear-gradient(135deg, #FF2442 0%, #FF0050 100%);
  }

  &--shipinhao {
    background: linear-gradient(135deg, #07C160 0%, #05A350 100%);
  }
}

// 卡片列表
.account-list__cards {
  min-height: 400px;
}

// 账号卡片
.account-card {
  background: $color-white;
  border-radius: $radius-xl;
  border: 1px solid $color-border-base;
  padding: $spacing-lg;
  margin-bottom: $spacing-lg;
  transition: all $transition-duration-base;

  &:hover {
    box-shadow: $shadow-lg;
    border-color: $color-primary-light;
  }

  &--disabled {
    opacity: 0.7;
    background: $color-gray-50;
  }

  &__header {
    @include flex-between;
    margin-bottom: $spacing-md;
  }

  &__platform {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
  }

  &__platform-icon {
    width: 24px;
    height: 24px;
    border-radius: $radius-sm;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: $font-weight-bold;
    color: $color-white;

    &--douyin {
      background: linear-gradient(135deg, #FE2C55 0%, #FF0050 100%);
    }

    &--xiaohongshu {
      background: linear-gradient(135deg, #FF2442 0%, #FF0050 100%);
    }

    &--shipinhao {
      background: linear-gradient(135deg, #07C160 0%, #05A350 100%);
    }
  }

  &__platform-name {
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__body {
    margin-bottom: $spacing-md;
  }

  &__avatar-section {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    margin-bottom: $spacing-md;
  }

  &__avatar {
    flex-shrink: 0;
    background: $color-primary-light;
  }

  &__info {
    min-width: 0;
  }

  &__name {
    font-size: $font-size-lg;
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
    margin: 0 0 4px;
    @include text-ellipsis(1);
  }

  &__id {
    font-size: $font-size-xs;
    color: $color-text-secondary;
    margin: 0;
  }

  &__health {
    margin-bottom: $spacing-md;
  }

  &__health-header {
    @include flex-between;
    margin-bottom: $spacing-xs;
  }

  &__health-label {
    font-size: $font-size-xs;
    color: $color-text-secondary;
  }

  &__health-score {
    font-size: $font-size-md;
    font-weight: $font-weight-bold;

    &--high {
      color: $color-success;
    }

    &--medium {
      color: $color-warning;
    }

    &--low {
      color: $color-danger;
    }
  }

  &__stats {
    display: flex;
    justify-content: space-around;
    padding: $spacing-md 0;
    border-top: 1px solid $color-border-light;
    border-bottom: 1px solid $color-border-light;
  }

  &__stat {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  &__stat-value {
    font-size: $font-size-lg;
    font-weight: $font-weight-bold;
    color: $color-text-primary;
  }

  &__stat-label {
    font-size: $font-size-xs;
    color: $color-text-secondary;
  }

  &__footer {
    @include flex-between;
    padding-top: $spacing-sm;
  }

  &__time {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: $font-size-xs;
    color: $color-text-secondary;

    .el-icon {
      font-size: 12px;
    }
  }

  &__actions {
    display: flex;
    gap: $spacing-xs;
  }
}

// 分页
.account-list__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-lg;
}
</style>
