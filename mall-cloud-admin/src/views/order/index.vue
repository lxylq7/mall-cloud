<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import OrderDetailDialog from '@/components/OrderDetailDialog.vue'
import { useOrderStore } from '@/stores/order'

const orderStore = useOrderStore()
const loading = ref(false)
const detailVisible = ref(false)
const detailData = ref({})

const queryForm = reactive({
  orderNo: '',
  status: '',
  pageNum: 1,
  pageSize: 10,
})

const statusMap = {
  pending: '待支付',
  paid: '待发货',
  shipped: '已发货',
  finished: '已完成',
  closed: '已关闭',
}

async function loadData() {
  loading.value = true
  try {
    await orderStore.fetchList(queryForm)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.pageNum = 1
  loadData()
}

async function openDetail(row) {
  detailData.value = await orderStore.fetchDetail(row.id)
  detailData.value.statusText = statusMap[detailData.value.status] || detailData.value.status
  detailVisible.value = true
}

async function handleShip(row) {
  const { value } = await ElMessageBox.prompt('请输入物流单号', '发货', {
    inputPlaceholder: '物流单号',
  })
  await orderStore.ship(row.id, value)
  ElMessage.success('发货成功')
  loadData()
}

function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="toolbar">
      <el-input v-model="queryForm.orderNo" clearable placeholder="订单号" style="width: 220px" />
      <el-select v-model="queryForm.status" clearable placeholder="订单状态" style="width: 140px">
        <el-option value="pending" label="待支付" />
        <el-option value="paid" label="待发货" />
        <el-option value="shipped" label="已发货" />
        <el-option value="finished" label="已完成" />
        <el-option value="closed" label="已关闭" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="loadData">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="orderStore.list" border>
      <el-table-column label="订单号" prop="orderNo" min-width="170" />
      <el-table-column label="用户名" prop="username" width="120" />
      <el-table-column label="订单金额" prop="payAmount" width="120">
        <template #default="{ row }">￥{{ row.payAmount }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="110">
        <template #default="{ row }">
          <el-tag>{{ statusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="210">
        <template #default="{ row }">
          <el-button v-permission="'order:detail'" link type="primary" @click="openDetail(row)">
            详情
          </el-button>
          <el-button
            v-if="row.status === 'paid'"
            v-permission="'order:ship'"
            link
            type="success"
            @click="handleShip(row)"
          >
            发货
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 30, 50]"
        :total="orderStore.total"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <OrderDetailDialog v-model:visible="detailVisible" :order="detailData" />
  </div>
</template>
