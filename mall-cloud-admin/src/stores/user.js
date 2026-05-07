import { defineStore } from 'pinia'
import { getProfileApi, loginApi, logoutApi } from '@/api/auth'
import {
  getRememberStatus,
  getToken,
  getUserInfo,
  removeToken,
  removeUserInfo,
  setToken,
  setUserInfo,
} from '@/utils/auth'

const demoProfile = {
  id: 1,
  username: 'admin',
  nickname: '系统管理员',
  roles: ['admin'],
  permissions: [
    'dashboard:view',
    'product:list',
    'product:create',
    'product:edit',
    'product:delete',
    'product:status',
    'order:list',
    'order:detail',
    'order:ship',
    'user:list',
    'user:status',
  ],
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: getUserInfo() || null,
    roles: [],
    permissions: [],
    routesLoaded: false,
  }),
  getters: {
    isLogin: (state) => !!state.token,
  },
  actions: {
    async login(form) {
      const remember = form.remember ?? getRememberStatus()
      try {
        const data = await loginApi(form)
        this.token = data.token
        this.userInfo = data.userInfo
        this.roles = data.roles || []
        this.permissions = data.permissions || []
        setToken(data.token, remember)
        setUserInfo(data.userInfo, remember)
      } catch {
        // 方便本地独立运行：后端未启动时走演示账号
        if (form.username === 'admin' && form.password === '123456') {
          this.token = `demo-token-${Date.now()}`
          this.userInfo = demoProfile
          this.roles = demoProfile.roles
          this.permissions = demoProfile.permissions
          setToken(this.token, remember)
          setUserInfo(demoProfile, remember)
          return
        }
        throw new Error('登录失败，请检查账号密码或后端服务')
      }
    },
    async fetchProfile() {
      if (!this.token) return
      try {
        const data = await getProfileApi()
        this.userInfo = data.userInfo || data
        this.roles = data.roles || this.userInfo?.roles || ['admin']
        this.permissions = data.permissions || this.userInfo?.permissions || []
      } catch {
        this.userInfo = this.userInfo || demoProfile
        this.roles = this.roles.length ? this.roles : demoProfile.roles
        this.permissions = this.permissions.length ? this.permissions : demoProfile.permissions
      }
    },
    async logout() {
      try {
        await logoutApi()
      } catch {
        // ignore
      }
      this.reset()
    },
    reset() {
      this.token = ''
      this.userInfo = null
      this.roles = []
      this.permissions = []
      this.routesLoaded = false
      removeToken()
      removeUserInfo()
    },
    markRoutesLoaded(status) {
      this.routesLoaded = status
    },
  },
})
