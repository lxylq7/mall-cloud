import request from '@/utils/request'

export function getOrderListApi(params) {
  return request({ url: '/order', method: 'get', params })
}

export function getOrderDetailApi(id) {
  return request({ url: `/order/${id}`, method: 'get', showLoading: false })
}

export function shipOrderApi(id, data) {
  return request({ url: `/order/${id}/ship`, method: 'patch', data })
}

export function getOrderStatsApi() {
  return request({ url: '/order/stats', method: 'get', showLoading: false })
}
