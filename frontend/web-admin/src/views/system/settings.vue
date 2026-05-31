<template>
  <div class="system-settings-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
    </div>

    <el-row :gutter="20">
      <!-- 左侧菜单 -->
      <el-col :span="4">
        <div class="settings-menu">
          <div
            v-for="item in settingTabs"
            :key="item.key"
            class="menu-item"
            :class="{ active: activeTab === item.key }"
            @click="activeTab = item.key"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </el-col>

      <!-- 右侧内容 -->
      <el-col :span="20">
        <!-- 基础设置 -->
        <div v-if="activeTab === 'basic'" class="settings-panel">
          <div class="panel-title">基础设置</div>
          <el-form :model="basicForm" label-width="160px" class="settings-form">
            <el-form-item label="平台名称">
              <el-input v-model="basicForm.platformName" style="width: 400px" />
            </el-form-item>
            <el-form-item label="平台Logo">
              <el-upload
                class="logo-uploader"
                action="#"
                :show-file-list="false"
                :before-upload="beforeLogoUpload"
              >
                <img v-if="basicForm.logoUrl" :src="basicForm.logoUrl" class="logo-preview" />
                <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <span class="form-tip">建议尺寸 200×200，支持 JPG/PNG</span>
            </el-form-item>
            <el-form-item label="平台简介">
              <el-input v-model="basicForm.description" type="textarea" :rows="3" style="width: 400px" />
            </el-form-item>
            <el-form-item label="版权信息">
              <el-input v-model="basicForm.copyright" style="width: 400px" placeholder="©2024 北极星AI" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasicSettings" :loading="saving">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 社媒平台配置 -->
        <div v-if="activeTab === 'social'" class="settings-panel">
          <div class="panel-title">社媒平台配置</div>
          <el-form label-width="160px" class="settings-form">
            <el-form-item
              v-for="platform in socialPlatforms"
              :key="platform.key"
              :label="platform.name"
            >
              <el-switch
                v-model="platform.enabled"
                active-text="已启用"
                inactive-text="已禁用"
              />
              <el-button
                v-if="platform.enabled"
                size="small"
                style="margin-left: 16px"
                @click="showPlatformConfig(platform)"
              >
                配置参数
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSocialSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 计费规则配置 -->
        <div v-if="activeTab === 'billing'" class="settings-panel">
          <div class="panel-title">计费规则配置</div>
          <el-form :model="billingForm" label-width="180px" class="settings-form">
            <el-divider content-position="left">积分充值规则</el-divider>
            <el-form-item label="基础充值比例">
              <el-input-number v-model="billingForm.rechargeRatio" :min="1" :step="0.1" />
              <span class="form-tip ml-10">元 = 积分</span>
            </el-form-item>
            <el-form-item label="100元以上赠送比例">
              <el-input-number v-model="billingForm.rechargeBonus100" :min="0" :max="100" /> %
              <span class="form-tip ml-10">额外赠送</span>
            </el-form-item>
            <el-form-item label="500元以上赠送比例">
              <el-input-number v-model="billingForm.rechargeBonus500" :min="0" :max="100" /> %
            </el-form-item>
            <el-form-item label="1000元以上赠送比例">
              <el-input-number v-model="billingForm.rechargeBonus1000" :min="0" :max="100" /> %
            </el-form-item>

            <el-divider content-position="left">AI消耗规则</el-divider>
            <el-form-item label="Token计费倍率">
              <el-input-number v-model="billingForm.tokenRate" :min="0.1" :step="0.1" :precision="1" />
              <span class="form-tip ml-10">Token数 × 倍率 = 消耗积分</span>
            </el-form-item>
            <el-form-item label="最低消费阈值">
              <el-input-number v-model="billingForm.minConsume" :min="0" />
              <span class="form-tip ml-10">每次调用最少消耗积分</span>
            </el-form-item>
            <el-form-item label="免费额度（每日）">
              <el-input-number v-model="billingForm.dailyFreeCredits" :min="0" />
              <span class="form-tip ml-10">每日免费积分额度</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveBillingSettings" :loading="saving">保存规则</el-button>
              <el-button @click="resetBillingSettings">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 风控规则 -->
        <div v-if="activeTab === 'risk'" class="settings-panel">
          <div class="panel-title">风控规则配置</div>
          <el-form label-width="160px" class="settings-form">
            <el-form-item label="风控总开关">
              <el-switch v-model="riskForm.enabled" />
            </el-form-item>
            <el-form-item label="高频操作阈值">
              <el-input-number v-model="riskForm.actionThreshold" :min="1" />
              <span class="form-tip ml-10">次/分钟，超过则触发风控</span>
            </el-form-item>
            <el-form-item label="账号封禁阈值">
              <el-input-number v-model="riskForm.banScore" :min="0" :max="100" />
              <span class="form-tip ml-10">评分低于此值自动封禁</span>
            </el-form-item>
            <el-form-item label="IP限制规则">
              <el-switch v-model="riskForm.ipLimit" />
              <span class="form-tip ml-10">同一IP最大并发登录数</span>
            </el-form-item>
            <el-form-item label="IP最大并发">
              <el-input-number v-model="riskForm.maxIpConcurrent" :min="1" :disabled="!riskForm.ipLimit" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveRiskSettings" :loading="saving">保存风控规则</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 支付配置 -->
        <div v-if="activeTab === 'payment'" class="settings-panel">
          <div class="panel-title">支付配置</div>
          <el-form label-width="160px" class="settings-form">
            <el-divider content-position="left">微信支付</el-divider>
            <el-form-item label="启用微信支付">
              <el-switch v-model="paymentForm.wxpayEnabled" />
            </el-form-item>
            <el-form-item label="AppID">
              <el-input v-model="paymentForm.wxpayAppId" style="width: 400px" placeholder="微信支付 AppID" />
            </el-form-item>
            <el-form-item label="商户号">
              <el-input v-model="paymentForm.wxpayMchId" style="width: 400px" placeholder="微信支付商户号" />
            </el-form-item>

            <el-divider content-position="left">支付宝</el-divider>
            <el-form-item label="启用支付宝">
              <el-switch v-model="paymentForm.alipayEnabled" />
            </el-form-item>
            <el-form-item label="AppID">
              <el-input v-model="paymentForm.alipayAppId" style="width: 400px" placeholder="支付宝 AppID" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="savePaymentSettings" :loading="saving">保存配置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 操作日志 -->
        <div v-if="activeTab === 'logs'" class="settings-panel">
          <div class="panel-title">操作日志</div>
          <AdminTable
            :data="logData"
            :columns="logColumns"
            :loading="logLoading"
            :page="logPagination.page"
            :page-size="logPagination.pageSize"
            :total="logPagination.total"
            :show-index="true"
          />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * SystemSettings - 系统设置页面
 * 功能：平台基础配置、社媒平台配置、计费规则、风控规则、支付配置、操作日志
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Connection, Coin, Warning, CreditCard, Document, Plus } from '@element-plus/icons-vue'
import AdminTable from '@/components/common/AdminTable.vue'
import { getSystemSettings, updateSystemSettings, getOperationLogList } from '@/api/system'

