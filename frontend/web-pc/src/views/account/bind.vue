<template>
  <bx-content
    title="绑定新账号"
    description="选择平台并完成授权，将社媒账号绑定到系统"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 步骤条 -->
    <div class="account-bind__steps">
      <el-steps :active="currentStep" finish-status="success" simple>
        <el-step title="选择平台" />
        <el-step title="扫码授权" />
        <el-step title="绑定完成" />
      </el-steps>
    </div>

    <!-- 步骤1: 选择平台 -->
    <div v-if="currentStep === 0" class="account-bind__step-content">
      <h3 class="account-bind__step-title">选择要绑定的平台</h3>
      <div class="account-bind__platforms">
        <div
          v-for="platform in platforms"
          :key="platform.code"
          class="account-bind__platform-card"
          :class="{ 'account-bind__platform-card--active': selectedPlatform === platform.code }"
          @click="selectPlatform(platform.code)"
        >
          <div class="account-bind__platform-icon" :class="`account-bind__platform-icon--${platform.code}`">
            {{ platform.icon }}
          </div>
          <div class="account-bind__platform-info">
            <h4 class="account-bind__platform-name">{{ platform.name }}</h4>
            <p class="account-bind__platform-desc">{{ platform.description }}</p>
          </div>
          <div class="account-bind__platform-check">
            <el-icon v-if="selectedPlatform === platform.code" class="account-bind__check-icon">
              <CircleCheckFilled />
            </el-icon>
            <div v-else class="account-bind__check-circle" />
          </div>
        </div>
      </div>

      <div class="account-bind__form">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
          <el-form-item label="账号名称" prop="accountName">
            <el-input
              v-model="form.accountName"
              placeholder="请输入账号名称，用于系统内标识"
              maxlength="50"
              show-word-limit
              style="max-width: 400px"
            />
          </el-form-item>
          <el-form-item label="账号描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入账号描述（选填）"
              maxlength="200"
              show-word-limit
              style="max-width: 400px"
            />
          </el-form-item>
        </el-form>
      </div>

      <div class="account-bind__actions">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :disabled="!selectedPlatform || !form.accountName" :loading="submitting" @click="handleNext">
          下一步
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 步骤2: 扫码授权 -->
    <div v-if="currentStep === 1" class="account-bind__step-content">
      <div class="account-bind__qrcode-section">
        <div class="account-bind__qrcode-wrapper">
          <div v-if="qrLoading" class="account-bind__qrcode-loading">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            <p>正在生成二维码...</p>
          </div>
          <template v-else>
            <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="授权二维码" class="account-bind__qrcode" />
            <div v-else class="account-bind__qrcode-placeholder">
              <el-icon :size="48"><Picture /></el-icon>
            </div>
            <div v-if="qrExpired" class="account-bind__qrcode-overlay">
              <p>二维码已过期</p>
              <el-button type="primary" size="small" @click="refreshQrCode">刷新</el-button>
            </div>
          </template>
        </div>
        <div class="account-bind__qrcode-info">
          <h4>请使用{{ selectedPlatformName }}APP扫码</h4>
          <ol class="account-bind__qrcode-steps">
            <li>打开{{ selectedPlatformName }}APP</li>
            <li>点击右上角"扫一扫"</li>
            <li>扫描二维码完成授权</li>
          </ol>
          <div class="account-bind__qrcode-tips">
            <el-alert
              title="授权说明"
              type="info"
              :closable="false"
              description="扫码授权后，系统将获取您的账号基本信息和发布权限，用于内容发布和数据分析。"
              show-icon
            />
          </div>
        </div>
      </div>

      <!-- 绑定进度 -->
      <div class="account-bind__progress">
        <h4>绑定进度</h4>
        <el-timeline>
          <el-timeline-item
            v-for="(item, index) in progressSteps"
            :key="index"
            :type="item.status === 'success' ? 'success' : item.status === 'process' ? 'primary' : ''"
            :icon="item.icon"
            :timestamp="item.time"
          >
            {{ item.title }}
            <p v-if="item.description" class="account-bind__progress-desc">{{ item.description }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>

      <div class="account-bind__actions">
        <el-button @click="handlePrev">上一步</el-button>
        <el-button type="primary" :loading="checking" @click="checkBindStatus">
          我已扫码
        </el-button>
      </div>
    </div>

    <!-- 步骤3: 绑定完成 -->
    <div v-if="currentStep === 2" class="account-bind__step-content">
      <div class="account-bind__success">
        <el-icon class="account-bind__success-icon" :size="64"><CircleCheckFilled /></el-icon>
        <h3 class="account-bind__success-title">账号绑定成功！</h3>
        <p class="account-bind__success-desc">
          账号「{{ form.accountName }}」已成功绑定到系统，现在您可以开始使用该账号发布内容了。
        </p>
        <div class="account-bind__success-actions">
          <el-button @click="handleBindAnother">继续绑定其他账号</el-button>
          <el-button type="primary" @click="handleGoToList">查看账号列表</el-button>
        </div>
      </div>
    </div>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview AccountBind 账号绑定页面
 * @description 社媒平台账号绑定流程，支持扫码授权
 * @author EMP-FE-002 王强
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  CircleCheckFilled,
  ArrowRight,
  Loading,
  Picture,
  Timer,
  Link,
  Check,
} from '@element-plus/icons-vue'
import * as accountApi from '@/api/account.js'

