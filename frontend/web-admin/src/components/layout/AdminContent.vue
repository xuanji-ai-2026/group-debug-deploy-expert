<template>
  <main class="admin-content">
    <div class="content-wrapper">
      <slot />
    </div>
  </main>
</template>

<script setup>
/**
 * AdminContent.vue - 内容区域组件
 * 功能：作为页面内容的容器，提供统一的内边距和背景色
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */
</script>

<style lang="scss" scoped>
.admin-content {
  flex: 1;
  overflow-y: auto;
  background-color: $bg-color-page;
}

.content-wrapper {
  padding: $content-padding;
  min-height: calc(100vh - #{$header-height});
}
</style>
