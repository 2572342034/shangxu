import { defineStore } from 'pinia'
import { allDicts } from '@/api/system'

export const useDictStore = defineStore('dict', {
  state: () => ({
    dicts: {},
    loaded: false
  }),
  actions: {
    async load(force = false) {
      if (this.loaded && !force) return this.dicts
      try {
        const { data } = await allDicts()
        this.dicts = data || {}
        this.loaded = true
      } catch (e) {
        this.dicts = {}
      }
      return this.dicts
    },
    label(type, value) {
      if (value === null || value === undefined || value === '') return '-'
      const list = this.dicts[type] || []
      const hit = list.find((item) => item.dictValue === String(value))
      return hit ? hit.dictLabel : value
    },
    options(type) {
      return (this.dicts[type] || []).map((item) => ({
        label: item.dictLabel,
        value: item.dictValue
      }))
    }
  }
})
