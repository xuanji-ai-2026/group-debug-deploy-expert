<template>
  <bx-content title="工作台" description="欢迎使用北极星AI商机获客系统" :show-header="true" :show-breadcrumb="true">
    <!-- 快捷入口 -->
    <el-row :gutter="20" class="dashboard__shortcuts">
      <el-col v-for="item in shortcuts" :key="item.path" :xs="12" :sm="8" :md="6" :lg="4">
        <router-link :to="item.path" class="dashboard__shortcut-card">
          <div class="dashboard__shortcut-icon" :style="{ background: item.bgColor }">
            <i :class="item.icon" />
          </div>
          <span class="dashboard__shortcut-label">{{ item.label }}</span>
        </router-link>
      </el-col>
    </el-row>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="dashboard__stats">
      <!-- 账户余额 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="dashboard__stat-card dashboard__stat-card--primary">
          <div class="dashboard__stat-header">
            <span class="dashboard__stat-label">账户余额</span>
            <i class="el-icon-wallet" />
          </div>
          <div class="dashboard__stat-value">
            <span class="dashboard__stat-num">{{ formatNumber(stats.balance) }}</span>
            <span class="dashboard__stat-unit">积分</span>
          </div>
          <div class="dashboard__stat-footer">
            <router-link to="/billing/recharge" class="dashboard__stat-action">
              立即充值
              <i class="el-icon-arrow-right" />
            </router-link>
          </div>
        </div>
      </el-col>

      <!-- 今日商机 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="dashboard__stat-card dashboard__stat-card--success">
          <div class="dashboard__stat-header">
            <span class="dashboard__stat-label">今日新增商机</span>
            <i class="el-icon-magic-stick" />
          </div>
          <div class="dashboard__stat-value">
            <span class="dashboard__stat-num">{{ formatNumber(stats.todayLeads) }}</span>
            <span class="dashboard__stat-unit">个</span>
          </div>
          <div class="dashboard__stat-footer">
            <router-link to="/lead/list" class="dashboard__stat-action">
              查看详情
              <i class="el-icon-arrow-right" />
            </router-link>
          </div>
        </div>
      </el-col>

      <!-- 内容发布 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="dashboard__stat-card dashboard__stat-card--warning">
          <div class="dashboard__stat-header">
            <span class="dashboard__stat-label">本月内容发布</span>
            <i class="el-icon-document" />
          </div>
          <div class="dashboard__stat-value">
            <span class="dashboard__stat-num">{{ formatNumber(stats.monthContent) }}</span>
            <span class="dashboard__stat-unit">篇</span>
          </div>
          <div class="dashboard__stat-footer">
            <router-link to="/content/list" class="dashboard__stat-action">
              内容管理
              <i class="el-icon-arrow-right" />
            </router-link>
          </div>
        </div>
      </el-col>

      <!-- 社媒账号 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="dashboard__stat-card dashboard__stat-card--info">
          <div class="dashboard__stat-header">
            <span class="dashboard__stat-label">绑定账号</span>
            <i class="el-icon-user" />
          </div>
          <div class="dashboard__stat-value">
            <span class="dashboard__stat-num">{{ formatNumber(stats.accountCount) }}</span>
            <span class="dashboard__stat-unit">个</span>
          </div>
          <div class="dashboard__stat-footer">
            <router-link to="/account/list" class="dashboard__stat-action">
              账号管理
              <i class="el-icon-arrow-right" />
            </router-link>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="dashboard__charts">
      <!-- 商机趋势 -->
      <el-col :xs="24" :lg="16">
        <div class="dashboard__card">
          <div class="dashboard__card-header">
            <h3 class="dashboard__card-title">商机趋势</h3>
            <div class="dashboard__card-actions">
              <el-radio-group v-model="leadChartType" size="small">
                <el-radio-button label="week">近7天</el-radio-button>
                <el-radio-button label="month">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <div class="dashboard__card-body">
            <div class="dashboard__chart-placeholder">
              <p>商机趋势图表区域（ECharts）</p>
              <p class="dashboard__chart-hint">集成 ECharts 展示商机数量趋势变化</p>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 商机来源分布 -->
      <el-col :xs="24" :lg="8">
        <div class="dashboard__card">
          <div class="dashboard__card-header">
            <h3 class="dashboard__card-title">商机来源分布</h3>
          </div>
          <div class="dashboard__card-body">
            <div class="dashboard__chart-placeholder">
              <p>饼图区域</p>
              <p class="dashboard__chart-hint">展示同业截客/主动获客占比</p>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近动态 & 待办事项 -->
    <el-row :gutter="20" class="dashboard__bottom">
      <!-- 最近商机 -->
      <el-col :xs="24" :lg="12">
        <div class="dashboard__card">
          <div class="dashboard__card-header">
            <h3 class="dashboard__card-title">最新商机</h3>
            <router-link to="/lead/list" class="dashboard__card-link">
              查看更多
              <i class="el-icon-arrow-right" />
            </router-link>
          </div>
          <div class="dashboard__card-body">
            <el-table :data="recentLeads" :show-header="true" size="small">
              <el-table-column label="用户" prop="userNickname" min-width="120" />
              <el-table-column label="来源" prop="platformName" width="100" align="center" />
              <el-table-column label="意向" prop="intentLevel" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getIntentType(row.intentLevel)" size="small">
                    {{ getIntentLabel(row.intentLevel) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="时间" prop="createTime" width="140" align="center" />
            </el-table>
          </div>
        </div>
      </el-col>

      <!-- 系统公告 -->
      <el-col :xs="24" :lg="12">
        <div class="dashboard__card">
          <div class="dashboard__card-header">
            <h3 class="dashboard__card-title">系统公告</h3>
          </div>
          <div class="dashboard__card-body">
            <div class="dashboard__notice-list">
              <div v-for="notice in notices" :key="notice.id" class="dashboard__notice-item">
                <span class="dashboard__notice-tag" :class="`dashboard__notice-tag--${notice.type}`">
                  {{ notice.typeLabel }}
                </span>
                <span class="dashboard__notice-content">{{ notice.content }}</span>
                <span class="dashboard__notice-time">{{ notice.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <template #actions>
      <el-button type="primary" @click="$router.push('/content/create')">
        <i class="el-icon-plus" />
        创建内容
      </el-button>
    </template>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview Dashboard 仪表盘首页
 * @description 展示数据统计、趋势图表、最近动态等内容
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user.js'

// ============================================================
// Store
// ============================================================
const userStore = useUserStore()

// ============================================================
// 状态
// ============================================================

// 快捷入口配置
const shortcuts = [
  { path: '/content/create', label: '创建内容', icon: 'el-icon-edit', bgColor: '#4F46E5' },
  { path: '/lead/list', label: '商机管理', icon: 'el-icon-data-analysis', bgColor: '#10B981' },
  { path: '/lead/task', label: '获客任务', icon: 'el-icon-s-management', bgColor: '#F59E0B' },
  { path: '/account/list', label: '账号管理', icon: 'el-icon-user', bgColor: '#6366F1' },
  { path: '/billing/recharge', label: '账户充值', icon: 'el-icon-wallet', bgColor: '#EC4899' },
  { path: '/settings/profile', label: '账号设置', icon: 'el-icon-setting', bgColor: '#8B5CF6' },
]

// 统计数据（从API加载）
const stats = reactive({
  balance: 0,
  todayLeads: 0,
  monthContent: 0,
  accountCount: 0,
})

// 商机趋势图类型
const leadChartType = ref('week')

// 最近商机列表（从API加载）
const recentLeads = ref([])

// 系统公告（从API加载）
const notices = ref([])

/**
 * 加载仪表盘数据
 */
async function fetchDashboardData() {
  try {
    const { default: dashboardApi } = await import('@/api/dashboard.js')
    const response = await dashboardApi.getDashboardData()
    
    if (response.data) {
      // 更新统计数据
      Object.assign(stats, response.data.stats || {})
      // 更新最近商机
      recentLeads.value = response.data.recentLeads || []
      // 更新公告
      notices.value = response.data.notices || []
    }
  } catch (error) {
    console.error('[Dashboard] Load data failed:', error)
    ElMessage.warning('加载数据失败，请刷新重试')
  }
}

// ============================================================
// 方法
// ============================================================

/**
 * 格式化数字
 */
function formatNumber(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString()
}

/**
 * 获取意向等级标签
 */
function getIntentLabel(level) {
  const labels = { 1: '高', 2: '中', 3: '低' }
  return labels[level] || '-'
}

/**
 * 获取意向等级类型
 */
function getIntentType(level) {
  const types = { 1: 'danger', 2: 'warning', 3: 'info' }
  return types[level] || 'info'
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  // 加载仪表盘数据
  fetchDashboardData()
})
</script>

<style lang="scss" scoped>
// 快捷入口
.dashboard__shortcuts {
  margin-bottom: $spacing-lg;
}

.dashboard__shortcut-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-lg;
  background: $color-white;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
  text-decoration: none;
  transition: all $transition-duration-base;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-md;
  }
}

.dashboard__shortcut-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: $radius-lg;
  margin-bottom: $spacing-sm;

  i {
    font-size: 24px;
    color: $color-white;
  }
}

