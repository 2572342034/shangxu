import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem('shangxu_crm_sidebar') === '1'
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem('shangxu_crm_sidebar', this.sidebarCollapsed ? '1' : '0')
    }
  }
})
