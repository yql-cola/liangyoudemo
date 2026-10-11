import axios from 'axios'
import { Message } from 'element-ui'
import { getToken, removeToken } from './auth'

export function normalizeApiResponse(response) {
  const res = response && response.code !== undefined ? response : response.data
  if (res.code === 0) {
    return res.data
  }
  throw new Error(res.message || 'request-error')
}

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 15000
})

service.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  response => normalizeApiResponse(response.data),
  error => {
    const status = error.response && error.response.status
    const message = error.response && error.response.data && error.response.data.message

    if (status === 401) {
      removeToken()
      window.location.href = '/login'
      return Promise.reject(error)
    }

    Message.error(message || error.message || '请求失败')
    return Promise.reject(error)
  }
)

export default service
