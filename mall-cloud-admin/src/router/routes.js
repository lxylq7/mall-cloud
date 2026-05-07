import Layout from '@/layout/Layout.vue'

export const asyncChildRoutes = [
  {
    path: 'dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '首页', icon: 'DataLine', roles: ['admin', 'operator'] },
  },
  {
    path: 'product',
    name: 'Product',
    component: () => import('@/views/product/index.vue'),
    meta: { title: '商品管理', icon: 'Goods', roles: ['admin', 'operator'] },
  },
  {
    path: 'order',
    name: 'Order',
    component: () => import('@/views/order/index.vue'),
    meta: { title: '订单管理', icon: 'Tickets', roles: ['admin', 'operator'] },
  },
  {
    path: 'user',
    name: 'User',
    component: () => import('@/views/user/index.vue'),
    meta: { title: '用户管理', icon: 'User', roles: ['admin'] },
  },
]

export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    name: 'Root',
    component: Layout,
    redirect: '/dashboard',
    children: [],
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' },
  },
]

export const notFoundRoute = {
  path: '/:pathMatch(.*)*',
  name: 'CatchAll',
  redirect: '/404',
}
