<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found-wrap">
    <el-result icon="warning" sub-title="抱歉，页面不存在" title="404">
      <template #extra>
        <el-button type="primary" @click="router.push('/dashboard')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.not-found-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: #f5f7fa;
}
</style>
