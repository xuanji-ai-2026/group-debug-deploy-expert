<template>
  <div class="dict-page">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="queryKeyword"
          placeholder="搜索字典名称/编码"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handleQuery">查询</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增字典
        </el-button>
      </div>
    </div>

    <!-- 字典列表 -->
    <el-table
      v-loading="loading"
      :data="dictList"
      stripe
      row-key="dictId"
      style="width: 100%; margin-top: 16px"
    >
      <el-table-column prop="dictId" label="字典ID" width="100" />
      <el-table-column prop="dictName" label="字典名称" min-width="150" />
      <el-table-column prop="dictCode" label="字典编码" min-width="150">
        <template #default="{ row }">
          <span class="dict-code">{{ row.dictCode }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="dictType" label="字典类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ getDictTypeLabel(row.dictType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="itemCount" label="字典项" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="info" size="small">{{ row.itemCount }} 条</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleEditItems(row)">字典项</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadDictList"
      />
    </div>

    <!-- 新增/编辑字典弹窗 -->
    <el-dialog
      v-model="dictDialogVisible"
      :title="dictForm.dictId ? '编辑字典' : '新增字典'"
      width="500px"
      @close="resetDictForm"
    >
      <el-form ref="dictFormRef" :model="dictForm" :rules="dictRules" label-width="100px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="dictForm.dictName" placeholder="请输入字典名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="字典编码" prop="dictCode">
          <el-input v-model="dictForm.dictCode" placeholder="请输入字典编码（英文字母）" :disabled="!!dictForm.dictId" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-select v-model="dictForm.dictType" placeholder="请选择字典类型" style="width: 100%">
            <el-option label="系统字典" :value="1" />
            <el-option label="业务字典" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dictForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dictForm.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dictSubmitting" @click="submitDictForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典项管理弹窗 -->
    <el-dialog
      v-model="itemsDialogVisible"
      :title="`字典项管理 - ${currentDict?.dictName}`"
      width="900px"
      top="5vh"
    >
      <div class="items-toolbar">
        <el-button type="primary" size="small" @click="handleAddItem">
          <el-icon><Plus /></el-icon> 新增字典项
        </el-button>
      </div>
      <el-table :data="dictItemList" stripe style="margin-top: 12px" max-height="500">
        <el-table-column prop="itemId" label="项ID" width="80" />
        <el-table-column prop="itemText" label="字典项文本" min-width="150">
          <template #default="{ row }">
            <span v-if="!row._editing">{{ row.itemText }}</span>
            <el-input v-else v-model="row._editForm.itemText" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="itemValue" label="字典项值" min-width="120">
          <template #default="{ row }">
            <span v-if="!row._editing">{{ row.itemValue }}</span>
            <el-input v-else v-model="row._editForm.itemValue" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="100">
          <template #default="{ row }">
            <span v-if="!row._editing">{{ row.sort }}</span>
            <el-input-number v-else v-model="row._editForm.sort" size="small" :min="0" :max="999" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="!row._editing"
              @change="handleItemStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="!row._editing">
              <el-button link type="primary" size="small" @click="startEditItem(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDeleteItem(row)">删除</el-button>
            </template>
            <template v-else>
              <el-button link type="success" size="small" @click="saveEditItem(row)">保存</el-button>
              <el-button link type="default" size="small" @click="cancelEditItem(row)">取消</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 新增/编辑字典项弹窗 -->
    <el-dialog
      v-model="itemDialogVisible"
      :title="itemForm.itemId ? '编辑字典项' : '新增字典项'"
      width="450px"
    >
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="100px">
        <el-form-item label="字典项文本" prop="itemText">
          <el-input v-model="itemForm.itemText" placeholder="请输入字典项文本" maxlength="50" />
        </el-form-item>
        <el-form-item label="字典项值" prop="itemValue">
          <el-input v-model="itemForm.itemValue" placeholder="请输入字典项值" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="itemForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="itemForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSubmitting" @click="submitItemForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 字典管理页面
 * 功能：字典类型管理、字典项CRUD
 * @author 张前端 (EMP-FE-001)
 */

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { request } from '@/api'

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const queryKeyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dictList = ref([])

const dictDialogVisible = ref(false)
const dictSubmitting = ref(false)
const dictFormRef = ref(null)
const dictForm = reactive({
  dictId: '',
  dictName: '',
  dictCode: '',
  dictType: 1,
  status: 1,
  remark: ''
})
const dictRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictCode: [
    { required: true, message: '请输入字典编码', trigger: 'blur' },
    { pattern: /^[A-Za-z_][A-Za-z0-9_]*$/, message: '编码格式：英文字母、数字、下划线', trigger: 'blur' }
  ],
  dictType: [{ required: true, message: '请选择字典类型', trigger: 'change' }]
}

