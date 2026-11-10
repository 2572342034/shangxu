import request from './request'

export const listAttachments = (bizType, bizId) => request.get('/attachments', { params: { bizType, bizId } })

export const uploadAttachment = (bizType, bizId, file, onProgress) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/attachments', form, {
    params: { bizType, bizId },
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100))
      }
    }
  })
}

export const deleteAttachment = (id) => request.delete(`/attachments/${id}`)

export const downloadAttachment = (id) => request.get(`/attachments/${id}/download`, { responseType: 'blob' })
