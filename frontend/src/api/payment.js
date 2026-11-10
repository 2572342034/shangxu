import request from './request'

export const pagePayments = (params) => request.get('/payments', { params })
export const getPayment = (id) => request.get(`/payments/${id}`)
export const getPaymentSummary = () => request.get('/payments/summary')
export const getOpenReceivables = (customerId) => request.get('/payments/open-receivables', { params: { customerId } })
export const createPayment = (data) => request.post('/payments', data)
export const allocatePayment = (id, data) => request.put(`/payments/${id}/allocate`, data)
export const cancelPayment = (id) => request.put(`/payments/${id}/cancel`)
