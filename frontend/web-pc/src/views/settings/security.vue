<template>
  <bx-content
    title="安全设置"
    description="管理您的账号安全和登录设备"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <div class="settings-security">
      <!-- 密码修改 -->
      <el-card class="settings-security__card" shadow="hover">
        <template #header>
          <div class="settings-security__card-header">
            <i class="el-icon-lock" />
            <span>修改密码</span>
          </div>
        </template>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          class="settings-security__form"
        >
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入当前密码"
                  show-password
                >
                  <template #prefix>
                    <i class="el-icon-lock" />
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                >
                  <template #prefix>
                    <i class="el-icon-key" />
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                >
                  <template #prefix>
                    <i class="el-icon-check" />
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>

          <div class="settings-security__password-tips">
            <p><i class="el-icon-info-filled" /> 密码要求：</p>
            <ul>
              <li :class="{ 'is-valid': passwordStrength.length }">至少8个字符</li>
              <li :class="{ 'is-valid': passwordStrength.hasNumber }">包含数字</li>
              <li :class="{ 'is-valid': passwordStrength.hasLetter }">包含字母</li>
              <li :class="{ 'is-valid': passwordStrength.hasSpecial }">包含特殊字符（!@#$%^&*等）</li>
            </ul>
          </div>

          <el-form-item>
            <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 登录设备管理 -->
      <el-card class="settings-security__card" shadow="hover">
        <template #header>
          <div class="settings-security__card-header">
            <i class="el-icon-monitor" />
            <span>登录设备管理</span>
            <el-button type="primary" link size="small" @click="handleLogoutAll">
              退出所有其他设备
            </el-button>
          </div>
        </template>

        <div class="settings-security__device-list">
          <div
            v-for="device in deviceList"
            :key="device.id"
            class="settings-security__device-item"
            :class="{ 'is-current': device.isCurrent }"
          >
            <div class="settings-security__device-icon">
              <i :class="getDeviceIcon(device.deviceType)" />
            </div>
            <div class="settings-security__device-info">
              <div class="settings-security__device-name">
                {{ device.deviceName }}
                <el-tag v-if="device.isCurrent" type="success" size="small" effect="plain">当前设备</el-tag>
              </div>
              <div class="settings-security__device-detail">
                <span>{{ device.location || '未知位置' }}</span>
                <span> · </span>
                <span>{{ device.ip || '未知IP' }}</span>
                <span> · </span>
                <span>{{ device.loginTime }}</span>
              </div>
            </div>
            <div class="settings-security__device-actions">
              <el-button
                v-if="!device.isCurrent"
                type="danger"
                link
                size="small"
                @click="handleLogoutDevice(device)"
              >
                退出登录
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-if="deviceList.length === 0" description="暂无登录设备记录" />
      </el-card>

      <!-- 操作日志 -->
      <el-card class="settings-security__card" shadow="hover">
        <template #header>
          <div class="settings-security__card-header">
            <i class="el-icon-document" />
            <span>操作日志</span>
            <div class="settings-security__log-filter">
              <el-date-picker
                v-model="logDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                size="small"
                style="width: 220px"
                @change="handleLogDateChange"
              />
            </div>
          </div>
        </template>

        <el-timeline>
          <el-timeline-item
            v-for="log in operationLogs"
            :key="log.id"
            :type="getLogType(log.type)"
            :timestamp="log.createTime"
            placement="top"
          >
            <el-card shadow="hover" class="settings-security__log-card">
              <div class="settings-security__log-content">
                <div class="settings-security__log-title">{{ log.title }}</div>
                <div class="settings-security__log-detail">{{ log.detail }}</div>
                <div class="settings-security__log-meta">
                  <span><i class="el-icon-map-location" /> {{ log.ip }}</span>
                  <span><i class="el-icon-monitor" /> {{ log.device }}</span>
                </div>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>

        <el-empty v-if="operationLogs.length === 0" description="暂无操作记录" />

        <div class="settings-security__pagination">
          <el-pagination
            :current-page="logPagination.page"
            :page-size="logPagination.size"
            :total="logPagination.total"
            layout="prev, pager, next"
            background
            @current-change="handleLogPageChange"
          />
        </div>
      </el-card>

      <!-- 账号注销 -->
      <el-card class="settings-security__card settings-security__danger-card" shadow="hover">
        <template #header>
          <div class="settings-security__card-header">
            <i class="el-icon-warning-filled" />
            <span>危险操作</span>
          </div>
        </template>

        <div class="settings-security__danger-section">
          <div class="settings-security__danger-item">
            <div class="settings-security__danger-info">
              <h4>注销账号</h4>
              <p>注销后，您的所有数据将被清除且无法恢复，请谨慎操作。</p>
              <el-alert
                v-if="deletionStatus.status === 'pending'"
                :title="`您的账号将于 ${deletionStatus.scheduledDate} 被注销，在此之前您可以取消注销`"
                type="warning"
                show-icon
                :closable="false"
                style="margin-top: 12px"
              />
            </div>
            <el-button
              v-if="deletionStatus.status === 'pending'"
              type="success"
              @click="handleCancelDeletion"
            >
              取消注销
            </el-button>
            <el-button
              v-else
              type="danger"
              plain
              @click="handleShowDeletionDialog"
            >
              申请注销
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 账号注销对话框 -->
    <el-dialog v-model="deletionDialogVisible" title="账号注销" width="500px">
      <div class="settings-security__deletion-content">
        <el-alert
          title="账号注销须知"
          type="warning"
          description="注销账号是不可逆的操作，请仔细阅读以下说明"
          show-icon
          :closable="false"
        />
        
        <div class="settings-security__deletion-rules">
          <h4>注销前请确认：</h4>
          <ul>
            <li>账号内的所有内容（包括已发布的内容）将被永久删除</li>
            <li>账号内的积分、余额等虚拟财产将被清零</li>
            <li>已绑定的第三方账号将自动解绑</li>
            <li>注销后有15天冷静期，期间可撤销注销申请</li>
            <li>冷静期结束后，账号将被永久删除，无法恢复</li>
          </ul>
        </div>

        <el-form ref="deletionFormRef" :model="deletionForm" :rules="deletionRules" label-position="top">
          <el-form-item label="注销原因" prop="reason">
            <el-select v-model="deletionForm.reason" placeholder="请选择注销原因" style="width: 100%">
              <el-option label="不再使用此服务" value="no_longer_use" />
              <el-option label="隐私顾虑" value="privacy" />
              <el-option label="账号安全问题" value="security" />
              <el-option label="遇到了使用问题" value="issues" />
              <el-option label="其他原因" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <div class="settings-security__verify-row">
              <el-input v-model="deletionForm.code" placeholder="请输入验证码" maxlength="6" />
              <el-button :disabled="deletionCodeCountdown > 0" @click="handleSendDeletionCode">
                {{ deletionCodeCountdown > 0 ? `${deletionCodeCountdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item prop="confirmed">
            <el-checkbox v-model="deletionForm.confirmed">
              我已阅读并了解注销账号的后果，确认注销
            </el-checkbox>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="deletionDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleting" :disabled="!deletionForm.confirmed" @click="handleConfirmDeletion">
          确认注销
        </el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview 安全设置页
 * @description 管理密码修改、登录设备、操作日志、账号注销等安全相关设置
 * @author EMP-FE-004 赵云
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logout } from '@/utils/auth.js'
import {
  changePassword,
  getLoginDevices,
  logoutDevice,
  logoutAllOtherDevices,
  getOperationLogs,
  applyAccountDeletion,
  cancelAccountDeletion,
  getAccountDeletionStatus,
  sendDeletionCode,
} from '@/api/settings.js'

// ============================================================
// 路由
// ============================================================
const router = useRouter()

// ============================================================
// 数据
// ============================================================

/** 密码表单 */
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

/** 密码表单引用 */
const passwordFormRef = ref(null)

/** 密码验证规则 */
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' },
    { validator: validateNewPassword, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

/** 修改密码状态 */
const changingPassword = ref(false)

/** 设备列表 */
const deviceList = ref([])

/** 日志日期范围 */
const logDateRange = ref([])

/** 操作日志 */
const operationLogs = ref([])

/** 日志分页 */
const logPagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

/** 账号注销状态 */
const deletionStatus = reactive({
  status: '',
  scheduledDate: '',
})

/** 注销对话框 */
const deletionDialogVisible = ref(false)
const deleting = ref(false)
const deletionForm = reactive({
  reason: '',
  code: '',
  confirmed: false,
})
const deletionFormRef = ref(null)
const deletionRules = {
  reason: [{ required: true, message: '请选择注销原因', trigger: 'change' }],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
  confirmed: [
    { validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('请确认已了解注销后果'))
      } else {
        callback()
      }
    }, trigger: 'change' },
  ],
}
const deletionCodeCountdown = ref(0)

// ============================================================
// 计算属性
// ============================================================

/** 密码强度 */
const passwordStrength = computed(() => {
  const pwd = passwordForm.newPassword
  return {
    length: pwd.length >= 8,
    hasNumber: /\d/.test(pwd),
    hasLetter: /[a-zA-Z]/.test(pwd),
    hasSpecial: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pwd),
  }
})

// ============================================================
// 方法
// ============================================================

/**
 * 验证新密码
 */
function validateNewPassword(rule, value, callback) {
  if (value === passwordForm.oldPassword) {
    callback(new Error('新密码不能与当前密码相同'))
  } else {
    callback()
  }
}

/**
 * 验证确认密码
 */
function validateConfirmPassword(rule, value, callback) {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/**
 * 修改密码
 */
async function handleChangePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    
    // 清空表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    
    // 延迟登出
    setTimeout(() => {
      logout()
      router.push('/login')
    }, 1500)
  } catch (error) {
    console.error('修改失败:', error)
    ElMessage.error('密码修改失败，请检查当前密码是否正确')
  } finally {
    changingPassword.value = false
  }
}

/**
 * 获取设备图标
 */
function getDeviceIcon(deviceType) {
  const icons = {
    desktop: 'el-icon-monitor',
    mobile: 'el-icon-mobile',
    tablet: 'el-icon-tablet',
    web: 'el-icon-connection',
  }
  return icons[deviceType] || 'el-icon-monitor'
}

/**
 * 加载登录设备列表
 */
async function loadLoginDevices() {
  try {
    const res = await getLoginDevices()
    deviceList.value = res || []
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

/**
 * 退出指定设备
 */
function handleLogoutDevice(device) {
  ElMessageBox.confirm(`确定要退出 "${device.deviceName}" 的登录吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await logoutDevice(device.id)
        ElMessage.success('已退出该设备')
        loadLoginDevices()
      } catch (error) {
        ElMessage.error('操作失败')
      }
    })
    .catch(() => {})
}

