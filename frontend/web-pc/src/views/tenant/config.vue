<template>
  <bx-content
    title="租户配置"
    description="配置系统参数和功能开关"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <el-row :gutter="20">
      <!-- 左侧配置菜单 -->
      <el-col :xs="24" :sm="8" :lg="6">
        <el-card class="tenant-config__menu">
          <div class="tenant-config__menu-title">配置分类</div>
          <el-menu
            :default-active="activeConfig"
            class="tenant-config__menu-list"
            @select="handleMenuSelect"
          >
            <el-menu-item index="general">
              <i class="el-icon-setting" />
              <span>基础配置</span>
            </el-menu-item>
            <el-menu-item index="lead">
              <i class="el-icon-data-analysis" />
              <span>商机配置</span>
            </el-menu-item>
            <el-menu-item index="content">
              <i class="el-icon-document" />
              <span>内容配置</span>
            </el-menu-item>
            <el-menu-item index="notification">
              <i class="el-icon-bell" />
              <span>通知配置</span>
            </el-menu-item>
            <el-menu-item index="security">
              <i class="el-icon-lock" />
              <span>安全配置</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      
      <!-- 右侧配置表单 -->
      <el-col :xs="24" :sm="16" :lg="18">
        <el-card v-loading="loading" class="tenant-config__content">
          <template #header>
            <div class="tenant-config__header">
              <span class="tenant-config__title">{{ configTitle }}</span>
              <el-button type="primary" :loading="saveLoading" @click="handleSave">
                <i class="el-icon-check" />
                保存配置
              </el-button>
            </div>
          </template>
          
          <!-- 基础配置 -->
          <div v-show="activeConfig === 'general'" class="tenant-config__form">
            <el-form label-width="160px">
              <el-form-item label="系统名称">
                <el-input v-model="config.general.systemName" placeholder="请输入系统显示名称" />
              </el-form-item>
              <el-form-item label="系统Logo">
                <el-upload
                  class="tenant-config__logo-uploader"
                  action="/api/v1/upload"
                  :show-file-list="false"
                  :on-success="handleLogoSuccess"
                  :before-upload="beforeLogoUpload"
                >
                  <img v-if="config.general.logoUrl" :src="config.general.logoUrl" class="tenant-config__logo">
                  <div v-else class="tenant-config__logo-placeholder">
                    <i class="el-icon-plus" />
                    <span>上传Logo</span>
                  </div>
                </el-upload>
                <div class="tenant-config__tip">建议尺寸：200x60px，支持 JPG、PNG 格式</div>
              </el-form-item>
              <el-form-item label="登录页背景">
                <el-upload
                  class="tenant-config__bg-uploader"
                  action="/api/v1/upload"
                  :show-file-list="false"
                  :on-success="handleBgSuccess"
                  :before-upload="beforeBgUpload"
                >
                  <img v-if="config.general.loginBgUrl" :src="config.general.loginBgUrl" class="tenant-config__bg">
                  <div v-else class="tenant-config__bg-placeholder">
                    <i class="el-icon-plus" />
                    <span>上传背景图</span>
                  </div>
                </el-upload>
                <div class="tenant-config__tip">建议尺寸：1920x1080px</div>
              </el-form-item>
              <el-form-item label="客服电话">
                <el-input v-model="config.general.servicePhone" placeholder="请输入客服电话" />
              </el-form-item>
              <el-form-item label="客服邮箱">
                <el-input v-model="config.general.serviceEmail" placeholder="请输入客服邮箱" />
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 商机配置 -->
          <div v-show="activeConfig === 'lead'" class="tenant-config__form">
            <el-form label-width="200px">
              <el-divider content-position="left">自动分配设置</el-divider>
              <el-form-item label="启用自动分配">
                <el-switch v-model="config.lead.autoAssignEnabled" />
                <span class="tenant-config__switch-hint">开启后新商机将自动分配给跟进人</span>
              </el-form-item>
              <el-form-item v-if="config.lead.autoAssignEnabled" label="分配规则">
                <el-radio-group v-model="config.lead.assignRule">
                  <el-radio label="round_robin">轮询分配</el-radio>
                  <el-radio label="load_balance">负载均衡</el-radio>
                  <el-radio label="random">随机分配</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-divider content-position="left">跟进提醒设置</el-divider>
              <el-form-item label="未跟进提醒">
                <el-switch v-model="config.lead.followReminderEnabled" />
              </el-form-item>
              <el-form-item v-if="config.lead.followReminderEnabled" label="提醒时间">
                <el-select v-model="config.lead.reminderHours" style="width: 200px">
                  <el-option label="24小时后" :value="24" />
                  <el-option label="48小时后" :value="48" />
                  <el-option label="72小时后" :value="72" />
                </el-select>
              </el-form-item>
              
              <el-divider content-position="left">商机回收设置</el-divider>
              <el-form-item label="启用商机回收">
                <el-switch v-model="config.lead.recycleEnabled" />
                <span class="tenant-config__switch-hint">开启后长时间未跟进的商机将自动回收到公海</span>
              </el-form-item>
              <el-form-item v-if="config.lead.recycleEnabled" label="回收时间">
                <el-select v-model="config.lead.recycleDays" style="width: 200px">
                  <el-option label="7天未跟进" :value="7" />
                  <el-option label="15天未跟进" :value="15" />
                  <el-option label="30天未跟进" :value="30" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 内容配置 -->
          <div v-show="activeConfig === 'content'" class="tenant-config__form">
            <el-form label-width="200px">
              <el-divider content-position="left">AI生成设置</el-divider>
              <el-form-item label="AI生成审核">
                <el-switch v-model="config.content.aiAuditEnabled" />
                <span class="tenant-config__switch-hint">开启后AI生成的内容需要审核后才能发布</span>
              </el-form-item>
              <el-form-item label="默认生成风格">
                <el-select v-model="config.content.defaultStyle" style="width: 200px">
                  <el-option label="专业严谨" value="professional" />
                  <el-option label="轻松活泼" value="casual" />
                  <el-option label="幽默风趣" value="humorous" />
                  <el-option label="简洁明了" value="concise" />
                </el-select>
              </el-form-item>
              <el-form-item label="默认内容长度">
                <el-radio-group v-model="config.content.defaultLength">
                  <el-radio label="short">短篇（100-300字）</el-radio>
                  <el-radio label="medium">中篇（300-800字）</el-radio>
                  <el-radio label="long">长篇（800字以上）</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-divider content-position="left">发布设置</el-divider>
              <el-form-item label="定时发布功能">
                <el-switch v-model="config.content.scheduleEnabled" />
              </el-form-item>
              <el-form-item label="多平台同步">
                <el-switch v-model="config.content.multiPlatformSync" />
                <span class="tenant-config__switch-hint">开启后可一键发布到多个平台</span>
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 通知配置 -->
          <div v-show="activeConfig === 'notification'" class="tenant-config__form">
            <el-form label-width="200px">
              <el-divider content-position="left">站内通知</el-divider>
              <el-form-item label="新商机通知">
                <el-switch v-model="config.notification.newLeadNotify" />
              </el-form-item>
              <el-form-item label="商机分配通知">
                <el-switch v-model="config.notification.assignNotify" />
              </el-form-item>
              <el-form-item label="任务完成通知">
                <el-switch v-model="config.notification.taskCompleteNotify" />
              </el-form-item>
              
              <el-divider content-position="left">短信通知</el-divider>
              <el-form-item label="启用短信通知">
                <el-switch v-model="config.notification.smsEnabled" />
              </el-form-item>
              <el-form-item v-if="config.notification.smsEnabled" label="短信接收手机号">
                <el-input v-model="config.notification.smsPhone" placeholder="请输入接收短信的手机号" />
              </el-form-item>
              
              <el-divider content-position="left">邮件通知</el-divider>
              <el-form-item label="启用邮件通知">
                <el-switch v-model="config.notification.emailEnabled" />
              </el-form-item>
              <el-form-item v-if="config.notification.emailEnabled" label="邮件接收地址">
                <el-input v-model="config.notification.emailAddress" placeholder="请输入接收邮件的地址" />
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 安全配置 -->
          <div v-show="activeConfig === 'security'" class="tenant-config__form">
            <el-form label-width="200px">
              <el-divider content-position="left">登录安全</el-divider>
              <el-form-item label="登录验证码">
                <el-switch v-model="config.security.captchaEnabled" />
              </el-form-item>
              <el-form-item label="登录失败锁定">
                <el-switch v-model="config.security.loginLockEnabled" />
              </el-form-item>
              <el-form-item v-if="config.security.loginLockEnabled" label="失败次数">
                <el-input-number v-model="config.security.loginFailLimit" :min="3" :max="10" />
                <span class="tenant-config__switch-hint">次失败后锁定账户</span>
              </el-form-item>
              
              <el-divider content-position="left">密码策略</el-divider>
              <el-form-item label="密码最小长度">
                <el-input-number v-model="config.security.passwordMinLength" :min="6" :max="20" />
              </el-form-item>
              <el-form-item label="密码复杂度要求">
                <el-checkbox-group v-model="config.security.passwordRules">
                  <el-checkbox label="uppercase">包含大写字母</el-checkbox>
                  <el-checkbox label="lowercase">包含小写字母</el-checkbox>
                  <el-checkbox label="number">包含数字</el-checkbox>
                  <el-checkbox label="special">包含特殊字符</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="密码有效期">
                <el-select v-model="config.security.passwordExpireDays" style="width: 200px">
                  <el-option label="永不过期" :value="0" />
                  <el-option label="30天" :value="30" />
                  <el-option label="60天" :value="60" />
                  <el-option label="90天" :value="90" />
                </el-select>
              </el-form-item>
              
              <el-divider content-position="left">操作安全</el-divider>
              <el-form-item label="敏感操作二次确认">
                <el-switch v-model="config.security.sensitiveConfirm" />
                <span class="tenant-config__switch-hint">删除、导出等操作需要二次确认</span>
              </el-form-item>
              <el-form-item label="操作日志记录">
                <el-switch v-model="config.security.operationLogEnabled" />
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview TenantConfig 租户配置页
 * @description 配置系统参数和功能开关
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const saveLoading = ref(false)
const activeConfig = ref('general')

