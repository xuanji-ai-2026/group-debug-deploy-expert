<template>
  <div class="config-page">
    <!-- 左侧分组导航 -->
    <div class="config-layout">
      <div class="config-sidebar">
        <div class="sidebar-title">参数分组</div>
        <el-menu
          :default-active="activeGroup"
          :default-openeds="['system', 'platform', 'billing', 'risk']"
          @select="handleGroupChange"
        >
          <el-sub-menu index="system">
            <template #title><el-icon><Setting /></el-icon>系统设置</template>
            <el-menu-item index="system:basic">基础配置</el-menu-item>
            <el-menu-item index="system:security">安全配置</el-menu-item>
            <el-menu-item index="system:email">邮件配置</el-menu-item>
            <el-menu-item index="system:sms">短信配置</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="platform">
            <template #title><el-icon><Connection /></el-icon>平台配置</template>
            <el-menu-item index="platform:douyin">抖音配置</el-menu-item>
            <el-menu-item index="platform:xiaohongshu">小红书配置</el-menu-item>
            <el-menu-item index="platform:kuaishou">快手配置</el-menu-item>
            <el-menu-item index="platform:weibo">微博配置</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="billing">
            <template #title><el-icon><Coin /></el-icon>计费配置</template>
            <el-menu-item index="billing:price">价格配置</el-menu-item>
            <el-menu-item index="billing:recharge">充值配置</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="risk">
            <template #title><el-icon><WarnTriangleFilled /></el-icon>风控配置</template>
            <el-menu-item index="risk:basic">基础配置</el-menu-item>
            <el-menu-item index="risk:keyword">关键词配置</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>

      <!-- 右侧配置项 -->
      <div class="config-content">
        <div class="config-header">
          <h3>{{ currentGroupTitle }}</h3>
          <el-button type="primary" :loading="saving" @click="handleSaveConfig">保存配置</el-button>
        </div>

        <div class="config-body">
          <!-- 动态渲染配置项 -->
          <div v-for="group in configGroups" :key="group.key" v-show="activeGroup === group.key">
            <el-form ref="configFormRef" :model="group.configs" :rules="group.rules" label-width="180px">
              <el-form-item
                v-for="item in group.items"
                :key="item.key"
                :label="item.label"
                :prop="item.key"
              >
                <!-- 文本输入 -->
                <template v-if="item.type === 'input'">
                  <el-input
                    v-model="group.configs[item.key]"
                    :placeholder="item.placeholder"
                    :maxlength="item.maxlength"
                    :disabled="item.readonly"
                    style="width: 400px"
                  />
                </template>

                <!-- 数字输入 -->
                <template v-else-if="item.type === 'number'">
                  <el-input-number
                    v-model="group.configs[item.key]"
                    :min="item.min"
                    :max="item.max"
                    :step="item.step"
                    :disabled="item.readonly"
                  />
                  <span v-if="item.unit" class="config-unit">{{ item.unit }}</span>
                </template>

                <!-- 开关 -->
                <template v-else-if="item.type === 'switch'">
                  <el-switch v-model="group.configs[item.key]" :disabled="item.readonly" />
                  <span class="config-tip">{{ item.tip }}</span>
                </template>

                <!-- 下拉选择 -->
                <template v-else-if="item.type === 'select'">
                  <el-select v-model="group.configs[item.key]" :disabled="item.readonly" style="width: 400px">
                    <el-option
                      v-for="opt in item.options"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                </template>

                <!-- 多行文本 -->
                <template v-else-if="item.type === 'textarea'">
                  <el-input
                    v-model="group.configs[item.key]"
                    type="textarea"
                    :rows="4"
                    :placeholder="item.placeholder"
                    :maxlength="item.maxlength"
                    :disabled="item.readonly"
                    style="width: 500px"
                  />
                </template>

                <!-- 密码 -->
                <template v-else-if="item.type === 'password'">
                  <el-input
                    v-model="group.configs[item.key]"
                    type="password"
                    show-password
                    :placeholder="item.placeholder"
                    :disabled="item.readonly"
                    style="width: 400px"
                  />
                </template>

                <!-- 日期范围 -->
                <template v-else-if="item.type === 'daterange'">
                  <el-date-picker
                    v-model="group.configs[item.key]"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    value-format="YYYY-MM-DD"
                    :disabled="item.readonly"
                  />
                </template>

                <!-- 说明文字 -->
                <span v-if="item.description" class="config-description">{{ item.description }}</span>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 系统参数配置页面
 * 功能：系统参数、平台参数、计费参数、风控参数的配置管理
 * @author 张前端 (EMP-FE-001)
 */

