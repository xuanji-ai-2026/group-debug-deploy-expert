<template>
  <el-breadcrumb class="bx-breadcrumb" separator="/">
    <el-breadcrumb-item
      v-for="(item, index) in breadcrumbs"
      :key="index"
      :to="index < breadcrumbs.length - 1 && item.path ? { path: item.path } : undefined"
    >
      <span
        v-if="index < breadcrumbs.length - 1 && item.path"
        class="bx-breadcrumb__link"
        @click="handleClick(item)"
      >
        {{ item.title }}
      </span>
      <span v-else class="bx-breadcrumb__current">
        {{ item.title }}
      </span>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  breadcrumbs: {
    type: Array,
    default: () => [],
  },
})

const router = useRouter()

function handleClick(item) {
  if (item.path) {
    router.push(item.path)
  }
}
</script>

<style lang="scss" scoped>
.bx-breadcrumb {
  font-size: $font-size-sm;

  &__link {
    color: $color-text-secondary;
    cursor: pointer;
    transition: color $transition-duration-fast;

    &:hover {
      color: $color-primary;
    }
  }

  &__current {
    color: $color-text-primary;
  }
}
</style>
