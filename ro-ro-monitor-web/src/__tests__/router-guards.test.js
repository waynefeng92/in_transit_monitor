import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

function createTestRouter() {
  const routes = [
    {
      path: '/login',
      name: 'Login',
      component: { template: '<div>Login</div>' },
      meta: { title: '登录', requiresAuth: false }
    },
    {
      path: '/dashboard/in-transit',
      name: 'DashboardInTransit',
      component: { template: '<div>Dashboard</div>' },
      meta: { title: '在途车辆监控' }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: { template: '<div>404</div>' },
      meta: { title: '页面不存在', requiresAuth: false }
    }
  ]

  const router = createRouter({
    history: createMemoryHistory(),
    routes
  })

  router.beforeEach((to, from) => {
    document.title = to.meta.title
      ? `${to.meta.title} - 在途车辆监控系统`
      : '在途车辆监控系统'

    const authStore = useAuthStore()
    const requiresAuth = to.meta.requiresAuth !== false

    if (requiresAuth && !authStore.isAuthenticated) {
      return { name: 'Login' }
    }
    if (to.name === 'Login' && authStore.isAuthenticated) {
      return { path: '/dashboard/in-transit' }
    }
  })

  return router
}

describe('router guards', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('redirects to /login when unauthenticated', async () => {
    const router = createTestRouter()
    await router.push('/dashboard/in-transit')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('allows navigation when authenticated', async () => {
    const router = createTestRouter()
    const authStore = useAuthStore()
    authStore.isAuthenticated = true
    await router.push('/dashboard/in-transit')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard/in-transit')
  })

  it('redirects from /login when already authenticated', async () => {
    const router = createTestRouter()
    const authStore = useAuthStore()
    authStore.isAuthenticated = true
    await router.push('/login')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard/in-transit')
  })
})