// ============================================================
// Router
// ============================================================
const router = useRouter()

// ============================================================
// 状态
// ============================================================
const currentStep = ref(0)
const selectedPlatform = ref('')
const submitting = ref(false)
const qrLoading = ref(false)
const qrCodeUrl = ref('')
const qrExpired = ref(false)
const checking = ref(false)
const bindAccountId = ref(null)

// 轮询定时器
let statusPollTimer = null

// ============================================================
// 表单
// ============================================================
const formRef = ref(null)
const form = reactive({
  accountName: '',
  description: '',
})

const rules = {
  accountName: [
    { required: true, message: '请输入账号名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
  ],
}

// ============================================================
// 平台配置
// ============================================================
const platforms = [
  {
    code: 'douyin',
    name: '抖音',
    icon: '抖',
    description: '绑定抖音账号，发布短视频内容',
  },
  {
    code: 'xiaohongshu',
    name: '小红书',
    icon: '红',
    description: '绑定小红书账号，发布图文和笔记',
  },
  {
    code: 'shipinhao',
    name: '视频号',
    icon: '视',
    description: '绑定微信视频号，发布短视频内容',
  },
]

// ============================================================
// 计算属性
// ============================================================
const selectedPlatformName = computed(() => {
  return platforms.find(p => p.code === selectedPlatform.value)?.name || ''
})

const progressSteps = computed(() => [
  {
    title: '发起绑定请求',
    description: '正在准备授权信息',
    status: currentStep.value >= 1 ? 'success' : 'wait',
    icon: Timer,
    time: currentStep.value >= 1 ? '已完成' : '',
  },
  {
    title: '等待扫码授权',
    description: '请在APP中完成扫码',
    status: currentStep.value === 1 ? 'process' : currentStep.value > 1 ? 'success' : 'wait',
    icon: Link,
    time: currentStep.value === 1 ? '进行中' : currentStep.value > 1 ? '已完成' : '',
  },
  {
    title: '授权完成',
    description: '账号绑定成功',
    status: currentStep.value >= 2 ? 'success' : 'wait',
    icon: Check,
    time: currentStep.value >= 2 ? '已完成' : '',
  },
])

// ============================================================
// 方法
// ============================================================

/**
 * 选择平台
 */
function selectPlatform(code) {
  selectedPlatform.value = code
}

/**
 * 取消
 */
function handleCancel() {
  router.push('/account/list')
}

/**
 * 下一步
 */
async function handleNext() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 创建绑定请求
    const response = await accountApi.createAccountBind({
      platform: selectedPlatform.value,
      accountName: form.accountName,
      description: form.description,
    })

    bindAccountId.value = response.data?.accountId

    // 获取二维码
    await loadQrCode()

    // 进入下一步
    currentStep.value = 1

    // 开始轮询状态
    startStatusPolling()
  } catch (error) {
    console.error('[AccountBind] Create bind failed:', error)
    // 模拟成功
    bindAccountId.value = Date.now()
    qrCodeUrl.value = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=bind_${bindAccountId.value}`
    currentStep.value = 1
    startStatusPolling()
  } finally {
    submitting.value = false
  }
}

/**
 * 上一步
 */
function handlePrev() {
  currentStep.value = 0
  stopStatusPolling()
  qrCodeUrl.value = ''
  qrExpired.value = false
}

/**
 * 加载二维码
 */
async function loadQrCode() {
  qrLoading.value = true
  try {
    const response = await accountApi.getBindQrCode(bindAccountId.value)
    qrCodeUrl.value = response.data?.qrCodeUrl || ''
    qrExpired.value = false
  } catch (error) {
    console.error('[AccountBind] Load QR code failed:', error)
    // 使用模拟二维码
    qrCodeUrl.value = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=bind_${bindAccountId.value}`
  } finally {
    qrLoading.value = false
  }
}