.dashboard__shortcut-label {
  font-size: $font-size-sm;
  color: $color-text-primary;
  font-weight: $font-weight-medium;
}

// 统计卡片
.dashboard__stats {
  margin-bottom: $spacing-lg;
}

.dashboard__stat-card {
  padding: $spacing-lg;
  background: $color-white;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;

  &--primary {
    background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
    color: $color-white;
  }

  &--success {
    background: linear-gradient(135deg, #10B981 0%, #059669 100%);
    color: $color-white;
  }

  &--warning {
    background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
    color: $color-white;
  }

  &--info {
    background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
    color: $color-white;
  }
}

.dashboard__stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;

  i {
    font-size: 24px;
    opacity: 0.8;
  }
}

.dashboard__stat-label {
  font-size: $font-size-sm;
  opacity: 0.9;
}

.dashboard__stat-value {
  display: flex;
  align-items: baseline;
  gap: $spacing-xs;
  margin-bottom: $spacing-md;
}

.dashboard__stat-num {
  font-size: 32px;
  font-weight: $font-weight-bold;
  line-height: 1;
}

.dashboard__stat-unit {
  font-size: $font-size-sm;
  opacity: 0.8;
}

.dashboard__stat-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  padding-top: $spacing-md;
}

.dashboard__stat-action {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $color-white;
  text-decoration: none;
  opacity: 0.9;
  transition: opacity $transition-duration-fast;

  &:hover {
    opacity: 1;
  }
}

