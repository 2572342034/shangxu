import request from './request'

export const pageActivities = (params) => request.get('/activities', { params })
export const recentActivities = (limit = 10) => request.get('/activities/recent', { params: { limit } })
export const createActivity = (data) => request.post('/activities', data)
export const deleteActivity = (id) => request.delete(`/activities/${id}`)
