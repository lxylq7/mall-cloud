<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import StatCard from '@/components/StatCard.vue'
import { useMemberStore } from '@/stores/member'
import { useOrderStore } from '@/stores/order'
import { useProductStore } from '@/stores/product'

const memberStore = useMemberStore()
const orderStore = useOrderStore()
const productStore = useProductStore()

const lineRef = ref()
const pieRef = ref()
let lineChart = null
let pieChart = null

function initChart() {
  if (lineRef.value) {
    lineChart = echarts.init(lineRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: [120, 200, 150, 80, 370, 420, 510], smooth: true }],
    })
  }
  if (pieRef.value) {
    pieChart = echarts.init(pieRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['45%', '70%'],
          data: [
            { value: 1048, name: '待支付' },
            { value: 735, name: '待发货' },
            { value: 580, name: '已发货' },
            { value: 484, name: '已完成' },
          ],
        },
      ],
    })
  }
}

onMounted(async () => {
  await Promise.all([memberStore.fetchStats(), orderStore.fetchStats(), productStore.fetchStats()])
  await nextTick()
  initChart()
})

onUnmounted(() => {
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<template>
  <div class="page-container">
    <div class="stat-grid">
      <StatCard title="用户总数" :value="memberStore.stats.totalUser" trend="+8.6% 较昨日" />
      <StatCard title="订单总数" :value="orderStore.stats.totalOrder" trend="+5.2% 较昨日" />
      <StatCard title="商品总数" :value="productStore.stats.totalProduct" trend="+2.4% 较昨日" />
      <StatCard title="销售额" :value="orderStore.stats.totalSales" suffix="元" trend="+12.1% 较昨日" />
    </div>

    <div class="chart-grid">
      <el-card shadow="never">
        <template #header>销售趋势</template>
        <div ref="lineRef" style="height: 320px"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>订单状态占比</template>
        <div ref="pieRef" style="height: 320px"></div>
      </el-card>
    </div>
  </div>
</template>