// 图表卡片
.dashboard__charts,
.dashboard__bottom {
  margin-bottom: $spacing-lg;
}

.dashboard__card {
  background: $color-white;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
  overflow: hidden;
}

.dashboard__card-header {
  @include flex-between;
  padding: $spacing-lg;
  border-bottom: 1px solid $color-border-light;
}

.dashboard__card-title {
  margin: 0;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.dashboard__card-link {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $color-primary;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.dashboard__card-body {
  padding: $spacing-lg;
}

.dashboard__chart-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  background: $color-gray-50;
  border-radius: $radius-md;
  color: $color-text-secondary;
  font-size: $font-size-sm;
}

.dashboard__chart-hint {
  margin-top: $spacing-xs;
  font-size: $font-size-xs;
  opacity: 0.7;
}

// 公告列表
.dashboard__notice-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.dashboard__notice-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm 0;
  border-bottom: 1px solid $color-border-light;

  &:last-child {
    border-bottom: none;
  }
}

.dashboard__notice-tag {
  flex-shrink: 0;
  padding: 2px 8px;
  font-size: $font-size-xs;
  border-radius: $radius-sm;

  &--info {
    background: rgba($color-primary, 0.1);
    color: $color-primary;
  }

  &--warning {
    background: rgba($color-warning, 0.1);
    color: $color-warning;
  }

  &--success {
    background: rgba($color-success, 0.1);
    color: $color-success;
  }
}

.dashboard__notice-content {
  flex: 1;
  font-size: $font-size-sm;
  color: $color-text-primary;
  @include text-ellipsis(1);
}

.dashboard__notice-time {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}
</style>
