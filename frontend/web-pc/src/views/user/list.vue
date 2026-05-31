<template>
  <bx-content
    title="用户列表"
    description="管理系统内的用户账号和权限"
    :show-header="true"
    :show-breadcrumb="true"
  >
    <!-- 操作栏 -->
    <div class="user-list__toolbar">
      <div class="user-list__toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名/手机号"
          prefix-icon="el-icon-search"
          clearable
          style="width: 240px"
          @change="handleSearch"
        />
        <el-select v-model="filterRoleType" placeholder="角色类型" clearable style="width: 140px" @change="handleFilterChange">
          <el-option label="租户管理员" :value="3" />
          <el-option label="操作员" :value="4" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px" @change="handleFilterChange">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </div>
      <div class="user-list__toolbar-right">
        <el-button type="primary" @click="handleCreate">
          <i class="el-icon-plus" />
          新建用户
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="user-list__table">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-list__user-cell">
              <el-avatar :size="36" :src="row.avatar">
                {{ row.realName?.charAt(0) || row.phone?.charAt(0) }}
              </el-avatar>
              <div class="user-list__user-info">
                <span class="user-list__user-name">{{ row.realName || '-' }}</span>
                <span class="user-list__user-phone">{{ row.phone }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" prop="roleTypeLabel" width="120" align="center" />
        <el-table-column label="手机号" prop="phone" width="140" align="center" />
        <el-table-column label="状态" prop="statusLabel" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" prop="lastLoginTime" width="180" align="center" />
        <el-table-column label="创建时间" prop="createTime" width="180" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">
              查看
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="user-list__pagination">
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

    <!-- 创建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="roleType">
          <el-select v-model="formData.roleType" placeholder="请选择角色" style="width: 100%">
            <el-option label="租户管理员" :value="3" />
            <el-option label="操作员" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入初始密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </bx-content>
</template>

<script setup>
/**
 * @fileoverview UserList 用户列表页面
 * @description 管理用户账号，支持搜索、筛选、创建、编辑、启用/禁用等操作
 * @author EMP-FE-001 张婷
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTable } from '@/composables/useTable.js'
import * as userApi from '@/api/user.js'

// ============================================================
// Router
// ============================================================
const router = useRouter()

// ============================================================
// 表格数据
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
const filterRoleType = ref('')
const filterStatus = ref('')

// 选中行
const selectedRows = ref([])

// ============================================================
// 弹窗相关
// ============================================================
const dialogVisible = ref(false)
const dialogTitle = ref('新建用户')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

// 表单数据
const formData = reactive({
  id: null,
  phone: '',
  realName: '',
  roleType: 4,
  password: '',
})

// 表单验证
const formRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
  ],
  roleType: [
    { required: true, message: '请选择角色', trigger: 'change' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
  ],
}

// 角色类型映射
const roleTypeMap = {
  1: '超级管理员',
  2: '运维管理员',
  3: '租户管理员',
  4: '操作员',
}

// 状态映射
const statusMap = {
  0: { label: '禁用', type: 'danger' },
  1: { label: '正常', type: 'success' },
}

// ============================================================
// 方法
// ============================================================

/**
 * 加载用户列表数据
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
    if (filterRoleType.value) {
      params.roleType = filterRoleType.value
    }
    if (filterStatus.value !== '') {
      params.status = filterStatus.value
    }

    const response = await userApi.getUserList(params)

    // 格式化数据
    tableData.value = (response.data?.list || []).map((item) => ({
      ...item,
      roleTypeLabel: roleTypeMap[item.roleType] || '-',
      statusLabel: statusMap[item.status]?.label || '-',
    }))

    // 更新分页
    if (response.data?.pagination) {
      pagination.total = response.data.pagination.total || 0
    }
  } catch (error) {
    console.error('[UserList] Load data failed:', error)
    tableData.value = []
    pagination.total = 0
    ElMessage.error('加载用户数据失败，请稍后重试')
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
 * 新建用户
 */
function handleCreate() {
  dialogTitle.value = '新建用户'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

/**
 * 查看用户
 */
function handleView(row) {
  router.push(`/user/detail/${row.id}`)
}

/**
 * 编辑用户
 */
function handleEdit(row) {
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  formData.id = row.id
  formData.phone = row.phone
  formData.realName = row.realName
  formData.roleType = row.roleType
  formData.password = ''
  dialogVisible.value = true
}

/**
 * 启用/禁用用户
 */
async function handleToggleStatus(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户「${row.realName || row.phone}」吗？`,
      `确认${action}`,
      { type: 'warning' }
    )

    await userApi.toggleUserStatus(row.id, row.status !== 1)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[UserList] Toggle status failed:', error)
    }
  }
}

/**
 * 提交表单
 */
async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await userApi.updateUser(formData.id, {
        realName: formData.realName,
        roleType: formData.roleType,
      })
      ElMessage.success('用户信息更新成功')
    } else {
      await userApi.createUser({
        phone: formData.phone,
        realName: formData.realName,
        roleType: formData.roleType,
        password: formData.password,
      })
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[UserList] Submit failed:', error)
  } finally {
    submitLoading.value = false
  }
}

/**
 * 重置表单
 */
function resetForm() {
  formData.id = null
  formData.phone = ''
  formData.realName = ''
  formData.roleType = 4
  formData.password = ''
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
.user-list__toolbar {
  @include flex-between;
  margin-bottom: $spacing-lg;

  &-left {
    display: flex;
    gap: $spacing-sm;
  }
}

// 表格
.user-list__table {
  background: $color-white;
  border-radius: $radius-lg;
  padding: $spacing-lg;
}

.user-list__user-cell {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.user-list__user-info {
  display: flex;
  flex-direction: column;
}

.user-list__user-name {
  font-weight: $font-weight-medium;
  color: $color-text-primary;
}

.user-list__user-phone {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

// 分页
.user-list__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-lg;
}
</style>
