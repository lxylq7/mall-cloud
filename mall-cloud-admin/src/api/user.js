import request from '@/utils/request'

export function getUserListApi(params) {
  return request({ url: '/user', method: 'get', params })
}

export function updateUserStatusApi(id, status) {
  return request({ url: `/user/${id}/status`, method: 'patch', data: { status } })
}

export function getUserStatsApi() {
  return request({ url: '/user/stats', method: 'get', showLoading: false })
}
