<template>
  <bx-content
    title="商机详情"
    description="查看客户详细信息和跟进记录"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <template #actions>
      <el-button @click="handleBack">
        <i class="el-icon-arrow-left" />
        返回列表
      </el-button>
      <el-button type="primary" @click="handleFollow">
        <i class="el-icon-chat-dot-round" />
        添加跟进
      </el-button>
    </template>

    <!-- 客户基本信息 -->
    <el-row :gutter="20" class="lead-detail__row">
      <el-col :xs="24" :lg="16">
        <el-card v-loading="loading" class="lead-detail__card">
          <template #header>
            <div class="lead-detail__card-header">
              <div class="lead-detail__customer-info">
                <el-avatar :size="60" :src="leadInfo.userAvatar">
                  {{ leadInfo.userNickname?.charAt(0) || '?' }}
                </el-avatar>
                <div class="lead-detail__customer-text">
                  <h3 class="lead-detail__customer-name">{{ leadInfo.userNickname || '未知客户' }}</h3>
                  <div class="lead-detail__customer-tags">
                    <el-tag :type="getSourceType(leadInfo.leadSource)" size="small">
                      {{ getSourceLabel(leadInfo.leadSource) }}
                    </el-tag>
                    <el-tag :type="getIntentType(leadInfo.intentLevel)" size="small" effect="dark">
                      {{ getIntentLabel(leadInfo.intentLevel) }}意向
                    </el-tag>
                    <el-tag :type="getStatusType(leadInfo.followStatus)" size="small">
                      {{ getStatusLabel(leadInfo.followStatus) }}
                    </el-tag>
                  </div>
                </div>
              </div>
              <div class="lead-detail__actions">
                <el-button type="primary" @click="handleAssign">
                  {{ leadInfo.assigneeId ? '转交' : '分配' }}
                </el-button>
                <el-dropdown @command="handleStatusChange">
                  <el-button type="warning">
                    变更状态
                    <i class="el-icon-arrow-down el-icon--right" />
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="deal">标记成交</el-dropdown-item>
                      <el-dropdown-item command="lost">标记流失</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </template>

          <div class="lead-detail__section">
            <h4 class="lead-detail__section-title">联系信息</h4>
            <el-row :gutter="20" class="lead-detail__info-grid">
              <el-col :xs="24" :sm="12">
                <div class="lead-detail__info-item">
                  <span class="lead-detail__info-label">客户ID</span>
                  <span class="lead-detail__info-value">{{ leadInfo.userId || '-' }}</span>
                </div>
              </el-col>
              <el-col :xs="24" :sm="12">
                <div class="lead-detail__info-item">
                  <span class="lead-detail__info-label">手机号</span>
                  <span class="lead-detail__info-value">{{ leadInfo.contactPhone || '-' }}</span>
                </div>
              </el-col>
              <el-col :xs="24" :sm="12">
                <div class="lead-detail__info-item">
                  <span class="lead-detail__info-label">微信号</span>
                  <span class="lead-detail__info-value">{{ leadInfo.contactWechat || '-' }}</span>
                </div>
              </el-col>
              <el-col :xs="24" :sm="12">
                <div class="lead-detail__info-item">
                  <span class="lead-detail__info-label">来源平台</span>
                  <span class="lead-detail__info-value">{{ leadInfo.platformName || '-' }}</span>
                </div>
              </el-col>
            </el-row>
          </div>

          <el-divider />

          <div class="lead-detail__section">
            <h4 class="lead-detail__section-title">客户标签</h4>
            <div class="lead-detail__tags">
              <el-tag
                v-for="(tag, index) in leadInfo.tags"
                :key="index"
                closable
                @close="handleRemoveTag(tag)"
              >
                {{ tag }}
              </el-tag>
              <el-input
                v-if="inputTagVisible"
                ref="tagInputRef"
                v-model="inputTagValue"
                class="lead-detail__tag-input"
                size="small"
                @keyup.enter="handleAddTag"
                @blur="handleAddTag"
              />
              <el-button v-else size="small" @click="showTagInput">
                <i class="el-icon-plus" />
                添加标签
              </el-button>
            </div>
          </div>

          <el-divider />

          <div class="lead-detail__section">
            <h4 class="lead-detail__section-title">备注</h4>
            <el-input
              v-model="leadInfo.remark"
              type="textarea"
              :rows="3"
              placeholder="添加客户备注..."
            />
            <div class="lead-detail__remark-action">
              <el-button type="primary" size="small" @click="handleSaveRemark">
                保存备注
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧信息 -->
      <el-col :xs="24" :lg="8">
        <!-- 跟进人信息 -->
        <el-card class="lead-detail__card">
          <template #header>
            <span class="lead-detail__card-title">跟进信息</span>
          </template>
          <div class="lead-detail__assignee">
            <div class="lead-detail__assignee-item">
              <span class="lead-detail__assignee-label">当前跟进人</span>
              <span class="lead-detail__assignee-value">
                {{ leadInfo.assigneeName || '未分配' }}
              </span>
            </div>
            <div class="lead-detail__assignee-item">
              <span class="lead-detail__assignee-label">最近跟进</span>
              <span class="lead-detail__assignee-value">
                {{ leadInfo.lastFollowTime || '从未跟进' }}
              </span>
            </div>
            <div class="lead-detail__assignee-item">
              <span class="lead-detail__assignee-label">跟进次数</span>
              <span class="lead-detail__assignee-value">{{ followList.length }} 次</span>
            </div>
            <div class="lead-detail__assignee-item">
              <span class="lead-detail__assignee-label">创建时间</span>
              <span class="lead-detail__assignee-value">{{ leadInfo.createTime || '-' }}</span>
            </div>
          </div>
        </el-card>

        <!-- 关联任务 -->
        <el-card class="lead-detail__card lead-detail__task-card">
          <template #header>
            <div class="lead-detail__card-header">
              <span class="lead-detail__card-title">关联任务</span>
              <el-button type="primary" link size="small" @click="handleViewTasks">
                查看全部
              </el-button>
            </div>
          </template>
          <div v-if="relatedTasks.length" class="lead-detail__task-list">
            <div
              v-for="task in relatedTasks"
              :key="task.id"
              class="lead-detail__task-item"
            >
              <div class="lead-detail__task-name">{{ task.taskName }}</div>
              <div class="lead-detail__task-meta">
                <el-tag :type="getTaskStatusType(task.status)" size="small">
                  {{ getTaskStatusLabel(task.status) }}
                </el-tag>
                <span class="lead-detail__task-time">{{ task.createTime }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无关联任务" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 跟进记录时间线 -->
    <el-card class="lead-detail__card lead-detail__timeline-card">
      <template #header>
        <span class="lead-detail__card-title">跟进记录</span>
      </template>
      <el-timeline v-if="followList.length">
        <el-timeline-item
          v-for="(follow, index) in followList"
          :key="index"
          :type="getFollowType(follow.followType)"
          :timestamp="follow.createTime"
          placement="top"
        >
          <el-card class="lead-detail__timeline-content">
            <div class="lead-detail__timeline-header">
              <span class="lead-detail__timeline-type">
                {{ getFollowTypeLabel(follow.followType) }}
              </span>
              <span class="lead-detail__timeline-author">{{ follow.operatorName }}</span>
            </div>
            <div class="lead-detail__timeline-body">
              <p>{{ follow.followContent }}</p>
              <p v-if="follow.followResult" class="lead-detail__timeline-result">
                结果：{{ follow.followResult }}
              </p>
              <p v-if="follow.nextFollowTime" class="lead-detail__timeline-next">
                下次跟进：{{ follow.nextFollowTime }}
              </p>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无跟进记录" />
    </el-card>

    <!-- 分配弹窗 -->
    <el-dialog
      v-model="assignDialogVisible"
      :title="leadInfo.assigneeId ? '转交商机' : '分配商机'"
      width="400px"
    >
      <el-form label-width="80px">
        <el-form-item label="选择人员">
          <el-select v-model="assignForm.assigneeId" placeholder="请选择跟进人" style="width: 100%">
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.realName"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 跟进弹窗 -->
    <el-dialog
      v-model="followDialogVisible"
      title="添加跟进记录"
      width="560px"
    >
      <el-form ref="followFormRef" :model="followForm" :rules="followRules" label-width="100px">
        <el-form-item label="跟进方式" prop="followType">
          <el-radio-group v-model="followForm.followType">
            <el-radio :label="1">电话</el-radio>
            <el-radio :label="2">微信</el-radio>
            <el-radio :label="3">私信</el-radio>
            <el-radio :label="4">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="跟进内容" prop="followContent">
          <el-input
            v-model="followForm.followContent"
            type="textarea"
            :rows="4"
            placeholder="请输入跟进内容"
          />
        </el-form-item>
        <el-form-item label="跟进结果">
          <el-input v-model="followForm.followResult" placeholder="请输入跟进结果" />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker
            v-model="followForm.nextFollowTime"
            type="datetime"
            placeholder="选择下次跟进时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="followLoading" @click="handleFollowSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 成交弹窗 -->
    <el-dialog
      v-model="dealDialogVisible"
      title="标记成交"
      width="400px"
    >
      <el-form ref="dealFormRef" :model="dealForm" :rules="dealRules" label-width="100px">
        <el-form-item label="成交金额" prop="dealAmount">
          <el-input-number v-model="dealForm.dealAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="成交时间" prop="dealTime">
          <el-date-picker
            v-model="dealForm.dealTime"
            type="datetime"
            placeholder="选择成交时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dealDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="statusLoading" @click="handleDealSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 流失弹窗 -->
    <el-dialog
      v-model="lostDialogVisible"
      title="标记流失"
      width="400px"
    >
      <el-form ref="lostFormRef" :model="lostForm" :rules="lostRules" label-width="100px">
        <el-form-item label="流失原因" prop="lostReason">
          <el-input
            v-model="lostForm.lostReason"
            type="textarea"
            :rows="3"
            placeholder="请输入流失原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lostDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="statusLoading" @click="handleLostSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview LeadDetail 商机详情页
 * @description 展示客户详细信息和跟进记录时间线
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as leadApi from '@/api/lead.js'
import * as userApi from '@/api/user.js'

// ============================================================
// Router
// ============================================================
const router = useRouter()
const route = useRoute()
const leadId = route.params.id

// ============================================================
// 状态
// ============================================================
const loading = ref(false)

// 商机信息
const leadInfo = reactive({
  id: null,
  userId: '',
  userNickname: '',
  userAvatar: '',
  contactPhone: '',
  contactWechat: '',
  leadSource: 1,
  platformName: '',
  intentLevel: 1,
  followStatus: 0,
  assigneeId: null,
  assigneeName: '',
  lastFollowTime: '',
  createTime: '',
  tags: [],
  remark: '',
})

// 跟进记录
const followList = ref([])

// 关联任务
const relatedTasks = ref([])

// 标签输入
const inputTagVisible = ref(false)
const inputTagValue = ref('')
const tagInputRef = ref(null)

// 分配弹窗
const assignDialogVisible = ref(false)
const assignLoading = ref(false)
const assignForm = reactive({ assigneeId: null })
const userList = ref([])

// 跟进弹窗
const followDialogVisible = ref(false)
const followLoading = ref(false)
const followFormRef = ref(null)
const followForm = reactive({
  followType: 1,
  followContent: '',
  followResult: '',
  nextFollowTime: '',
})
const followRules = {
  followType: [{ required: true, message: '请选择跟进方式', trigger: 'change' }],
  followContent: [{ required: true, message: '请输入跟进内容', trigger: 'blur' }],
}

// 状态变更弹窗
const dealDialogVisible = ref(false)
const lostDialogVisible = ref(false)
const statusLoading = ref(false)
const dealFormRef = ref(null)
const lostFormRef = ref(null)
const dealForm = reactive({ dealAmount: 0, dealTime: '' })
const lostForm = reactive({ lostReason: '' })
const dealRules = {
  dealAmount: [{ required: true, message: '请输入成交金额', trigger: 'blur' }],
  dealTime: [{ required: true, message: '请选择成交时间', trigger: 'change' }],
}
const lostRules = {
  lostReason: [{ required: true, message: '请输入流失原因', trigger: 'blur' }],
}

// 映射
const sourceMap = {
  1: { label: '同业截客', type: 'primary' },
  2: { label: '主动获客', type: 'success' },
  3: { label: '手动导入', type: 'info' },
}

const intentMap = {
  1: { label: '高', type: 'danger' },
  2: { label: '中', type: 'warning' },
  3: { label: '低', type: 'info' },
}

const statusMap = {
  0: { label: '未跟进', type: 'info' },
  1: { label: '跟进中', type: 'primary' },
  2: { label: '已成交', type: 'success' },
  3: { label: '已流失', type: 'danger' },
}

const followTypeMap = {
  1: { label: '电话', type: 'primary' },
  2: { label: '微信', type: 'success' },
  3: { label: '私信', type: 'warning' },
  4: { label: '其他', type: 'info' },
}

const taskStatusMap = {
  0: { label: '待开始', type: 'info' },
  1: { label: '进行中', type: 'primary' },
  2: { label: '已暂停', type: 'warning' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'danger' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载商机详情
 */
async function loadLeadDetail() {
  loading.value = true
  try {
    const response = await leadApi.getLeadDetail(leadId)
    Object.assign(leadInfo, response.data || {})
  } catch (error) {
    console.error('[LeadDetail] Load lead failed:', error)
    // 模拟数据
    Object.assign(leadInfo, {
      id: leadId,
      userId: 'U123456',
      userNickname: '李老板',
      contactPhone: '13800138000',
      contactWechat: 'liboss123',
      leadSource: 1,
      platformName: '抖音',
      intentLevel: 1,
      followStatus: 1,
      assigneeId: 1,
      assigneeName: '张三',
      lastFollowTime: '2024-01-15 10:30:00',
      createTime: '2024-01-10 09:00:00',
      tags: ['高价值', '意向明确', '急需成交'],
      remark: '客户对产品有明确需求，预算充足，需要尽快跟进',
    })
  } finally {
    loading.value = false
  }
}

/**
 * 加载跟进记录
 */
async function loadFollowList() {
  try {
    const response = await leadApi.getLeadFollowList(leadId)
    followList.value = response.data || []
  } catch (error) {
    // 模拟数据
    followList.value = [
      {
        followType: 1,
        followContent: '电话联系客户，介绍了产品功能和价格，客户表示感兴趣，需要再考虑一下',
        followResult: '客户有意向，需要发送详细资料',
        nextFollowTime: '2024-01-16 14:00:00',
        operatorName: '张三',
        createTime: '2024-01-15 10:30:00',
      },
      {
        followType: 2,
        followContent: '添加客户微信，发送了产品介绍和案例',
        operatorName: '张三',
        createTime: '2024-01-12 16:45:00',
      },
    ]
  }
}

/**
 * 加载关联任务
 */
async function loadRelatedTasks() {
  // 模拟数据
  relatedTasks.value = [
    { id: 1, taskName: '抖音截客任务A', status: 1, createTime: '2024-01-10' },
    { id: 2, taskName: '小红书获客任务B', status: 3, createTime: '2024-01-08' },
  ]
}

/**
 * 加载用户列表
 */
async function loadUserList() {
  try {
    const response = await userApi.getUserList({ size: 100 })
    userList.value = response.data?.list || []
  } catch (error) {
    userList.value = [
      { id: 1, realName: '张三' },
      { id: 2, realName: '李四' },
      { id: 3, realName: '王五' },
    ]
  }
}

// 获取显示方法
function getSourceLabel(source) { return sourceMap[source]?.label || '-' }
function getSourceType(source) { return sourceMap[source]?.type || 'info' }
function getIntentLabel(level) { return intentMap[level]?.label || '-' }
function getIntentType(level) { return intentMap[level]?.type || 'info' }
function getStatusLabel(status) { return statusMap[status]?.label || '-' }
function getStatusType(status) { return statusMap[status]?.type || 'info' }
function getFollowTypeLabel(type) { return followTypeMap[type]?.label || '-' }
function getFollowType(type) { return followTypeMap[type]?.type || 'info' }
function getTaskStatusLabel(status) { return taskStatusMap[status]?.label || '-' }
function getTaskStatusType(status) { return taskStatusMap[status]?.type || 'info' }

/**
 * 返回列表
 */
function handleBack() {
  router.push('/lead/list')
}

/**
 * 显示标签输入
 */
function showTagInput() {
  inputTagVisible.value = true
  nextTick(() => {
    tagInputRef.value?.focus()
  })
}

/**
 * 添加标签
 */
async function handleAddTag() {
  if (inputTagValue.value) {
    if (!leadInfo.tags.includes(inputTagValue.value)) {
      leadInfo.tags.push(inputTagValue.value)
      // 保存到后端
      await saveTags()
    }
  }
  inputTagVisible.value = false
  inputTagValue.value = ''
}

/**
 * 移除标签
 */
async function handleRemoveTag(tag) {
  leadInfo.tags = leadInfo.tags.filter(t => t !== tag)
  await saveTags()
}

/**
 * 保存标签
 */
async function saveTags() {
  try {
    await leadApi.updateLead(leadId, { tags: leadInfo.tags })
  } catch (error) {
    console.error('[LeadDetail] Save tags failed:', error)
  }
}

/**
 * 保存备注
 */
async function handleSaveRemark() {
  try {
    await leadApi.updateLead(leadId, { remark: leadInfo.remark })
    ElMessage.success('备注保存成功')
  } catch (error) {
    console.error('[LeadDetail] Save remark failed:', error)
    ElMessage.error('保存失败')
  }
}

/**
 * 打开分配弹窗
 */
function handleAssign() {
  assignForm.assigneeId = leadInfo.assigneeId || null
  assignDialogVisible.value = true
  loadUserList()
}

/**
 * 提交分配
 */
async function handleAssignSubmit() {
  if (!assignForm.assigneeId) {
    ElMessage.warning('请选择跟进人')
    return
  }
  assignLoading.value = true
  try {
    await leadApi.assignLead(leadId, assignForm.assigneeId)
    ElMessage.success('分配成功')
    assignDialogVisible.value = false
    loadLeadDetail()
  } catch (error) {
    console.error('[LeadDetail] Assign failed:', error)
    ElMessage.error('分配失败')
  } finally {
    assignLoading.value = false
  }
}

/**
 * 打开跟进弹窗
 */
function handleFollow() {
  followForm.followType = 1
  followForm.followContent = ''
  followForm.followResult = ''
  followForm.nextFollowTime = ''
  followDialogVisible.value = true
}

/**
 * 提交跟进
 */
async function handleFollowSubmit() {
  if (!followFormRef.value) return
  const valid = await followFormRef.value.validate().catch(() => false)
  if (!valid) return

  followLoading.value = true
  try {
    await leadApi.addLeadFollow(leadId, followForm)
    ElMessage.success('跟进记录添加成功')
    followDialogVisible.value = false
    loadFollowList()
    loadLeadDetail()
  } catch (error) {
    console.error('[LeadDetail] Follow failed:', error)
    ElMessage.error('添加失败')
  } finally {
    followLoading.value = false
  }
}

/**
 * 状态变更
 */
function handleStatusChange(command) {
  if (command === 'deal') {
    dealForm.dealAmount = 0
    dealForm.dealTime = new Date().toISOString().slice(0, 19).replace('T', ' ')
    dealDialogVisible.value = true
  } else if (command === 'lost') {
    lostForm.lostReason = ''
    lostDialogVisible.value = true
  }
}

/**
 * 提交成交
 */
async function handleDealSubmit() {
  if (!dealFormRef.value) return
  const valid = await dealFormRef.value.validate().catch(() => false)
  if (!valid) return

  statusLoading.value = true
  try {
    await leadApi.updateLeadStatus(leadId, {
      followStatus: 2,
      dealAmount: dealForm.dealAmount,
      dealTime: dealForm.dealTime,
    })
    ElMessage.success('已标记为成交')
    dealDialogVisible.value = false
    loadLeadDetail()
  } catch (error) {
    console.error('[LeadDetail] Deal failed:', error)
    ElMessage.error('操作失败')
  } finally {
    statusLoading.value = false
  }
}

/**
 * 提交流失
 */
async function handleLostSubmit() {
  if (!lostFormRef.value) return
  const valid = await lostFormRef.value.validate().catch(() => false)
  if (!valid) return

  statusLoading.value = true
  try {
    await leadApi.updateLeadStatus(leadId, {
      followStatus: 3,
      lostReason: lostForm.lostReason,
    })
    ElMessage.success('已标记为流失')
    lostDialogVisible.value = false
    loadLeadDetail()
  } catch (error) {
    console.error('[LeadDetail] Lost failed:', error)
    ElMessage.error('操作失败')
  } finally {
    statusLoading.value = false
  }
}

/**
 * 查看任务
 */
function handleViewTasks() {
  router.push('/lead/task')
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadLeadDetail()
  loadFollowList()
  loadRelatedTasks()
})
</script>

<style lang="scss" scoped>
.lead-detail {
  &__row {
    margin-bottom: $spacing-lg;
  }

  &__card {
    margin-bottom: $spacing-lg;
  }

  &__card-header {
    @include flex-between;
    flex-wrap: wrap;
    gap: $spacing-md;
  }

  &__card-title {
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
  }

  &__customer-info {
    display: flex;
    align-items: center;
    gap: $spacing-md;
  }

  &__customer-text {
    display: flex;
    flex-direction: column;
    gap: $spacing-xs;
  }

  &__customer-name {
    margin: 0;
    font-size: $font-size-xl;
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
  }

  &__customer-tags {
    display: flex;
    gap: $spacing-xs;
  }

  &__actions {
    display: flex;
    gap: $spacing-sm;
  }

  &__section {
    margin-bottom: $spacing-lg;

    &:last-child {
      margin-bottom: 0;
    }
  }

  &__section-title {
    margin: 0 0 $spacing-md;
    font-size: $font-size-base;
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
  }

  &__info-grid {
    margin: 0 -$spacing-sm;
  }

  &__info-item {
    display: flex;
    padding: $spacing-sm;
    background: $color-gray-50;
    border-radius: $radius-md;
    margin-bottom: $spacing-sm;
  }

  &__info-label {
    width: 80px;
    flex-shrink: 0;
    color: $color-text-secondary;
    font-size: $font-size-sm;
  }

  &__info-value {
    flex: 1;
    color: $color-text-primary;
    font-weight: $font-weight-medium;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-xs;
    align-items: center;
  }

  &__tag-input {
    width: 100px;
  }

  &__remark-action {
    margin-top: $spacing-sm;
    text-align: right;
  }

  &__assignee {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }

  &__assignee-item {
    display: flex;
    justify-content: space-between;
    padding-bottom: $spacing-sm;
    border-bottom: 1px dashed $color-border-light;

    &:last-child {
      border-bottom: none;
      padding-bottom: 0;
    }
  }

  &__assignee-label {
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__assignee-value {
    font-size: $font-size-sm;
    font-weight: $font-weight-medium;
    color: $color-text-primary;
  }

  &__task-card {
    margin-top: $spacing-lg;
  }

  &__task-list {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }

  &__task-item {
    padding: $spacing-md;
    background: $color-gray-50;
    border-radius: $radius-md;
  }

  &__task-name {
    font-weight: $font-weight-medium;
    color: $color-text-primary;
    margin-bottom: $spacing-xs;
  }

  &__task-meta {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  &__task-time {
    font-size: $font-size-xs;
    color: $color-text-secondary;
  }

  &__timeline-card {
    .el-timeline {
      padding-left: $spacing-sm;
    }
  }

  &__timeline-content {
    margin-left: $spacing-sm;
  }

  &__timeline-header {
    @include flex-between;
    margin-bottom: $spacing-sm;
  }

  &__timeline-type {
    font-weight: $font-weight-medium;
    color: $color-primary;
  }

  &__timeline-author {
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }

  &__timeline-body {
    p {
      margin: 0 0 $spacing-xs;
      color: $color-text-primary;
      line-height: 1.6;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }

  &__timeline-result {
    font-size: $font-size-sm;
    color: $color-success;
  }

  &__timeline-next {
    font-size: $font-size-sm;
    color: $color-warning;
  }
}
</style>