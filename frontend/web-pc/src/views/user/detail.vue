<template>
  <div class="user-detail-page">
    <div class="page-header">
      <h2>用户详情</h2>
      <el-button @click="goBack">返回</el-button>
    </div>
    <div class="page-content">
      <el-card>
        <div class="user-info">
          <p>用户ID: {{ $route.params.id }}</p>
          <p>用户详情页面开发中...</p>
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
.user-detail-page {
  padding: $spacing-lg;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
}

.page-content {
  .user-info {
    padding: $spacing-lg;
  }
}
</style>
