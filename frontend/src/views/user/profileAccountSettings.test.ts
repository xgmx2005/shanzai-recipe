import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const profileSource = readFileSync(fileURLToPath(new URL('./ProfileView.vue', import.meta.url)), 'utf-8')
const authSource = readFileSync(fileURLToPath(new URL('../../api/auth.ts', import.meta.url)), 'utf-8')
const storeSource = readFileSync(fileURLToPath(new URL('../../stores/auth.ts', import.meta.url)), 'utf-8')

describe('profile account settings', () => {
  it('lets users edit account name and avatar theme from the health profile page', () => {
    expect(profileSource).toContain('账号资料')
    expect(profileSource).toContain('accountForm')
    expect(profileSource).toContain('avatarOptions')
    expect(profileSource).toContain('saveAccount')
    expect(profileSource).toContain('用户名')
    expect(profileSource).toContain('显示名称')
  })

  it('persists account text and avatar theme through the auth api and store', () => {
    expect(authSource).toContain('updateCurrentUser')
    expect(authSource).toContain("patch<AuthUser>('/auth/me'")
    expect(storeSource).toContain('updateAccount')
    expect(storeSource).toContain('avatarTheme')
    expect(storeSource).not.toContain('setAvatarTheme')
  })
})