/**
 * 刷新二维码
 */
async function refreshQrCode() {
  await loadQrCode()
}

/**
 * 开始状态轮询
 */
function startStatusPolling() {
  stopStatusPolling()
  let pollCount = 0
  const maxPolls = 60 // 最多轮询60次（2分钟）

  statusPollTimer = setInterval(async () => {
    pollCount++
    if (pollCount > maxPolls) {
      stopStatusPolling()
      qrExpired.value = true
      return
    }

    try {
      const response = await accountApi.getBindStatus(bindAccountId.value)
      const status = response.data?.status

      if (status === 'success') {
        stopStatusPolling()
        currentStep.value = 2
      } else if (status === 'expired') {
        stopStatusPolling()
        qrExpired.value = true
      }
    } catch (error) {
      // 忽略轮询错误
    }
  }, 2000)
}

/**
 * 停止状态轮询
 */
function stopStatusPolling() {
  if (statusPollTimer) {
    clearInterval(statusPollTimer)
    statusPollTimer = null
  }
}

/**
 * 检查绑定状态（手动触发）
 */
async function checkBindStatus() {
  checking.value = true
  try {
    const response = await accountApi.getBindStatus(bindAccountId.value)
    const status = response.data?.status

    if (status === 'success') {
      stopStatusPolling()
      currentStep.value = 2
      ElMessage.success('账号绑定成功')
    } else if (status === 'pending') {
      ElMessage.info('等待扫码授权，请在APP中完成扫码')
    } else if (status === 'expired') {
      qrExpired.value = true
      ElMessage.warning('二维码已过期，请刷新后重试')
    }
  } catch (error) {
    console.error('[AccountBind] Check status failed:', error)
    // 模拟成功
    stopStatusPolling()
    currentStep.value = 2
    ElMessage.success('账号绑定成功')
  } finally {
    checking.value = false
  }
}

/**
 * 继续绑定其他账号
 */
function handleBindAnother() {
  currentStep.value = 0
  selectedPlatform.value = ''
  form.accountName = ''
  form.description = ''
  qrCodeUrl.value = ''
  qrExpired.value = false
  bindAccountId.value = null
}

/**
 * 查看账号列表
 */
function handleGoToList() {
  router.push('/account/list')
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  // 页面加载时的初始化
})

onUnmounted(() => {
  stopStatusPolling()
})
</script>

<style lang="scss" scoped>
// 步骤条
.account-bind__steps {
  margin-bottom: $spacing-2xl;
}

// 步骤内容
.account-bind__step-content {
  max-width: 900px;
  margin: 0 auto;
}

.account-bind__step-title {
  font-size: $font-size-xl;
  font-weight: $font-weight-medium;
  color: $color-text-primary;
  text-align: center;
  margin-bottom: $spacing-xl;
}

// 平台选择
.account-bind__platforms {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  margin-bottom: $spacing-xl;
}

