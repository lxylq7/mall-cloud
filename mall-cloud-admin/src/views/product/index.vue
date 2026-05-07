<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useProductStore } from '@/stores/product'

const productStore = useProductStore()
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)

const queryForm = reactive({
  keyword: '',
  status: '',
  pageNum: 1,
  pageSize: 10,
})

const editForm = reactive({
  name: '',
  category: '',
  price: 0,
  stock: 0,
  status: 1,
})

async function loadData() {
  loading.value = true
  try {
    await productStore.fetchList(queryForm)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.pageNum = 1
  loadData()
}

function handleReset() {
  Object.assign(queryForm, { keyword: '', status: '', pageNum: 1, pageSize: 10 })
  loadData()
}

function openCreate() {
  isEdit.value = false
  currentId.value = null
  Object.assign(editForm, { name: '', category: '', price: 0, stock: 0, status: 1 })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(editForm, row)
  dialogVisible.value = true
}

async function submit() {
  if (isEdit.value) {
    await productStore.update(currentId.value, editForm)
    ElMessage.success('更新成功')
  } else {
    await productStore.create(editForm)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除商品「${row.name}」吗？`, '提示', { type: 'warning' })
  await productStore.remove(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSwitch(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  await productStore.toggleStatus(row.id, nextStatus)
  ElMessage.success('状态更新成功')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="toolbar">
      <el-input v-model="queryForm.keyword" clearable placeholder="商品名称/类目" style="width: 220px" />
      <el-select v-model="queryForm.status" clearable placeholder="上架状态" style="width: 140px">
        <el-option :value="1" label="上架" />
        <el-option :value="0" label="下架" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button v-permission="'product:create'" type="success" @click="openCreate">新增商品</el-button>
    </div>

    <el-table v-loading="loading" :data="productStore.list" border>
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="商品名称" prop="name" min-width="180" />
      <el-table-column label="类目" prop="category" min-width="140" />
      <el-table-column label="价格" prop="price" width="120">
        <template #default="{ row }">￥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="库存" prop="stock" width="100" />
      <el-table-column label="状态" prop="status" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="260">
        <template #default="{ row }">
          <el-button v-permission="'product:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'product:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
          <el-button v-permission="'product:status'" link @click="handleSwitch(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
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
        :total="productStore.total"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="520px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="商品名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="商品类目"><el-input v-model="editForm.category" /></el-form-item>
        <el-form-item label="商品价格"><el-input-number v-model="editForm.price" :min="0" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="editForm.stock" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