// ============================================================
// Tab 切换
// ============================================================
const activeTab = ref('basic')

const settingTabs = [
  { key: 'basic', label: '基础设置', icon: Setting },
  { key: 'social', label: '社媒平台', icon: Connection },
  { key: 'billing', label: '计费规则', icon: Coin },
  { key: 'risk', label: '风控规则', icon: Warning },
  { key: 'payment', label: '支付配置', icon: CreditCard },
  { key: 'logs', label: '操作日志', icon: Document }
]

// ============================================================
// 表单数据
// ============================================================
const basicForm = reactive({
  platformName: '北极星AI商机获客系统',
  logoUrl: '',
  description: '面向企业/商户的多租户SaaS智能运营获客工具',
  copyright: '©2024 北极星AI 版权所有'
})

const billingForm = reactive({
  rechargeRatio: 1,
  rechargeBonus100: 5,
  rechargeBonus500: 10,
  rechargeBonus1000: 15,
  tokenRate: 10,
  minConsume: 10,
  dailyFreeCredits: 0
})

const riskForm = reactive({
  enabled: true,
  actionThreshold: 60,
  banScore: 30,
  ipLimit: true,
  maxIpConcurrent: 3
})

const paymentForm = reactive({
  wxpayEnabled: false,
  wxpayAppId: '',
  wxpayMchId: '',
  alipayEnabled: false,
  alipayAppId: ''
})

const saving = ref(false)

// ============================================================
// 社媒平台配置
// ============================================================
const socialPlatforms = reactive([
  { key: 'wechat', name: '微信公众号', enabled: true },
  { key: 'weibo', name: '微博', enabled: false },
  { key: 'douyin', name: '抖音', enabled: true },
  { key: 'xiaohongshu', name: '小红书', enabled: false }
])

// ============================================================
// 日志相关
// ============================================================
const logLoading = ref(false)
const logData = ref([])
const logPagination = reactive({ page: 1, pageSize: 10, total: 0 })
const logColumns = [
  { prop: 'adminName', label: '管理员', width: 120 },
  { prop: 'action', label: '操作类型', width: 140 },
  { prop: 'target', label: '操作对象', minWidth: 150 },
  { prop: 'ip', label: 'IP地址', width: 130 },
  { prop: 'createdAt', label: '操作时间', width: 170 }
]

// ============================================================
// 保存操作 (真实API调用)
// ============================================================
async function saveBasicSettings() {
  saving.value = true
  try {
    await updateSystemSettings(basicForm)
    ElMessage.success('基础设置已保存')
  } catch (error) {
    console.error('保存基础设置失败:', error)
    ElMessage.error('保存失败：' + (error.message || '网络错误，请重试'))
  } finally {
    saving.value = false
  }
}