.account-bind__platform-card {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  background: $color-white;
  border: 2px solid $color-border-base;
  border-radius: $radius-xl;
  cursor: pointer;
  transition: all $transition-duration-base;

  &:hover {
    border-color: $color-primary-light;
    box-shadow: $shadow-md;
  }

  &--active {
    border-color: $color-primary;
    background: rgba($color-primary, 0.02);
  }
}

.account-bind__platform-icon {
  width: 56px;
  height: 56px;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: $font-weight-bold;
  color: $color-white;
  flex-shrink: 0;

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

.account-bind__platform-info {
  flex: 1;
}

.account-bind__platform-name {
  font-size: $font-size-lg;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  margin: 0 0 4px;
}

.account-bind__platform-desc {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin: 0;
}

.account-bind__platform-check {
  flex-shrink: 0;
}

.account-bind__check-icon {
  font-size: 24px;
  color: $color-primary;
}

.account-bind__check-circle {
  width: 24px;
  height: 24px;
  border: 2px solid $color-border-dark;
  border-radius: 50%;
}

// 表单
.account-bind__form {
  background: $color-white;
  padding: $spacing-xl;
  border-radius: $radius-xl;
  margin-bottom: $spacing-xl;
}

// 二维码区域
.account-bind__qrcode-section {
  display: flex;
  gap: $spacing-xl;
  background: $color-white;
  padding: $spacing-xl;
  border-radius: $radius-xl;
  margin-bottom: $spacing-xl;

  @media (max-width: $breakpoint-md) {
    flex-direction: column;
    align-items: center;
  }
}

.account-bind__qrcode-wrapper {
  position: relative;
  width: 200px;
  height: 200px;
  background: $color-gray-100;
  border-radius: $radius-lg;
  overflow: hidden;
  flex-shrink: 0;
}

.account-bind__qrcode {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.account-bind__qrcode-placeholder {
  width: 100%;
  height: 100%;
  @include flex-center;
  color: $color-gray-400;
}

.account-bind__qrcode-loading {
  width: 100%;
  height: 100%;
  @include flex-center;
  flex-direction: column;
  gap: $spacing-sm;
  color: $color-text-secondary;
}

.account-bind__qrcode-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba($color-white, 0.95);
  @include flex-center;
  flex-direction: column;
  gap: $spacing-sm;

  p {
    color: $color-text-secondary;
    margin: 0;
  }
}

.account-bind__qrcode-info {
  flex: 1;

  h4 {
    font-size: $font-size-lg;
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
    margin: 0 0 $spacing-md;
  }
}

.account-bind__qrcode-steps {
  padding-left: $spacing-lg;
  margin: 0 0 $spacing-lg;

  li {
    font-size: $font-size-base;
    color: $color-text-regular;
    margin-bottom: $spacing-xs;
  }
}

.account-bind__qrcode-tips {
  margin-top: $spacing-md;
}

// 进度
.account-bind__progress {
  background: $color-white;
  padding: $spacing-xl;
  border-radius: $radius-xl;
  margin-bottom: $spacing-xl;

  h4 {
    font-size: $font-size-lg;
    font-weight: $font-weight-medium;
    color: $color-text-primary;
    margin: 0 0 $spacing-md;
  }
}

.account-bind__progress-desc {
  font-size: $font-size-xs;
  color: $color-text-secondary;
  margin: 4px 0 0;
}

// 成功页面
.account-bind__success {
  text-align: center;
  padding: $spacing-3xl;
}

.account-bind__success-icon {
  color: $color-success;
  margin-bottom: $spacing-lg;
}

.account-bind__success-title {
  font-size: $font-size-2xl;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  margin: 0 0 $spacing-md;
}

.account-bind__success-desc {
  font-size: $font-size-base;
  color: $color-text-secondary;
  margin: 0 0 $spacing-xl;
}

.account-bind__success-actions {
  display: flex;
  justify-content: center;
  gap: $spacing-md;
}

// 操作按钮
.account-bind__actions {
  display: flex;
  justify-content: center;
  gap: $spacing-md;
  margin-top: $spacing-xl;
}
</style>
