import { defineStore } from 'pinia'
import {
  deleteCurrentUser,
  getCurrentUser,
  login as loginApi,
  register as registerApi,
  updateCurrentUser,
  uploadCurrentUserAvatar,
} from '@/api/auth'
import { AUTH_STORAGE_KEY } from '@/api/http'
import { getProfile, saveProfile as saveProfileApi } from '@/api/profile'
import type { AuthSession, AuthUser, Profile, ProfileRequest, UserRole } from '@/types'

const defaultProfile: Profile = {
  heightCm: 165,
  weightKg: 55,
  age: 26,
  gender: '女',
  dietGoal: 'FAT_LOSS',
  tastePreferences: ['清淡', '健康', '不油腻'],
  avoidIngredients: [],
  allergyIngredients: [],
  cookingTimePreference: 30,
  profileCompleted: false,
}

const DEFAULT_AVATAR_THEME = 'leaf'
const DEFAULT_AVATAR_URL = ''

interface AuthState {
  token: string | null
  user: AuthUser | null
  profile: Profile
  initialized: boolean
}

interface StoredAuth {
  token: string
  user: AuthUser
}

function readStoredAuth(): StoredAuth | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null

  try {
    const stored = JSON.parse(raw) as StoredAuth
    if (!stored.token || !stored.user) return null
    stored.user.avatarTheme = stored.user.avatarTheme ?? DEFAULT_AVATAR_THEME
    stored.user.avatarUrl = stored.user.avatarUrl ?? DEFAULT_AVATAR_URL
    return stored
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

function persistSession(session: AuthSession | { token: string; user: AuthUser }) {
  const user =
    'user' in session
      ? session.user
      : {
          userId: session.userId,
          username: session.username,
          nickname: session.nickname,
          avatarTheme: session.avatarTheme ?? DEFAULT_AVATAR_THEME,
          avatarUrl: session.avatarUrl ?? DEFAULT_AVATAR_URL,
          role: session.role,
        }

  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ token: session.token, user }))
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: readStoredAuth()?.token ?? null,
    user: readStoredAuth()?.user ?? null,
    profile: { ...defaultProfile },
    initialized: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user),
    role: (state): UserRole | null => state.user?.role ?? null,
  },
  actions: {
    async login(username: string, password: string) {
      const session = await loginApi({ username, password })
      return this.applySession(session)
    },
    async register(username: string, password: string, nickname: string) {
      const session = await registerApi({ username, password, nickname })
      return this.applySession(session)
    },
    applySession(session: AuthSession) {
      const user: AuthUser = {
        userId: session.userId,
        username: session.username,
        nickname: session.nickname,
        avatarTheme: session.avatarTheme ?? DEFAULT_AVATAR_THEME,
        avatarUrl: session.avatarUrl ?? DEFAULT_AVATAR_URL,
        role: session.role,
      }
      this.token = session.token
      this.user = user
      this.initialized = true
      persistSession(session)
      return user
    },
    async initSession() {
      if (this.initialized) return this.user
      if (!this.token) {
        this.initialized = true
        return null
      }

      try {
        const user = await getCurrentUser()
        this.user = {
          ...user,
          avatarTheme: user.avatarTheme ?? DEFAULT_AVATAR_THEME,
          avatarUrl: user.avatarUrl ?? DEFAULT_AVATAR_URL,
        }
        persistSession({ token: this.token, user: this.user })
        return this.user
      } catch {
        this.logout()
        return null
      } finally {
        this.initialized = true
      }
    },
    logout() {
      this.token = null
      this.user = null
      this.profile = { ...defaultProfile }
      this.initialized = true
      localStorage.removeItem(AUTH_STORAGE_KEY)
    },
    async loadProfile() {
      const profile = await getProfile()
      if (profile) {
        this.profile = { ...profile }
      }
      return this.profile
    },
    async saveProfile(profile: ProfileRequest) {
      const saved = await saveProfileApi(profile)
      this.profile = { ...saved }
      return saved
    },
    async updateAccount(payload: { username: string; nickname: string; avatarTheme: string }) {
      const user = await updateCurrentUser(payload)
      this.user = {
        ...user,
        avatarTheme: user.avatarTheme ?? DEFAULT_AVATAR_THEME,
        avatarUrl: user.avatarUrl ?? DEFAULT_AVATAR_URL,
      }
      if (this.token) {
        persistSession({ token: this.token, user: this.user })
      }
      return this.user
    },
    async uploadAvatar(file: File) {
      const user = await uploadCurrentUserAvatar(file)
      this.user = {
        ...user,
        avatarTheme: user.avatarTheme ?? DEFAULT_AVATAR_THEME,
        avatarUrl: user.avatarUrl ?? DEFAULT_AVATAR_URL,
      }
      if (this.token) {
        persistSession({ token: this.token, user: this.user })
      }
      return this.user
    },
    async deleteAccount() {
      await deleteCurrentUser()
      this.logout()
    },
  },
})
