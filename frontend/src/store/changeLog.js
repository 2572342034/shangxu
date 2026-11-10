import { reactive } from 'vue'

export const changeLogDialog = reactive({
  visible: false,
  bizType: 'CUSTOMER',
  bizId: null,
  title: '修改记录'
})

export function showChangeLog(bizType, bizId, title) {
  changeLogDialog.bizType = bizType || 'CUSTOMER'
  changeLogDialog.bizId = bizId
  changeLogDialog.title = title ? `${title} · 修改记录` : '修改记录'
  changeLogDialog.visible = true
}
