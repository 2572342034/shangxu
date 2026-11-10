import request from './request'

export const getChangeLogs = (params) => request.get('/change-logs', { params })
export const pageChangeLogs = (params) => request.get('/change-logs/page', { params })

export const BIZ_TYPES = [
  { value: 'CUSTOMER', label: '客户' },
  { value: 'CONTACT', label: '联系人' },
  { value: 'LEAD', label: '线索' },
  { value: 'OPPORTUNITY', label: '商机' },
  { value: 'CONTRACT', label: '合同' },
  { value: 'ORDER', label: '订单' },
  { value: 'DELIVERY', label: '出库单' },
  { value: 'DELIVERY_PLAN', label: '发货计划' },
  { value: 'INVOICE', label: '发票' },
  { value: 'RECEIVABLE', label: '应收单' },
  { value: 'PAYMENT', label: '收款单' },
  { value: 'RETURN', label: '退货单' },
  { value: 'PRODUCT', label: '产品' },
  { value: 'PRODUCT_CATEGORY', label: '产品目录' },
  { value: 'ACTIVITY', label: '跟进记录' },
  { value: 'ATTACHMENT', label: '附件' }
]

export const ACTION_LABELS = {
  UPDATE: '修改', CREATE: '新建', DELETE: '删除',
  CLAIM: '领取', TRANSFER: '转移', TO_PUBLIC: '放入公海', RECYCLE: '自动回收'
}

export function bizTypeLabel(value) {
  return BIZ_TYPES.find(t => t.value === value)?.label || value
}

export function actionLabel(value) {
  return ACTION_LABELS[value] || value || '-'
}
