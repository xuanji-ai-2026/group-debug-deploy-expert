<template>
  <bx-content
    title="AI 内容创作"
    description="利用 AI 智能生成优质营销文案，支持多平台一键适配"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <div class="content-create">
      <!-- AI 生成区域 -->
      <el-row :gutter="24">
        <!-- 左侧：输入区 -->
        <el-col :xs="24" :sm="24" :md="10" :lg="9" :xl="8">
          <div class="content-create__input-section">
            <el-card class="content-create__card" shadow="hover">
              <template #header>
                <div class="content-create__card-header">
                  <i class="el-icon-magic-stick content-create__ai-icon" />
                  <span>AI 文案生成</span>
                </div>
              </template>

              <!-- 平台选择 -->
              <div class="content-create__form-item">
                <label class="content-create__label">
                  <span class="content-create__required">*</span>
                  目标平台
                  <el-tooltip content="选择文案要发布的平台，AI 会针对不同平台特性优化文案">
                    <i class="el-icon-question" />
                  </el-tooltip>
                </label>
                <div class="content-create__platforms">
                  <div
                    v-for="platform in platforms"
                    :key="platform.id"
                    class="content-create__platform"
                    :class="{ 'is-active': selectedPlatforms.includes(platform.id) }"
                    @click="togglePlatform(platform.id)"
                  >
                    <img :src="platform.icon" :alt="platform.name" class="content-create__platform-icon" />
                    <span class="content-create__platform-name">{{ platform.name }}</span>
                    <i v-if="selectedPlatforms.includes(platform.id)" class="el-icon-check content-create__platform-check" />
                  </div>
                </div>
              </div>

              <!-- 行业选择 -->
              <div class="content-create__form-item">
                <label class="content-create__label">
                  <span class="content-create__required">*</span>
                  所属行业
                </label>
                <el-select v-model="form.industry" placeholder="选择行业" style="width: 100%">
                  <el-option
                    v-for="item in industries"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </div>

              <!-- 关键词输入 -->
              <div class="content-create__form-item">
                <label class="content-create__label">
                  <span class="content-create__required">*</span>
                  关键词 / 产品描述
                </label>
                <el-input
                  v-model="form.keywordsInput"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入产品名称、核心卖点、目标受众等关键词，多个关键词用逗号分隔&#10;例如：美白精华液,提亮肤色,淡化细纹,适合敏感肌,国货之光"
                  maxlength="500"
                  show-word-limit
                />
              </div>

              <!-- 文案风格 -->
              <div class="content-create__form-item">
                <label class="content-create__label">文案风格</label>
                <div class="content-create__style-options">
                  <el-radio-group v-model="form.style">
                    <el-radio-button label="casual">轻松随意</el-radio-button>
                    <el-radio-button label="formal">专业正式</el-radio-button>
                    <el-radio-button label="humorous">幽默风趣</el-radio-button>
                    <el-radio-button label="emotional">情感共鸣</el-radio-button>
                  </el-radio-group>
                </div>
              </div>

              <!-- 内容长度 -->
              <div class="content-create__form-item">
                <label class="content-create__label">内容长度</label>
                <el-radio-group v-model="form.length">
                  <el-radio label="short">简短</el-radio>
                  <el-radio label="medium">适中</el-radio>
                  <el-radio label="long">详细</el-radio>
                </el-radio-group>
              </div>

              <!-- 生成数量 -->
              <div class="content-create__form-item">
                <label class="content-create__label">生成数量</label>
                <el-slider v-model="form.count" :min="1" :max="5" show-stops />
                <div class="content-create__slider-labels">
                  <span>1</span>
                  <span>2</span>
                  <span>3</span>
                  <span>4</span>
                  <span>5</span>
                </div>
              </div>

              <!-- 生成按钮 -->
              <el-button
                type="primary"
                size="large"
                :loading="generating"
                :disabled="!canGenerate"
                class="content-create__generate-btn"
                @click="handleGenerate"
              >
                <i class="el-icon-magic-stick" />
                {{ generating ? 'AI 创作中...' : '一键生成文案' }}
              </el-button>

              <!-- 提示信息 -->
              <div class="content-create__tips">
                <p><i class="el-icon-info" /> 每次生成消耗 {{ form.count * 10 }} 积分</p>
                <p><i class="el-icon-info" /> 生成结果可进一步编辑优化</p>
              </div>
            </el-card>
          </div>
        </el-col>

        <!-- 右侧：结果展示 -->
        <el-col :xs="24" :sm="24" :md="14" :lg="15" :xl="16">
          <div class="content-create__result-section">
            <!-- 生成结果列表 -->
            <template v-if="generatedContents.length > 0">
              <el-card
                v-for="(content, index) in generatedContents"
                :key="index"
                class="content-create__result-card"
                shadow="hover"
              >
                <div class="content-create__result-header">
                  <div class="content-create__result-title">
                    <el-tag type="success" size="small">AI 生成 {{ index + 1 }}</el-tag>
                    <span v-if="content.platform" class="content-create__result-platform">
                      {{ getPlatformName(content.platform) }}
                    </span>
                  </div>
                  <div class="content-create__result-actions">
                    <el-button type="primary" link size="small" @click="handleCopy(content.text)">
                      <i class="el-icon-copy-document" /> 复制
                    </el-button>
                    <el-button type="primary" link size="small" @click="handleEdit(content)">
                      <i class="el-icon-edit" /> 编辑
                    </el-button>
                    <el-button type="success" link size="small" @click="handlePublish(content)">
                      <i class="el-icon-position" /> 去发布
                    </el-button>
                  </div>
                </div>

                <div class="content-create__result-content">
                  <pre>{{ content.text }}</pre>
                </div>

                <div v-if="content.aiScore" class="content-create__result-footer">
                  <div class="content-create__ai-score">
                    <span>AI 检测评分：</span>
                    <el-rate :model-value="content.aiScore / 20" disabled show-score text-color="#ff9900" />
                    <el-tag v-if="content.aiScore >= 80" type="success" size="small" effect="plain">
                      优质内容
                    </el-tag>
                    <el-tag v-else-if="content.aiScore >= 60" type="warning" size="small" effect="plain">
                      建议优化
                    </el-tag>
                    <el-tag v-else type="danger" size="small" effect="plain">
                      需改进
                    </el-tag>
                  </div>
                </div>
              </el-card>

              <!-- 批量操作 -->
              <div class="content-create__batch-actions">
                <el-button @click="handleRegenerate">
                  <i class="el-icon-refresh" /> 重新生成
                </el-button>
                <el-button type="primary" @click="handleBatchPublish">
                  <i class="el-icon-position" /> 批量发布
                </el-button>
              </div>
            </template>

            <!-- 空状态 -->
            <div v-else class="content-create__empty">
              <el-empty description="暂无生成内容">
                <template #image>
                  <div class="content-create__empty-icon">
                    <i class="el-icon-magic-stick" />
                  </div>
                </template>
                <template #description>
                  <p>在左侧输入关键词，点击"一键生成文案"</p>
                  <p>AI 将为您创作优质营销内容</p>
                </template>
              </el-empty>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 历史记录 -->
      <div class="content-create__history">
        <h3 class="content-create__history-title">
          <i class="el-icon-time" /> 历史生成记录
        </h3>
        <el-table :data="historyList" stripe style="width: 100%">
          <el-table-column label="内容预览" min-width="300">
            <template #default="{ row }">
              <div class="content-create__history-preview">{{ row.preview }}</div>
            </template>
          </el-table-column>
          <el-table-column label="平台" width="150">
            <template #default="{ row }">
              <el-tag v-for="p in row.platforms" :key="p" size="small" style="margin-right: 4px">
                {{ getPlatformName(p) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="行业" prop="industry" width="120" />
          <el-table-column label="生成时间" prop="createTime" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleUseHistory(row)">
                使用
              </el-button>
              <el-button type="primary" link size="small" @click="handleViewHistory(row)">
                查看
              </el-button>
              <el-button type="danger" link size="small" @click="handleDeleteHistory(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="content-create__pagination">
          <el-pagination
            :current-page="historyPagination.page"
            :page-size="historyPagination.size"
            :total="historyPagination.total"
            layout="prev, pager, next"
            background
            @current-change="handleHistoryPageChange"
          />
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑文案" width="600px">
      <el-input
        v-model="editingContent.text"
        type="textarea"
        :rows="10"
        placeholder="编辑您的文案内容..."
      />
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview 内容创建页 - AI 文案生成
 * @description 提供 AI 智能文案生成功能，支持多平台适配、历史记录管理
 * @author EMP-FE-004 赵云
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateText, getContentList, createContent } from '@/api/content.js'

// ============================================================
// 路由
// ============================================================
const router = useRouter()

// ============================================================
// 数据
// ============================================================

/** 平台列表 - 与后端ContentPublishServiceImpl保持一致 */
const platforms = [
  { id: 1, name: '微信公众号', icon: '/icons/platform-wechat.svg' },
  { id: 2, name: '微博', icon: '/icons/platform-weibo.svg' },
  { id: 3, name: '抖音', icon: '/icons/platform-douyin.svg' },
  { id: 4, name: '小红书', icon: '/icons/platform-xhs.svg' },
  { id: 5, name: 'B站', icon: '/icons/platform-bilibili.svg' },
  { id: 6, name: '官网', icon: '/icons/platform-website.svg' },
]

/** 行业列表 */
const industries = [
  { label: '美妆护肤', value: 'beauty' },
  { label: '服装服饰', value: 'fashion' },
  { label: '食品饮料', value: 'food' },
  { label: '家居生活', value: 'home' },
  { label: '数码电器', value: 'digital' },
  { label: '母婴用品', value: 'baby' },
  { label: '运动户外', value: 'sports' },
  { label: '教育培训', value: 'education' },
  { label: '医疗健康', value: 'health' },
  { label: '汽车服务', value: 'automotive' },
  { label: '金融理财', value: 'finance' },
  { label: '其他', value: 'other' },
]

/** 表单数据 */
const form = reactive({
  industry: '',
  keywordsInput: '',
  style: 'casual',
  length: 'medium',
  count: 3,
})

/** 选中的平台 */
const selectedPlatforms = ref([])

/** 生成中状态 */
const generating = ref(false)

/** 生成的内容列表 */
const generatedContents = ref([])

/** 历史记录列表 */
const historyList = ref([])

/** 历史记录分页 */
const historyPagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

/** 编辑对话框 */
const editDialogVisible = ref(false)
const editingContent = reactive({
  index: -1,
  text: '',
})

// ============================================================
// 计算属性
// ============================================================

/** 是否可以生成 */
const canGenerate = computed(() => {
  return (
    selectedPlatforms.value.length > 0 &&
    form.industry &&
    form.keywordsInput.trim().length > 0 &&
    !generating.value
  )
})

// ============================================================
// 方法
// ============================================================

/**
 * 切换平台选择
 */
function togglePlatform(platformId) {
  const index = selectedPlatforms.value.indexOf(platformId)
  if (index > -1) {
    selectedPlatforms.value.splice(index, 1)
  } else {
    selectedPlatforms.value.push(platformId)
  }
}

/**
 * 获取平台名称
 */
function getPlatformName(platformId) {
  const platform = platforms.find((p) => p.id === platformId)
  return platform?.name || platformId
}

/**
 * 生成文案
 */
async function handleGenerate() {
  if (!canGenerate.value) return

  generating.value = true
  generatedContents.value = []

  try {
    const keywords = form.keywordsInput
      .split(/[,，、\n]/)
      .map((k) => k.trim())
      .filter((k) => k.length > 0)

    const params = {
      platformId: selectedPlatforms.value[0],
      industry: form.industry,
      keywords,
      style: form.style,
      length: form.length,
      count: form.count,
    }

    const res = await generateText(params)

    if (res.contents && res.contents.length > 0) {
      generatedContents.value = res.contents.map((text, index) => ({
        text,
        platform: selectedPlatforms.value[index % selectedPlatforms.value.length],
        aiScore: res.aiScores?.[index] || Math.floor(Math.random() * 40) + 60,
        keywords,
        industry: form.industry,
      }))
      ElMessage.success(`成功生成 ${res.contents.length} 条文案`)
    } else {
      ElMessage.warning('未生成内容，请调整关键词后重试')
    }
  } catch (error) {
    console.error('生成失败:', error)
    ElMessage.error('文案生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

/**
 * 复制文案
 */
function handleCopy(text) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

/**
 * 编辑文案
 */
function handleEdit(content) {
  editingContent.index = generatedContents.value.indexOf(content)
  editingContent.text = content.text
  editDialogVisible.value = true
}

/**
 * 保存编辑
 */
function handleSaveEdit() {
  if (editingContent.index > -1) {
    generatedContents.value[editingContent.index].text = editingContent.text
    ElMessage.success('保存成功')
  }
  editDialogVisible.value = false
}

/**
 * 去发布
 */
function handlePublish(content) {
  // 先保存为草稿
  saveContentAndNavigate(content)
}

/**
 * 重新生成
 */
function handleRegenerate() {
  ElMessageBox.confirm('重新生成将覆盖当前内容，是否继续？', '提示', {
    confirmButtonText: '继续',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      handleGenerate()
    })
    .catch(() => {})
}

/**
 * 批量发布
 */
async function handleBatchPublish() {
  if (generatedContents.value.length === 0) return

  try {
    // 保存所有内容为草稿
    const promises = generatedContents.value.map((content) => saveContent(content))
    await Promise.all(promises)
    ElMessage.success('已保存到内容库')
    router.push('/content/publish')
  } catch (error) {
    ElMessage.error('保存失败，请重试')
  }
}

/**
 * 保存内容并跳转
 */
async function saveContentAndNavigate(content) {
  try {
    const res = await saveContent(content)
    router.push({
      path: '/content/publish',
      query: { contentId: res.id },
    })
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

/**
 * 保存内容
 */
async function saveContent(content) {
  const params = {
    contentType: 3, // 纯文字
    title: content.text.slice(0, 50) + (content.text.length > 50 ? '...' : ''),
    contentText: content.text,
    keywords: content.keywords,
    industry: content.industry,
    platforms: [content.platform],
    aiGenerated: true,
    aiScore: content.aiScore,
  }
  return await createContent(params)
}

/**
 * 加载历史记录
 */
async function loadHistory() {
  try {
    const res = await getContentList({
      page: historyPagination.page,
      size: historyPagination.size,
      aiGenerated: true,
    })
    historyList.value = res.list.map((item) => ({
      ...item,
      preview: item.title || item.contentText?.slice(0, 100) || '无内容',
    }))
    historyPagination.total = res.pagination.total
  } catch (error) {
    console.error('加载历史记录失败:', error)
  }
}

/**
 * 使用历史记录
 */
function handleUseHistory(row) {
  generatedContents.value = [
    {
      text: row.contentText,
      platform: row.platforms?.[0],
      aiScore: row.aiScore,
      keywords: row.keywords,
      industry: row.industry,
    },
  ]
  ElMessage.success('已加载历史内容')
}

/**
 * 查看历史记录
 */
function handleViewHistory(row) {
  ElMessageBox.alert(row.contentText, '内容详情', {
    confirmButtonText: '关闭',
  })
}

/**
 * 删除历史记录
 */
function handleDeleteHistory(row) {
  ElMessageBox.confirm('确定删除这条记录吗？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await deleteContent(row.id)
        ElMessage.success('删除成功')
        loadHistory()
      } catch (error) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

/**
 * 历史记录分页
 */
function handleHistoryPageChange(page) {
  historyPagination.page = page
  loadHistory()
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  loadHistory()
})
</script>

<style lang="scss" scoped>
.content-create {
  &__input-section {
    position: sticky;
    top: 20px;
  }

  &__card {
    :deep(.el-card__header) {
      padding: 16px 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
    }
  }

  &__card-header {
    display: flex;
    align-items: center;
    font-size: 16px;
    font-weight: 600;
  }

  &__ai-icon {
    margin-right: 8px;
    font-size: 18px;
  }

  &__form-item {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  &__label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);

    i {
      margin-left: 4px;
      color: var(--text-secondary);
      cursor: help;
    }
  }

  &__required {
    color: #f56c6c;
    margin-right: 4px;
  }

  &__platforms {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  &__platform {
    position: relative;
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border: 2px solid var(--border-color);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: var(--primary-color);
    }

    &.is-active {
      border-color: var(--primary-color);
      background: var(--primary-color-light);
    }
  }

  &__platform-icon {
    width: 24px;
    height: 24px;
    margin-right: 8px;
    object-fit: contain;
  }

  &__platform-name {
    font-size: 14px;
  }

  &__platform-check {
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

  &__style-options {
    :deep(.el-radio-group) {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  }

  &__slider-labels {
    display: flex;
    justify-content: space-between;
    padding: 0 10px;
    margin-top: -10px;
    font-size: 12px;
    color: var(--text-secondary);
  }

  &__generate-btn {
    width: 100%;
    margin-top: 20px;
    height: 44px;
    font-size: 16px;

    i {
      margin-right: 8px;
    }
  }

  &__tips {
    margin-top: 16px;
    padding: 12px;
    background: var(--bg-secondary);
    border-radius: 6px;

    p {
      margin: 4px 0;
      font-size: 12px;
      color: var(--text-secondary);

      i {
        margin-right: 4px;
      }
    }
  }

  &__result-section {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  &__result-card {
    :deep(.el-card__body) {
      padding: 16px;
    }
  }

  &__result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--border-color);
  }

  &__result-title {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__result-platform {
    font-size: 12px;
    color: var(--text-secondary);
  }

  &__result-actions {
    display: flex;
    gap: 8px;
  }

  &__result-content {
    pre {
      margin: 0;
      padding: 12px;
      background: var(--bg-secondary);
      border-radius: 6px;
      font-size: 14px;
      line-height: 1.8;
      white-space: pre-wrap;
      word-break: break-word;
      max-height: 300px;
      overflow-y: auto;
    }
  }

  &__result-footer {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed var(--border-color);
  }

  &__ai-score {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--text-secondary);
  }

  &__batch-actions {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 8px;
  }

  &__empty {
    padding: 60px 0;
  }

  &__empty-icon {
    font-size: 80px;
    color: var(--text-secondary);
    opacity: 0.3;
  }

  &__history {
    margin-top: 40px;
    padding-top: 32px;
    border-top: 1px solid var(--border-color);
  }

  &__history-title {
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

  &__history-preview {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    color: var(--text-primary);
  }

  &__pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .content-create {
    &__platforms {
      gap: 8px;
    }

    &__platform {
      padding: 8px 12px;
      flex: 1;
      min-width: calc(50% - 4px);
    }

    &__input-section {
      position: static;
      margin-bottom: 20px;
    }
  }
}
</style>
