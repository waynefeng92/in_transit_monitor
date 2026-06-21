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
        const userData = await request.post('/auth/login', { username, password })
        this.user = userData
        this.isAuthenticated = true
        return true
      } catch (error) {
        this.isAuthenticated = false
        throw error
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        await request.post('/auth/logout')
      } finally {
        this.user = null
        this.isAuthenticated = false
      }
    },

    async checkAuth() {
      try {
        const userData = await request.get('/auth/me')
        this.user = userData
        this.isAuthenticated = true
        return true
      } catch {
        this.user = null
        this.isAuthenticated = false
        return false
      }
    }
  }
})
