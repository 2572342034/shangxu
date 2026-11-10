import request from './request'

// ---------- 订单 ----------
export const pageOrders = (params) => request.get('/orders', { params })
export const getOrder = (id) => request.get(`/orders/${id}`)
export const createOrder = (data) => request.post('/orders', data)
export const updateOrder = (id, data) => request.put(`/orders/${id}`, data)
export const deleteOrder = (id) => request.delete(`/orders/${id}`)
export const changeOrderStatus = (id, status) => request.put(`/orders/${id}/status`, null, { params: { status } })

// ---------- 出库 ----------
export const pageDeliveries = (params) => request.get('/deliveries', { params })
export const getDelivery = (id) => request.get(`/deliveries/${id}`)
export const deliveriesByOrder = (orderId) => request.get(`/deliveries/by-order/${orderId}`)
export const createDeliveryFromOrder = (orderId) => request.post(`/deliveries/from-order/${orderId}`)
export const updateDelivery = (id, data) => request.put(`/deliveries/${id}`, data)
export const changeDeliveryStatus = (id, status) => request.put(`/deliveries/${id}/status`, null, { params: { status } })
export const shipDelivery = (id) => request.put(`/deliveries/${id}/ship`)
export const signDelivery = (id) => request.put(`/deliveries/${id}/sign`)
export const cancelDelivery = (id) => request.put(`/deliveries/${id}/cancel`)
export const deleteDelivery = (id) => request.delete(`/deliveries/${id}`)

// ---------- 应收 ----------
export const pageReceivables = (params) => request.get('/receivables', { params })
export const receivableSummary = () => request.get('/receivables/summary')
export const pendingReceivables = () => request.get('/receivables/pending')
export const createReceivable = (data) => request.post('/receivables', data)
export const createReceivableFromOrder = (orderId) => request.post(`/receivables/from-order/${orderId}`)
export const createReceivableFromContract = (contractId) => request.post(`/receivables/from-contract/${contractId}`)
export const receiveReceivable = (id, data) => request.put(`/receivables/${id}/receive`, data)
export const auditReceivable = (id, status) => request.put(`/receivables/${id}/audit`, null, { params: { status } })
export const deleteReceivable = (id) => request.delete(`/receivables/${id}`)
