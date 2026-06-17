import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory } from 'vue-router'
import { Monitor } from '@element-plus/icons-vue'
import Login from '@/views/Login.vue'

// ── Module-level mocks ────────────────────────────────────────────

const { mockLogin } = vi.hoisted(() => ({
  mockLogin: vi.fn()
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    login: mockLogin,
    loading: false
  })
}))

// Spy on ElMessage feedback methods (do nothing in jsdom)
vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
vi.spyOn(ElMessage, 'error').mockImplementation(() => {})

// ── Test suite ────────────────────────────────────────────────────

describe('Login', () => {
  let router

  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())

    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/login', name: 'Login', component: Login },
        { path: '/dashboard', name: 'Dashboard' }
      ]
    })
  })

  const mountLogin = () => {
    return mount(Login, {
      global: {
        plugins: [router, ElementPlus],
        components: { Monitor }
      }
    })
  }

  // Helper: fill in valid credentials and submit
  const fillAndSubmit = async (wrapper) => {
    const usernameInput = wrapper.find('input[placeholder="请输入用户名"]')
    await usernameInput.setValue('admin')

    const passwordInput = wrapper.find('input[placeholder="请输入密码"]')
    await passwordInput.setValue('1234')

    await wrapper.find('.el-button--primary').trigger('click')
  }

  // ── Test 1: renders form ───────────────────────────────────────
  it('renders login form', () => {
    const wrapper = mountLogin()

    expect(wrapper.find('input[placeholder="请输入用户名"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="请输入密码"]').exists()).toBe(true)
    expect(wrapper.find('.el-button--primary').exists()).toBe(true)
    expect(wrapper.find('.login-title').text()).toBe('在途车辆监控系统')
  })

  // ── Test 2: shows error on failed login ────────────────────────
  it('shows error on failed login', async () => {
    mockLogin.mockRejectedValue(new Error('用户名或密码错误'))

    const wrapper = mountLogin()
    await fillAndSubmit(wrapper)

    // Wait for async validation + API call
    await new Promise((resolve) => setTimeout(resolve, 100))

    expect(ElMessage.error).toHaveBeenCalled()
  })

  // ── Test 3: redirects on successful login ──────────────────────
  it('redirects on successful login', async () => {
    mockLogin.mockResolvedValue({})
    const pushSpy = vi.spyOn(router, 'push')

    const wrapper = mountLogin()
    await fillAndSubmit(wrapper)

    // Wait for async validation + API call
    await new Promise((resolve) => setTimeout(resolve, 100))

    expect(pushSpy).toHaveBeenCalledWith('/dashboard')
  })
})
