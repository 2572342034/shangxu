import request from './request'

export const listNotices = (params) => request.get('/notices', { params })
export const getUnreadCount = () => request.get('/notices/unread-count')
export const readNotice = (id) => request.put(`/notices/${id}/read`)
export const readAllNotices = () => request.put('/notices/read-all')
