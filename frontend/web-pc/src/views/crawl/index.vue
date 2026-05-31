<template>
  <bx-content
    title="评论抓取与商机挖掘"
    description="智能抓取多平台评论，AI分析用户意向，自动生成商机"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <div class="crawl-management">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 抓取任务 -->
        <el-tab-pane label="📡 抓取任务" name="tasks">
          <div class="crawl-tasks">
            <!-- 创建任务 -->
            <el-card class="create-task-card" shadow="hover">
              <template #header>
                <div class="card-header">
                  <span>新建抓取任务</span>
                  <el-button type="primary" size="small" @click="showCreateDialog = true">
                    <el-icon><Plus /></el-icon> 创建任务
                  </el-button>
                </div>
              </template>

              <el-form :model="taskForm" label-width="100px" size="default">
                <el-row :gutter="20">
                  <el-col :span="8">
                    <el-form-item label="目标平台">
                      <el-select v-model="taskForm.platformCode" placeholder="选择平台" style="width: 100%">
                        <el-option label="抖音" value="DOUYIN">
                          <span style="display: flex; align-items: center; gap: 8px">
                            <span class="platform-icon platform-douyin">抖</span> 抖音
                          </span>
                        </el-option>
                        <el-option label="小红书" value="XIAOHONGSHU">
                          <span style="display: flex; align-items: center; gap: 8px">
                            <span class="platform-icon platform-xhs">红</span> 小红书
                          </span>
                        </el-option>
                        <el-option label="快手" value="KUAISHOU">
                          <span style="display: flex; align-items: center; gap: 8px">
                            <span class="platform-icon platform-kuaishou">快</span> 快手
                          </span>
                        </el-option>
                        <el-option label="微博" value="WEIBO">
                          <span style="display: flex; align-items: center; gap: 8px">
                            <span class="platform-icon platform-weibo">微</span> 微博
                          </span>
                        </el-option>
                        <el-option label="B站" value="BILIBILI">
                          <span style="display: flex; align-items: center; gap: 8px">
                            <span class="platform-icon platform-bilibili">B</span> B站
                          </span>
                        </el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>

                  <el-col :span="8">
                    <el-form-item label="目标类型">
                      <el-select v-model="taskForm.targetType" placeholder="选择类型" style="width: 100%">
                        <el-option label="视频/笔记ID" value="VIDEO_NOTE" />
                        <el-option label="账号主页" value="USER_PROFILE" />
                        <el-option label="话题/标签" value="TOPIC" />
                        <el-option label="搜索关键词" value="SEARCH_KEYWORD" />
                      </el-select>
                    </el-form-item>
                  </el-col>

                  <el-col :span="8">
                    <el-form-item label="目标ID/URL">
                      <el-input 
                        v-model="taskForm.targetId" 
                        placeholder="输入视频ID、笔记链接或搜索词"
                        clearable
                      />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="关键词过滤">
                      <el-input 
                        v-model="taskForm.keywords" 
                        type="textarea"
                        :rows="2"
                        placeholder="输入要筛选的关键词，多个用逗号分隔&#10;例如：购买,咨询,报价,合作"
                      />
                    </el-form-item>
                  </el-col>

                  <el-col :span="6">
                    <el-form-item label="最大抓取数">
                      <el-input-number v-model="taskForm.maxCrawlCount" :min="10" :max="10000" :step="100" />
                    </el-form-item>
                  </el-col>

                  <el-col :span="6">
                    <el-form-item label="抓取间隔(秒)">
                      <el-input-number v-model="taskForm.crawlIntervalSeconds" :min="1" :max="60" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-button type="primary" @click="createTask" :loading="creating">
                  <el-icon><VideoPlay /></el-icon> 开始抓取
                </el-button>
              </el-form>
            </el-card>

            <!-- 任务列表 -->
            <el-card class="task-list-card" shadow="hover" style="margin-top: 20px;">
              <template #header>
                <div class="card-header">
                  <span>历史任务</span>
                  <el-tag type="info" size="small">{{ taskList.length }} 个任务</el-tag>
                </div>
              </template>

              <el-table :data="taskList" v-loading="loadingTasks" stripe>
                <el-table-column prop="id" label="ID" width="70" />

                <el-table-column prop="taskName" label="任务名称" min-width="150">
                  <template #default="{ row }">
                    <el-link type="primary" @click="viewTaskDetail(row)">{{ row.taskName }}</el-link>
                  </template>
                </el-table-column>

                <el-table-column label="平台" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getPlatformType(row.platformCode)" size="small">
                      {{ getPlatformName(row.platformCode) }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status)" size="small" effect="dark">
                      {{ getStatusName(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="进度" width="180">
                  <template #default="{ row }">
                    <el-progress 
                      :percentage="row.progressPercent || 0" 
                      :status="row.status === 3 ? 'exception' : ''"
                      :stroke-width="15"
                    />
                    <div style="font-size: 12px; color: #999; margin-top: 4px;">
                      {{ row.totalCommentsFound || 0 }}条评论 | 
                      {{ row.highIntentCount || 0 }}高意向 | 
                      {{ row.leadsGenerated || 0 }}商机
                    </div>
                  </template>
                </el-table-column>

                <el-table-column prop="createTime" label="创建时间" width="170" />

                <el-table-column label="操作" width="250" fixed="right">
                  <template #default="{ row }">
                    <el-button-group size="small">
                      <el-button 
                        v-if="row.status === 1" 
                        type="warning" 
                        @click="pauseTask(row)"
                      >暂停</el-button>
                      
                      <el-button 
                        v-if="row.status === 1 || row.status === 0" 
                        type="danger" 
                        @click="stopTask(row)"
                      >停止</el-button>

                      <el-button 
                        v-if="row.status === 2" 
                        type="primary" 
                        @click="analyzeComments(row)"
                      >AI分析</el-button>

                      <el-button 
                        v-if="row.status === 2 && (row.highIntentCount || 0) > 0" 
                        type="success" 
                        @click="generateLeads(row)"
                      >生成商机</el-button>

                      <el-button 
                        @click="viewComments(row)"
                      >查看评论</el-button>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-tab-pane>

        <!-- 评论列表 -->
        <el-tab-pane label="💬 高意向评论" name="comments">
          <div class="comment-list">
            <!-- 筛选条件 -->
            <el-card shadow="hover" style="margin-bottom: 20px;">
              <el-form :inline="true" :model="filterForm" size="default">
                <el-form-item label="平台">
                  <el-select v-model="filterForm.platformCode" placeholder="全部平台" clearable style="width: 120px">
                    <el-option label="抖音" value="DOUYIN" />
                    <el-option label="小红书" value="XIAOHONGSHU" />
                    <el-option label="快手" value="KUAISHOU" />
                    <el-option label="微博" value="WEIBO" />
                  </el-select>
                </el-form-item>

                <el-form-item label="意向等级">
                  <el-select v-model="filterForm.intentLevel" placeholder="全部等级" clearable style="width: 120px">
                    <el-option label="A级(超高)" value="A" />
                    <el-option label="B级(高)" value="B" />
                    <el-option label="C级(中)" value="C" />
                    <el-option label="D级(低)" value="D" />
                  </el-select>
                </el-form-item>

                <el-form-item label="最低分">
                  <el-input-number v-model="filterForm.minScore" :min="0" :max="100" style="width: 100px" />
                </el-form-item>

                <el-form-item label="留联系方式">
                  <el-switch v-model="filterForm.onlyWithContact" />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="filterComments">
                    <el-icon><Search /></el-icon> 筛选
                  </el-button>
                  
                  <el-button type="success" @click="batchSendMessage" :disabled="selectedComments.length === 0">
                    <el-icon><Message /></el-icon> 批量私信({{ selectedComments.length }})
                  </el-button>

                  <el-button type="warning" @click="batchGenerateLeads" :disabled="selectedComments.length === 0">
                    <el-icon><DocumentAdd /></el-icon> 批量生成商机
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- 统计卡片 -->
            <el-row :gutter="16" style="margin-bottom: 20px;">
              <el-col :span="6">
                <el-card shadow="hover" class="stat-card stat-total">
                  <div class="stat-number">{{ statistics.totalCount }}</div>
                  <div class="stat-label">总评论数</div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card shadow="hover" class="stat-card stat-high-intent">
                  <div class="stat-number">{{ statistics.highIntentCount }}</div>
                  <div class="stat-label">高意向评论</div>
                </el-card>
              </el-col>

              <el-col :span="6">
                <el-card shadow="hover" class="stat-card stat-phone">
                  <div class="stat-number">{{ statistics.withPhoneCount }}</div>
                  <div class="stat-label">留电话</div>
                </el-card>
              </el-col>

              <el-col :span="6">
                <el-card shadow="hover" class="stat-card stat-wechat">
                  <div class="stat-number">{{ statistics.withWechatCount }}</div>
                  <div class="stat-label">留微信</div>
                </el-card>
              </el-col>
            </el-row>

            <!-- 评论列表表格 -->
            <el-card shadow="hover">
              <el-table 
                :data="filteredComments" 
                v-loading="loadingComments"
                @selection-change="handleSelectionChange"
                stripe
                max-height="600"
              >
                <el-table-column type="selection" width="50" />

                <el-table-column label="用户" min-width="150">
                  <template #default="{ row }">
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <el-avatar :size="36" :src="row.authorAvatar">
                        {{ (row.authorName || 'U').charAt(0) }}
                      </el-avatar>
                      <div>
                        <div style="font-weight: 500;">{{ row.authorName }}</div>
                        <div style="font-size: 12px; color: #999;">
                          {{ row.userFollowerCount ? formatNumber(row.userFollowerCount) + '粉丝' : '' }}
                          {{ row.userVerified ? ' ✓' : '' }}
                        </div>
                      </div>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column prop="commentText" label="评论内容" min-width="300">
                  <template #default="{ row }">
                    <div class="comment-text">
                      {{ row.commentText?.length > 100 ? row.commentText.substring(0, 100) + '...' : row.commentText }}
                      
                      <div v-if="row.hasPhoneContact || row.hasWechatContact" style="margin-top: 4px;">
                        <el-tag v-if="row.hasPhoneContact" type="danger" size="small" effect="dark">
                          📱 {{ row.extractedPhone }}
                        </el-tag>
                        <el-tag v-if="row.hasWechatContact" type="success" size="small" effect="dark">
                          💬 {{ row.extractedWechat }}
                        </el-tag>
                      </div>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column label="意向评分" width="110">
                  <template #default="{ row }">
                    <div :class="['score-badge', getScoreClass(row.aiIntentScore)]">
                      {{ row.aiIntentScore || '-' }}分
                    </div>
                    <el-tag 
                      v-if="row.aiIntentLevel" 
                      :type="getLevelType(row.aiIntentLevel)" 
                      size="small" 
                      style="margin-top: 4px;"
                    >
                      {{ row.aiIntentLevel }}级
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="互动数据" width="120">
                  <template #default="{ row }">
                    <div style="font-size: 12px;">
                      <div>❤️ {{ row.likeCount || 0 }} 赞</div>
                      <div>💬 {{ row.replyCount || 0 }} 回复</div>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column label="时间" width="160">
                  <template #default="{ row }">
                    {{ formatTime(row.publishTime) }}
                  </template>
                </el-table-column>

                <el-table-column label="操作" width="200" fixed="right">
                  <template #default="{ row }">
                    <el-button-group size="small">
                      <el-button type="primary" @click="sendMessage(row)">
                        私信
                      </el-button>
                      
                      <el-button type="success" @click="generateSingleLead(row)">
                        生成商机
                      </el-button>

                      <el-button @click="viewCommentDetail(row)">
                        详情
                      </el-button>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>

              <div style="margin-top: 16px; text-align: right;">
                <el-pagination
                  v-model:current-page="pagination.page"
                  v-model:page-size="pagination.size"
                  :total="statistics.filteredCount"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next"
                  @size-change="handleSizeChange"
                  @current-change="handlePageChange"
                />
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <!-- 私信模板 -->
        <el-tab-pane label="✉️ 私信模板" name="templates">
          <div class="message-templates">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header">
                  <span>私信模板库</span>
                  <div>
                    <el-button type="primary" size="small" @click="showTemplateDialog = true">
                      <el-icon><Plus /></el-icon> 新建模板
                    </el-button>
                    
                    <el-button type="success" size="small" @click="showAiGenerateDialog = true">
                      <el-icon><MagicStick /></el-icon> AI智能生成
                    </el-button>
                  </div>
                </div>
              </template>

              <el-tabs v-model="templateType">
                <el-tab-pane label="全部模板" name="all" />
                <el-tab-pane label="A级-超高意向" name="A" />
                <el-tab-pane label="B级-高意向" name="B" />
                <el-tab-pane label="C级-中意向" name="C" />
                <el-tab-pane label="自定义" name="CUSTOM" />
              </el-tabs>

              <el-row :gutter="16">
                <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="tpl in templateList" :key="tpl.id">
                  <el-card class="template-card" shadow="hover" style="margin-bottom: 16px;">
                    <div class="template-header">
                      <el-tag :type="getLevelType(tpl.intentLevel)" size="small">
                        {{ tpl.intentLevel }}级
                      </el-tag>
                      
                      <el-tag v-if="tpl.aiGenerated" type="warning" size="small" effect="dark">
                        AI生成
                      </el-tag>
                    </div>

                    <h4>{{ tpl.templateName }}</h4>
                    
                    <div class="template-content">
                      {{ tpl.templateContent?.substring(0, 80) }}...
                    </div>

                    <div class="template-stats">
                      <span>使用{{ tpl.useCount || 0 }}次</span>
                      <span>成功率{{ ((tpl.successRate || 0) * 100).toFixed(1) }}%</span>
                    </div>

                    <div class="template-actions">
                      <el-button size="small" type="primary" @click="useTemplate(tpl)">
                        使用此模板
                      </el-button>
                      
                      <el-button size="small" @click="editTemplate(tpl)">
                        编辑
                      </el-button>
                      
                      <el-button 
                        size="small" 
                        type="danger" 
                        @click="deleteTemplate(tpl)"
                      >
                        删除
                      </el-button>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 发送私信对话框 -->
    <el-dialog v-model="showMessageDialog" title="发送私信" width="500px" destroy-on-close>
      <el-form :model="messageForm" label-width="80px">
        <el-form-item label="接收人">
          <div style="font-weight: 500;">{{ currentComment?.authorName }}</div>
        </el-form-item>

        <el-form-item label="选择模板">
          <el-select v-model="messageForm.templateId" placeholder="选择私信模板" style="width: 100%" @change="onTemplateChange">
            <el-option 
              v-for="tpl in availableTemplates" 
              :key="tpl.id" 
              :label="tpl.templateName" 
              :value="tpl.id"
            >
              <span>{{ tpl.templateName }}</span>
              <span style="float: right; color: #999; font-size: 12px;">
                成功率{{ ((tpl.successRate || 0) * 100).toFixed(1) }}%
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="消息内容">
          <el-input 
            v-model="messageForm.content" 
            type="textarea" 
            :rows="5"
            show-word-limit
            maxlength="500"
          />
        </el-form-item>

        <el-alert 
          v-if="currentComment?.extractedPhone || currentComment?.extractedWechat"
          type="success" 
          :closable="false"
          style="margin-bottom: 12px;"
        >
          <template #title>
            该用户已留下联系方式：
            <span v-if="currentComment?.extractedPhone">📱 {{ currentComment.extractedPhone }}</span>
            <span v-if="currentComment?.extractedWechat">💬 {{ currentComment.extractedWechat }}</span>
          </template>
        </el-alert>
      </el-form>

      <template #footer>
        <el-button @click="showMessageDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmSendMessage" :loading="sending">
          立即发送
        </el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Message, DocumentAdd, VideoPlay, MagicStick } from '@element-plus/icons-vue'
import request from '@/utils/request'

const activeTab = ref('tasks')
const loadingTasks = ref(false)
const loadingComments = ref(false)
const creating = ref(false)
const sending = ref(false)

const taskList = ref([])
const filteredComments = ref([])
const selectedComments = ref([])
const templateList = ref([])

const taskForm = reactive({
  platformCode: '',
  targetType: '',
  targetId: '',
  keywords: '',
  maxCrawlCount: 500,
  crawlIntervalSeconds: 3
})

const filterForm = reactive({
  platformCode: '',
  intentLevel: '',
  minScore: null,
  onlyWithContact: false
})

const pagination = reactive({
  page: 1,
  size: 20
})

const statistics = reactive({
  totalCount: 0,
  filteredCount: 0,
  highIntentCount: 0,
  withPhoneCount: 0,
  withWechatCount: 0,
  avgIntentScore: 0
})

const showMessageDialog = ref(false)
const showCreateDialog = ref(false)
const showTemplateDialog = ref(false)
const showAiGenerateDialog = ref(false)
const currentComment = ref(null)

const messageForm = reactive({
  templateId: null,
  content: ''
})

const templateType = ref('all')

const availableTemplates = computed(() => {
  if (!currentComment.value?.aiIntentLevel) return templateList.value
  
  return templateList.value.filter(t => 
    t.intentLevel === currentComment.value.aiIntentLevel || t.isDefault
  )
})

onMounted(() => {
  loadTaskList()
  loadTemplates()
})

async function createTask() {
  creating.value = true
  try {
    const res = await request.post('/api/crawl/task/create', {
      ...taskForm,
      taskName: `${getPlatformName(taskForm.platformCode)}-${taskForm.targetType}-${Date.now()}`
    })
    
    ElMessage.success('抓取任务已创建，正在执行...')
    creating.value = false
    
    setTimeout(() => {
      loadTaskList()
    }, 2000)
    
  } catch (e) {
    ElMessage.error('创建失败: ' + e.message)
    creating.value = false
  }
}

async function loadTaskList() {
  loadingTasks.value = true
  try {
    // 实际应该调用API获取任务列表
    // const res = await request.get('/api/crawl/task/list')
    
    // 模拟数据用于演示
    taskList.value = [
      {
        id: 1,
        taskName: '抖音-视频评论抓取',
        platformCode: 'DOUYIN',
        status: 2,
        progressPercent: 100,
        totalCommentsFound: 1250,
        highIntentCount: 86,
        leadsGenerated: 42,
        createTime: '2026-01-10 14:30:00'
      },
      {
        id: 2,
        taskName: '小红书-话题监控',
        platformCode: 'XIAOHONGSHU',
        status: 1,
        progressPercent: 65,
        totalCommentsFound: 820,
        highIntentCount: 34,
        leadsGenerated: 18,
        createTime: '2026-01-11 09:15:00'
      }
    ]
    
    loadingTasks.value = false
    
  } catch (e) {
    console.error(e)
    loadingTasks.value = false
  }
}

async function analyzeComments(task) {
  try {
    await request.post(`/api/crawl/task/${task.id}/analyze`)
    ElMessage.success('AI分析已启动，请稍后查看结果')
    
  } catch (e) {
    ElMessage.error('分析失败: ' + e.message)
  }
}

async function generateLeads(task) {
  try {
    const res = await request.post(`/api/crawl/task/${task.id}/generate-leads`, {
      minScore: 70,
      autoAssign: true,
      generateFollowUpTask: true
    })
    
    ElMessage.success(`成功生成 ${res.generatedCount} 条商机！`)
    
  } catch (e) {
    ElMessage.error('生成失败: ' + e.message)
  }
}

function viewComments(task) {
  activeTab.value = 'comments'
}

function handleSelectionChange(selection) {
  selectedComments.value = selection
}

async function sendMessage(comment) {
  currentComment.value = comment
  messageForm.templateId = null
  messageForm.content = ''
  showMessageDialog.value = true
}

function onTemplateChange(templateId) {
  const tpl = templateList.value.find(t => t.id === templateId)
  if (tpl) {
    messageForm.content = tpl.templateContent
      .replace('{昵称}', comment.authorName || '亲')
      .replace('{产品名}', '我们的产品')
      .replace('{时间}', new Date().toLocaleDateString())
  }
}

async function confirmSendMessage() {
  sending.value = true
  try {
    const res = await request.post('/api/message/send', {
      commentId: currentComment.value.id,
      templateId: messageForm.templateId
    })
    
    if (res.success) {
      ElMessage.success('私信发送成功！')
      showMessageDialog.value = false
      
      currentComment.value.messageSent = true
    } else {
      ElMessage.error('发送失败: ' + res.errorMessage)
    }
    
  } catch (e) {
    ElMessage.error('发送失败: ' + e.message)
  } finally {
    sending.value = false
  }
}

async function generateSingleLead(comment) {
  try {
    await ElMessageBox.confirm(
      `确定要将「${comment.authorName}」的评论转化为商机吗？`,
      '确认生成商机',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    
    const res = await request.post('/api/crawl/comment/generate-lead', {
      commentId: comment.id,
      autoAssign: true
    })
    
    ElMessage.success('商机已生成！')
    comment.leadGenerated = true
    
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('生成失败: ' + e.message)
    }
  }
}

async function batchSendMessage() {
  try {
    await ElMessageBox.confirm(
      `确定要向 ${selectedComments.value.length} 位用户发送私信吗？`,
      '批量发送确认',
      { confirmButtonText: '确定发送', cancelButtonText: '取消' }
    )
    
    const res = await request.post('/api/message/batch-send', {
      commentIds: selectedComments.value.map(c => c.id),
      maxConcurrent: 5,
      intervalMs: 30000
    })
    
    ElMessage.success(`批量发送完成！成功 ${res.successCount}，失败 ${res.failCount}`)
    
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量发送失败: ' + e.message)
    }
  }
}

async function loadTemplates() {
  try {
    const res = await request.get('/api/message/templates')
    templateList.value = res.data || []
    
    // 如果没有模板，添加默认模板
    if (templateList.value.length === 0) {
      templateList.value = getDefaultTemplates()
    }
    
  } catch (e) {
    templateList.value = getDefaultTemplates()
  }
}

function getDefaultTemplates() {
  return [
    {
      id: 1,
      templateName: 'A级-超高意向-立即联系',
      intentLevel: 'A',
      aiGenerated: false,
      templateContent: '您好{昵称}！看到您对我们的产品很感兴趣，我是专属顾问小王，想了解一下具体需求吗？随时欢迎咨询~ 📞{联系方式}',
      useCount: 156,
      successRate: 0.78
    },
    {
      id: 2,
      templateName: 'A级-留电话-快速响应',
      intentLevel: 'A',
      aiGenerated: false,
      templateContent: '{昵称}您好！看到您留言了，马上联系您~ 我们的产品{产品名}正好符合您的需求，详细资料已发您微信，请查收！',
      useCount: 234,
      successRate: 0.85
    },
    {
      id: 3,
      templateName: 'B级-高意向-产品介绍',
      intentLevel: 'B',
      aiGenerated: true,
      templateContent: '{昵称}您好！感谢关注我们~ {产品名}是一款专为像您这样有{用户需求}的用户设计的产品，主要特点：\n1. ✨ 特点一\n2. ✨ 特点二\n\n想了解更多吗？随时私信我~',
      useCount: 89,
      successRate: 0.62
    },
    {
      id: 4,
      templateName: 'C级-中意向-引导咨询',
      intentLevel: 'C',
      aiGenerated: true,
      templateContent: '{昵称}好呀！看到您在了解{产品名}，有什么我可以帮您的吗？😊\n\n不管是产品咨询、价格对比还是使用建议，都可以问我哦~',
      useCount: 167,
      successRate: 0.45
    },
    {
      id: 5,
      templateName: '通用-友好问候',
      intentLevel: 'C',
      aiGenerated: false,
      isDefault: true,
      templateContent: '您好{昵称}！看到您对我们的产品很感兴趣，想了解一下具体需求吗？随时欢迎咨询~',
      useCount: 512,
      successRate: 0.38
    }
  ]
}

function useTemplate(tpl) {
  messageForm.templateId = tpl.id
  onTemplateChange(tpl.id)
}

function editTemplate(tpl) {
  ElMessage.info('编辑功能开发中...')
}

async function deleteTemplate(tpl) {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板「${tpl.templateName}」吗？`,
      '确认删除'
    )
    
    await request.delete(`/api/message/template/${tpl.id}`)
    ElMessage.success('删除成功')
    loadTemplates()
    
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function getPlatformName(code) {
  const map = {
    DOUYIN: '抖音',
    XIAOHONGSHU: '小红书',
    KUAISHOU: '快手',
    WEIBO: '微博',
    BILIBILI: 'B站'
  }
  return map[code] || code
}

function getStatusType(status) {
  const map = { 0: 'info', 1: '', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}

function getStatusName(status) {
  const map = { 0: '待执行', 1: '进行中', 2: '已完成', 3: '失败' }
  return map[status] || '未知'
}

function getPlatformType(platform) {
  const map = {
    DOUYIN: '',
    XIAOHONGSHU: 'danger',
    KUAISHOU: 'warning',
    WEIBO: '',
    BILIBILI: 'success'
  }
  return map[platform] || ''
}

function getLevelType(level) {
  const map = { A: 'danger', B: 'warning', C: '', D: 'info' }
  return map[level] || ''
}

function getScoreClass(score) {
  if (score >= 85) return 'score-a'
  if (score >= 70) return 'score-b'
  if (score >= 55) return 'score-c'
  return 'score-d'
}

function formatNumber(num) {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}
</script>

<style scoped>
.crawl-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.platform-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  color: white;
}

.platform-douyin { background: #000000; }
.platform-xhs { background: #ff2442; }
.platform-kuaishou { background: #ff4906; }
.platform-weibo { background: #e6162d; }
.platform-bilibili { background: #00a1d6; }

.stat-card {
  text-align: center;
  padding: 20px 0;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-total .stat-number { color: #409eff; }
.stat-high-intent .stat-number { color: #e6a23c; }
.stat-phone .stat-number { color: #f56c6c; }
.stat-wechat .stat-number { color: #67c23a; }

.comment-text {
  line-height: 1.6;
}

.score-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: bold;
  font-size: 14px;
}

.score-a { background: #fef0f0; color: #f56c6c; }
.score-b { background: #fdf6ec; color: #e6a23c; }
.score-c { background: #ecf5ff; color: #409eff; }
.score-d { background: #f4f4f5; color: #909399; }

.template-card {
  cursor: pointer;
  transition: all 0.3s;
}

.template-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.template-header {
  margin-bottom: 8px;
}

.template-content {
  color: #666;
  font-size: 13px;
  line-height: 1.6;
  margin: 12px 0;
  min-height: 60px;
}

.template-stats {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin: 12px 0;
}

.template-actions {
  display: flex;
  gap: 8px;
}
</style>
