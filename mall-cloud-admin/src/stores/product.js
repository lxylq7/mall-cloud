import { defineStore } from 'pinia'
import {
  createProductApi,
  deleteProductApi,
  getProductListApi,
  getProductStatsApi,
  toggleProductStatusApi,
  updateProductApi,
} from '@/api/product'

export const useProductStore = defineStore('product', {
  state: () => ({
    list: [],
    total: 0,
    stats: {
      totalProduct: 0,
      onSale: 0,
    },
  }),
  actions: {
    async fetchList(params) {
      try {
        const data = await getProductListApi(params)
        this.list = data.records || data.list || []
        this.total = data.total || 0
      } catch {
        this.list = [
          { id: 1, name: '蓝牙耳机', category: '数码', price: 299, stock: 90, status: 1 },
          { id: 2, name: '办公鼠标', category: '办公', price: 89, stock: 320, status: 1 },
          { id: 3, name: '27寸显示器', category: '电脑外设', price: 1299, stock: 46, status: 0 },
        ]
        this.total = 3
      }
    },
    async create(data) {
      await createProductApi(data)
    },
    async update(id, data) {
      await updateProductApi(id, data)
    },
    async remove(id) {
      await deleteProductApi(id)
    },
    async toggleStatus(id, status) {
      await toggleProductStatusApi(id, status)
    },
    async fetchStats() {
      try {
        const data = await getProductStatsApi()
        this.stats = {
          totalProduct: data.totalProduct ?? 1280,
          onSale: data.onSale ?? 968,
        }
      } catch {
        this.stats = { totalProduct: 1280, onSale: 968 }
      }
    },
  },
})