/**
 * 退出所有其他设备
 */
function handleLogoutAll() {
  ElMessageBox.confirm('确定要退出除当前设备外的所有登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await logoutAllOtherDevices()
        ElMessage.success('已退出其他所有设备')
        loadLoginDevices()
      } catch (error) {
        ElMessage.error('操作失败')
      }
    })
    .catch(() => {})
}

/**
 * 获取日志类型
 */
function getLogType(type) {
  const types = {
    login: 'success',
    logout: 'info',
    password_change: 'warning',
    profile_update: 'primary',
    publish: 'success',
    delete: 'danger',
  }
  return types[type] || 'info'
}

/**
 * 加载操作日志
 */
async function loadOperationLogs() {
  try {
    const params = {
      page: logPagination.page,
      size: logPagination.size,
    }
    if (logDateRange.value && logDateRange.value.length === 2) {
      params.startDate = logDateRange.value[0]
      params.endDate = logDateRange.value[1]
    }
    const res = await getOperationLogs(params)
    operationLogs.value = res.list || []
    logPagination.total = res.pagination?.total || 0
  } catch (error) {
    console.error('加载操作日志失败:', error)
  }
}

/**
 * 日志日期变化
 */
function handleLogDateChange() {
  logPagination.page = 1
  loadOperationLogs()
}

