import request from './request'

// ---------- 发货计划 ----------
export const pagePlans = (params) => request.get('/delivery-plans', { params })
export const getPlan = (id) => request.get(`/delivery-plans/${id}`)
export const createPlanFromOrder = (orderId, params) => request.post(`/delivery-plans/from-order/${orderId}`, null, { params })
export const createPlan = (data) => request.post('/delivery-plans', data)
export const updatePlan = (id, data) => request.put(`/delivery-plans/${id}`, data)
export const generateDeliveryFromPlan = (id) => request.put(`/delivery-plans/${id}/generate-delivery`)
export const cancelPlan = (id) => request.put(`/delivery-plans/${id}/cancel`)
export const deletePlan = (id) => request.delete(`/delivery-plans/${id}`)

// ---------- 开票 ----------
export const pageInvoices = (params) => request.get('/invoices', { params })
export const getInvoice = (id) => request.get(`/invoices/${id}`)
export const createInvoice = (data) => request.post('/invoices', data)
export const createInvoiceFromOrder = (orderId) => request.post(`/invoices/from-order/${orderId}`)
export const createInvoiceFromContract = (contractId) => request.post(`/invoices/from-contract/${contractId}`)
export const issueInvoice = (id, data) => request.put(`/invoices/${id}/issue`, data)
export const voidInvoice = (id) => request.put(`/invoices/${id}/void`)
export const deleteInvoice = (id) => request.delete(`/invoices/${id}`)

// ---------- 退货退款 ----------
export const pageReturns = (params) => request.get('/returns', { params })
export const getReturn = (id) => request.get(`/returns/${id}`)
export const createReturn = (data) => request.post('/returns', data)
export const updateReturn = (id, data) => request.put(`/returns/${id}`, data)
export const auditReturn = (id, status) => request.put(`/returns/${id}/audit`, null, { params: { status } })
export const refundReturn = (id) => request.put(`/returns/${id}/refund`)
export const deleteReturn = (id) => request.delete(`/returns/${id}`)
