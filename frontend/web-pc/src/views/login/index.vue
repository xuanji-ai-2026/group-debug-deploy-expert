<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-page__bg">
      <div class="login-page__particles">
        <span v-for="i in 20" :key="i" class="login-page__particle" :style="getParticleStyle(i)" />
      </div>
    </div>

    <!-- 登录容器 -->
    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="login-container__left">
        <div class="login-brand">
          <!-- Logo -->
          <div class="login-brand__logo">
            <img src="/logo.png" alt="北极星AI" class="login-brand__icon" @error="handleLogoError" />
          </div>
          <!-- 标题 -->
          <h1 class="login-brand__title">北极星AI商机获客系统</h1>
          <p class="login-brand__subtitle">
            智能社媒运营 · 精准商机获客 · 全方位风控保护
          </p>

          <!-- 功能特点 -->
          <div class="login-brand__features">
            <div class="login-brand__feature">
              <i class="el-icon-magic-stick" />
              <span>AI智能生成内容</span>
            </div>
            <div class="login-brand__feature">
              <i class="el-icon-connection" />
              <span>多平台账号管理</span>
            </div>
            <div class="login-brand__feature">
              <i class="el-icon-shield" />
              <span>智能风控防封</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-container__right">
        <div class="login-form-wrapper">
          <h2 class="login-form__title">欢迎登录</h2>
          <p class="login-form__subtitle">
            请输入您的账号信息
          </p>

          <!-- 登录表单 -->
          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            :label-position="'top'"
            class="login-form"
            @submit.prevent="handleSubmit"
          >
            <!-- 登录类型切换 -->
            <div class="login-form__tabs">
              <span
                v-for="tab in loginTabs"
                :key="tab.value"
                :class="['login-form__tab', { 'is-active': formData.loginType === tab.value }]"
                @click="switchLoginType(tab.value)"
              >
                {{ tab.label }}
              </span>
            </div>

            <!-- 手机号 -->
            <el-form-item v-if="formData.loginType !== 'email'" prop="phone" label="手机号">
              <el-input
                v-model="formData.phone"
                :prefix-icon="'el-icon-mobile'"
                :placeholder="'请输入手机号'"
                :maxlength="11"
                size="large"
              />
            </el-form-item>

            <!-- 邮箱（邮箱登录时显示） -->
            <el-form-item
              v-if="formData.loginType === 'email'"
              prop="email"
              label="邮箱地址"
            >
              <el-input
                v-model="formData.email"
                :prefix-icon="'el-icon-message'"
                :placeholder="'请输入邮箱地址'"
                size="large"
              />
            </el-form-item>

            <!-- 密码登录 - 密码 -->
            <el-form-item
              v-if="formData.loginType === 'password'"
              prop="password"
              label="密码"
            >
              <el-input
                v-model="formData.password"
                :prefix-icon="'el-icon-lock'"
                :placeholder="'请输入密码'"
                :type="showPassword ? 'text' : 'password'"
                size="large"
              >
                <template #suffix>
                  <i
                    :class="showPassword ? 'el-icon-view' : 'el-icon-hide'"
                    class="login-form__password-toggle"
                    @click="showPassword = !showPassword"
                  />
                </template>
              </el-input>
            </el-form-item>

            <!-- 验证码登录 - 验证码 -->
            <el-form-item
              v-if="formData.loginType === 'phone' || formData.loginType === 'email'"
              prop="verifyCode"
              label="验证码"
            >
              <el-input
                v-model="formData.verifyCode"
                :prefix-icon="'el-icon-key'"
                :placeholder="'请输入验证码'"
                :maxlength="6"
                size="large"
              >
                <template #append>
                  <el-button
                    :disabled="countdown > 0"
                    :loading="sendingCode"
                    class="login-form__code-btn"
                    @click="handleSendCode"
                  >
                    {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>

            <!-- 新用户设置密码 -->
            <el-form-item
              v-if="showPasswordSetup"
              prop="newPassword"
              label="设置密码"
            >
              <el-input
                v-model="formData.newPassword"
                :prefix-icon="'el-icon-lock'"
                :placeholder="'请设置密码（6-20位）'"
                :type="showNewPassword ? 'text' : 'password'"
                size="large"
              >
                <template #suffix>
                  <i
                    :class="showNewPassword ? 'el-icon-view' : 'el-icon-hide'"
                    class="login-form__password-toggle"
                    @click="showNewPassword = !showNewPassword"
                  />
                </template>
              </el-input>
            </el-form-item>

            <!-- 记住我 & 忘记密码 -->
            <div v-if="formData.loginType === 'password'" class="login-form__options">
              <el-checkbox v-model="formData.remember">
                记住我
              </el-checkbox>
              <span class="login-form__forgot">
                <span class="login-form__link" @click="handleForgotPassword">忘记密码？</span>
              </span>
            </div>

            <!-- 提交按钮 -->
            <el-form-item>
              <el-button
                type="primary"
                native-type="submit"
                :loading="authLoading"
                :disabled="authLoading"
                size="large"
                class="login-form__submit"
                @click="handleSubmit"
              >
                {{ showPasswordSetup ? '注册并登录' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 版本信息 -->
          <div class="login-form__version">
            <span>版本 v1.0.4</span>
            <span class="login-form__version-tip">修复登录问题 · 添加邮箱登录</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * @fileoverview Login 登录页面
 * @description 用户登录/注册页面，支持手机号+验证码、密码、邮箱登录
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '@/composables/useAuth.js'

// ============================================================
// Router & Composables
// ============================================================
const router = useRouter()
const route = useRoute()
const {
  login,
  sendSmsCode,
  sendEmailCode,
  registerAndLogin,
  loading: authLoading
} = useAuth()

// ============================================================
// 状态
// ============================================================
const formRef = ref(null)
const sendingCode = ref(false)
const countdown = ref(0)
const showPassword = ref(false)
const showNewPassword = ref(false)
const showPasswordSetup = ref(false)
const pendingRegisterInfo = ref(null)

// 登录类型选项
const loginTabs = [
  { label: '验证码登录', value: 'phone' },
  { label: '密码登录', value: 'password' },
  { label: '邮箱登录', value: 'email' },
]

// 表单数据
const formData = reactive({
  loginType: 'phone',
  phone: '',
  password: '',
  verifyCode: '',
  email: '',
  newPassword: '',
  remember: false,
})

// 表单验证规则
const formRules = computed(() => {
  const rules = {}

  if (formData.loginType !== 'email') {
    rules.phone = [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      {
        pattern: /^1[3-9]\d{9}$/,
        message: '手机号格式不正确',
        trigger: 'blur',
      },
    ]
  }

  if (formData.loginType === 'password') {
    rules.password = [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
    ]
  }

  if (formData.loginType === 'phone' || formData.loginType === 'email') {
    rules.verifyCode = [
      { required: true, message: '请输入验证码', trigger: 'blur' },
      { len: 6, message: '验证码为 6 位', trigger: 'blur' },
    ]
  }

  if (formData.loginType === 'email') {
    rules.email = [
      { required: true, message: '请输入邮箱地址', trigger: 'blur' },
      {
        pattern: /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
        message: '邮箱格式不正确',
        trigger: 'blur',
      },
    ]
  }

  if (showPasswordSetup.value) {
    rules.newPassword = [
      { required: true, message: '请设置密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
    ]
  }

  return rules
})

// ============================================================
// 方法
// ============================================================

/**
 * 切换登录类型
 */
function switchLoginType(type) {
  formData.loginType = type
  showPasswordSetup.value = false
  pendingRegisterInfo.value = null

  // 重置输入
  formData.verifyCode = ''
  formData.password = ''
  formData.newPassword = ''
}

/**
 * 发送验证码
 */
async function handleSendCode() {
  if (formData.loginType === 'email') {
    // 邮箱验证码
    if (!formData.email || !/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(formData.email)) {
      ElMessage.warning('请输入正确的邮箱地址')
      return
    }

    sendingCode.value = true
    try {
      await sendEmailCode({ email: formData.email })

      startCountdown()
    } catch (error) {
      console.error('[Login] Send email code failed:', error)
    } finally {
      sendingCode.value = false
    }
  } else {
    // 手机号验证码
    if (!formData.phone || !/^1[3-9]\d{9}$/.test(formData.phone)) {
      ElMessage.warning('请输入正确的手机号')
      return
    }

    sendingCode.value = true
    try {
      await sendSmsCode({ phone: formData.phone })

      startCountdown()
    } catch (error) {
      console.error('[Login] Send code failed:', error)
    } finally {
      sendingCode.value = false
    }
  }
}

/**
 * 开始倒计时
 */
function startCountdown() {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

/**
 * 提交表单
 */
async function handleSubmit() {
  if (!formRef.value) return

  // 如果是设置密码阶段
  if (showPasswordSetup.value && pendingRegisterInfo.value) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    try {
      const success = await registerAndLogin({
        phone: pendingRegisterInfo.value.phone,
        email: pendingRegisterInfo.value.email,
        code: formData.verifyCode,
        password: formData.newPassword,
      })

      if (success) {
        ElMessage.success('注册成功，已自动登录')
        const redirect = route.query.redirect || '/'
        await router.push(redirect)
      }
    } catch (error) {
      console.error('[Login] Register failed:', error)
    }
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const result = await login({
      loginType: formData.loginType,
      phone: formData.loginType !== 'email' ? formData.phone : undefined,
      email: formData.loginType === 'email' ? formData.email : undefined,
      verifyCode: (formData.loginType === 'phone' || formData.loginType === 'email') ? formData.verifyCode : undefined,
      password: formData.loginType === 'password' ? formData.password : undefined,
    })

    if (result?.success) {
      const redirect = route.query.redirect || '/'
      await router.push(redirect)
    }
  } catch (error) {
    // 检查是否是新用户需要设置密码
    if (error?.message === 'NEW_USER_NEED_PASSWORD' || error?.code === 10001) {
      showPasswordSetup.value = true
      pendingRegisterInfo.value = {
        phone: formData.phone,
        email: formData.email,
        code: formData.verifyCode,
      }
      ElMessage.warning('新用户，请设置密码完成注册')
      return
    }

    console.error('[Login] Submit failed:', error)
    ElMessage.error(error?.message || '登录失败')
  }
}

/**
 * 忘记密码
 */
function handleForgotPassword() {
  ElMessage.info('忘记密码功能开发中')
}

/**
 * Logo加载失败处理
 */
function handleLogoError(event) {
  const target = event.target
  target.outerHTML = `
    <svg viewBox="0 0 64 64" class="login-brand__icon">
      <circle cx="32" cy="32" r="30" fill="#4F46E5" />
      <polygon
        points="32,8 38,24 56,24 42,34 48,52 32,42 16,52 22,34 8,24 26,24"
        fill="#FBBF24"
      />
      <circle cx="32" cy="32" r="8" fill="#FFFFFF" />
    </svg>
  `
}

/**
 * 获取粒子样式
 */
function getParticleStyle(index) {
  const size = Math.random() * 6 + 2
  const left = Math.random() * 100
  const delay = Math.random() * 10
  const duration = Math.random() * 20 + 10

  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
  }
}
</script>

<style lang="scss" scoped>
// 变量定义
$color-white: #ffffff;
$color-primary: #667eea;
$color-text-primary: #1e293b;
$color-text-secondary: #64748b;
$color-border-light: #e2e8f0;
$color-bg-hover: #f1f5f9;
$radius-lg: 12px;
$radius-xl: 16px;
$radius-2xl: 24px;
$shadow-2xl: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
$spacing-xs: 4px;
$spacing-sm: 8px;
$spacing-base: 12px;
$spacing-md: 16px;
$spacing-lg: 24px;
$spacing-xl: 32px;
$spacing-2xl: 48px;
$spacing-3xl: 64px;
$font-size-xs: 12px;
$font-size-sm: 14px;
$font-size-base: 16px;
$font-size-xl: 20px;
$font-size-2xl: 24px;
$font-size-3xl: 30px;
$font-weight-medium: 500;
$font-weight-bold: 700;
$line-height-base: 1.6;
$transition-duration-fast: 0.2s;
$breakpoint-lg: 1024px;
$breakpoint-md: 768px;

// 登录页面容器
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;

  // 背景装饰
  &__bg {
    position: absolute;
    inset: 0;
    overflow: hidden;
  }

  &__particles {
    position: absolute;
    inset: 0;
  }

  &__particle {
    position: absolute;
    top: -10px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.3);
    animation: float linear infinite;
  }

  @keyframes float {
    0% {
      transform: translateY(0) rotate(0deg);
      opacity: 0;
    }
    10% {
      opacity: 1;
    }
    90% {
      opacity: 1;
    }
    100% {
      transform: translateY(100vh) rotate(720deg);
      opacity: 0;
    }
  }
}

// 登录容器
.login-container {
  position: relative;
  display: flex;
  width: 900px;
  min-height: 560px;
  background: $color-white;
  border-radius: $radius-2xl;
  box-shadow: $shadow-2xl;
  overflow: hidden;
  z-index: 1;

  @media (max-width: #{$breakpoint-md}) {
    width: 100%;
    min-height: 100vh;
    border-radius: 0;
  }

  // 左侧品牌区
  &__left {
    flex: 1;
    display: none;
    padding: $spacing-3xl;
    background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
    color: $color-white;

    @media (min-width: #{$breakpoint-lg}) {
      display: flex;
      align-items: center;
    }
  }

  // 右侧表单区
  &__right {
    width: 450px;
    padding: $spacing-3xl;
    display: flex;
    align-items: center;
    justify-content: center;

    @media (max-width: #{$breakpoint-md}) {
      width: 100%;
      padding: $spacing-xl;
    }
  }
}

// 品牌区
.login-brand {
  &__logo {
    margin-bottom: $spacing-xl;

    .login-brand__icon {
      width: 80px;
      height: 80px;
    }
  }

  &__title {
    margin: 0 0 $spacing-sm;
    font-size: $font-size-3xl;
    font-weight: $font-weight-bold;
  }

  &__subtitle {
    margin: 0 0 $spacing-2xl;
    font-size: $font-size-base;
    opacity: 0.8;
    line-height: $line-height-base;
  }

  &__features {
    display: flex;
    flex-direction: column;
    gap: $spacing-lg;
  }

  &__feature {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    font-size: $font-size-base;

    i {
      font-size: 24px;
    }
  }
}

// 登录表单
.login-form-wrapper {
  width: 100%;
  max-width: 340px;
}

.login-form {
  &__title {
    margin: 0 0 $spacing-xs;
    font-size: $font-size-2xl;
    font-weight: $font-weight-bold;
    color: $color-text-primary;
  }

  &__subtitle {
    margin: 0 0 $spacing-xl;
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  // 登录类型切换
  &__tabs {
    display: flex;
    gap: $spacing-lg;
    margin-bottom: $spacing-lg;
    padding-bottom: $spacing-md;
    border-bottom: 1px solid $color-border-light;
  }

  &__tab {
    padding-bottom: $spacing-sm;
    font-size: $font-size-sm;
    color: $color-text-secondary;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    transition: all $transition-duration-fast;

    &.is-active {
      color: $color-primary;
      border-bottom-color: $color-primary;
      font-weight: $font-weight-medium;
    }
  }

  // 密码切换按钮
  &__password-toggle {
    cursor: pointer;
    color: $color-text-secondary;

    &:hover {
      color: $color-primary;
    }
  }

  // 验证码按钮
  &__code-btn {
    min-width: 100px;
    font-size: $font-size-sm;
  }

  // 选项
  &__options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;
  }

  &__forgot {
    font-size: $font-size-sm;
  }

  &__link {
    color: $color-primary;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }

  // 提交按钮
  &__submit {
    width: 100%;
    height: 44px;
    font-size: $font-size-base;
  }

  // 版本信息
  &__version {
    margin-top: $spacing-xl;
    text-align: center;
    font-size: $font-size-xs;
    color: $color-text-secondary;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__version-tip {
    color: $color-primary;
  }
}
</style>
