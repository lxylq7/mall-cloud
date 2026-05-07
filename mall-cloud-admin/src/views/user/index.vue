<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useMemberStore } from '@/stores/member'

const memberStore = useMemberStore()
const loading = ref(false)

const queryForm = reactive({
  keyword: '',
  status: '',
  pageNum: 1,
  pageSize: 10,
})

async function loadData() {
  loading.value = true
  try {
    await memberStore.fetchList(queryForm)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.pageNum = 1
  loadData()
}

async function handleStatusChange(row) {
  await memberStore.updateStatus(row.id, row.status)
  ElMessage.success('用户状态更新成功')
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="toolbar">
      <el-input v-model="queryForm.keyword" clearable placeholder="用户名/手机号" style="width: 220px" />
      <el-select v-model="queryForm.status" clearable placeholder="状态" style="width: 140px">
        <el-option :value="1" label="启用" />
        <el-option :value="0" label="禁用" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="loadData">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="memberStore.list" border>
      <el-table-column label="用户ID" prop="id" width="100" />
      <el-table-column label="用户名" prop="username" min-width="130" />
      <el-table-column label="昵称" prop="nickname" min-width="130" />
      <el-table-column label="手机号" prop="mobile" min-width="140" />
      <el-table-column label="状态" prop="status" width="130">
        <template #default="{ row }">
          <el-switch
            v-permission="'user:status'"
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="注册时间" prop="createTime" min-width="180" />
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 30, 50]"
        :total="memberStore.total"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>
  </div>
</template>
