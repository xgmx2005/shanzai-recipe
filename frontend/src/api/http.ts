import axios, { AxiosError } from 'axios'
import type { ApiResponse } from '@/types'

export const AUTH_STORAGE_KEY = 'shanzai-auth'

interface StoredAuth {
  token: string
}

function readToken() {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null

  try {
    const stored = JSON.parse(raw) as Partial<StoredAuth>
    return stored.token ?? null
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

function clearStoredAuth() {
  localStorage.removeItem(AUTH_STORAGE_KEY)
}

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    window.location.assign('/login')
  }
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const token = readToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload && typeof payload === 'object' && 'success' in payload) {
      if (!payload.success) {
        throw new Error(payload.message || '请求失败')
      }
      response.data = payload.data
    }
    return response
  },
  (error: AxiosError<ApiResponse<null>>) => {
    if (error.response?.status === 401) {
      clearStoredAuth()
      redirectToLogin()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }

    if (error.response?.status === 403) {
      return Promise.reject(new Error(error.response.data?.message || '没有权限访问该功能'))
    }

    const message = error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  },
)

export function backendAssetUrl(url?: string) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const origin = import.meta.env.VITE_BACKEND_ORIGIN ?? 'http://localhost:8081'
  return `${origin}${url.startsWith('/') ? url : `/${url}`}`
}
