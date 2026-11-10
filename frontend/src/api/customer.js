import request from './request'

export const pageCustomers = (params) => request.get('/customers', { params })
export const getCustomer = (id) => request.get(`/customers/${id}`)
export const getCustomerDetail = (id) => request.get(`/customers/${id}/detail`)
export const createCustomer = (data) => request.post('/customers', data)
export const updateCustomer = (id, data) => request.put(`/customers/${id}`, data)
export const deleteCustomer = (id) => request.delete(`/customers/${id}`)
export const transferCustomer = (id, params) => request.put(`/customers/${id}/transfer`, null, { params })
export const toPublicCustomer = (id) => request.put(`/customers/${id}/to-public`)
export const claimCustomer = (id) => request.put(`/customers/${id}/claim`)

export const pageContacts = (params) => request.get('/contacts', { params })
export const createContact = (data) => request.post('/contacts', data)
export const updateContact = (id, data) => request.put(`/contacts/${id}`, data)
export const deleteContact = (id) => request.delete(`/contacts/${id}`)
