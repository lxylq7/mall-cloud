import request from '@/utils/request'

export function getProductListApi(params) {
  return request({ url: '/product', method: 'get', params })
}

export function createProductApi(data) {
  return request({ url: '/product', method: 'post', data })
}

export function updateProductApi(id, data) {
  return request({ url: `/product/${id}`, method: 'put', data })
}

export function deleteProductApi(id) {
  return request({ url: `/product/${id}`, method: 'delete' })
}

export function toggleProductStatusApi(id, status) {
  return request({ url: `/product/${id}/status`, method: 'patch', data: { status } })
}

export function getProductStatsApi() {
  return request({ url: '/product/stats', method: 'get', showLoading: false })
}
