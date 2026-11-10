import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000
})

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let redirecting = false

service.interceptors.response.use(
  (response) => {
    // 文件下载：直接把完整响应交给调用方处理 Blob 与文件名
    if (response.config && response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code === 200) {
      return res
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.msg
    if (status === 401) {
      if (!redirecting) {
        redirecting = true
        removeToken()
        ElMessage.error(message || '登录状态已失效，请重新登录')
        const redirect = encodeURIComponent(window.location.hash.replace('#', '') || '/')
        setTimeout(() => {
          window.location.href = `/#/login?redirect=${redirect}`
          redirecting = false
        }, 400)
      }
    } else if (status === 403) {
      ElMessage.error(message || '没有操作权限')
    } else {
      ElMessage.error(message || error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default service
