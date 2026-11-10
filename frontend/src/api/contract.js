import request from './request'

export const pageContracts = (params) => request.get('/contracts', { params })
export const getContract = (id) => request.get(`/contracts/${id}`)
export const createContract = (data) => request.post('/contracts', data)
export const updateContract = (id, data) => request.put(`/contracts/${id}`, data)
export const deleteContract = (id) => request.delete(`/contracts/${id}`)
export const changeContractStatus = (id, status) => request.put(`/contracts/${id}/status`, null, { params: { status } })
export const listPayments = (id) => request.get(`/contracts/${id}/payments`)
export const addPayment = (id, data) => request.post(`/contracts/${id}/payments`, data)
export const receivePayment = (paymentId, data) => request.put(`/contracts/payments/${paymentId}/receive`, data)
export const selectableContracts = (customerId) => request.get('/contracts/selectable', { params: { customerId } })

export const pageProducts = (params) => request.get('/products', { params })
export const createProduct = (data) => request.post('/products', data)
export const updateProduct = (id, data) => request.put(`/products/${id}`, data)
export const deleteProduct = (id) => request.delete(`/products/${id}`)
