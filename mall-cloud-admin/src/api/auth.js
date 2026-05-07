import request from '@/utils/request'

export function loginApi(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data,
  })
}

export function getProfileApi() {
  return request({
    url: '/user/profile',
    method: 'get',
    showLoading: false,
  })
}

export function logoutApi() {
  return request({
    url: '/user/logout',
    method: 'post',
    showLoading: false,
  })
}
