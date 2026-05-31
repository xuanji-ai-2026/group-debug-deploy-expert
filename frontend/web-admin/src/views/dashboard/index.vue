<template>
  <div class="dashboard-page">
    <!-- 欢迎区域 -->
    <div class="welcome-area">
      <div class="welcome-text">
        <h2>👋 欢迎回来，{{ nickname }}！</h2>
        <p>今天是 {{ currentDate }}，祝您工作愉快</p>
      </div>
      <div class="quick-stats">
        <div class="stat-item">
          <span class="stat-value">{{ stats.pendingAudit }}</span>
          <span class="stat-label">待审核</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.activeTenant }}</span>
          <span class="stat-label">活跃租户</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.todayRecharge }}</span>
          <span class="stat-label">今日充值(元)</span>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <el-row :gutter="20" class="quick-entry">
      <el-col :span="6" v-for="item in quickEntries" :key="item.path">
        <div class="entry-card" @click="goTo(item.path)">
          <el-icon :size="32" :style="{ color: item.color }">
            <component :is="item.icon" />
          </el-icon>
          <span class="entry-title">{{ item.title }}</span>
          <span class="entry-desc">{{ item.desc }}</span>
        </div>
      </el-col>
    </el-row>

    <!-- 数据统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-name">租户总数</span>
            <el-tag size="small" type="success">实时</el-tag>
          </div>
          <div class="stat-number">{{ stats.totalTenant }}</div>
          <div class="stat-trend up">
            <el-icon><Top /></el-icon>
            <span>+12% 较上月</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-name">本月消耗积分</span>
            <el-tag size="small" type="warning">本月</el-tag>
          </div>
          <div class="stat-number">{{ stats.monthConsume }}</div>
          <div class="stat-trend down">
            <el-icon><Bottom /></el-icon>
            <span>-8% 较上月</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-name">本月充值金额</span>
            <el-tag size="small" type="primary">本月</el-tag>
          </div>
          <div class="stat-number">¥{{ stats.monthRecharge }}</div>
          <div class="stat-trend up">
            <el-icon><Top /></el-icon>
            <span>+25% 较上月</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-header">
            <span class="stat-name">系统告警</span>
            <el-tag size="small" type="danger" v-if="stats.alarmCount > 0">告警</el-tag>
            <el-tag size="small" v-else>正常</el-tag>
          </div>
          <div class="stat-number">{{ stats.alarmCount }}</div>
          <div class="stat-trend neutral">
            <span>运行稳定</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 待办事项 & 最新动态 -->
    <el-row :gutter="20" class="bottom-section">
      <!-- 待办事项 -->
      <el-col :span="8">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">待办事项</span>
          </div>
          <div class="panel-body">
            <div v-if="todoList.length === 0" class="empty-state">
              <el-icon size="32" color="#c0c4cc"><SuccessFilled /></el-icon>
              <p>暂无待办事项</p>
            </div>
            <div
              v-for="item in todoList"
              :key="item.id"
              class="todo-item"
              @click="goTo(item.path)"
            >
              <div class="todo-left">
                <el-tag size="small" :type="item.type">{{ item.tag }}</el-tag>
                <span class="todo-text">{{ item.text }}</span>
              </div>
              <el-icon class="todo-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 近期公告 -->
      <el-col :span="8">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">系统公告</span>
            <el-button link type="primary" size="small" @click="goTo('/system/settings')">查看更多</el-button>
          </div>
          <div class="panel-body">
            <div v-for="notice in notices" :key="notice.id" class="notice-item">
              <div class="notice-title">{{ notice.title }}</div>
              <div class="notice-time">{{ notice.time }}</div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 活跃租户 TOP5 -->
      <el-col :span="8">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">活跃租户 TOP5</span>
            <el-button link type="primary" size="small" @click="goTo('/tenant/list')">全部</el-button>
          </div>
          <div class="panel-body">
            <div v-for="(tenant, index) in topTenants" :key="tenant.id" class="rank-item">
              <div class="rank-left">
                <span class="rank-num" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
                <span class="rank-name">{{ tenant.name }}</span>
              </div>
              <span class="rank-score">{{ tenant.consume }}积分</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * Dashboard - 控制台首页
 * 功能：展示平台核心数据统计、快捷入口、待办事项等
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/modules/admin'
import { request } from '@/api'

const router = useRouter()
const adminStore = useAdminStore()

// 当前用户昵称
const nickname = computed(() => adminStore.nickname)

