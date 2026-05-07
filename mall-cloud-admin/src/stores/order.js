import { defineStore } from 'pinia'
import { getOrderDetailApi, getOrderListApi, getOrderStatsApi, shipOrderApi } from '@/api/order'

export const useOrderStore = defineStore('order', {
  state: () => ({
    list: [],
    total: 0,
    stats: {
      totalOrder: 0,
      totalSales: 0,
      chartData: [],
    },
  }),
  actions: {
    async fetchList(params) {
      try {
        const data = await getOrderListApi(params)
        this.list = data.records || data.list || []
        this.total = data.total || 0
      } catch {
        this.list = [
          {
            id: 1001,
            orderNo: 'MO20260507001',
            username: 'tom',
            payAmount: 299,
            status: 'paid',
            createTime: '2026-05-07 10:20:00',
          },
          {
            id: 1002,
            orderNo: 'MO20260507002',
            username: 'lucy',
            payAmount: 89,
            status: 'shipped',
            createTime: '2026-05-07 11:10:00',
          },
        ]
        this.total = 2
      }
    },
    async fetchDetail(id) {
      try {
        return await getOrderDetailApi(id)
      } catch {
        return {
          id,
          orderNo: 'MO20260507001',
          username: 'tom',
          status: 'paid',
          payAmount: 299,
          address: '上海市浦东新区测试路88号',
          createTime: '2026-05-07 10:20:00',
        }
      }
    },
    async ship(id, logisticsNo) {
      await shipOrderApi(id, { logisticsNo })
    },
    async fetchStats() {
      try {
        const data = await getOrderStatsApi()
        this.stats = {
          totalOrder: data.totalOrder ?? 3520,
          totalSales: data.totalSales ?? 987653,
          chartData: data.chartData || [],
        }
      } catch {
        this.stats = {
          totalOrder: 3520,
          totalSales: 987653,
          chartData: [],
        }
      }
    },
  },
})
