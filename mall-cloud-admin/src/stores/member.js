import { defineStore } from 'pinia'
import { getUserListApi, getUserStatsApi, updateUserStatusApi } from '@/api/user'

export const useMemberStore = defineStore('member', {
  state: () => ({
    list: [],
    total: 0,
    stats: {
      totalUser: 0,
      activeUser: 0,
    },
  }),
  actions: {
    async fetchList(params) {
      try {
        const data = await getUserListApi(params)
        this.list = data.records || data.list || []
        this.total = data.total || 0
      } catch {
        this.list = [
          {
            id: 11,
            username: 'tom',
            nickname: 'Tom',
            mobile: '13800001111',
            status: 1,
            createTime: '2026-05-01 12:00:00',
          },
          {
            id: 12,
            username: 'lucy',
            nickname: 'Lucy',
            mobile: '13800002222',
            status: 0,
            createTime: '2026-05-03 09:00:00',
          },
        ]
        this.total = 2
      }
    },
    async updateStatus(id, status) {
      await updateUserStatusApi(id, status)
    },
    async fetchStats() {
      try {
        const data = await getUserStatsApi()
        this.stats = {
          totalUser: data.totalUser ?? 5630,
          activeUser: data.activeUser ?? 4270,
        }
      } catch {
        this.stats = { totalUser: 5630, activeUser: 4270 }
      }
    },
  },
})
