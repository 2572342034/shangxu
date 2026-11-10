import request from './request'

export const exportCustomers = () => request.get('/data/export/customers', { responseType: 'blob' })
export const exportLeads = () => request.get('/data/export/leads', { responseType: 'blob' })
export const exportReceivables = () => request.get('/data/export/receivables', { responseType: 'blob' })
export const exportProducts = () => request.get('/data/export/products', { responseType: 'blob' })
export const downloadProductTemplate = () => request.get('/data/export/product-template', { responseType: 'blob' })
export const importProducts = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/data/import/products', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const importCustomers = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/data/import/customers', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
