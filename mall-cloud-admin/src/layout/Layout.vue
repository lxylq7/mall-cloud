<script setup>
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
</script>

<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">mall-cloud-admin</div>
      <AppSidebar />
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <AppHeader />
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.layout-aside {
  background: #ffffff;
  border-right: 1px solid #ebeef5;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #111827;
  font-weight: 700;
  border-bottom: 1px solid #ebeef5;
}

.layout-header {
  padding: 10px 12px;
  background: #f5f7fa;
}

.layout-main {
  padding: 0 12px 12px;
  background: #f5f7fa;
}
</style>
