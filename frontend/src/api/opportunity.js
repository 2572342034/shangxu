import request from './request'

export const pageOpportunities = (params) => request.get('/opportunities', { params })
export const getKanban = () => request.get('/opportunities/kanban')
export const getOpportunity = (id) => request.get(`/opportunities/${id}`)
export const createOpportunity = (data) => request.post('/opportunities', data)
export const updateOpportunity = (id, data) => request.put(`/opportunities/${id}`, data)
export const changeStage = (id, stage) => request.put(`/opportunities/${id}/stage`, null, { params: { stage } })
export const deleteOpportunity = (id) => request.delete(`/opportunities/${id}`)
