<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as Icons from '@element-plus/icons-vue'
import { Menu } from '@element-plus/icons-vue'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

const menus = computed(() => permissionStore.menuRoutes)
const activeMenu = computed(() => route.path)

function go(path) {
  router.push(path)
}
</script>

<template>
  <el-menu :default-active="activeMenu" class="sidebar-menu" router>
    <el-menu-item
      v-for="item in menus"
      :key="item.path"
      :index="`/${item.path}`"
      @click="go(`/${item.path}`)"
    >
      <el-icon>
        <component :is="Icons[item.meta?.icon] || Menu" />
      </el-icon>
      <span>{{ item.meta?.title }}</span>
    </el-menu-item>
  </el-menu>
</template>

<style scoped>
.sidebar-menu {
  border-right: none;
  min-height: calc(100vh - 64px);
}
</style>
