import request from './request'

// ---------- 产品 ----------
export const pageProducts = (params) => request.get('/products', { params })
export const getProduct = (id) => request.get(`/products/${id}`)
export const createProduct = (data) => request.post('/products', data)
export const updateProduct = (id, data) => request.put(`/products/${id}`, data)
export const changeProductStatus = (id, status) => request.put(`/products/${id}/status`, null, { params: { status } })
export const deleteProduct = (id) => request.delete(`/products/${id}`)

// ---------- 产品目录 ----------
export const categoryTree = () => request.get('/product-categories/tree')
export const createCategory = (data) => request.post('/product-categories', data)
export const updateCategory = (id, data) => request.put(`/product-categories/${id}`, data)
export const changeCategoryStatus = (id, status) => request.put(`/product-categories/${id}/status`, null, { params: { status } })
export const deleteCategory = (id) => request.delete(`/product-categories/${id}`)
