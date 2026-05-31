<template>
  <bx-content
    title="个人设置"
    description="管理您的个人信息和账号设置"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <div class="settings-profile">
      <el-row :gutter="24">
        <!-- 左侧：个人信息卡片 -->
        <el-col :xs="24" :sm="24" :md="8" :lg="7" :xl="6">
          <el-card class="settings-profile__card settings-profile__profile-card" shadow="hover">
            <div class="settings-profile__avatar-section">
              <div class="settings-profile__avatar-wrapper">
                <el-avatar :size="100" :src="userInfo.avatar" class="settings-profile__avatar">
                  {{ userInfo.realName?.charAt(0) || userInfo.phone?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="settings-profile__avatar-overlay" @click="handleAvatarClick">
                  <i class="el-icon-camera" />
                  <span>更换头像</span>
                </div>
              </div>
              <input
                ref="avatarInput"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handleAvatarChange"
              />
              <h3 class="settings-profile__user-name">{{ userInfo.realName || '未设置昵称' }}</h3>
              <p class="settings-profile__user-phone">{{ maskPhone(userInfo.phone) }}</p>
            </div>

            <el-divider />

            <div class="settings-profile__stats">
              <div class="settings-profile__stat-item">
                <span class="settings-profile__stat-value">{{ userStats.contentCount || 0 }}</span>
                <span class="settings-profile__stat-label">发布内容</span>
              </div>
              <div class="settings-profile__stat-item">
                <span class="settings-profile__stat-value">{{ userStats.leadCount || 0 }}</span>
                <span class="settings-profile__stat-label">获客数量</span>
              </div>
              <div class="settings-profile__stat-item">
                <span class="settings-profile__stat-value">{{ userStats.joinDays || 0 }}</span>
                <span class="settings-profile__stat-label">加入天数</span>
              </div>
            </div>

            <el-divider />

            <div class="settings-profile__info-list">
              <div class="settings-profile__info-item">
                <i class="el-icon-user" />
                <span>{{ userInfo.roleName || '普通用户' }}</span>
              </div>
              <div class="settings-profile__info-item">
                <i class="el-icon-office-building" />
                <span>{{ userInfo.tenantName || '默认企业' }}</span>
              </div>
              <div class="settings-profile__info-item">
                <i class="el-icon-time" />
                <span>注册于 {{ userInfo.createTime || '-' }}</span>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：设置表单 -->
        <el-col :xs="24" :sm="24" :md="16" :lg="17" :xl="18">
          <!-- 基本信息 -->
          <el-card class="settings-profile__card" shadow="hover">
            <template #header>
              <div class="settings-profile__card-header">
                <i class="el-icon-user-solid" />
                <span>基本信息</span>
              </div>
            </template>

            <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-position="top"
              class="settings-profile__form"
            >
              <el-row :gutter="20">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" maxlength="20" show-word-limit>
                      <template #prefix>
                        <i class="el-icon-user" />
                      </template>
                    </el-input>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="性别" prop="gender">
                    <el-radio-group v-model="profileForm.gender">
                      <el-radio :label="0">保密</el-radio>
                      <el-radio :label="1">男</el-radio>
                      <el-radio :label="2">女</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="个人简介" prop="bio">
                <el-input
                  v-model="profileForm.bio"
                  type="textarea"
                  :rows="3"
                  placeholder="简单介绍一下自己..."
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">
                  保存基本信息
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 联系方式 -->
          <el-card class="settings-profile__card" shadow="hover">
            <template #header>
              <div class="settings-profile__card-header">
                <i class="el-icon-phone" />
                <span>联系方式</span>
              </div>
            </template>

            <!-- 手机号 -->
            <div class="settings-profile__contact-item">
              <div class="settings-profile__contact-info">
                <div class="settings-profile__contact-icon">
                  <i class="el-icon-mobile-phone" />
                </div>
                <div class="settings-profile__contact-detail">
                  <span class="settings-profile__contact-label">手机号</span>
                  <span class="settings-profile__contact-value">
                    {{ userInfo.phone ? maskPhone(userInfo.phone) : '未绑定' }}
                    <el-tag v-if="userInfo.phone" type="success" size="small" effect="plain">已绑定</el-tag>
                  </span>
                </div>
              </div>
              <el-button type="primary" link @click="handleBindPhone">
                {{ userInfo.phone ? '更换' : '绑定' }}
              </el-button>
            </div>

            <el-divider />

            <!-- 邮箱 -->
            <div class="settings-profile__contact-item">
              <div class="settings-profile__contact-info">
                <div class="settings-profile__contact-icon email">
                  <i class="el-icon-message" />
                </div>
                <div class="settings-profile__contact-detail">
                  <span class="settings-profile__contact-label">邮箱</span>
                  <span class="settings-profile__contact-value">
                    {{ userInfo.email || '未绑定' }}
                    <el-tag v-if="userInfo.email" type="success" size="small" effect="plain">已绑定</el-tag>
                    <el-tag v-else type="info" size="small" effect="plain">未绑定</el-tag>
                  </span>
                </div>
              </div>
              <el-button type="primary" link @click="handleBindEmail">
                {{ userInfo.email ? '更换' : '绑定' }}
              </el-button>
            </div>
          </el-card>

          <!-- 账号信息 -->
          <el-card class="settings-profile__card" shadow="hover">
            <template #header>
              <div class="settings-profile__card-header">
                <i class="el-icon-info-filled" />
                <span>账号信息</span>
              </div>
            </template>

            <div class="settings-profile__account-info">
              <div class="settings-profile__info-row">
                <span class="settings-profile__info-label">用户ID</span>
                <span class="settings-profile__info-value">{{ userInfo.id || '-' }}</span>
                <el-button type="primary" link size="small" @click="handleCopyId">
                  <i class="el-icon-copy-document" /> 复制
                </el-button>
              </div>
              <div class="settings-profile__info-row">
                <span class="settings-profile__info-label">注册时间</span>
                <span class="settings-profile__info-value">{{ userInfo.createTime || '-' }}</span>
              </div>
              <div class="settings-profile__info-row">
                <span class="settings-profile__info-label">最后登录</span>
                <span class="settings-profile__info-value">{{ userInfo.lastLoginTime || '-' }}</span>
              </div>
              <div class="settings-profile__info-row">
                <span class="settings-profile__info-label">登录IP</span>
                <span class="settings-profile__info-value">{{ userInfo.lastLoginIp || '-' }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 绑定手机对话框 -->
    <el-dialog v-model="phoneDialogVisible" title="绑定手机号" width="400px">
      <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-position="top">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="phoneForm.phone" placeholder="请输入手机号" maxlength="11">
            <template #prefix>
              <i class="el-icon-mobile-phone" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="settings-profile__verify-code">
            <el-input v-model="phoneForm.code" placeholder="请输入验证码" maxlength="6">
              <template #prefix>
                <i class="el-icon-key" />
              </template>
            </el-input>
            <el-button
              :disabled="phoneCodeCountdown > 0"
              @click="handleSendPhoneCode"
            >
              {{ phoneCodeCountdown > 0 ? `${phoneCodeCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="phoneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindingPhone" @click="handleConfirmBindPhone">确认绑定</el-button>
      </template>
    </el-dialog>

    <!-- 绑定邮箱对话框 -->
    <el-dialog v-model="emailDialogVisible" title="绑定邮箱" width="400px">
      <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-position="top">
        <el-form-item label="邮箱地址" prop="email">
          <el-input v-model="emailForm.email" placeholder="请输入邮箱地址">
            <template #prefix>
              <i class="el-icon-message" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="settings-profile__verify-code">
            <el-input v-model="emailForm.code" placeholder="请输入验证码" maxlength="6">
              <template #prefix>
                <i class="el-icon-key" />
              </template>
            </el-input>
            <el-button
              :disabled="emailCodeCountdown > 0"
              @click="handleSendEmailCode"
            >
              {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emailDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindingEmail" @click="handleConfirmBindEmail">确认绑定</el-button>
      </template>
    </el-dialog>

    <!-- 头像裁剪对话框 -->
    <el-dialog v-model="avatarDialogVisible" title="裁剪头像" width="400px">
      <div class="settings-profile__avatar-crop">
        <img v-if="avatarPreview" :src="avatarPreview" alt="" />
      </div>
      <template #footer>
        <el-button @click="avatarDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadingAvatar" @click="handleConfirmAvatar">确认上传</el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview 个人设置页
 * @description 管理用户个人信息、头像、手机号、邮箱等
 * @author EMP-FE-004 赵云
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user.js'
import {
  getUserProfile,
  updateUserProfile,
  uploadAvatar,
  bindPhone,
  bindEmail,
  sendBindCode,
} from '@/api/settings.js'

// ============================================================
// Store
// ============================================================
const userStore = useUserStore()

// ============================================================
// 数据
// ============================================================

/** 用户信息 */
const userInfo = reactive({
  id: '',
  realName: '',
  phone: '',
  email: '',
  avatar: '',
  gender: 0,
  bio: '',
  roleName: '',
  tenantName: '',
  createTime: '',
  lastLoginTime: '',
  lastLoginIp: '',
})

/** 用户统计 */
const userStats = reactive({
  contentCount: 0,
  leadCount: 0,
  joinDays: 0,
})

/** 头像输入框引用 */
const avatarInput = ref(null)

/** 个人资料表单 */
const profileForm = reactive({
  realName: '',
  gender: 0,
  bio: '',
})

/** 个人资料表单引用 */
const profileFormRef = ref(null)

/** 表单验证规则 */
const profileRules = {
  realName: [
    { min: 2, max: 20, message: '姓名长度为2-20个字符', trigger: 'blur' },
  ],
  bio: [
    { max: 200, message: '个人简介最多200个字符', trigger: 'blur' },
  ],
}

/** 保存状态 */
const savingProfile = ref(false)

/** 手机绑定对话框 */
const phoneDialogVisible = ref(false)
const phoneForm = reactive({
  phone: '',
  code: '',
})
const phoneFormRef = ref(null)
const phoneRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
}
const phoneCodeCountdown = ref(0)
const bindingPhone = ref(false)

/** 邮箱绑定对话框 */
const emailDialogVisible = ref(false)
const emailForm = reactive({
  email: '',
  code: '',
})
const emailFormRef = ref(null)
const emailRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
}
const emailCodeCountdown = ref(0)
const bindingEmail = ref(false)

/** 头像对话框 */
const avatarDialogVisible = ref(false)
const avatarPreview = ref('')
const uploadingAvatar = ref(false)
let avatarFile = null

// ============================================================
// 方法
// ============================================================

/**
 * 加载用户信息
 */
async function loadUserInfo() {
  try {
    const res = await getUserProfile()
    Object.assign(userInfo, res)
    Object.assign(profileForm, {
      realName: res.realName || '',
      gender: res.gender || 0,
      bio: res.bio || '',
    })
    // 加载用户统计数据（可扩展）
    userStats.joinDays = Math.floor((Date.now() - new Date(res.createTime).getTime()) / (1000 * 60 * 60 * 24))
  } catch (error) {
    console.error('加载用户信息失败:', error)
    ElMessage.error('加载用户信息失败')
  }
}

/**
 * 手机号脱敏
 */
function maskPhone(phone) {
  if (!phone) return '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/**
 * 保存个人资料
 */
async function handleSaveProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  savingProfile.value = true
  try {
    await updateUserProfile({
      realName: profileForm.realName,
      gender: profileForm.gender,
      bio: profileForm.bio,
    })
    ElMessage.success('保存成功')
    Object.assign(userInfo, profileForm)
    userStore.updateUserInfo(profileForm)
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    savingProfile.value = false
  }
}

/**
 * 点击头像
 */
function handleAvatarClick() {
  avatarInput.value.click()
}

/**
 * 头像文件变化
 */
function handleAvatarChange(e) {
  const file = e.target.files[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }

  // 验证文件大小（2MB）
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return
  }

  avatarFile = file
  avatarPreview.value = URL.createObjectURL(file)
  avatarDialogVisible.value = true
}

/**
 * 确认上传头像
 */
async function handleConfirmAvatar() {
  if (!avatarFile) return

  uploadingAvatar.value = true
  try {
    const formData = new FormData()
    formData.append('avatar', avatarFile)

    const res = await uploadAvatar(formData, (percent) => {
      console.log('上传进度:', percent)
    })

    userInfo.avatar = res.url
    userStore.updateUserInfo({ avatar: res.url })
    ElMessage.success('头像上传成功')
    avatarDialogVisible.value = false
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('头像上传失败')
  } finally {
    uploadingAvatar.value = false
    avatarFile = null
    avatarInput.value.value = ''
  }
}

/**
 * 打开绑定手机对话框
 */
function handleBindPhone() {
  phoneForm.phone = ''
  phoneForm.code = ''
  phoneCodeCountdown.value = 0
  phoneDialogVisible.value = true
}

/**
 * 发送手机验证码
 */
async function handleSendPhoneCode() {
  const valid = await phoneFormRef.value.validateField('phone').catch(() => false)
  if (!valid) return

  try {
    await sendBindCode({ type: 'phone', target: phoneForm.phone })
    ElMessage.success('验证码已发送')
    startPhoneCountdown()
  } catch (error) {
    console.error('发送失败:', error)
    ElMessage.error('验证码发送失败')
  }
}

/**
 * 手机验证码倒计时
 */
function startPhoneCountdown() {
  phoneCodeCountdown.value = 60
  const timer = setInterval(() => {
    phoneCodeCountdown.value--
    if (phoneCodeCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

/**
 * 确认绑定手机
 */
async function handleConfirmBindPhone() {
  const valid = await phoneFormRef.value.validate().catch(() => false)
  if (!valid) return

  bindingPhone.value = true
  try {
    await bindPhone({
      phone: phoneForm.phone,
      verifyCode: phoneForm.code,
    })
    ElMessage.success('手机号绑定成功')
    userInfo.phone = phoneForm.phone
    phoneDialogVisible.value = false
  } catch (error) {
    console.error('绑定失败:', error)
    ElMessage.error('绑定失败，请检查验证码')
  } finally {
    bindingPhone.value = false
  }
}

/**
 * 打开绑定邮箱对话框
 */
function handleBindEmail() {
  emailForm.email = ''
  emailForm.code = ''
  emailCodeCountdown.value = 0
  emailDialogVisible.value = true
}

/**
 * 发送邮箱验证码
 */
async function handleSendEmailCode() {
  const valid = await emailFormRef.value.validateField('email').catch(() => false)
  if (!valid) return

  try {
    await sendBindCode({ type: 'email', target: emailForm.email })
    ElMessage.success('验证码已发送')
    startEmailCountdown()
  } catch (error) {
    console.error('发送失败:', error)
    ElMessage.error('验证码发送失败')
  }
}

/**
 * 邮箱验证码倒计时
 */
function startEmailCountdown() {
  emailCodeCountdown.value = 60
  const timer = setInterval(() => {
    emailCodeCountdown.value--
    if (emailCodeCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

/**
 * 确认绑定邮箱
 */
async function handleConfirmBindEmail() {
  const valid = await emailFormRef.value.validate().catch(() => false)
  if (!valid) return

  bindingEmail.value = true
  try {
    await bindEmail({
      email: emailForm.email,
      verifyCode: emailForm.code,
    })
    ElMessage.success('邮箱绑定成功')
    userInfo.email = emailForm.email
    emailDialogVisible.value = false
  } catch (error) {
    console.error('绑定失败:', error)
    ElMessage.error('绑定失败，请检查验证码')
  } finally {
    bindingEmail.value = false
  }
}

/**
 * 复制用户ID
 */
function handleCopyId() {
  navigator.clipboard.writeText(userInfo.id).then(() => {
    ElMessage.success('已复制用户ID')
  })
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadUserInfo()
})
</script>

<style lang="scss" scoped>
.settings-profile {
  &__card {
    margin-bottom: 20px;

    :deep(.el-card__header) {
      padding: 16px 20px;
      background: var(--bg-secondary);
    }
  }

  &__profile-card {
    :deep(.el-card__body) {
      padding: 24px;
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
  }

  &__avatar-section {
    text-align: center;
  }

  &__avatar-wrapper {
    position: relative;
    display: inline-block;
    cursor: pointer;

    &:hover .settings-profile__avatar-overlay {
      opacity: 1;
    }
  }

  &__avatar {
    border: 3px solid var(--border-color);
  }

  &__avatar-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
    opacity: 0;
    transition: opacity 0.3s;
    color: #fff;

    i {
      font-size: 24px;
      margin-bottom: 4px;
    }

    span {
      font-size: 12px;
    }
  }

  &__user-name {
    margin: 16px 0 4px;
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
  }

  &__user-phone {
    margin: 0;
    font-size: 14px;
    color: var(--text-secondary);
  }

  &__stats {
    display: flex;
    justify-content: space-around;
    padding: 8px 0;
  }

  &__stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__stat-value {
    font-size: 20px;
    font-weight: 600;
    color: var(--primary-color);
  }

  &__stat-label {
    margin-top: 4px;
    font-size: 12px;
    color: var(--text-secondary);
  }

  &__info-list {
    padding: 8px 0;
  }

  &__info-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;
    font-size: 14px;
    color: var(--text-primary);

    i {
      color: var(--text-secondary);
      font-size: 16px;
    }
  }

  &__form {
    :deep(.el-form-item__label) {
      font-weight: 500;
    }
  }

  &__contact-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
  }

  &__contact-info {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__contact-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;

    i {
      font-size: 24px;
      color: #fff;
    }

    &.email {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    }
  }

  &__contact-detail {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__contact-label {
    font-size: 12px;
    color: var(--text-secondary);
  }

  &__contact-value {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
  }

  &__account-info {
    padding: 8px 0;
  }

  &__info-row {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid var(--border-color);

    &:last-child {
      border-bottom: none;
    }
  }

  &__info-label {
    width: 100px;
    font-size: 14px;
    color: var(--text-secondary);
  }

  &__info-value {
    flex: 1;
    font-size: 14px;
    color: var(--text-primary);
  }

  &__verify-code {
    display: flex;
    gap: 12px;

    .el-input {
      flex: 1;
    }

    .el-button {
      width: 120px;
    }
  }

  &__avatar-crop {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 20px;

    img {
      max-width: 100%;
      max-height: 300px;
      border-radius: 8px;
    }
  }
}

@media (max-width: 768px) {
  .settings-profile {
    &__stats {
      flex-direction: column;
      gap: 16px;
    }

    &__contact-info {
      flex: 1;
    }

    &__contact-detail {
      flex: 1;
    }

    &__info-row {
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
    }

    &__info-label {
      width: auto;
    }
  }
}
</style>