const itemsDialogVisible = ref(false)
const currentDict = ref(null)
const dictItemList = ref([])

const itemDialogVisible = ref(false)
const itemSubmitting = ref(false)
const itemFormRef = ref(null)
const itemForm = reactive({
  itemId: '',
  dictId: '',
  itemText: '',
  itemValue: '',
  sort: 0,
  status: 1,
  remark: ''
})
const itemRules = {
  itemText: [{ required: true, message: '请输入字典项文本', trigger: 'blur' }],
  itemValue: [{ required: true, message: '请输入字典项值', trigger: 'blur' }]
}

// ============================================================
// 工具函数
// ============================================================
function getDictTypeLabel(type) {
  const map = { 1: '系统字典', 2: '业务字典' }
  return map[type] || '未知'
}

// ============================================================
// 字典管理
// ============================================================
async function loadDictList() {
  loading.value = true
  try {
    const data = await request.get('/admin/dicts', {
      page: page.value,
      pageSize: pageSize.value,
      keyword: queryKeyword.value || undefined
    })
    dictList.value = data?.list || getDemoDictList()
    total.value = data?.pagination?.total || dictList.value.length
  } catch {
    dictList.value = getDemoDictList()
    total.value = dictList.value.length
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  page.value = 1
  loadDictList()
}

function handleAdd() {
  dictForm.dictId = ''
  dictDialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(dictForm, {
    dictId: row.dictId,
    dictName: row.dictName,
    dictCode: row.dictCode,
    dictType: row.dictType,
    status: row.status,
    remark: row.remark || ''
  })
  dictDialogVisible.value = true
}

async function submitDictForm() {
  const valid = await dictFormRef.value.validate().catch(() => false)
  if (!valid) return
  dictSubmitting.value = true
  try {
    if (dictForm.dictId) {
      await request.put(`/admin/dicts/${dictForm.dictId}`, dictForm)
    } else {
      await request.post('/admin/dicts', dictForm)
    }
    ElMessage.success('保存成功')
    dictDialogVisible.value = false
    loadDictList()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    dictSubmitting.value = false
  }
}

function resetDictForm() {
  dictFormRef.value?.resetFields()
  Object.assign(dictForm, { dictId: '', dictName: '', dictCode: '', dictType: 1, status: 1, remark: '' })
}

async function handleStatusChange(row) {
  try {
    await request.put(`/admin/dicts/${row.dictId}/status`, { status: row.status })
    ElMessage.success('状态更新成功')
  } catch {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除字典"${row.dictName}"吗？删除后不可恢复。`, '删除确认', { type: 'warning' })
  try {
    await request.delete(`/admin/dicts/${row.dictId}`)
    ElMessage.success('删除成功')
    loadDictList()
  } catch {
    ElMessage.error('删除失败')
  }
}

// ============================================================
// 字典项管理
// ============================================================
async function handleEditItems(row) {
  currentDict.value = row
  itemsDialogVisible.value = true
  await loadDictItems(row.dictId)
}

async function loadDictItems(dictId) {
  try {
    const data = await request.get(`/admin/dicts/${dictId}/items`)
    dictItemList.value = (data || []).map(item => ({ ...item, _editing: false, _editForm: {} }))
  } catch {
    dictItemList.value = getDemoDictItems()
  }
}

function handleAddItem() {
  itemForm.itemId = ''
  itemForm.dictId = currentDict.value.dictId
  itemForm.itemText = ''
  itemForm.itemValue = ''
  itemForm.sort = dictItemList.value.length
  itemForm.status = 1
  itemForm.remark = ''
  itemDialogVisible.value = true
}

async function submitItemForm() {
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return
  itemSubmitting.value = true
  try {
    if (itemForm.itemId) {
      await request.put(`/admin/dict-items/${itemForm.itemId}`, itemForm)
    } else {
      await request.post('/admin/dict-items', itemForm)
    }
    ElMessage.success('保存成功')
    itemDialogVisible.value = false
    await loadDictItems(currentDict.value.dictId)
    // 刷新字典列表的项数量
    loadDictList()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    itemSubmitting.value = false
  }
}

function startEditItem(row) {
  row._editing = true
  row._editForm = { itemText: row.itemText, itemValue: row.itemValue, sort: row.sort, status: row.status, remark: row.remark || '' }
}

async function saveEditItem(row) {
  try {
    await request.put(`/admin/dict-items/${row.itemId}`, row._editForm)
    ElMessage.success('保存成功')
    row._editing = false
    Object.assign(row, row._editForm)
  } catch {
    ElMessage.error('保存失败')
  }
}

function cancelEditItem(row) {
  row._editing = false
  delete row._editForm
}

async function handleItemStatusChange(row) {
  try {
    await request.put(`/admin/dict-items/${row.itemId}/status`, { status: row.status })
    ElMessage.success('状态更新成功')
  } catch {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

async function handleDeleteItem(row) {
  await ElMessageBox.confirm(`确定删除字典项"${row.itemText}"吗？`, '删除确认', { type: 'warning' })
  try {
    await request.delete(`/admin/dict-items/${row.itemId}`)
    ElMessage.success('删除成功')
    await loadDictItems(currentDict.value.dictId)
    loadDictList()
  } catch {
    ElMessage.error('删除失败')
  }
}

// ============================================================
// 演示数据
// ============================================================
function getDemoDictList() {
  return [
    { dictId: 1, dictName: '性别', dictCode: 'sys_gender', dictType: 1, itemCount: 2, status: 1, remark: '系统基础字典', createTime: '2024-01-10 10:30:00' },
    { dictId: 2, dictName: '账号状态', dictCode: 'account_status', dictType: 1, itemCount: 4, status: 1, remark: '社媒账号状态', createTime: '2024-01-10 10:35:00' },
    { dictId: 3, dictName: '商机意向等级', dictCode: 'lead_intent_level', dictType: 2, itemCount: 4, status: 1, remark: '商机意向程度', createTime: '2024-01-11 09:00:00' },
    { dictId: 4, dictName: '商机来源', dictCode: 'lead_source', dictType: 2, itemCount: 3, status: 1, remark: '商机获取渠道', createTime: '2024-01-11 09:10:00' },
    { dictId: 5, dictName: '跟进方式', dictCode: 'follow_type', dictType: 2, itemCount: 4, status: 1, remark: '跟进记录类型', createTime: '2024-01-12 14:20:00' },
    { dictId: 6, dictName: '平台类型', dictCode: 'platform_type', dictType: 1, itemCount: 5, status: 1, remark: '社媒平台类型', createTime: '2024-01-12 14:30:00' },
    { dictId: 7, dictName: '任务状态', dictCode: 'task_status', dictType: 1, itemCount: 5, status: 1, remark: '获客任务状态', createTime: '2024-01-13 08:00:00' },
    { dictId: 8, dictName: '消息类型', dictCode: 'message_type', dictType: 1, itemCount: 5, status: 0, remark: '私信消息类型', createTime: '2024-01-13 08:10:00' }
  ]
}

function getDemoDictItems() {
  return [
    { itemId: 1, itemText: '高意向', itemValue: 'high', sort: 1, status: 1, remark: '强购买意向' },
    { itemId: 2, itemText: '中意向', itemValue: 'medium', sort: 2, status: 1, remark: '中等购买意向' },
    { itemId: 3, itemText: '低意向', itemValue: 'low', sort: 3, status: 1, remark: '较弱购买意向' },
    { itemId: 4, itemText: '无意向', itemValue: 'none', sort: 4, status: 0, remark: '暂无购买意向' }
  ]
}

// ============================================================
// 初始化
// ============================================================
onMounted(() => {
  loadDictList()
})
</script>

<style lang="scss" scoped>
.dict-page {
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

.dict-code {
  font-family: 'Courier New', monospace;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.items-toolbar {
  margin-bottom: 4px;
}
</style>
