import axios from 'axios'
import { ElMessage } from 'element-plus'
import {
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
  clearTokens,
  logout,
} from './auth.js'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const service = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    'X-Platform': 'web',
  },
})

let isRefreshing = false
let refreshSubscribers = []

function subscribeTokenRefresh(callback) {
  refreshSubscribers.push(callback)
}

function onTokenRefreshed(token) {
  refreshSubscribers.forEach((callback) => callback(token))
  refreshSubscribers = []
}

let isLoggingOut = false

function triggerLogout() {
  if (isLoggingOut) return
  isLoggingOut = true
  
  clearTokens()
  
  setTimeout(() => {
    logout()
    setTimeout(() => {
      isLoggingOut = false
    }, 10000)
  }, 500)
}

service.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    const { data } = response
    
    const code = data?.code ?? 0
    if (code === 0 || code === 200 || code === null || code === undefined) {
      return data
    }
    
    const message = data?.message || '操作失败'
    if (response.config.showError !== false) {
      ElMessage.error(message)
    }
    
    return Promise.reject(new Error(message))
  },
  async (error) => {
    const originalRequest = error.config
    
    if (error.response?.status === 401) {
      if (originalRequest.url && originalRequest.url.includes('/auth/refresh')) {
        triggerLogout()
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
      
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((newToken) => {
            if (newToken) {
              originalRequest.headers.Authorization = `Bearer ${newToken}`
              resolve(service(originalRequest))
            } else {
              reject(new Error('登录已过期，请重新登录'))
            }
          })
        })
      }
      
      isRefreshing = true
      
      try {
        const refreshTokenValue = getRefreshToken()
        
        if (!refreshTokenValue) {
          isRefreshing = false
          triggerLogout()
          return Promise.reject(new Error('登录已过期，请重新登录'))
        }
        
        const refreshResponse = await axios.post(
          `${BASE_URL}/auth/refresh`,
          { refreshToken: refreshTokenValue },
          { timeout: 10000 }
        )
        
        const { accessToken, refreshToken: newRefreshToken } = refreshResponse.data?.data || refreshResponse.data || {}
        
        if (accessToken) {
          setAccessToken(accessToken)
          if (newRefreshToken) {
            setRefreshToken(newRefreshToken)
          }
          
          isRefreshing = false
          
          onTokenRefreshed(accessToken)
          
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return service(originalRequest)
        } else {
          isRefreshing = false
          onTokenRefreshed(null)
          triggerLogout()
          return Promise.reject(new Error('登录已过期，请重新登录'))
        }
      } catch (refreshError) {
        isRefreshing = false
        onTokenRefreshed(null)
        triggerLogout()
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
    }
    
    let errorMessage = '网络错误，请稍后重试'
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          errorMessage = data?.message || '请求参数错误'
          break
        case 403:
          errorMessage = data?.message || '没有权限'
          break
        case 404:
          errorMessage = data?.message || '请求资源不存在'
          break
        case 500:
          errorMessage = '服务器内部错误'
          break
        default:
          errorMessage = data?.message || `请求失败 (${status})`
      }
    } else if (error.request) {
      errorMessage = error.code === 'ECONNABORTED' ? '请求超时' : '网络连接失败'
    }
    
    if (error.config?.showError !== false) {
      ElMessage.error(errorMessage)
    }
    
    return Promise.reject(new Error(errorMessage))
  }
)

export function get(url, params = {}, config = {}) {
  return service.get(url, { params, ...config })
}

export function post(url, data = {}, config = {}) {
  return service.post(url, data, config)
}

export function put(url, data = {}, config = {}) {
  return service.put(url, data, config)
}

export function del(url, params = {}, config = {}) {
  return service.delete(url, { params, ...config })
}

export function patch(url, data = {}, config = {}) {
  return service.patch(url, data, config)
}

export { service as request }
export default service
