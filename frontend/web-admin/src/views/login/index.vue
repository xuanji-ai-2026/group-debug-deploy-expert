<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-circle circle-1" />
      <div class="bg-circle circle-2" />
      <div class="bg-circle circle-3" />
    </div>

    <div class="login-card">
      <div class="card-header">
        <div class="logo-area">
          <img src="@/assets/images/logo.png" alt="logo" class="logo" @error="handleImageError" />
          <h1 class="title">北极星AI管理后台</h1>
        </div>
        <p class="subtitle">Beijixing AI Admin System</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入管理员手机号"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入登录密码"
            size="large"
            prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="form.remember">记住登录状态</el-checkbox>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '立即登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        <span class="tips">仅限管理员访问</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/store/modules/admin'
import { request } from '@/utils/request'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  phone: '',
  password: '',
  remember: false
})

const rules = {
  phone: [
    { required: true, message: '请输入管理员手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度在 6 到 32 个字符', trigger: 'blur' }
  ]
}

async function handleLogin() {
  try {
    await formRef.value?.validate()

    loading.value = true

    const resData = await request.post('/api/auth/login', {
      loginType: 'password',
      phone: form.phone,
      password: form.password
    })

    const loginData = resData.data || resData
    const accessToken = loginData.token
    const refreshToken = loginData.refresh_token
    const user = loginData.user

    if (!accessToken) {
      ElMessage.error('登录失败：未获取到Token')
      return
    }

    const adminInfo = {
      id: user?.id || user?.userId || '',
      username: user?.phone || form.phone,
      nickname: user?.nickname || user?.realName || '管理员',
      role: user?.roleType === 1 ? 'super_admin' : 'ops_admin',
      avatar: user?.avatar || '',
      phone: user?.phone || form.phone
    }

    adminStore.login({
      token: accessToken,
      refreshToken: refreshToken,
      adminInfo: adminInfo
    })

    ElMessage.success('登录成功，欢迎回来！')

    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch (error) {
    console.error('Login error:', error)
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    loading.value = false
  }
}

function handleImageError(e) {
  e.target.style.display = 'none'
}
</script>

<style lang="scss" scoped>
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f2027, #203a43, #2c5364);
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
}

.circle-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  right: -100px;
}

.circle-2 {
  width: 400px;
  height: 400px;
  bottom: -100px;
  left: -100px;
}

.circle-3 {
  width: 300px;
  height: 300px;
  top: 50%;
  left: 30%;
  background: rgba(26, 115, 232, 0.08);
}

.login-card {
  position: relative;
  z-index: 10;
  width: 420px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.logo {
  width: 40px;
  height: 40px;
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.subtitle {
  font-size: 12px;
  color: #666;
  letter-spacing: 1px;
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
}

.form-options {
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
}

.card-footer {
  text-align: center;
  margin-top: 16px;

  .tips {
    font-size: 12px;
    color: #c0c4cc;
  }
}
</style>