// 配置数据
const config = reactive({
  general: {
    systemName: '北极星AI商机获客系统',
    logoUrl: '',
    loginBgUrl: '',
    servicePhone: '400-888-8888',
    serviceEmail: 'service@beijixing.com',
  },
  lead: {
    autoAssignEnabled: false,
    assignRule: 'round_robin',
    followReminderEnabled: true,
    reminderHours: 48,
    recycleEnabled: true,
    recycleDays: 15,
  },
  content: {
    aiAuditEnabled: true,
    defaultStyle: 'professional',
    defaultLength: 'medium',
    scheduleEnabled: true,
    multiPlatformSync: true,
  },
  notification: {
    newLeadNotify: true,
    assignNotify: true,
    taskCompleteNotify: true,
    smsEnabled: false,
    smsPhone: '',
    emailEnabled: false,
    emailAddress: '',
  },
  security: {
    captchaEnabled: true,
    loginLockEnabled: true,
    loginFailLimit: 5,
    passwordMinLength: 8,
    passwordRules: ['uppercase', 'lowercase', 'number'],
    passwordExpireDays: 90,
    sensitiveConfirm: true,
    operationLogEnabled: true,
  },
})

// 配置标题映射
const titleMap = {
  general: '基础配置',
  lead: '商机配置',
  content: '内容配置',
  notification: '通知配置',
  security: '安全配置',
}

