<template>
  <div class="content-edit-page">
    <div class="page-header">
      <h2>编辑内容</h2>
      <el-button @click="goBack">返回</el-button>
    </div>
    <div class="page-content">
      <el-card>
        <div class="content-edit">
          <p>内容ID: {{ $route.params.id }}</p>
          <p>内容编辑页面开发中...</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function goBack() { router.back() }
</script>

<style lang="scss" scoped>
.content-edit-page {
  padding: $spacing-lg;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
}

.page-content {
  .content-edit {
    padding: $spacing-lg;
  }
}
</style>
