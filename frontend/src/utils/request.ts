import axios from 'axios'
import { ElMessage } from 'element-plus'

/** 统一响应结构（对应后端 ApiResponse） */
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('sm_token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body && typeof body.code === 'number') {
      if (body.code !== 0) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message))
      }
      return body.data as never
    }
    return response.data
  },
  (error) => {
    // HTTP 层错误或全局异常处理器返回的业务错误
    const body = error?.response?.data as ApiResponse<unknown> | undefined
    if (body && body.code === 401) {
      localStorage.removeItem('sm_token')
      localStorage.removeItem('sm_user')
      if (!location.hash.includes('/login') && !location.pathname.includes('/login')) {
        location.href = '/login'
      }
    }
    if (body && body.message) {
      ElMessage.error(body.message)
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request as {
  <T = any>(config: import('axios').AxiosRequestConfig): Promise<T>
  get<T = any>(url: string, config?: import('axios').AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: unknown, config?: import('axios').AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: unknown, config?: import('axios').AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: import('axios').AxiosRequestConfig): Promise<T>
}
