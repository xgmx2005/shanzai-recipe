import { defineStore } from 'pinia'
import type { AuthUser, Profile, UserRole } from '@/types'
import { demoProfile } from '@/mock/data'

const STORAGE_KEY = 'shanzai-auth'

interface AuthState {
  user: AuthUser | null
  profile: Profile
}

function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: readStoredUser(),
    profile: { ...demoProfile },
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.user),
    role: (state): UserRole | null => state.user?.role ?? null,
  },
  actions: {
    login(username: string, password: string) {
      if (password !== '123456') {
        throw new Error('账号或密码不正确')
      }

      const role: UserRole = username === 'maintainer' ? 'MAINTAINER' : 'USER'
      const user: AuthUser = {
        username,
        role,
        nickname: role === 'MAINTAINER' ? '小膳维护员' : '小秦爱吃鱼',
      }
      this.user = user
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
      return user
    },
    logout() {
      this.user = null
      localStorage.removeItem(STORAGE_KEY)
    },
    saveProfile(profile: Profile) {
      this.profile = { ...profile }
    },
  },
})