import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post } from '@/utils/request'
import { Setting, Connection, Coin, WarnTriangleFilled } from '@element-plus/icons-vue'

// ============================================================
// 状态
// ============================================================
const activeGroup = ref('system:basic')
const saving = ref(false)
const configFormRef = ref(null)

const currentGroupTitle = computed(() => {
  const group = configGroups.find(g => g.key === activeGroup.value)
  return group?.title || '系统配置'
})

// ============================================================
// 配置分组定义
// ============================================================
const configGroups = reactive([
  {
    key: 'system:basic',
    title: '基础配置',
    configs: {},
    rules: {},
    items: [
      { key: 'system_name', label: '系统名称', type: 'input', placeholder: '请输入系统名称', maxlength: 50 },
      { key: 'system_logo', label: '系统Logo', type: 'input', placeholder: 'Logo图片URL', maxlength: 200 },
      { key: 'default_page_size', label: '默认分页大小', type: 'number', min: 10, max: 100, step: 10, default: 20 },
      { key: 'upload_max_size', label: '文件上传大小限制', type: 'number', min: 1, max: 100, step: 1, unit: 'MB', default: 10 },
      { key: 'upload_allowed_types', label: '允许上传的文件类型', type: 'input', placeholder: '如: jpg,png,pdf,doc', maxlength: 200 },
      { key: 'maintenance_mode', label: '维护模式', type: 'switch', tip: '开启后仅管理员可访问' },
      { key: 'maintenance_message', label: '维护提示', type: 'textarea', placeholder: '维护模式时显示的提示信息' }
    ]
  },
  {
    key: 'system:security',
    title: '安全配置',
    configs: {},
    rules: {},
    items: [
      { key: 'password_min_length', label: '密码最小长度', type: 'number', min: 6, max: 32, default: 8 },
      { key: 'password_require_special', label: '密码必须包含特殊字符', type: 'switch' },
      { key: 'password_expire_days', label: '密码过期天数', type: 'number', min: 0, max: 365, unit: '天，0表示不过期' },
      { key: 'login_max_attempts', label: '登录失败最大次数', type: 'number', min: 3, max: 10, default: 5 },
      { key: 'login_lock_duration', label: '账号锁定时长', type: 'number', min: 5, max: 1440, unit: '分钟' },
      { key: 'session_timeout', label: '会话超时时间', type: 'number', min: 5, max: 480, unit: '分钟' },
      { key: 'allow_ip_list', label: '允许访问的IP列表', type: 'textarea', placeholder: '每行一个IP，留空表示不限制' },
      { key: 'jwt_secret', label: 'JWT密钥', type: 'password', placeholder: '请输入JWT签名密钥', readonly: true }
    ]
  },
  {
    key: 'system:email',
    title: '邮件配置',
    configs: {},
    rules: {},
    items: [
      { key: 'smtp_host', label: 'SMTP服务器', type: 'input', placeholder: '如: smtp.qq.com' },
      { key: 'smtp_port', label: 'SMTP端口', type: 'number', min: 1, max: 65535, default: 465 },
      { key: 'smtp_username', label: '用户名', type: 'input', placeholder: '邮箱账号' },
      { key: 'smtp_password', label: '密码', type: 'password', placeholder: '邮箱密码或授权码' },
      { key: 'smtp_from_address', label: '发件人地址', type: 'input', placeholder: '如: noreply@example.com' },
      { key: 'smtp_from_name', label: '发件人名称', type: 'input', placeholder: '如: 北极星AI系统' },
      { key: 'smtp_use_ssl', label: '使用SSL加密', type: 'switch', tip: '推荐开启以确保安全' }
    ]
  },
  {
    key: 'system:sms',
    title: '短信配置',
    configs: {},
    rules: {},
    items: [
      { key: 'sms_provider', label: '短信服务商', type: 'select', options: [
        { label: '阿里云短信', value: 'aliyun' },
        { label: '腾讯云短信', value: 'tencent' },
        { label: '华为云短信', value: 'huawei' }
      ]},
      { key: 'sms_access_key', label: 'AccessKey', type: 'input', placeholder: '短信平台AccessKey' },
      { key: 'sms_secret_key', label: 'SecretKey', type: 'password', placeholder: '短信平台SecretKey' },
      { key: 'sms_sign_name', label: '签名名称', type: 'input', placeholder: '短信签名' },
      { key: 'sms_template_code', label: '验证码模板Code', type: 'input', placeholder: '短信模板CODE' }
    ]
  },
  {
    key: 'platform:douyin',
    title: '抖音平台配置',
    configs: {},
    rules: {},
    items: [
      { key: 'douyin_app_id', label: 'App ID', type: 'input', placeholder: '抖音开放平台App ID' },
      { key: 'douyin_app_secret', label: 'App Secret', type: 'password', placeholder: '抖音开放平台App Secret' },
      { key: 'douyin_enable', label: '启用抖音平台', type: 'switch' },
      { key: 'douyin_api_rate_limit', label: 'API调用频率限制', type: 'number', min: 1, max: 1000, default: 100, unit: '次/分钟' },
      { key: 'douyin_retry_times', label: '失败重试次数', type: 'number', min: 0, max: 5, default: 3 }
    ]
  },
  {
    key: 'platform:xiaohongshu',
    title: '小红书平台配置',
    configs: {},
    rules: {},
    items: [
      { key: 'xhs_app_id', label: 'App ID', type: 'input', placeholder: '小红书开放平台App ID' },
      { key: 'xhs_app_secret', label: 'App Secret', type: 'password', placeholder: '小红书开放平台App Secret' },
      { key: 'xhs_enable', label: '启用小红书平台', type: 'switch' },
      { key: 'xhs_api_rate_limit', label: 'API调用频率限制', type: 'number', min: 1, max: 1000, default: 80, unit: '次/分钟' }
    ]
  },
  {
    key: 'platform:kuaishou',
    title: '快手平台配置',
    configs: {},
    rules: {},
    items: [
      { key: 'ks_app_id', label: 'App ID', type: 'input', placeholder: '快手开放平台App ID' },
      { key: 'ks_app_secret', label: 'App Secret', type: 'password', placeholder: '快手开放平台App Secret' },
      { key: 'ks_enable', label: '启用快手平台', type: 'switch' }
    ]
  },
  {
    key: 'platform:weibo',
    title: '微博平台配置',
    configs: {},
    rules: {},
    items: [
      { key: 'wb_app_key', label: 'App Key', type: 'input', placeholder: '微博开放平台App Key' },
      { key: 'wb_app_secret', label: 'App Secret', type: 'password', placeholder: '微博开放平台App Secret' },
      { key: 'wb_enable', label: '启用微博平台', type: 'switch' }
    ]
  },
  {
    key: 'billing:price',
    title: '价格配置',
    configs: {},
    rules: {},
    items: [
      { key: 'price_per_message', label: '私信单价', type: 'number', min: 0, max: 10, step: 0.001, unit: '元/条' },
      { key: 'price_per_lead', label: '获客单价', type: 'number', min: 0, max: 100, step: 0.01, unit: '元/人' },
      { key: 'price_api_call', label: 'API调用单价', type: 'number', min: 0, max: 1, step: 0.0001, unit: '元/次' },
      { key: 'price_storage', label: '存储单价', type: 'number', min: 0, max: 1, step: 0.001, unit: '元/GB/月' },
      { key: 'min_recharge_amount', label: '最小充值金额', type: 'number', min: 0, max: 10000, default: 100, unit: '元' },
      { key: 'recharge_gift_ratio', label: '充值赠送比例', type: 'number', min: 0, max: 1, step: 0.01, default: 0, description: '如: 0.1表示充100送10' }
    ]
  },
  {
    key: 'billing:recharge',
    title: '充值配置',
    configs: {},
    rules: {},
    items: [
      { key: 'recharge_enabled', label: '启用在线充值', type: 'switch', tip: '关闭后用户只能线下充值' },
      { key: 'recharge_payment_methods', label: '支付方式', type: 'select', options: [
        { label: '微信支付', value: 'wechat' },
        { label: '支付宝', value: 'alipay' },
        { label: '银行转账', value: 'bank' }
      ]},
      { key: 'auto_recharge_threshold', label: '自动充值阈值', type: 'number', min: 0, default: 100, unit: '积分，低于此值自动提醒' },
      { key: 'recharge_invoice_enabled', label: '支持开具发票', type: 'switch' }
    ]
  },
  {
    key: 'risk:basic',
    title: '风控基础配置',
    configs: {},
    rules: {},
    items: [
      { key: 'risk_check_enabled', label: '启用风控检查', type: 'switch', tip: '关闭后将跳过所有风控检查' },
      { key: 'risk_check_level', label: '风控级别', type: 'select', options: [
        { label: '宽松（仅高风险拦截）', value: 'loose' },
        { label: '标准（推荐）', value: 'normal' },
        { label: '严格（低风险也告警）', value: 'strict' }
      ]},
      { key: 'risk_auto_action', label: '自动处置', type: 'switch', tip: '开启后自动执行风控动作，无需人工审核' },
      { key: 'risk_block_threshold', label: '拦截阈值', type: 'number', min: 0, max: 100, default: 80, unit: '分（0-100），超过此分数自动拦截' }
    ]
  },
  {
    key: 'risk:keyword',
    title: '关键词配置',
    configs: {},
    rules: {},
    items: [
      { key: 'risk_block_keywords', label: '拦截关键词', type: 'textarea', placeholder: '每行一个关键词，命中后直接拦截' },
      { key: 'risk_warn_keywords', label: '告警关键词', type: 'textarea', placeholder: '每行一个关键词，命中后发送告警但不拦截' },
      { key: 'risk_ai_audit_enabled', label: '启用AI内容审核', type: 'switch', tip: '使用AI模型对私信内容进行审核' },
      { key: 'risk_ai_sensitivity', label: 'AI审核敏感度', type: 'select', options: [
        { label: '低', value: 'low' },
        { label: '中（推荐）', value: 'medium' },
        { label: '高', value: 'high' }
      ]}
    ]
  }
])

