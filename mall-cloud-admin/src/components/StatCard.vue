<script setup>
defineProps({
  title: { type: String, default: '' },
  value: { type: [String, Number], default: 0 },
  suffix: { type: String, default: '' },
  trend: { type: String, default: '' },
})
</script>

<template>
  <el-card shadow="hover">
    <div class="stat-title">{{ title }}</div>
    <div class="stat-value">{{ value }}{{ suffix }}</div>
    <div class="stat-trend">{{ trend }}</div>
  </el-card>
</template>

<style scoped>
.stat-title {
  color: #6b7280;
  font-size: 13px;
}

.stat-value {
  margin: 8px 0 6px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.stat-trend {
  font-size: 12px;
  color: #10b981;
}
</style>
