import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ElMessage } from 'element-plus'

const { mockReset, mockPush } = vi.hoisted(() => ({
  mockReset: vi.fn(),
  mockPush: vi.fn()
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ $reset: mockReset })
}))

vi.mock('@/router', () => ({
  default: { push: mockPush }
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

import request from '@/api/request'

// Extract handlers registered by the interceptor
const handlers = request.interceptors.response.handlers
const successHandler = handlers[0].fulfilled
const errorHandler = handlers[0].rejected

describe('API Interceptor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('redirects to /login on 401 response', async () => {
    const err = new Error('Unauthorized')
    err.response = { status: 401, data: {}, headers: {}, config: {} }

    await expect(errorHandler(err)).rejects.toThrow()

    expect(mockReset).toHaveBeenCalledTimes(1)
    expect(mockPush).toHaveBeenCalledWith('/login')
    expect(ElMessage.error).toHaveBeenCalledWith('登录已过期，请重新登录')
  })

  it('does not redirect on successful response', async () => {
    const res = { data: { code: 200, data: { id: 1, name: 'test' } } }

    const result = await successHandler(res)

    expect(result).toEqual({ code: 200, data: { id: 1, name: 'test' } })
    expect(mockPush).not.toHaveBeenCalled()
    expect(mockReset).not.toHaveBeenCalled()
  })

  it('shows error message on 500 response', async () => {
    const err = new Error('Server Error')
    err.response = { status: 500, data: {}, headers: {}, config: {} }

    await expect(errorHandler(err)).rejects.toThrow()

    expect(ElMessage.error).toHaveBeenCalledWith('服务器错误，请稍后重试')
    expect(mockPush).not.toHaveBeenCalled()
    expect(mockReset).not.toHaveBeenCalled()
  })

  it('shows 403 error message without redirect', async () => {
    const err = new Error('Forbidden')
    err.response = { status: 403, data: {}, headers: {}, config: {} }

    await expect(errorHandler(err)).rejects.toThrow()

    expect(ElMessage.error).toHaveBeenCalledWith('无权限访问')
    expect(mockPush).not.toHaveBeenCalled()
    expect(mockReset).not.toHaveBeenCalled()
  })

  it('shows network error message when no response', async () => {
    const err = new Error('Network Error')
    // No error.response

    await expect(errorHandler(err)).rejects.toThrow()

    expect(ElMessage.error).toHaveBeenCalledWith('网络连接失败，请检查网络')
    expect(mockPush).not.toHaveBeenCalled()
    expect(mockReset).not.toHaveBeenCalled()
  })
})