// ============================================================
// 方法
// ============================================================
async function loadConfig(groupKey) {
  const group = configGroups.find(g => g.key === groupKey)
  if (!group) return
  try {
    const data = await get('/admin/system/config', { group: groupKey })
    Object.assign(group.configs, data || {})
  } catch (error) {
    console.error('加载配置失败:', error)
    // 使用默认值
    group.items.forEach(item => {
      if (item.default !== undefined) {
        group.configs[item.key] = item.default
      } else if (item.type === 'switch') {
        group.configs[item.key] = false
      }
    })
  }
}

function handleGroupChange(key) {
  loadConfig(key)
}

async function handleSaveConfig() {
  saving.value = true
  try {
    const group = configGroups.find(g => g.key === activeGroup.value)
    await post('/admin/system/config/update', {
      group: activeGroup.value,
      configs: group.configs
    })
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig(activeGroup.value)
})
</script>

<style lang="scss" scoped>
.config-page {
  height: 100%;
  padding: 20px;
}

.config-layout {
  display: flex;
  gap: 20px;
  height: calc(100vh - 120px);
}

.config-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  overflow-y: auto;

  .sidebar-title {
    padding: 16px 20px;
    font-weight: 600;
    font-size: 14px;
    color: #303133;
    border-bottom: 1px solid #ebeef5;
  }
}

.config-content {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  overflow-y: auto;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}

.config-body {
  padding: 24px;
}

.config-unit {
  margin-left: 8px;
  color: #909399;
  font-size: 14px;
}

.config-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.config-description {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.4;
}
</style>
