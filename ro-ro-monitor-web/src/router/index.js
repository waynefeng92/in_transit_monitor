import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard/in-transit'
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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router