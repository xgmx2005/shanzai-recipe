import axios, { AxiosError } from 'axios'
import type { ApiResponse } from '@/types'

export const AUTH_STORAGE_KEY = 'shanzai-auth'
const seededRecipeImageFallbacks = [
  'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1559847844-5315695dadae?auto=format&fit=crop&w=900&q=80',
]

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
  if (url.startsWith('/images/recipes/')) {
    let hash = 0
    for (const char of url) {
      hash = (hash * 31 + char.charCodeAt(0)) >>> 0
    }
    return seededRecipeImageFallbacks[hash % seededRecipeImageFallbacks.length]
  }
  const origin = import.meta.env.VITE_BACKEND_ORIGIN ?? 'http://localhost:8081'
  return `${origin}${url.startsWith('/') ? url : `/${url}`}`
}
