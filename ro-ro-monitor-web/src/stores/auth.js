import { defineStore } from 'pinia'
import request from '@/api/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isAuthenticated: false,
    loading: false
  }),

  getters: {
    isAdmin: (state) => state.user?.role === 'ADMIN',
    username: (state) => state.user?.username
  },

  actions: {
    async login(username, password) {
      this.loading = true
      try {
        const res = await request.post('/api/auth/login', { username, password })
        if (res.code === 200) {
          this.user = res.data
          this.isAuthenticated = true
          return true
        }
        return false
      } catch (error) {
        this.isAuthenticated = false
        throw error
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        await request.post('/api/auth/logout')
      } finally {
        this.user = null
        this.isAuthenticated = false
      }
    },

    async checkAuth() {
      try {
        const res = await request.get('/api/auth/me')
        if (res.code === 200) {
          this.user = res.data
          this.isAuthenticated = true
          return true
        }
      } catch {
        this.user = null
        this.isAuthenticated = false
      }
      return false
    }
  }
})
