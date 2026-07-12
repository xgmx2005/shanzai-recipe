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
    expect(profileSource).toContain('uploadAvatar')
    expect(profileSource).toContain('type="file"')
    expect(profileSource).toContain('accept="image/png,image/jpeg,image/webp"')
    expect(profileSource).toContain('用户名')
    expect(profileSource).toContain('显示名称')
  })

  it('presents the health profile as a personal diet settings center', () => {
    expect(profileSource).toContain('personal-diet-center')
    expect(profileSource).toContain('profile-command-center')
    expect(profileSource).toContain('profile-summary-strip')
    expect(profileSource).toContain('profile-settings-layout')
    expect(profileSource).toContain('个人饮食设置中心')
    expect(profileSource).toContain('推荐会使用这份档案')
    expect(profileSource).toContain('推荐偏好配置')
    expect(profileSource).toContain('身份与头像')
    expect(profileSource).not.toContain('profile-hero')
  })

  it('keeps the profile settings details polished and non-misleading', () => {
    expect(profileSource).toContain('实时预览')
    expect(profileSource).toContain('position: sticky')
    expect(profileSource).toContain('top: 92px')
    expect(profileSource).not.toContain('已同步')
    expect(profileSource).not.toContain('account-preview')
  })

  it('keeps uploaded avatars aligned with the visible avatar frame', () => {
    expect(profileSource).toContain('.account-avatar img')
    expect(profileSource).toContain('border-radius: inherit')
    expect(profileSource).toContain('object-position: center')
    expect(profileSource).not.toContain('profile-command-center::after')
  })

  it('treats save success messages as temporary notices', () => {
    expect(profileSource).toContain('showSuccessNotice')
    expect(profileSource).toContain('window.setTimeout')
    expect(profileSource).toContain('clearSuccessNoticeTimer')
    expect(profileSource).toContain('onUnmounted(clearSuccessNoticeTimer)')
  })

  it('persists account text and avatar theme through the auth api and store', () => {
    expect(authSource).toContain('updateCurrentUser')
    expect(authSource).toContain('uploadCurrentUserAvatar')
    expect(authSource).toContain("patch<AuthUser>('/auth/me'")
    expect(authSource).toContain("post<AuthUser>('/auth/me/avatar'")
    expect(storeSource).toContain('updateAccount')
    expect(storeSource).toContain('uploadAvatar')
    expect(storeSource).toContain('avatarTheme')
    expect(storeSource).toContain('avatarUrl')
    expect(storeSource).not.toContain('setAvatarTheme')
  })

  it('offers a guarded production-grade account deletion flow', () => {
    expect(authSource).toContain('deleteCurrentUser')
    expect(authSource).toContain("delete<null>('/auth/me'")
    expect(storeSource).toContain('deleteAccount')
    expect(storeSource).toContain('await deleteCurrentUser()')
    expect(storeSource).toContain('this.logout()')
    expect(profileSource).toContain('danger-account-zone')
    expect(profileSource).toContain('deleteConfirmText')
    expect(profileSource).toContain('确认注销账号')
    expect(profileSource).toContain('注销账号')
    expect(profileSource).toContain("deleteConfirmText !== '注销账号'")
    expect(profileSource).toContain('handleDeleteAccount')
  })
})
