import { http } from './http'
import type { AuthSession, AuthUser } from '@/types'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  nickname: string
}

export interface UpdateUserRequest {
  username: string
  nickname: string
  avatarTheme: string
}

export function login(payload: LoginRequest) {
  return http.post<AuthSession>('/auth/login', payload).then((res) => res.data)
}

export function register(payload: RegisterRequest) {
  return http.post<AuthSession>('/auth/register', payload).then((res) => res.data)
}

export function getCurrentUser() {
  return http.get<AuthUser>('/auth/me').then((res) => res.data)
}

export function updateCurrentUser(payload: UpdateUserRequest) {
  return http.patch<AuthUser>('/auth/me', payload).then((res) => res.data)
}
