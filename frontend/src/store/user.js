import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getInfo } from '@/api/auth'
import { getToken, setToken, removeToken, getStoredUser, setStoredUser } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    user: getStoredUser()
  }),
  getters: {
    permissions: (state) => state.user?.permissions || [],
    nickname: (state) => state.user?.nickname || '未登录',
    avatar: (state) => state.user?.avatar || '',
    deptName: (state) => state.user?.deptName || '',
    roleNames: (state) => state.user?.roleNames || []
  },
  actions: {
    async login(form) {
      const { data } = await loginApi(form)
      this.token = data.token
      setToken(data.token)
      await this.fetchInfo()
      return data
    },
    async fetchInfo() {
      const { data } = await getInfo()
      this.user = data
      setStoredUser(data)
      return data
    },
    hasPerm(permission) {
      if (!permission) return true
      const list = this.permissions
      return list.includes('*:*:*') || list.includes(permission)
    },
    hasAnyPerm(permissions = []) {
      if (!permissions.length) return true
      return permissions.some((p) => this.hasPerm(p))
    },
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 忽略退出接口异常
      }
      this.reset()
    },
    reset() {
      this.token = ''
      this.user = null
      removeToken()
    }
  }
})
