<script setup>
import dayjs from 'dayjs'

defineProps({
  visible: { type: Boolean, default: false },
  order: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:visible'])

function close() {
  emit('update:visible', false)
}

function formatTime(value) {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}
</script>

<template>
  <el-dialog :model-value="visible" title="订单详情" width="720px" @close="close">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="订单号">{{ order.orderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ order.statusText || '-' }}</el-descriptions-item>
      <el-descriptions-item label="用户">{{ order.username || '-' }}</el-descriptions-item>
      <el-descriptions-item label="金额">￥{{ order.payAmount || 0 }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatTime(order.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="收货地址">{{ order.address || '-' }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="close">关闭</el-button>
    </template>
  </el-dialog>
</template>
