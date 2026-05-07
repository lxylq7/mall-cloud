import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { getToken, removeToken, removeUserInfo } from './auth'

let loadingInstance = null
let loadingCount = 0

function openLoading() {
  if (!loadingInstance) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: '加载中...',
      background: 'rgba(255, 255, 255, 0.45)',
    })
  }
  loadingCount += 1
}

function closeLoading() {
  loadingCount = Math.max(0, loadingCount - 1)
  if (!loadingCount && loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 12000,
})

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (config.showLoading !== false) {
      openLoading()
    }
    return config
  },
  (error) => {
    closeLoading()
    return Promise.reject(error)
  },
)

service.interceptors.response.use(
  (response) => {
    closeLoading()
    const res = response.data
    if (res && typeof res.code !== 'undefined') {
      if (res.code === 200) return res.data
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  (error) => {
    closeLoading()
    const status = error.response?.status
    if (status === 401) {
      removeToken()
      removeUserInfo()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  },
)

export default service
