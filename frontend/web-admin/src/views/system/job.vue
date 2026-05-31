<template>
  <div class="job-page">
    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="job-tabs">
      <!-- 定时任务列表 -->
      <el-tab-pane label="定时任务" name="job">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="jobQuery.keyword" placeholder="任务名称/Bean名称" clearable style="width: 200px" @keyup.enter="loadJobList" />
            <el-select v-model="jobQuery.status" placeholder="任务状态" clearable style="width: 150px" @change="loadJobList">
              <el-option label="运行中" :value="1" />
              <el-option label="已停止" :value="0" />
            </el-select>
            <el-button type="primary" @click="loadJobList">查询</el-button>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" @click="handleAddJob">
              <el-icon><Plus /></el-icon> 新增任务
            </el-button>
            <el-button @click="loadJobList">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>

        <el-table v-loading="jobLoading" :data="jobList" stripe style="margin-top: 16px" row-key="jobId">
          <el-table-column prop="jobId" label="任务ID" width="80" />
          <el-table-column prop="jobName" label="任务名称" min-width="150" />
          <el-table-column prop="beanName" label="Bean名称" min-width="150">
            <template #default="{ row }"><code>{{ row.beanName }}</code></template>
          </el-table-column>
          <el-table-column prop="methodName" label="执行方法" width="120">
            <template #default="{ row }"><code>{{ row.methodName }}</code></template>
          </el-table-column>
          <el-table-column prop="cronExpression" label="Cron表达式" width="130">
            <template #default="{ row }"><code class="cron">{{ row.cronExpression }}</code></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '运行中' : '已停止' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="concurrent" label="并发" width="70" align="center">
            <template #default="{ row }">
              <el-tag :type="row.concurrent === 1 ? 'warning' : 'default'" size="small">
                {{ row.concurrent === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastRunTime" label="上次执行" width="160" />
          <el-table-column prop="nextRunTime" label="下次执行" width="160" />
          <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleEditJob(row)">编辑</el-button>
              <el-button link type="success" size="small" :disabled="row.status === 1" @click="handleStartJob(row)">启动</el-button>
              <el-button link type="warning" size="small" :disabled="row.status === 0" @click="handleStopJob(row)">停止</el-button>
              <el-button link type="danger" size="small" @click="handleDeleteJob(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="jobPage"
            v-model:page-size="jobPageSize"
            :total="jobTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="loadJobList"
          />
        </div>
      </el-tab-pane>

      <!-- 任务执行日志 -->
      <el-tab-pane label="执行日志" name="log">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-select v-model="logQuery.jobId" placeholder="选择任务" clearable style="width: 200px" @change="loadJobLog">
              <el-option v-for="j in jobList" :key="j.jobId" :label="j.jobName" :value="j.jobId" />
            </el-select>
            <el-select v-model="logQuery.status" placeholder="执行状态" clearable style="width: 130px" @change="loadJobLog">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
            <el-date-picker
              v-model="logDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 240px"
              @change="handleDateRangeChange"
            />
            <el-button type="primary" @click="loadJobLog">查询</el-button>
          </div>
          <div class="toolbar-right">
            <el-button :loading="exporting" @click="handleExportLog">导出日志</el-button>
          </div>
        </div>

        <el-table v-loading="logLoading" :data="logList" stripe style="margin-top: 16px" max-height="500">
          <el-table-column prop="logId" label="日志ID" width="90" />
          <el-table-column prop="jobName" label="任务名称" min-width="150" />
          <el-table-column prop="beanName" label="Bean" min-width="130">
            <template #default="{ row }"><code>{{ row.beanName }}</code></template>
          </el-table-column>
          <el-table-column prop="methodName" label="方法" width="110">
            <template #default="{ row }"><code>{{ row.methodName }}</code></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="耗时" width="90" align="center">
            <template #default="{ row }">
              <span :class="row.duration > 5000 ? 'text-danger' : ''">{{ row.duration }}ms</span>
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" width="160" />
          <el-table-column prop="endTime" label="结束时间" width="160" />
          <el-table-column prop="errorMsg" label="错误信息" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.status === 0" class="text-danger">{{ row.errorMsg }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleViewLogDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="logPage"
            v-model:page-size="logPageSize"
            :total="logTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="loadJobLog"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑任务弹窗 -->
    <el-dialog
      v-model="jobDialogVisible"
      :title="jobForm.jobId ? '编辑定时任务' : '新增定时任务'"
      width="560px"
      @close="resetJobForm"
    >
      <el-form ref="jobFormRef" :model="jobForm" :rules="jobRules" label-width="110px">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="jobForm.jobName" placeholder="请输入任务名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="Bean名称" prop="beanName">
          <el-input v-model="jobForm.beanName" placeholder="Spring Bean名称，如: leadTaskJob" />
        </el-form-item>
        <el-form-item label="执行方法" prop="methodName">
          <el-input v-model="jobForm.methodName" placeholder="方法名，如: execute" />
        </el-form-item>
        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input v-model="jobForm.cronExpression" placeholder="如: 0 0 2 * * ?" />
        </el-form-item>
        <el-form-item label="并发执行">
          <el-switch v-model="jobForm.concurrent" :active-value="1" :inactive-value="0" />
          <span class="form-tip">关闭后，上次任务未完成时不会启动新任务</span>
        </el-form-item>
        <el-form-item label="任务状态">
          <el-switch v-model="jobForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="jobForm.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="jobDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="jobSubmitting" @click="submitJobForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 日志详情弹窗 -->
    <el-dialog v-model="logDetailVisible" title="执行日志详情" width="700px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="任务名称">{{ currentLog.jobName }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Bean名称"><code>{{ currentLog.beanName }}</code></el-descriptions-item>
        <el-descriptions-item label="执行方法"><code>{{ currentLog.methodName }}</code></el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentLog.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentLog.endTime }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.duration }}ms</el-descriptions-item>
        <el-descriptions-item label="执行参数">{{ currentLog.params || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行结果" :span="2">
          <div v-if="currentLog.status === 1" class="log-success">{{ currentLog.result || '执行成功' }}</div>
          <div v-else class="log-error">{{ currentLog.errorMsg }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentLog.stackTrace" label="堆栈信息" :span="2">
          <pre class="stack-trace">{{ currentLog.stackTrace }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 定时任务管理页面
 * 功能：定时任务CRUD、任务启动/停止、执行日志查询
 * @author 张前端 (EMP-FE-001)
 */

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { request } from '@/api'

// ============================================================
// 状态
// ============================================================
const activeTab = ref('job')
const jobLoading = ref(false)
const logLoading = ref(false)
const exporting = ref(false)
const jobList = ref([])
const logList = ref([])

const jobPage = ref(1)
const jobPageSize = ref(10)
const jobTotal = ref(0)
const jobQuery = reactive({ keyword: '', status: undefined })

const logPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)
const logQuery = reactive({ jobId: undefined, status: undefined, startDate: undefined, endDate: undefined })
const logDateRange = ref(null)

const jobDialogVisible = ref(false)
const jobSubmitting = ref(false)
const jobFormRef = ref(null)
const jobForm = reactive({
  jobId: '',
  jobName: '',
  beanName: '',
  methodName: '',
  cronExpression: '',
  concurrent: 0,
  status: 1,
  remark: ''
})
const jobRules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  beanName: [{ required: true, message: '请输入Bean名称', trigger: 'blur' }],
  methodName: [{ required: true, message: '请输入执行方法', trigger: 'blur' }],
  cronExpression: [
    { required: true, message: '请输入Cron表达式', trigger: 'blur' },
    { pattern: /^[\w\s\*\/\-,\,]+$/, message: 'Cron表达式格式不正确', trigger: 'blur' }
  ]
}

const logDetailVisible = ref(false)
const currentLog = ref(null)

// ============================================================
// 任务管理
// ============================================================
async function loadJobList() {
  jobLoading.value = true
  try {
    const data = await get('/admin/system/job/list', {
      page: jobPage.value,
      pageSize: jobPageSize.value,
      ...jobQuery
    })
    jobList.value = data?.list || []
    jobTotal.value = data?.pagination?.total || 0
  } catch (error) {
    console.error('加载任务列表失败:', error)
    jobList.value = []
    jobTotal.value = 0
  } finally {
    jobLoading.value = false
  }
}

function handleAddJob() {
  jobForm.jobId = ''
  jobDialogVisible.value = true
}

function handleEditJob(row) {
  Object.assign(jobForm, {
    jobId: row.jobId,
    jobName: row.jobName,
    beanName: row.beanName,
    methodName: row.methodName,
    cronExpression: row.cronExpression,
    concurrent: row.concurrent,
    status: row.status,
    remark: row.remark || ''
  })
  jobDialogVisible.value = true
}

async function submitJobForm() {
  const valid = await jobFormRef.value.validate().catch(() => false)
  if (!valid) return
  jobSubmitting.value = true
  try {
    if (jobForm.jobId) {
      await request.put(`/admin/system/job/update/${jobForm.jobId}`, jobForm)
    } else {
      await request.post('/admin/system/job/add', jobForm)
    }
    ElMessage.success('保存成功')
    jobDialogVisible.value = false
    loadJobList()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    jobSubmitting.value = false
  }
}

function resetJobForm() {
  jobFormRef.value?.resetFields()
  Object.assign(jobForm, { jobId: '', jobName: '', beanName: '', methodName: '', cronExpression: '', concurrent: 0, status: 1, remark: '' })
}

async function handleStartJob(row) {
  try {
    await request.post(`/admin/system/job/start/${row.jobId}`)
    ElMessage.success('任务已启动')
    loadJobList()
  } catch {
    ElMessage.error('启动失败')
  }
}

async function handleStopJob(row) {
  try {
    await request.post(`/admin/system/job/stop/${row.jobId}`)
    ElMessage.success('任务已停止')
    loadJobList()
  } catch {
    ElMessage.error('停止失败')
  }
}

async function handleDeleteJob(row) {
  await ElMessageBox.confirm(`确定删除任务"${row.jobName}"吗？`, '删除确认', { type: 'warning' })
  try {
    await request.post(`/admin/system/job/delete/${row.jobId}`)
    ElMessage.success('删除成功')
    loadJobList()
  } catch {
    ElMessage.error('删除失败')
  }
}

// ============================================================
// 日志管理
// ============================================================
async function loadJobLog() {
  logLoading.value = true
  try {
    const data = await request.get('/admin/system/job/logs', {
      page: logPage.value,
      pageSize: logPageSize.value,
      ...logQuery
    })
    logList.value = data?.list || []
    logTotal.value = data?.pagination?.total || 0
  } catch (error) {
    console.error('加载任务日志失败:', error)
    logList.value = []
    logTotal.value = 0
  } finally {
    logLoading.value = false
  }
}

function handleDateRangeChange(val) {
  if (val) {
    logQuery.startDate = val[0]
    logQuery.endDate = val[1]
  } else {
    logQuery.startDate = undefined
    logQuery.endDate = undefined
  }
}

function handleViewLogDetail(row) {
  currentLog.value = row
  logDetailVisible.value = true
}

async function handleExportLog() {
  exporting.value = true
  try {
    await post('/admin/system/job/logs/export', logQuery)
    ElMessage.success('导出任务已创建')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadJobList()
  loadJobLog()
})
</script>

<style lang="scss" scoped>
.job-page {
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
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

code {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 1px 4px;
  border-radius: 3px;
}

.cron {
  color: #e6a23c;
  background: #fdf6ec;
}

.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.text-danger { color: #f56c6c; }
.text-muted { color: #909399; }

.log-success {
  color: #67c23a;
  font-family: monospace;
  font-size: 13px;
  word-break: break-all;
}

.log-error {
  color: #f56c6c;
  font-family: monospace;
  font-size: 13px;
  word-break: break-all;
}

.stack-trace {
  max-height: 200px;
  overflow: auto;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #f56c6c;
}
</style>
