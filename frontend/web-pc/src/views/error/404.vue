<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-code">404</div>
      <div class="error-title">页面不存在</div>
      <div class="error-desc">抱歉，您访问的页面不存在或已被删除</div>
      <div class="error-actions">
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 404 - 页面不存在
 */
import { useRouter } from 'vue-router'
const router = useRouter()
function goHome() { router.push('/dashboard') }
function goBack() { router.back() }
</script>

<style lang="scss" scoped>
.error-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $color-bg-page;
}

.error-content {
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: 800;
  color: $color-primary;
  line-height: 1;
  margin-bottom: 16px;
  opacity: 0.2;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  color: $color-text-primary;
  margin-bottom: 8px;
}

.error-desc {
  font-size: $font-size-base;
  color: $color-text-secondary;
  margin-bottom: 32px;
}

.error-actions {
  display: flex;
  gap: $spacing-base;
  justify-content: center;
}
</style>
