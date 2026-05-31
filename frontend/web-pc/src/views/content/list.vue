<template>
  <bx-content
    title="内容列表"
    description="管理营销内容，支持 AI 生成、编辑和发布"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 操作栏 -->
    <div class="content-list__toolbar">
      <div class="content-list__toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标题/关键词"
          prefix-icon="el-icon-search"
          clearable
          style="width: 240px"
          @change="handleSearch"
        />
        <el-select v-model="filterContentType" placeholder="内容类型" clearable style="width: 130px" @change="handleFilterChange">
          <el-option label="图文" :value="1" />
          <el-option label="视频" :value="2" />
          <el-option label="纯文字" :value="3" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 130px" @change="handleFilterChange">
          <el-option label="草稿" :value="0" />
          <el-option label="待审核" :value="1" />
          <el-option label="已发布" :value="2" />
          <el-option label="发布失败" :value="3" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="handleDateChange"
        />
      </div>
      <div class="content-list__toolbar-right">
        <el-button @click="handleGenerate">
          <i class="el-icon-magic-stick" />
          AI 生成
        </el-button>
        <el-button type="primary" @click="handleCreate">
          <i class="el-icon-plus" />
          创建内容
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="content-list__table">
      <el-table v-loading="loading" :data="tableData" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="内容" min-width="300">
          <template #default="{ row }">
            <div class="content-list__content-cell">
              <div class="content-list__content-thumbnail">
                <img v-if="row.images && row.images.length > 0" :src="row.images[0]" alt="" />
                <i v-else class="el-icon-picture-outline" />
              </div>
              <div class="content-list__content-info">
                <span class="content-list__content-title">{{ row.title || '无标题' }}</span>
                <div class="content-list__content-tags">
                  <el-tag v-if="row.aiGenerated" type="success" size="small" effect="plain">
                    AI生成
                  </el-tag>
                  <span class="content-list__content-type">{{ getContentTypeLabel(row.contentType) }}</span>
                  <span v-if="row.keywords && row.keywords.length > 0" class="content-list__content-keywords">
                    {{ row.keywords.slice(0, 3).join(', ') }}
                  </span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="平台" width="160">
          <template #default="{ row }">
            <div class="content-list__platforms">
              <span v-for="platform in row.platforms" :key="platform" class="content-list__platform-tag">
                {{ platform }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="statusLabel" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI评分" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.aiScore" :class="['content-list__score', getScoreClass(row.aiScore)]">
              {{ row.aiScore }}
            </span>
            <span v-else class="content-list__score content-list__score--none">-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              link
              size="small"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === 2"
              type="info"
              link
              size="small"
              @click="handleViewPublish(row)"
            >
              发布记录
            </el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button type="primary" link size="small">
                更多
                <i class="el-icon-arrow-down el-icon--right" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="copy">复制内容</el-dropdown-item>
                  <el-dropdown-item command="preview">预览</el-dropdown-item>
                  <el-dropdown-item divided command="delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="content-list__pagination">
      <el-pagination
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 发布弹窗 -->
    <el-dialog v-model="publishDialogVisible" title="发布内容" width="600px">
      <el-form ref="publishFormRef" :model="publishForm" label-width="100px">
        <el-form-item label="选择账号" prop="accountIds">
          <el-checkbox-group v-model="publishForm.accountIds">
            <el-checkbox v-for="account in availableAccounts" :key="account.id" :label="account.id">
              {{ account.accountName }}
              <span class="content-list__account-platform">({{ account.platformName }})</span>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="发布方式" prop="publishType">
          <el-radio-group v-model="publishForm.publishType">
            <el-radio :label="1">立即发布</el-radio>
            <el-radio :label="2">定时发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="publishForm.publishType === 2" label="定时时间" prop="scheduledTime">
          <el-date-picker
            v-model="publishForm.scheduledTime"
            type="datetime"
            placeholder="选择发布时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishLoading" @click="handlePublishSubmit">
          确认发布
        </el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview ContentList 内容列表页面
 * @description 管理系统营销内容，支持 AI 生成、编辑、发布等功能
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as contentApi from '@/api/content.js'

// ============================================================
// Router
// ============================================================
const router = useRouter()

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const tableData = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// 搜索和筛选
const searchKeyword = ref('')
const filterContentType = ref('')
const filterStatus = ref('')
const dateRange = ref([])

// 选中行
const selectedRows = ref([])

// ============================================================
// 发布相关
// ============================================================
const publishDialogVisible = ref(false)
const publishFormRef = ref(null)
const publishLoading = ref(false)
const publishForm = reactive({
  id: null,
  accountIds: [],
  publishType: 1,
  scheduledTime: '',
})
const availableAccounts = ref([])

// 内容类型映射
const contentTypeMap = {
  1: '图文',
  2: '视频',
  3: '纯文字',
}

// 状态映射
const statusMap = {
  0: { label: '草稿', type: 'info' },
  1: { label: '待审核', type: 'warning' },
  2: { label: '已发布', type: 'success' },
  3: { label: '发布失败', type: 'danger' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载数据
 */
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
    }

    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (filterContentType.value) {
      params.contentType = filterContentType.value
    }
    if (filterStatus.value !== '') {
      params.status = filterStatus.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const response = await contentApi.getContentList(params)

    tableData.value = response.data?.list || []
    pagination.total = response.data?.pagination?.total || 0
  } catch (error) {
    console.error('[ContentList] Load data failed:', error)
    tableData.value = []
    pagination.total = 0
    ElMessage.error('加载内容数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
function handleSearch() {
  pagination.page = 1
  loadData()
}

/**
 * 筛选变化
 */
function handleFilterChange() {
  pagination.page = 1
  loadData()
}

/**
 * 日期变化
 */
function handleDateChange() {
  pagination.page = 1
  loadData()
}

/**
 * 分页变化
 */
function handlePageChange(page) {
  pagination.page = page
  loadData()
}

/**
 * 每页数量变化
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadData()
}

/**
 * 选中行变化
 */
function handleSelectionChange(selection) {
  selectedRows.value = selection
}

/**
 * 创建内容
 */
function handleCreate() {
  router.push('/content/create')
}

/**
 * AI 生成
 */
function handleGenerate() {
  router.push('/content/create?mode=ai')
}

/**
 * 编辑内容
 */
function handleEdit(row) {
  router.push(`/content/edit/${row.id}`)
}

/**
 * 发布内容
 */
function handlePublish(row) {
  publishForm.id = row.id
  publishForm.accountIds = []
  publishForm.publishType = 1
  publishForm.scheduledTime = ''

  // 加载可用账号
  availableAccounts.value = [
    { id: 1, accountName: '北极星官方', platformName: '抖音' },
    { id: 2, accountName: '运营小助手', platformName: '小红书' },
    { id: 3, accountName: '营销中心', platformName: '快手' },
  ]

  publishDialogVisible.value = true
}

/**
 * 发布提交
 */
async function handlePublishSubmit() {
  if (publishForm.accountIds.length === 0) {
    ElMessage.warning('请至少选择一个发布账号')
    return
  }

  publishLoading.value = true
  try {
    await contentApi.publishContent(publishForm.id, {
      accountIds: publishForm.accountIds,
      publishType: publishForm.publishType,
      scheduledTime: publishForm.publishType === 2 ? publishForm.scheduledTime : undefined,
    })

    ElMessage.success('发布任务已创建')
    publishDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[ContentList] Publish failed:', error)
  } finally {
    publishLoading.value = false
  }
}

/**
 * 查看发布记录
 */
function handleViewPublish(row) {
  ElMessage.info('发布记录功能开发中')
}

/**
 * 下拉菜单命令
 */
function handleCommand(command, row) {
  switch (command) {
    case 'copy':
      ElMessage.success('内容已复制')
      break
    case 'preview':
      ElMessage.info('预览功能开发中')
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

/**
 * 删除内容
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该内容吗？删除后无法恢复。', '确认删除', {
      type: 'warning',
    })

    await contentApi.deleteContent(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[ContentList] Delete failed:', error)
    }
  }
}

/**
 * 获取内容类型标签
 */
function getContentTypeLabel(type) {
  return contentTypeMap[type] || '-'
}

/**
 * 获取状态标签类型
 */
function getStatusType(status) {
  return statusMap[status]?.type || 'info'
}

/**
 * 获取评分样式类
 */
function getScoreClass(score) {
  if (score >= 80) return 'content-list__score--high'
  if (score >= 60) return 'content-list__score--medium'
  return 'content-list__score--low'
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
// 工具栏
.content-list__toolbar {
  @include flex-between;
  margin-bottom: $spacing-lg;

  &-left {
    display: flex;
    gap: $spacing-sm;
    flex-wrap: wrap;
  }

  &-right {
    display: flex;
    gap: $spacing-sm;
  }
}

// 表格
.content-list__table {
  background: $color-white;
  border-radius: $radius-lg;
  padding: $spacing-lg;
}

.content-list__content-cell {
  display: flex;
  gap: $spacing-sm;
}

.content-list__content-thumbnail {
  flex-shrink: 0;
  width: 60px;
  height: 45px;
  border-radius: $radius-sm;
  overflow: hidden;
  background: $color-gray-100;
  @include flex-center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  i {
    font-size: 24px;
    color: $color-gray-400;
  }
}

.content-list__content-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.content-list__content-title {
  font-weight: $font-weight-medium;
  color: $color-text-primary;
  @include text-ellipsis(1);
  margin-bottom: 4px;
}

.content-list__content-tags {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.content-list__platforms {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.content-list__platform-tag {
  padding: 2px 6px;
  background: $color-gray-100;
  border-radius: $radius-sm;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.content-list__score {
  font-weight: $font-weight-bold;

  &--high {
    color: $color-success;
  }

  &--medium {
    color: $color-warning;
  }

  &--low {
    color: $color-danger;
  }

  &--none {
    color: $color-text-placeholder;
  }
}

.content-list__account-platform {
  color: $color-text-secondary;
  font-size: $font-size-xs;
}

// 分页
.content-list__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-lg;
}
</style>
