import request from './request'

export const login = (data) => request.post('/auth/login', data)
export const getInfo = () => request.get('/auth/info')
export const logout = () => request.post('/auth/logout')
export const changePassword = (data) => request.put('/auth/password', data)
export const updateProfile = (data) => request.put('/auth/profile', data)
