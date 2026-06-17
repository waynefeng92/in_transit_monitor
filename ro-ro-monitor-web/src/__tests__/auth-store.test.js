import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

const mockRequest = vi.hoisted(() => ({
  post: vi.fn(),
  get: vi.fn()
}))

vi.mock('@/api/request', () => ({
  default: mockRequest
}))

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockRequest.post.mockReset()
    mockRequest.get.mockReset()
  })

  it('login action sets user and isAuthenticated on success', async () => {
    mockRequest.post.mockResolvedValue({
      code: 200,
      data: { username: 'admin', role: 'ADMIN' }
    })

    const store = useAuthStore()
    const result = await store.login('admin', 'admin123')

    expect(result).toBe(true)
    expect(store.user).toEqual({ username: 'admin', role: 'ADMIN' })
    expect(store.isAuthenticated).toBe(true)
    expect(store.loading).toBe(false)
    expect(mockRequest.post).toHaveBeenCalledWith('/api/auth/login', {
      username: 'admin',
      password: 'admin123'
    })
  })

  it('logout action clears state', async () => {
    mockRequest.post.mockResolvedValue({ code: 200 })

    const store = useAuthStore()
    store.user = { username: 'admin', role: 'ADMIN' }
    store.isAuthenticated = true

    await store.logout()

    expect(store.isAuthenticated).toBe(false)
    expect(store.user).toBeNull()
    expect(mockRequest.post).toHaveBeenCalledWith('/api/auth/logout')
  })

  it('checkAuth fetches current user and updates store', async () => {
    mockRequest.get.mockResolvedValue({
      code: 200,
      data: { username: 'admin', role: 'ADMIN' }
    })

    const store = useAuthStore()
    const result = await store.checkAuth()

    expect(result).toBe(true)
    expect(store.user).toEqual({ username: 'admin', role: 'ADMIN' })
    expect(store.isAuthenticated).toBe(true)
    expect(mockRequest.get).toHaveBeenCalledWith('/api/auth/me')
  })

  it('failed login does not authenticate user', async () => {
    mockRequest.post.mockRejectedValue(new Error('Invalid credentials'))

    const store = useAuthStore()
    await expect(store.login('admin', 'wrong')).rejects.toThrow('Invalid credentials')

    expect(store.isAuthenticated).toBe(false)
    expect(store.user).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('checkAuth returns false on failure', async () => {
    mockRequest.get.mockRejectedValue(new Error('Network error'))

    const store = useAuthStore()
    store.user = { username: 'admin', role: 'ADMIN' }
    store.isAuthenticated = true

    const result = await store.checkAuth()

    expect(result).toBe(false)
    expect(store.user).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('getter isAdmin returns true when role is ADMIN', () => {
    const store = useAuthStore()
    store.user = { username: 'admin', role: 'ADMIN' }
    expect(store.isAdmin).toBe(true)
  })

  it('getter isAdmin returns false when role is not ADMIN', () => {
    const store = useAuthStore()
    store.user = { username: 'user', role: 'USER' }
    expect(store.isAdmin).toBe(false)
  })

  it('getter isAdmin returns false when user is null', () => {
    const store = useAuthStore()
    expect(store.isAdmin).toBe(false)
  })

  it('getter username returns the username', () => {
    const store = useAuthStore()
    store.user = { username: 'admin', role: 'ADMIN' }
    expect(store.username).toBe('admin')
  })

  it('getter username returns undefined when user is null', () => {
    const store = useAuthStore()
    expect(store.username).toBeUndefined()
  })
})
