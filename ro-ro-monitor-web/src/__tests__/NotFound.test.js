import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'

// Mock vue-router
const pushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock })
}))

// Mock auth store
const mockAuthStore = { isAuthenticated: false }
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mockAuthStore
}))

import NotFound from '@/components/NotFound.vue'

describe('NotFound', () => {
  it('renders 404 message', () => {
    const wrapper = mount(NotFound, {
      global: {
        stubs: {
          'el-result': {
            template: '<div><slot name="extra" /><div class="title">{{ title }}</div><div class="sub-title">{{ subTitle }}</div></div>',
            props: ['title', 'subTitle']
          },
          'el-button': {
            template: '<button @click="$emit(\'click\')"><slot /></button>',
            emits: ['click']
          }
        }
      }
    })

    expect(wrapper.text()).toContain('404')
    expect(wrapper.text()).toContain('页面不存在')
  })

  it('has link back to home', () => {
    const wrapper = mount(NotFound, {
      global: {
        stubs: {
          'el-result': {
            template: '<div><slot name="extra" /><div class="title">{{ title }}</div><div class="sub-title">{{ subTitle }}</div></div>',
            props: ['title', 'subTitle']
          },
          'el-button': {
            template: '<button @click="$emit(\'click\')"><slot /></button>',
            emits: ['click']
          }
        }
      }
    })

    const button = wrapper.find('button')
    expect(button.exists()).toBe(true)
    expect(button.text()).toBe('返回首页')
  })
})
