import request from './request'

export const pageLeads = (params) => request.get('/leads', { params })
export const getLead = (id) => request.get(`/leads/${id}`)
export const createLead = (data) => request.post('/leads', data)
export const updateLead = (id, data) => request.put(`/leads/${id}`, data)
export const deleteLead = (id) => request.delete(`/leads/${id}`)
export const convertLead = (id, data) => request.post(`/leads/${id}/convert`, data)
