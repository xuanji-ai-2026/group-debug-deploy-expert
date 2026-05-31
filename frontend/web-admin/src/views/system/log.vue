<template>
  <div class="log-page">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="query.keyword"
          placeholder="搜索操作人/操作内容"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
        <el-select v-model="query.module" placeholder="操作模块" clearable style="width: 150px" @change="handleQuery">
          <el-option label="用户管理" value="user" />
          <el-option label="租户管理" value="tenant" />
          <el-option label="账号管理" value="account" />
          <el-option label="商机管理" value="lead" />
          <el-option label="系统设置" value="system" />
          <el-option label="任务管理" value="task" />
          <el-option label="消息管理" value="message" />
          <el-option label="登录登出" value="auth" />
        </el-select>
        <el-select v-model="query.actionType" placeholder="操作类型" clearable style="width: 130px" @change="handleQuery">
          <el-option label="新增" value="create" />
          <el-option label="更新" value="update" />
          <el-option label="删除" value="delete" />
          <el-option label="查询" value="query" />
          <el-option label="登录" value="login" />
          <el-option label="登出" value="logout" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
          @change="handleDateRangeChange"
        />
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button :loading="exporting" @click="handleExport">导出日志</el-button>
      </div>
    </div>

    <!-- 日志列表 -->
    <el-table
      v-loading="loading"
      :data="logList"
      stripe
      style="margin-top: 16px"
      max-height="600"
      row-key="logId"
    >
      <el-table-column prop="logId" label="日志ID" width="100" />
      <el-table-column prop="operatorName" label="操作人" width="120">
        <template #default="{ row }">
          <span class="operator">{{ row.operatorName || row.operator }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="operatorIp" label="IP地址" width="140">
        <template #default="{ row }">
          <code>{{ row.operatorIp }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="module" label="模块" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ getModuleLabel(row.module) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="actionType" label="操作类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="getActionTypeTag(row.actionType)">
            {{ getActionLabel(row.actionType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="结果" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="耗时" width="90" align="center">
        <template #default="{ row }">
          <span :class="row.duration > 3000 ? 'text-danger' : ''">
            {{ row.duration }}ms
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="180" />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @change="loadLogList"
      />
    </div>

    <!-- 日志详情弹窗 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="750px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="日志ID">{{ currentLog.logId }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.operatorName || currentLog.operator }}</el-descriptions-item>
        <el-descriptions-item label="用户角色">{{ currentLog.roleName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">
          <code>{{ currentLog.operatorIp }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ getModuleLabel(currentLog.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag size="small" :type="getActionTypeTag(currentLog.actionType)">
            {{ getActionLabel(currentLog.actionType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求方法" :span="2">
          <code>{{ currentLog.requestMethod }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">
          <code>{{ currentLog.requestUrl }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ currentLog.description }}</el-descriptions-item>
        <el-descriptions-item label="执行结果" :span="2">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
          <span v-if="currentLog.status === 0" class="error-reason"> — {{ currentLog.errorMessage }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ currentLog.duration }}ms</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.createTime }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="detail-pre">{{ formatJson(currentLog.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果" :span="2">
          <pre class="detail-pre" :class="currentLog.status === 0 ? 'text-danger' : ''">{{ formatJson(currentLog.responseData) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 操作日志页面
 * 功能：操作日志查询、日志详情查看、日志导出
 * @author 张前端 (EMP-FE-001)
 */

import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { request } from '@/api'

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const exporting = ref(false)
const detailVisible = ref(false)
const currentLog = ref(null)
const logList = ref([])
const dateRange = ref(null)

const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const query = reactive({
  keyword: '',
  module: '',
  actionType: '',
  startDate: '',
  endDate: ''
})

// ============================================================
// 工具函数
// ============================================================
const moduleMap = {
  user: '用户管理', tenant: '租户管理', account: '账号管理',
  lead: '商机管理', system: '系统设置', task: '任务管理',
  message: '消息管理', auth: '登录登出', billing: '计费管理',
  risk: '风控管理', content: '内容管理', workflow: '工作流'
}

const actionMap = {
  create: '新增', update: '更新', delete: '删除',
  query: '查询', login: '登录', logout: '登出',
  export: '导出', import: '导入', enable: '启用',
  disable: '禁用', approve: '审核通过', reject: '审核拒绝'
}

function getModuleLabel(module) {
  return moduleMap[module] || module || '-'
}

function getActionLabel(action) {
  return actionMap[action] || action || '-'
}

function getActionTypeTag(action) {
  const map = {
    create: 'success', update: 'warning', delete: 'danger',
    query: 'info', login: 'primary', logout: 'default',
    export: 'info', import: 'success', enable: 'success', disable: 'warning'
  }
  return map[action] || 'info'
}

function formatJson(str) {
  if (!str) return '-'
  try {
    const obj = typeof str === 'string' ? JSON.parse(str) : str
    return JSON.stringify(obj, null, 2)
  } catch {
    return str
  }
}

// ============================================================
// 数据加载 (真实API调用)
// ============================================================
async function loadLogList() {
  loading.value = true
  try {
    const data = await request.get('/admin/system/log/operation/list', {
      page: page.value,
      pageSize: pageSize.value,
      ...query
    })
    logList.value = data?.list || []
    total.value = data?.pagination?.total || 0
    
    if (logList.value.length === 0 && page.value === 1) {
      console.log('暂无操作日志数据')
    }
  } catch (error) {
    console.error('加载操作日志失败:', error)
    logList.value = []
    total.value = 0
    ElMessage.error('加载操作日志失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  page.value = 1
  loadLogList()
}

function handleReset() {
  query.keyword = ''
  query.module = ''
  query.actionType = ''
  query.startDate = ''
  query.endDate = ''
  dateRange.value = null
  page.value = 1
  loadLogList()
}

function handleDateRangeChange(val) {
  if (val) {
    query.startDate = val[0]
    query.endDate = val[1]
  } else {
    query.startDate = ''
    query.endDate = ''
  }
}

function handleViewDetail(row) {
  currentLog.value = row
  detailVisible.value = true
}

async function handleExport() {
  exporting.value = true
  try {
    await post('/admin/system/log/operation/export', { ...query })
    ElMessage.success('导出任务已创建，请在导出记录中下载')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// ============================================================
// 初始化
// ============================================================
onMounted(() => {
  loadLogList()
})
</script>

<style lang="scss" scoped>
.log-page {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

.operator {
  color: #409eff;
  font-weight: 500;
}

code {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 1px 4px;
  border-radius: 3px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.text-danger { color: #f56c6c; }

.error-reason {
  color: #f56c6c;
  margin-left: 4px;
}

.detail-pre {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
  max-height: 200px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  color: #606266;
  margin: 0;

  &.text-danger {
    color: #f56c6c;
  }
}
</style>