const configTitle = computed(() => titleMap[activeConfig.value] || '配置')

// ============================================================
// 方法
// ============================================================

/**
 * 菜单选择
 */
function handleMenuSelect(index) {
  activeConfig.value = index
}

/**
 * Logo上传成功
 */
function handleLogoSuccess(response) {
  config.general.logoUrl = response.data?.url || ''
  ElMessage.success('Logo上传成功')
}

/**
 * Logo上传前检查
 */
function beforeLogoUpload(file) {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJpgOrPng) {
    ElMessage.error('只支持 JPG 或 PNG 格式的图片')
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
  }
  return isJpgOrPng && isLt2M
}

/**
 * 背景图上传成功
 */
function handleBgSuccess(response) {
  config.general.loginBgUrl = response.data?.url || ''
  ElMessage.success('背景图上传成功')
}

/**
 * 背景图上传前检查
 */
function beforeBgUpload(file) {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isJpgOrPng) {
    ElMessage.error('只支持 JPG 或 PNG 格式的图片')
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
  }
  return isJpgOrPng && isLt5M
}

/**
 * 保存配置
 */
async function handleSave() {
  saveLoading.value = true
  try {
    console.log('[TenantConfig] 保存配置(预留):', formData)
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('配置保存成功')
  } catch (error) {
    console.error('[TenantConfig] Save failed:', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    saveLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.tenant-config {
  &__menu {
    margin-bottom: $spacing-lg;
  }

  &__menu-title {
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
    margin-bottom: $spacing-md;
    padding: 0 $spacing-sm;
  }

  &__menu-list {
    border-right: none;
  }

  &__content {
    min-height: 600px;
  }

  &__header {
    @include flex-between;
  }

  &__title {
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
  }

  &__form {
    padding: $spacing-md 0;
  }

  &__switch-hint {
    margin-left: $spacing-sm;
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__tip {
    margin-top: $spacing-xs;
    font-size: $font-size-xs;
    color: $color-text-secondary;
  }

  &__logo-uploader,
  &__bg-uploader {
    :deep(.el-upload) {
      border: 1px dashed $color-border-dark;
      border-radius: $radius-md;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: border-color $transition-duration-base;

      &:hover {
        border-color: $color-primary;
      }
    }
  }

  &__logo-uploader {
    :deep(.el-upload) {
      width: 200px;
      height: 60px;
    }
  }

  &__bg-uploader {
    :deep(.el-upload) {
      width: 300px;
      height: 168px;
    }
  }

  &__logo {
    width: 200px;
    height: 60px;
    object-fit: contain;
  }

  &__bg {
    width: 300px;
    height: 168px;
    object-fit: cover;
  }

  &__logo-placeholder,
  &__bg-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: $color-text-secondary;

    i {
      font-size: 28px;
      margin-bottom: $spacing-xs;
    }

    span {
      font-size: $font-size-sm;
    }
  }
}
</style>