// 当前日期
const currentDate = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${['周日','周一','周二','周三','周四','周五','周六'][d.getDay()]}`
})

// ============================================================
// 统计数据
// ============================================================
const stats = ref({
  totalTenant: 0,
  activeTenant: 0,
  pendingAudit: 0,
  monthConsume: 0,
  monthRecharge: 0,
  todayRecharge: 0,
  alarmCount: 0
})

// ============================================================
// 快捷入口
// ============================================================
const quickEntries = [
  { title: '租户列表', desc: '管理所有租户', path: '/tenant/list', icon: OfficeBuilding, color: '#1a73e8' },
  { title: '租户审核', desc: '审核入驻申请', path: '/tenant/audit', icon: DocumentChecked, color: '#faad14' },
  { title: '计费概览', desc: '查看财务数据', path: '/billing/overview', icon: Coin, color: '#52c41a' },
  { title: '系统设置', desc: '配置系统参数', path: '/system/settings', icon: Setting, color: '#ff4d4f' }
]

// ============================================================
// 待办事项
// ============================================================
const todoList = ref([
  { id: 1, text: '有 3 条租户审核待处理', path: '/tenant/audit', tag: '待审核', type: 'warning' },
  { id: 2, text: '2 条充值记录待确认', path: '/billing/overview', tag: '待确认', type: 'info' },
  { id: 3, text: '系统配置项已更新', path: '/system/settings', tag: '新公告', type: 'success' }
])

// ============================================================
// 系统公告
// ============================================================
const notices = ref([
  { id: 1, title: '系统将于本周日凌晨 2:00-4:00 进行维护', time: '2024-01-15' },
  { id: 2, title: '新增租户批量导入功能上线', time: '2024-01-12' },
  { id: 3, title: '计费规则调整通知', time: '2024-01-10' }
])

// ============================================================
// TOP5 活跃租户
// ============================================================
const topTenants = ref([
  { id: 1, name: '示例科技有限公司', consume: '12,580' },
  { id: 2, name: '深圳市XX贸易公司', consume: '9,240' },
  { id: 3, name: '北京XX文化传媒', consume: '8,160' },
  { id: 4, name: '杭州XX网络科技', consume: '6,820' },
  { id: 5, name: '广州XX品牌管理', consume: '5,430' }
])

// ============================================================
// 路由跳转
// ============================================================
function goTo(path) {
  router.push(path)
}

// ============================================================
// 数据加载
// ============================================================
async function loadStats() {
  try {
    const data = await request.get('/admin/dashboard/stats')
    stats.value = data
  } catch (error) {
    // 使用默认值（演示数据）
    stats.value = {
      totalTenant: 128,
      activeTenant: 96,
      pendingAudit: 3,
      monthConsume: '856,420',
      monthRecharge: '128,600',
      todayRecharge: '3,250',
      alarmCount: 0
    }
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  padding: 0;
}

// 欢迎区域
.welcome-area {
  background: linear-gradient(135deg, $primary-color, #4a9eff);
  border-radius: $border-radius-large;
  padding: 24px 32px;
  margin-bottom: $spacing-base;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #ffffff;
}

.welcome-text {
  h2 {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 4px;
  }
  p {
    font-size: $font-size-base;
    opacity: 0.85;
  }
}

.quick-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  text-align: center;

  .stat-value {
    display: block;
    font-size: 28px;
    font-weight: 700;
  }

  .stat-label {
    font-size: $font-size-small;
    opacity: 0.85;
  }
}

// 快捷入口
.quick-entry {
  margin-bottom: $spacing-base;
}

.entry-card {
  background: $bg-color;
  border-radius: $border-radius-base;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: $transition-base;
  border: 1px solid $border-color-light;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-base;
  }

  .entry-title {
    display: block;
    margin-top: 8px;
    font-weight: 600;
    color: $text-primary;
  }

  .entry-desc {
    display: block;
    margin-top: 4px;
    font-size: $font-size-small;
    color: $text-secondary;
  }
}

// 统计卡片
.stats-cards {
  margin-bottom: $spacing-base;
}

.stat-card {
  background: $bg-color;
  border-radius: $border-radius-base;
  padding: 20px;
  border: 1px solid $border-color-light;

  .stat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .stat-name {
    font-size: $font-size-base;
    color: $text-secondary;
  }

  .stat-number {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 8px;
  }

  .stat-trend {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: $font-size-small;

    &.up { color: $success-color; }
    &.down { color: $danger-color; }
    &.neutral { color: $text-secondary; }
  }
}

// 底部面板
.bottom-section {
  margin-bottom: $spacing-base;
}

.panel {
  background: $bg-color;
  border-radius: $border-radius-base;
  border: 1px solid $border-color-light;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid $border-color-light;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .panel-title {
    font-weight: 600;
    color: $text-primary;
  }
}

.panel-body {
  padding: 8px 0;
}

// 待办事项
.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  transition: $transition-fast;

  &:hover {
    background-color: $bg-color-page;
  }
}

.todo-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-text {
  font-size: $font-size-base;
  color: $text-primary;
}

// 公告
.notice-item {
  padding: 12px 20px;
  border-bottom: 1px solid $border-color-lighter;

  &:last-child { border-bottom: none; }

  .notice-title {
    font-size: $font-size-base;
    color: $text-primary;
    margin-bottom: 4px;
  }

  .notice-time {
    font-size: $font-size-small;
    color: $text-secondary;
  }
}

// 排名
.rank-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;

  &:hover { background-color: $bg-color-page; }
}

.rank-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rank-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: $bg-color-page;
  color: $text-secondary;

  &.rank-1 { background: #fff7e6; color: #fa8c16; }
  &.rank-2 { background: #f5f5f5; color: #8c8c8c; }
  &.rank-3 { background: #fff1e6; color: #ad6800; }
}

.rank-name {
  font-size: $font-size-base;
  color: $text-primary;
}

.rank-score {
  font-size: $font-size-small;
  color: $text-secondary;
}

.empty-state {
  padding: 32px 0;
  text-align: center;
  color: $text-secondary;

  p { margin-top: 8px; font-size: $font-size-base; }
}
</style>
