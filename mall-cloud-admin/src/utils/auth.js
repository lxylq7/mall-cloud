const TOKEN_KEY = 'mall_admin_token'
const USER_KEY = 'mall_admin_user'
const REMEMBER_KEY = 'mall_admin_remember'

function getStorage(remember = true) {
  return remember ? localStorage : sessionStorage
}

export function setToken(token, remember = true) {
  localStorage.setItem(REMEMBER_KEY, remember ? '1' : '0')
  getStorage(remember).setItem(TOKEN_KEY, token)
  getStorage(!remember).removeItem(TOKEN_KEY)
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || ''
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}

export function setUserInfo(userInfo, remember = true) {
  getStorage(remember).setItem(USER_KEY, JSON.stringify(userInfo || {}))
  getStorage(!remember).removeItem(USER_KEY)
}

export function getUserInfo() {
  const raw = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function removeUserInfo() {
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export function getRememberStatus() {
  return localStorage.getItem(REMEMBER_KEY) !== '0'
}