async function saveSocialSettings() {
  saving.value = true
  try {
    await updateSystemSettings({ socialPlatforms })
    ElMessage.success('社媒平台配置已保存')
  } catch (error) {
    console.error('保存社媒配置失败:', error)
    ElMessage.error('保存失败：' + (error.message || '网络错误，请重试'))
  } finally {
    saving.value = false
  }
}

async function saveBillingSettings() {
  saving.value = true
  try {
    await updateSystemSettings(billingForm)
    ElMessage.success('计费规则已保存')
  } catch (error) {
    console.error('保存计费规则失败:', error)
    ElMessage.error('保存失败：' + (error.message || '网络错误，请重试'))
  } finally {
    saving.value = false
  }
}

async function saveRiskSettings() {
  saving.value = true
  try {
    await updateSystemSettings(riskForm)
    ElMessage.success('风控规则已保存')
  } catch (error) {
    console.error('保存风控规则失败:', error)
    ElMessage.error('保存失败：' + (error.message || '网络错误，请重试'))
  } finally {
    saving.value = false
  }
}

async function savePaymentSettings() {
  saving.value = true
  try {
    await updateSystemSettings(paymentForm)
    ElMessage.success('支付配置已保存')
  } catch (error) {
    console.error('保存支付配置失败:', error)
    ElMessage.error('保存失败：' + (error.message || '网络错误，请重试'))
  } finally {
    saving.value = false
  }
}

function resetBillingSettings() {
  billingForm.rechargeRatio = 1
  billingForm.rechargeBonus100 = 5
  billingForm.rechargeBonus500 = 10
  billingForm.rechargeBonus1000 = 15
  billingForm.tokenRate = 10
  billingForm.minConsume = 10
  billingForm.dailyFreeCredits = 0
}

function showPlatformConfig(platform) {
  ElMessage.info(`「${platform.name}」参数配置开发中...`)
}

function beforeLogoUpload(file) {
  const isImage = ['image/jpeg', 'image/png'].includes(file.type)
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) ElMessage.error('只能上传 JPG/PNG 图片')
  if (!isLt2M) ElMessage.error('图片大小不能超过 2MB')
  return isImage && isLt2M
}

// ============================================================
// 日志加载 (真实API调用)
// ============================================================
async function loadLogs() {
  logLoading.value = true
  try {
    const data = await getOperationLogList(logPagination)
    logData.value = data.list || []
    logPagination.total = data.total || 0
  } catch (error) {
    console.error('加载操作日志失败:', error)
    logData.value = []
    logPagination.total = 0
  } finally {
    logLoading.value = false
  }
}

// ============================================================
// 初始化
// ============================================================
onMounted(async () => {
  try {
    const data = await getSystemSettings()
    if (data) {
      Object.assign(basicForm, data.basic || {})
      Object.assign(billingForm, data.billing || {})
      Object.assign(riskForm, data.risk || {})
      Object.assign(paymentForm, data.payment || {})
    }
  } catch {
    // 使用默认值（演示模式）
  }

  loadLogs()
})
</script>

<style lang="scss" scoped>
.system-settings-page {
  padding: 0;
}

.page-header {
  margin-bottom: $spacing-base;

  .page-title {
    font-size: $font-size-extra-large;
    font-weight: 600;
    color: $text-primary;
  }
}

// 左侧菜单
.settings-menu {
  background: $bg-color;
  border-radius: $border-radius-base;
  border: 1px solid $border-color-light;
  padding: 8px;
  position: sticky;
  top: 20px;

  .menu-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: $border-radius-base;
    cursor: pointer;
    font-size: $font-size-base;
    color: $text-regular;
    transition: $transition-fast;

    &:hover {
      background-color: $bg-color-page;
      color: $text-primary;
    }

    &.active {
      background-color: rgba($primary-color, 0.1);
      color: $primary-color;
      font-weight: 500;
    }
  }
}

// 右侧设置面板
.settings-panel {
  background: $bg-color;
  border-radius: $border-radius-base;
  border: 1px solid $border-color-light;
  padding: 24px;

  .panel-title {
    font-size: $font-size-large;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid $border-color-light;
  }
}

.settings-form {
  max-width: 700px;
}

.form-tip {
  margin-left: 10px;
  font-size: $font-size-small;
  color: $text-secondary;
}

.logo-uploader {
  border: 1px dashed $border-color-base;
  border-radius: $border-radius-base;
  cursor: pointer;
  overflow: hidden;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover { border-color: $primary-color; }
}

.logo-preview {
  width: 100px;
  height: 100px;
  object-fit: cover;
}

.logo-uploader-icon {
  font-size: 28px;
  color: $text-secondary;
}

.ml-10 {
  margin-left: 10px;
}
</style>
