import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const loginSource = readFileSync(fileURLToPath(new URL('./LoginView.vue', import.meta.url)), 'utf-8')
const shellSource = readFileSync(fileURLToPath(new URL('../components/AuthShell.vue', import.meta.url)), 'utf-8')

describe('LoginView refined glass design', () => {
  it('removes demo account shortcuts and starts from an empty login form', () => {
    expect(loginSource).not.toContain('演示账号')
    expect(loginSource).not.toContain('fillDemo')
    expect(loginSource).not.toContain("username: 'user1'")
    expect(loginSource).not.toContain("password: '123456'")
  })

  it('uses a frosted lifestyle shell for the auth experience', () => {
    expect(shellSource).toContain('frosted-auth-shell')
    expect(shellSource).toContain('backdrop-filter')
    expect(shellSource.indexOf('class="brand-panel"')).toBeLessThan(shellSource.indexOf('class="form-panel"'))
    expect(shellSource).toContain('position: absolute')
    expect(shellSource).toContain('right: 42px')
    expect(shellSource).toContain('font-size: 28px')
    expect(shellSource).not.toContain('nutrition-float-card')
    expect(shellSource).not.toContain('今日推荐')
  })
})
