import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { constantRoutes, notFoundRoute } from './routes'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
})

const whiteList = ['/login']

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  const token = getToken()
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (token) {
    if (to.path === '/login') {
      next('/')
      NProgress.done()
      return
    }
    if (!userStore.routesLoaded) {
      await userStore.fetchProfile()
      const accessRoutes = permissionStore.generateRoutes(userStore.roles)
      accessRoutes.forEach((route) => router.addRoute('Root', route))
      if (!router.hasRoute(notFoundRoute.name)) {
        router.addRoute(notFoundRoute)
      }
      userStore.markRoutesLoaded(true)
      next({ ...to, replace: true })
      return
    }
    if (!router.hasRoute(notFoundRoute.name)) {
      router.addRoute(notFoundRoute)
    }
    next()
  } else if (whiteList.includes(to.path)) {
    next()
  } else {
    next(`/login?redirect=${to.fullPath}`)
    NProgress.done()
  }
})

router.afterEach((to) => {
  document.title = `${to.meta?.title || '后台'} - mall-cloud-admin`
  NProgress.done()
})

export default router
