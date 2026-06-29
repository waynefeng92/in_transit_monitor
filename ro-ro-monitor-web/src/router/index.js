import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/dashboard',
    redirect: '/dashboard/in-transit'
  },
  {
    path: '/dashboard/in-transit',
    name: 'DashboardInTransit',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '在途车辆监控' }
  },
  {
    path: '/dashboard/arrived',
    name: 'DashboardArrived',
    component: () => import('@/views/ArrivedDashboard.vue'),
    meta: { title: '到达车辆监控' }
  },
  {
    path: '/dashboard/history-replay',
    name: 'HistoryReplay',
    component: () => import('@/views/HistoryReplay.vue'),
    meta: { title: '历史回放' }
  },
  {
    path: '/upload',
    name: 'Upload',
    component: () => import('@/views/Upload.vue'),
    meta: { title: '文件上传' }
  },
  {
    path: '/excel-mapping',
    name: 'ExcelMapping',
    component: () => import('@/views/ExcelMapping.vue'),
    meta: { title: 'Excel映射配置' }
  },
  {
    path: '/order-manage',
    name: 'OrderManage',
    component: () => import('@/views/OrderManage.vue'),
    meta: { title: '订单管理' }
  },
  // 基础信息维护
  {
    path: '/brand',
    name: 'BrandManage',
    component: () => import('@/views/BrandManage.vue'),
    meta: { title: '品牌管理' }
  },
  {
    path: '/port',
    name: 'PortManage',
    component: () => import('@/views/PortManage.vue'),
    meta: { title: '港口管理' }
  },
  {
    path: '/route',
    name: 'RouteManage',
    component: () => import('@/views/RouteManage.vue'),
    meta: { title: '线路管理' }
  },
  {
    path: '/otd-config',
    name: 'OtdConfig',
    component: () => import('@/views/OtdConfig.vue'),
    meta: { title: 'OTD时效配置' }
  },
  {
    path: '/location-alias',
    name: 'LocationAlias',
    component: () => import('@/views/LocationAliasManage.vue'),
    meta: { title: '地点别名' }
  },
  {
    path: '/vehicle-detail',
    name: 'VehicleDetail',
    component: () => import('@/views/VehicleDetail.vue'),
    meta: { title: '车辆详情' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/components/NotFound.vue'),
    meta: { title: '页面不存在', requiresAuth: false }
  }
]

let authChecked = false

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from) => {
  // Set page title
  document.title = to.meta.title ? `${to.meta.title} - 在途车辆监控系统` : '在途车辆监控系统'

  // Auth check
  const authStore = useAuthStore()
  // requiresAuth defaults to true if not set
  const requiresAuth = to.meta.requiresAuth !== false

  // Check auth on first navigation
  if (!authChecked) {
    authChecked = true
    await authStore.checkAuth()
  }

  if (requiresAuth && !authStore.isAuthenticated) {
    return { name: 'Login' }
  }
  if (to.name === 'Login' && authStore.isAuthenticated) {
    return { path: '/dashboard/in-transit' }
  }


})

export default router