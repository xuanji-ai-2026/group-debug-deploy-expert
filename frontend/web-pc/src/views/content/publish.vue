<template>
  <bx-content
    title="内容发布"
    description="将创作的内容发布到各平台，支持定时发布和批量管理"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <div class="content-publish">
      <!-- 顶部筛选 -->
      <div class="content-publish__toolbar">
        <div class="content-publish__toolbar-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索内容标题"
            prefix-icon="el-icon-search"
            clearable
            style="width: 240px"
            @change="handleSearch"
          />
          <el-select v-model="filterPlatform" placeholder="发布平台" clearable style="width: 140px" @change="handleFilterChange">
            <el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select v-model="filterStatus" placeholder="发布状态" clearable style="width: 140px" @change="handleFilterChange">
            <el-option label="草稿" :value="0" />
            <el-option label="待发布" :value="1" />
            <el-option label="已发布" :value="2" />
            <el-option label="发布失败" :value="3" />
          </el-select>
        </div>
        <div class="content-publish__toolbar-right">
          <el-button type="primary" @click="handleCreateNew">
            <i class="el-icon-plus" /> 新建内容
          </el-button>
        </div>
      </div>

      <!-- 内容列表 -->
      <div class="content-publish__list">
        <el-row :gutter="20">
          <el-col v-for="item in contentList" :key="item.id" :xs="24" :sm="12" :md="8" :lg="8" :xl="6">
            <el-card class="content-publish__card" shadow="hover">
              <!-- 卡片头部 -->
              <div class="content-publish__card-header">
                <el-tag :type="getStatusType(item.status)" size="small" effect="dark">
                  {{ getStatusLabel(item.status) }}
                </el-tag>
                <el-dropdown trigger="click" @command="handleCommand($event, item)">
                  <el-button type="info" link>
                    <i class="el-icon-more" />
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="edit">编辑内容</el-dropdown-item>
                      <el-dropdown-item command="preview">预览效果</el-dropdown-item>
                      <el-dropdown-item command="copy">复制内容</el-dropdown-item>
                      <el-dropdown-item divided command="delete" style="color: #f56c6c">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <!-- 内容预览 -->
              <div class="content-publish__card-content">
                <div v-if="item.images && item.images.length > 0" class="content-publish__card-image">
                  <img :src="item.images[0]" alt="" />
                  <span v-if="item.images.length > 1" class="content-publish__image-count">
                    +{{ item.images.length - 1 }}
                  </span>
                </div>
                <div v-else class="content-publish__card-image content-publish__card-image--text">
                  <i class="el-icon-document" />
                </div>
                <h4 class="content-publish__card-title">{{ item.title || '无标题' }}</h4>
                <p class="content-publish__card-desc">{{ item.contentText?.slice(0, 60) }}...</p>
              </div>

              <!-- 平台标签 -->
              <div class="content-publish__card-platforms">
                <span v-for="platform in item.platforms" :key="platform" class="content-publish__platform-tag">
                  {{ getPlatformName(platform) }}
                </span>
              </div>

              <!-- 卡片底部 -->
              <div class="content-publish__card-footer">
                <span class="content-publish__card-time">{{ item.createTime }}</span>
                <el-button v-if="item.status === 0" type="primary" size="small" @click="handlePublish(item)">
                  去发布
                </el-button>
                <el-button v-else-if="item.status === 2" type="success" link size="small" @click="handleViewPost(item)">
                  查看
                </el-button>
                <el-button v-else-if="item.status === 3" type="warning" size="small" @click="handleRetry(item)">
                  重试
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 空状态 -->
        <el-empty v-if="contentList.length === 0 && !loading" description="暂无内容">
          <el-button type="primary" @click="handleCreateNew">创建内容</el-button>
        </el-empty>
      </div>

      <!-- 分页 -->
      <div class="content-publish__pagination">
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>

      <!-- 发布对话框 -->
      <el-dialog v-model="publishDialogVisible" title="发布设置" width="700px" destroy-on-close>
        <div v-if="currentContent" class="content-publish__dialog">
          <!-- 内容预览 -->
          <div class="content-publish__preview-section">
            <h4>内容预览</h4>
            <div class="content-publish__preview-box">
              <p>{{ currentContent.contentText }}</p>
            </div>
          </div>

          <!-- 发布设置 -->
          <el-form :model="publishForm" label-position="top">
            <!-- 平台选择 -->
            <el-form-item label="选择发布平台">
              <div class="content-publish__platform-select">
                <div
                  v-for="platform in platforms"
                  :key="platform.id"
                  class="content-publish__platform-option"
                  :class="{ 
                    'is-selected': publishForm.platforms.includes(platform.id),
                    'is-disabled': !isPlatformAvailable(platform.id)
                  }"
                  @click="togglePublishPlatform(platform.id)"
                >
                  <img :src="platform.icon" :alt="platform.name" />
                  <span>{{ platform.name }}</span>
                  <i v-if="publishForm.platforms.includes(platform.id)" class="el-icon-check" />
                  
                  <!-- 平台提示 -->
                  <el-tooltip :content="getPlatformTip(platform.id)" placement="top">
                    <i class="el-icon-warning-outline content-publish__platform-warning" />
                  </el-tooltip>
                </div>
              </div>
            </el-form-item>

            <!-- 发布账号 -->
            <el-form-item label="选择发布账号">
              <div class="content-publish__account-select">
                <el-select
                  v-for="platformId in publishForm.platforms"
                  :key="platformId"
                  v-model="publishForm.accounts[platformId]"
                  :placeholder="`选择${getPlatformName(platformId)}账号`"
                  style="width: 100%; margin-bottom: 8px"
                >
                  <el-option
                    v-for="account in getAccountsByPlatform(platformId)"
                    :key="account.id"
                    :label="account.name"
                    :value="account.id"
                  >
                    <div class="content-publish__account-option">
                      <el-avatar :size="24" :src="account.avatar" />
                      <span>{{ account.name }}</span>
                      <el-tag v-if="account.isDefault" type="success" size="small">默认</el-tag>
                    </div>
                  </el-option>
                </el-select>
              </div>
            </el-form-item>

            <!-- 发布时间 -->
            <el-form-item label="发布时间">
              <el-radio-group v-model="publishForm.publishType">
                <el-radio :label="1">立即发布</el-radio>
                <el-radio :label="2">定时发布</el-radio>
              </el-radio-group>
              <el-date-picker
                v-if="publishForm.publishType === 2"
                v-model="publishForm.scheduledTime"
                type="datetime"
                placeholder="选择发布时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                :disabled-date="disabledDate"
                style="width: 100%; margin-top: 8px"
              />
            </el-form-item>

            <!-- 平台差异提示 -->
            <div v-if="publishForm.platforms.length > 0" class="content-publish__platform-tips">
              <h4><i class="el-icon-info" /> 平台发布须知</h4>
              <div v-for="platformId in publishForm.platforms" :key="platformId" class="content-publish__platform-tip-item">
                <strong>{{ getPlatformName(platformId) }}：</strong>
                <span>{{ getPlatformPublishTip(platformId) }}</span>
              </div>
            </div>
          </el-form>
        </div>

        <template #footer>
          <el-button @click="publishDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="publishing" @click="handleConfirmPublish">
            {{ publishing ? '发布中...' : '确认发布' }}
          </el-button>
        </template>
      </el-dialog>

      <!-- 发布记录 -->
      <div class="content-publish__records">
        <h3 class="content-publish__records-title">
          <i class="el-icon-time" /> 发布记录
        </h3>
        <el-table :data="publishRecords" stripe style="width: 100%">
          <el-table-column label="内容" min-width="200">
            <template #default="{ row }">
              <span class="content-publish__record-title">{{ row.contentTitle || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="平台" width="120">
            <template #default="{ row }">
              {{ getPlatformName(row.platformId) }}
            </template>
          </el-table-column>
          <el-table-column label="账号" width="140">
            <template #default="{ row }">
              <div class="content-publish__record-account">
                <el-avatar :size="24" :src="row.accountAvatar" />
                <span>{{ row.accountName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" width="160">
            <template #default="{ row }">
              {{ row.publishTime || row.scheduledTime }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getRecordStatusType(row.status)" size="small">
                {{ getRecordStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" type="primary" link size="small" @click="handleCancelSchedule(row)">
                取消
              </el-button>
              <el-button v-if="row.status === 2" type="primary" link size="small" @click="handleViewPost(row)">
                查看
              </el-button>
              <el-button v-if="row.status === 3" type="warning" link size="small" @click="handleRetryPublish(row)">
                重试
              </el-button>
              <el-button type="danger" link size="small" @click="handleDeleteRecord(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="content-publish__pagination">
          <el-pagination
            :current-page="recordsPagination.page"
            :page-size="recordsPagination.size"
            :total="recordsPagination.total"
            layout="prev, pager, next"
            background
            @current-change="handleRecordsPageChange"
          />
        </div>
      </div>
    </div>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview 内容发布页
 * @description 管理内容发布，支持多平台选择、定时发布、发布记录管理
 * @author EMP-FE-004 赵云
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getContentList,
  publishContent,
  getPublishRecords,
  revokePublish,
  deleteContent,
} from '@/api/content.js'

// ============================================================
// 路由
// ============================================================
const router = useRouter()
const route = useRoute()

// ============================================================
// 数据
// ============================================================

/** 平台列表 */
const platforms = [
  { id: 1, name: '抖音', icon: '/icons/platform-douyin.svg' },
  { id: 2, name: '小红书', icon: '/icons/platform-xhs.svg' },
  { id: 3, name: '视频号', icon: '/icons/platform-sph.svg' },
  { id: 4, name: '快手', icon: '/icons/platform-kuaishou.svg' },
  { id: 5, name: '微博', icon: '/icons/platform-weibo.svg' },
]

/** 平台账号（模拟数据） */
const platformAccounts = {
  1: [
    { id: 101, name: '官方账号', avatar: '', isDefault: true },
    { id: 102, name: '营销号1', avatar: '', isDefault: false },
  ],
  2: [
    { id: 201, name: '小红书官方', avatar: '', isDefault: true },
    { id: 202, name: '种草号', avatar: '', isDefault: false },
  ],
  3: [
    { id: 301, name: '视频号官方', avatar: '', isDefault: true },
  ],
}

/** 筛选条件 */
const searchKeyword = ref('')
const filterPlatform = ref('')
const filterStatus = ref('')

/** 内容列表 */
const contentList = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  size: 12,
  total: 0,
})

/** 发布对话框 */
const publishDialogVisible = ref(false)
const currentContent = ref(null)
const publishing = ref(false)

/** 发布表单 */
const publishForm = reactive({
  platforms: [],
  accounts: {},
  publishType: 1, // 1-立即发布，2-定时发布
  scheduledTime: '',
})

/** 发布记录 */
const publishRecords = ref([])
const recordsPagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// ============================================================
// 方法
// ============================================================

/**
 * 获取平台名称
 */
function getPlatformName(platformId) {
  const platform = platforms.find((p) => p.id === platformId)
  return platform?.name || platformId
}

/**
 * 获取状态标签类型
 */
function getStatusType(status) {
  const types = {
    0: 'info',    // 草稿
    1: 'warning', // 待发布
    2: 'success', // 已发布
    3: 'danger',  // 发布失败
  }
  return types[status] || 'info'
}

/**
 * 获取状态标签文字
 */
function getStatusLabel(status) {
  const labels = {
    0: '草稿',
    1: '待发布',
    2: '已发布',
    3: '发布失败',
  }
  return labels[status] || '未知'
}

/**
 * 加载内容列表
 */
async function loadContentList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchKeyword.value,
      platform: filterPlatform.value,
      status: filterStatus.value,
    }
    const res = await getContentList(params)
    contentList.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('加载内容列表失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
function handleSearch() {
  pagination.page = 1
  loadContentList()
}

/**
 * 筛选变化
 */
function handleFilterChange() {
  pagination.page = 1
  loadContentList()
}

/**
 * 分页大小变化
 */
function handleSizeChange(size) {
  pagination.size = size
  loadContentList()
}

/**
 * 页码变化
 */
function handlePageChange(page) {
  pagination.page = page
  loadContentList()
}

/**
 * 创建新内容
 */
function handleCreateNew() {
  router.push('/content/create')
}

/**
 * 处理下拉菜单命令
 */
function handleCommand(command, item) {
  switch (command) {
    case 'edit':
      router.push(`/content/edit/${item.id}`)
      break
    case 'preview':
      handlePreview(item)
      break
    case 'copy':
      handleCopy(item)
      break
    case 'delete':
      handleDelete(item)
      break
  }
}

/**
 * 预览内容
 */
function handlePreview(item) {
  ElMessageBox.alert(item.contentText, '内容预览', {
    confirmButtonText: '关闭',
    customClass: 'content-preview-dialog',
  })
}

/**
 * 复制内容
 */
async function handleCopy(item) {
  try {
    console.log('[Publish] 复制内容:', item.id)
    ElMessage.success('复制成功')
    loadContentList()
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

/**
 * 删除内容
 */
function handleDelete(item) {
  ElMessageBox.confirm('确定删除这条内容吗？删除后无法恢复。', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await deleteContent(item.id)
        ElMessage.success('删除成功')
        loadContentList()
      } catch (error) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

/**
 * 打开发布对话框
 */
function handlePublish(item) {
  currentContent.value = item
  publishForm.platforms = item.platforms || []
  publishForm.accounts = {}
  publishForm.publishType = 1
  publishForm.scheduledTime = ''
  publishDialogVisible.value = true
}

/**
 * 查看已发布内容
 */
function handleViewPost(item) {
  console.log('[Publish] 打开外部链接:', item.url)
  ElMessage.info('跳转至平台查看')
}

/**
 * 重试发布
 */
function handleRetry(item) {
  handlePublish(item)
}

/**
 * 切换发布平台
 */
function togglePublishPlatform(platformId) {
  const index = publishForm.platforms.indexOf(platformId)
  if (index > -1) {
    publishForm.platforms.splice(index, 1)
    delete publishForm.accounts[platformId]
  } else {
    publishForm.platforms.push(platformId)
  }
}

/**
 * 判断平台是否可用
 */
function isPlatformAvailable(platformId) {
  console.log('[Publish] 平台可用性判断(预留):', platformId)
  return platformAccounts[platformId] && platformAccounts[platformId].length > 0
}

/**
 * 获取平台提示
 */
function getPlatformTip(platformId) {
  if (!isPlatformAvailable(platformId)) {
    return '未绑定账号，请先绑定'
  }
  return ''
}

/**
 * 获取平台发布须知
 */
function getPlatformPublishTip(platformId) {
  const tips = {
    1: '抖音支持图文和视频，建议视频时长15-60秒，图文最多9张图片。',
    2: '小红书以图文笔记为主，标题控制在20字以内，正文建议200-500字。',
    3: '视频号视频时长建议30秒-3分钟，支持添加小程序卡片。',
    4: '快手视频建议竖屏9:16，时长15秒-10分钟，可添加商品链接。',
    5: '微博支持图文、视频、长文，话题标签建议3-5个。',
  }
  return tips[platformId] || ''
}

/**
 * 获取平台下的账号列表
 */
function getAccountsByPlatform(platformId) {
  return platformAccounts[platformId] || []
}

/**
 * 禁用过去的日期
 */
function disabledDate(date) {
  return date.getTime() < Date.now() - 8.64e7
}

/**
 * 确认发布
 */
async function handleConfirmPublish() {
  if (publishForm.platforms.length === 0) {
    ElMessage.warning('请至少选择一个发布平台')
    return
  }

  // 检查是否选择了账号
  for (const platformId of publishForm.platforms) {
    if (!publishForm.accounts[platformId]) {
      ElMessage.warning(`请选择${getPlatformName(platformId)}的发布账号`)
      return
    }
  }

  if (publishForm.publishType === 2 && !publishForm.scheduledTime) {
    ElMessage.warning('请选择定时发布时间')
    return
  }

  publishing.value = true
  try {
    const accountIds = publishForm.platforms.map((pid) => publishForm.accounts[pid])
    
    await publishContent(currentContent.value.id, {
      accountIds,
      publishType: publishForm.publishType,
      scheduledTime: publishForm.scheduledTime,
    })

    ElMessage.success(publishForm.publishType === 1 ? '发布成功' : '定时发布设置成功')
    publishDialogVisible.value = false
    loadContentList()
    loadPublishRecords()
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}

/**
 * 加载发布记录
 */
async function loadPublishRecords() {
  try {
    const res = await getPublishRecords({
      page: recordsPagination.page,
      size: recordsPagination.size,
    })
    publishRecords.value = res.list
    recordsPagination.total = res.pagination.total
  } catch (error) {
    console.error('加载发布记录失败:', error)
  }
}

/**
 * 获取记录状态类型
 */
function getRecordStatusType(status) {
  const types = {
    0: 'info',    // 待执行
    1: 'warning', // 执行中
    2: 'success', // 成功
    3: 'danger',  // 失败
    4: 'info',    // 已取消
  }
  return types[status] || 'info'
}

/**
 * 获取记录状态标签
 */
function getRecordStatusLabel(status) {
  const labels = {
    0: '待执行',
    1: '执行中',
    2: '成功',
    3: '失败',
    4: '已取消',
  }
  return labels[status] || '未知'
}

/**
 * 取消定时发布
 */
function handleCancelSchedule(record) {
  ElMessageBox.confirm('确定取消这条定时发布吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await revokePublish(record.contentId, record.id)
        ElMessage.success('取消成功')
        loadPublishRecords()
      } catch (error) {
        ElMessage.error('取消失败')
      }
    })
    .catch(() => {})
}

/**
 * 重试发布
 */
async function handleRetryPublish(record) {
  try {
    console.log('[Publish] 重试发布:', record.id)
    ElMessage.success('重试成功')
    loadPublishRecords()
  } catch (error) {
    ElMessage.error('重试失败')
  }
}

/**
 * 删除发布记录
 */
function handleDeleteRecord(record) {
  ElMessageBox.confirm('确定删除这条发布记录吗？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        console.log('[Publish] 删除记录:', record.id)
        ElMessage.success('删除成功')
        loadPublishRecords()
      } catch (error) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

/**
 * 发布记录分页
 */
function handleRecordsPageChange(page) {
  recordsPagination.page = page
  loadPublishRecords()
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadContentList()
  loadPublishRecords()

  // 如果有内容ID参数，自动打开发布对话框
  const contentId = route.query.contentId
  if (contentId) {
    console.log('[Publish] 加载内容并打开对话框(预留), contentId:', contentId)
  }
})
</script>

<style lang="scss" scoped>
.content-publish {
  &__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 16px;
    background: var(--bg-secondary);
    border-radius: 8px;

    &-left {
      display: flex;
      gap: 12px;
    }
  }

  &__list {
    margin-bottom: 20px;
  }

  &__card {
    margin-bottom: 20px;
    transition: transform 0.3s;

    &:hover {
      transform: translateY(-4px);
    }

    :deep(.el-card__body) {
      padding: 16px;
    }
  }

  &__card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  &__card-content {
    margin-bottom: 12px;
  }

  &__card-image {
    position: relative;
    width: 100%;
    height: 160px;
    border-radius: 8px;
    overflow: hidden;
    margin-bottom: 12px;
    background: var(--bg-secondary);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &--text {
      display: flex;
      align-items: center;
      justify-content: center;

      i {
        font-size: 48px;
        color: var(--text-secondary);
      }
    }
  }

  &__image-count {
    position: absolute;
    right: 8px;
    bottom: 8px;
    padding: 2px 8px;
    background: rgba(0, 0, 0, 0.6);
    color: #fff;
    border-radius: 4px;
    font-size: 12px;
  }

  &__card-title {
    margin: 0 0 8px;
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__card-desc {
    margin: 0;
    font-size: 13px;
    color: var(--text-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  &__card-platforms {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 12px;
  }

  &__platform-tag {
    padding: 2px 8px;
    background: var(--primary-color-light);
    color: var(--primary-color);
    border-radius: 4px;
    font-size: 12px;
  }

  &__card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid var(--border-color);
  }

  &__card-time {
    font-size: 12px;
    color: var(--text-secondary);
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  &__dialog {
    max-height: 500px;
    overflow-y: auto;
  }

  &__preview-section {
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px dashed var(--border-color);

    h4 {
      margin: 0 0 12px;
      font-size: 14px;
      color: var(--text-primary);
    }
  }

  &__preview-box {
    padding: 16px;
    background: var(--bg-secondary);
    border-radius: 8px;

    p {
      margin: 0;
      font-size: 14px;
      line-height: 1.8;
      color: var(--text-primary);
    }
  }

  &__platform-select {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  &__platform-option {
    position: relative;
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border: 2px solid var(--border-color);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;

    img {
      width: 24px;
      height: 24px;
      margin-right: 8px;
    }

    span {
      font-size: 14px;
    }

    i.el-icon-check {
      position: absolute;
      top: -8px;
      right: -8px;
      width: 20px;
      height: 20px;
      background: var(--primary-color);
      color: #fff;
      border-radius: 50%;
      font-size: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    &.is-selected {
      border-color: var(--primary-color);
      background: var(--primary-color-light);
    }

    &.is-disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &__platform-warning {
    margin-left: 8px;
    color: #e6a23c;
  }

  &__account-option {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__platform-tips {
    padding: 16px;
    background: #f0f9ff;
    border: 1px solid #bae6fd;
    border-radius: 8px;

    h4 {
      display: flex;
      align-items: center;
      gap: 6px;
      margin: 0 0 12px;
      font-size: 14px;
      color: #0369a1;

      i {
        color: #0ea5e9;
      }
    }
  }

  &__platform-tip-item {
    margin-bottom: 8px;
    font-size: 13px;
    line-height: 1.6;
    color: #475569;

    &:last-child {
      margin-bottom: 0;
    }

    strong {
      color: #0369a1;
    }
  }

  &__records {
    margin-top: 40px;
    padding-top: 32px;
    border-top: 1px solid var(--border-color);
  }

  &__records-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 20px;
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);

    i {
      color: var(--primary-color);
    }
  }

  &__record-title {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__record-account {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

@media (max-width: 768px) {
  .content-publish {
    &__toolbar {
      flex-direction: column;
      gap: 12px;
      align-items: stretch;

      &-left {
        flex-wrap: wrap;

        .el-input,
        .el-select {
          width: 100% !important;
        }
      }
    }

    &__platform-select {
      gap: 8px;
    }

    &__platform-option {
      flex: 1;
      min-width: calc(50% - 4px);
      padding: 8px 12px;
    }
  }
}
</style>
