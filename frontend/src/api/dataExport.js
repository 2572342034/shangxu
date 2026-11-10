import request from './request'

export const exportMeta = (module) => request.get('/data/exports/meta', { params: { module } })
export const exportPreview = (params) => request.get('/data/exports/preview', { params })
export const exportDownload = (params) => request.get('/data/exports/download', {
  params,
  responseType: 'blob'
})
