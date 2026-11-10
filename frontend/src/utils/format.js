import dayjs from 'dayjs'

export function formatMoney(value, withSymbol = true) {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return '-'
  const text = num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return withSymbol ? `¥${text}` : text
}

/** 金额转万元，用于卡片/图表 */
export function toWan(value, digits = 1) {
  const num = Number(value || 0)
  return Number((num / 10000).toFixed(digits))
}

export function formatWan(value) {
  const num = Number(value || 0)
  if (num >= 100000000) return `${(num / 100000000).toFixed(2)} 亿`
  if (num >= 10000) return `${(num / 10000).toFixed(1)} 万`
  return num.toLocaleString('zh-CN')
}

export function formatDate(value, pattern = 'YYYY-MM-DD') {
  if (!value) return '-'
  const d = dayjs(value)
  return d.isValid() ? d.format(pattern) : '-'
}

export function formatDateTime(value) {
  return formatDate(value, 'YYYY-MM-DD HH:mm')
}

export function fromNow(value) {
  if (!value) return '-'
  const d = dayjs(value)
  if (!d.isValid()) return '-'
  const diff = dayjs().diff(d, 'minute')
  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff} 分钟前`
  if (diff < 60 * 24) return `${Math.floor(diff / 60)} 小时前`
  if (diff < 60 * 24 * 30) return `${Math.floor(diff / 60 / 24)} 天前`
  return d.format('YYYY-MM-DD')
}

export function initials(name) {
  if (!name) return '?'
  return String(name).slice(0, 2)
}

/** 头像文字：取姓名首字 */
export function avatarChar(name) {
  if (!name) return '?'
  return String(name).slice(0, 1)
}
