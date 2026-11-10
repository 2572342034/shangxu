import request from './request'

export const tradeOverview = () => request.get('/trade/overview')
export const tradeTrend = () => request.get('/trade/trend')
export const tradeFlow = (limit = 20) => request.get('/trade/flow', { params: { limit } })
export const tradeRank = () => request.get('/trade/rank')
