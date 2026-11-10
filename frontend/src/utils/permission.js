import { useUserStore } from '@/store/user'

/**
 * v-perm="'crm:customer:add'" 或 v-perm="['a','b']"（任一满足）
 *
 * 这里用 display:none 隐藏，而不是 removeChild。
 * 直接删 DOM 会让 Vue 的虚拟 DOM 与真实 DOM 不一致：
 * 列表刷新复用行元素时按钮会错位或消失，必须 F5 整页重载才恢复。
 */
function apply(el, binding) {
  const userStore = useUserStore()
  const value = binding.value
  const pass = Array.isArray(value) ? userStore.hasAnyPerm(value) : userStore.hasPerm(value)
  el.style.display = pass ? '' : 'none'
}

export default {
  mounted(el, binding) {
    apply(el, binding)
  },
  updated(el, binding) {
    apply(el, binding)
  }
}
