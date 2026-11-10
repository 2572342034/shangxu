import request from './request'

export const getOverview = () => request.get('/dashboard/overview')
export const getFunnel = () => request.get('/dashboard/funnel')
export const getTrend = () => request.get('/dashboard/trend')
export const getRank = () => request.get('/dashboard/rank')
export const getSource = () => request.get('/dashboard/source')
export const getLevel = () => request.get('/dashboard/level')
export const getTodo = () => request.get('/dashboard/todo')
