import { defineStore } from 'pinia'
import { asyncChildRoutes } from '@/router/routes'
import { hasRole } from '@/utils/permission'

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    dynamicRoutes: [],
    menuRoutes: [],
  }),
  actions: {
    generateRoutes(userRoles = []) {
      const accessed = asyncChildRoutes.filter((route) =>
        hasRole(route.meta?.roles || [], userRoles),
      )
      this.dynamicRoutes = accessed
      this.menuRoutes = accessed
      return accessed
    },
    resetRoutes() {
      this.dynamicRoutes = []
      this.menuRoutes = []
    },
  },
})
