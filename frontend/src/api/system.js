import request from './request'

export const pageUsers = (params) => request.get('/system/users', { params })
export const userOptions = () => request.get('/system/users/options')
export const getUser = (id) => request.get(`/system/users/${id}`)
export const createUser = (data) => request.post('/system/users', data)
export const updateUser = (id, data) => request.put(`/system/users/${id}`, data)
export const deleteUser = (id) => request.delete(`/system/users/${id}`)
export const resetUserPassword = (id, password) => request.put(`/system/users/${id}/password`, { password })
export const changeUserStatus = (id, status) => request.put(`/system/users/${id}/status`, null, { params: { status } })

export const listRoles = (params) => request.get('/system/roles', { params })
export const createRole = (data) => request.post('/system/roles', data)
export const updateRole = (id, data) => request.put(`/system/roles/${id}`, data)
export const deleteRole = (id) => request.delete(`/system/roles/${id}`)

export const deptTree = () => request.get('/system/depts/tree')
export const deptList = () => request.get('/system/depts')
export const createDept = (data) => request.post('/system/depts', data)
export const updateDept = (id, data) => request.put(`/system/depts/${id}`, data)
export const deleteDept = (id) => request.delete(`/system/depts/${id}`)

export const pageDicts = (params) => request.get('/system/dicts', { params })
export const dictsByType = (type) => request.get(`/system/dicts/type/${type}`)
export const allDicts = () => request.get('/system/dicts/all')
export const createDict = (data) => request.post('/system/dicts', data)
export const updateDict = (id, data) => request.put(`/system/dicts/${id}`, data)
export const deleteDict = (id) => request.delete(`/system/dicts/${id}`)

export const pageLogs = (params) => request.get('/system/logs', { params })
export const clearLogs = () => request.delete('/system/logs')
