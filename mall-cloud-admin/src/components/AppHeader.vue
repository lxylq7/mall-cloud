<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const crumbs = computed(() => route.matched.filter((item) => item.meta?.title))
const username = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员')

async function handleCommand(command) {
  if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="header-wrap">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item v-for="item in crumbs" :key="item.path">
        {{ item.meta.title }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <el-dropdown @command="handleCommand">
      <span class="user-info">
        {{ username }}
        <el-icon><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped>
.header-wrap {
  background: #fff;
  border-radius: 8px;
  height: 56px;
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.user-info {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #374151;
  cursor: pointer;
}
</style>