/**
 * 日志分页变化
 */
function handleLogPageChange(page) {
  logPagination.page = page
  loadOperationLogs()
}

/**
 * 加载注销状态
 */
async function loadDeletionStatus() {
  try {
    const res = await getAccountDeletionStatus()
    deletionStatus.status = res.status
    deletionStatus.scheduledDate = res.scheduledDate
  } catch (error) {
    console.error('加载注销状态失败:', error)
  }
}

/**
 * 显示注销对话框
 */
function handleShowDeletionDialog() {
  deletionForm.reason = ''
  deletionForm.code = ''
  deletionForm.confirmed = false
  deletionCodeCountdown.value = 0
  deletionDialogVisible.value = true
}

/**
 * 发送注销验证码
 */
async function handleSendDeletionCode() {
  try {
    // 优先使用短信，其次邮箱
    const type = 'sms'
    await sendDeletionCode(type)
    ElMessage.success('验证码已发送')
    startDeletionCountdown()
  } catch (error) {
    console.error('发送失败:', error)
    ElMessage.error('验证码发送失败')
  }
}

/**
 * 注销验证码倒计时
 */
function startDeletionCountdown() {
  deletionCodeCountdown.value = 60
  const timer = setInterval(() => {
    deletionCodeCountdown.value--
    if (deletionCodeCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

/**
 * 确认注销
 */
async function handleConfirmDeletion() {
  const valid = await deletionFormRef.value.validate().catch(() => false)
  if (!valid) return

  deleting.value = true
  try {
    await applyAccountDeletion({
      reason: deletionForm.reason,
      verifyCode: deletionForm.code,
    })
    ElMessage.success('注销申请已提交，账号将在15天后被注销')
    deletionDialogVisible.value = false
    loadDeletionStatus()
  } catch (error) {
    console.error('注销失败:', error)
    ElMessage.error('注销申请失败，请稍后重试')
  } finally {
    deleting.value = false
  }
}

/**
 * 取消注销
 */
async function handleCancelDeletion() {
  ElMessageBox.confirm('确定要取消账号注销吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await cancelAccountDeletion()
        ElMessage.success('已取消注销申请')
        loadDeletionStatus()
      } catch (error) {
        ElMessage.error('取消失败')
      }
    })
    .catch(() => {})
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadLoginDevices()
  loadOperationLogs()
  loadDeletionStatus()
})
</script>

<style lang="scss" scoped>
.settings-security {
  &__card {
    margin-bottom: 20px;

    :deep(.el-card__header) {
      padding: 16px 20px;
      background: var(--bg-secondary);
    }
  }

  &__danger-card {
    :deep(.el-card__header) {
      background: #fef2f2;
      color: #dc2626;

      i {
        color: #dc2626;
      }
    }
  }

  &__card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    color: var(--text-primary);

    i {
      color: var(--primary-color);
    }

    .el-button {
      margin-left: auto;
    }
  }

  &__form {
    :deep(.el-form-item__label) {
      font-weight: 500;
    }
  }

  &__password-tips {
    margin-bottom: 20px;
    padding: 16px;
    background: var(--bg-secondary);
    border-radius: 8px;

    p {
      margin: 0 0 8px;
      font-size: 14px;
      font-weight: 500;
      color: var(--text-primary);

      i {
        color: var(--primary-color);
        margin-right: 4px;
      }
    }

    ul {
      margin: 0;
      padding-left: 20px;
      list-style: none;
    }

    li {
      position: relative;
      padding-left: 16px;
      margin-bottom: 4px;
      font-size: 13px;
      color: var(--text-secondary);

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 8px;
        width: 6px;
        height: 6px;
        background: #ccc;
        border-radius: 50%;
        transition: background 0.3s;
      }

      &.is-valid {
        color: var(--success-color);

        &::before {
          background: var(--success-color);
        }
      }
    }
  }

  &__device-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__device-item {
    display: flex;
    align-items: center;
    padding: 16px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    transition: all 0.3s;

    &:hover {
      border-color: var(--primary-color);
      background: var(--bg-secondary);
    }

    &.is-current {
      border-color: var(--success-color);
      background: #f0fdf4;
    }
  }

  &__device-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-secondary);
    border-radius: 12px;
    margin-right: 16px;

    i {
      font-size: 24px;
      color: var(--text-secondary);
    }
  }

  &__device-info {
    flex: 1;
  }

  &__device-name {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  &__device-detail {
    font-size: 13px;
    color: var(--text-secondary);
  }

  &__device-actions {
    margin-left: 16px;
  }

  &__log-filter {
    margin-left: auto;
  }

  &__log-card {
    :deep(.el-card__body) {
      padding: 12px 16px;
    }
  }

  &__log-content {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__log-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
  }

  &__log-detail {
    font-size: 13px;
    color: var(--text-secondary);
  }

  &__log-meta {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: var(--text-secondary);

    i {
      margin-right: 4px;
    }
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  &__danger-section {
    padding: 8px 0;
  }

  &__danger-item {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 16px 0;
  }

  &__danger-info {
    flex: 1;

    h4 {
      margin: 0 0 8px;
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
    }

    p {
      margin: 0;
      font-size: 14px;
      color: var(--text-secondary);
      line-height: 1.6;
    }
  }

  &__deletion-content {
    :deep(.el-alert) {
      margin-bottom: 20px;
    }
  }

  &__deletion-rules {
    margin-bottom: 20px;
    padding: 16px;
    background: var(--bg-secondary);
    border-radius: 8px;

    h4 {
      margin: 0 0 12px;
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
    }

    ul {
      margin: 0;
      padding-left: 20px;
    }

    li {
      margin-bottom: 8px;
      font-size: 13px;
      color: var(--text-secondary);
      line-height: 1.6;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }

  &__verify-row {
    display: flex;
    gap: 12px;

    .el-input {
      flex: 1;
    }
  }
}

@media (max-width: 768px) {
  .settings-security {
    &__card-header {
      flex-wrap: wrap;
      gap: 12px;

      .el-button {
        margin-left: 0;
      }
    }

    &__device-item {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    &__device-icon {
      margin-right: 0;
    }

    &__device-actions {
      margin-left: 0;
      width: 100%;

      .el-button {
        width: 100%;
      }
    }

    &__log-filter {
      width: 100%;
      margin-left: 0;
      margin-top: 12px;

      .el-date-editor {
        width: 100% !important;
      }
    }

    &__danger-item {
      flex-direction: column;
      gap: 16px;
    }

    &__verify-row {
      flex-direction: column;

      .el-button {
        width: 100%;
      }
    }
  }
}
</style>
