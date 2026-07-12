import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const routerSource = readFileSync(fileURLToPath(new URL('../router/index.ts', import.meta.url)), 'utf-8')
const registerSource = readFileSync(fileURLToPath(new URL('./RegisterView.vue', import.meta.url)), 'utf-8')
const typesSource = readFileSync(fileURLToPath(new URL('../types.ts', import.meta.url)), 'utf-8')
const homeSource = readFileSync(fileURLToPath(new URL('./user/HomeView.vue', import.meta.url)), 'utf-8')
const recommendSource = readFileSync(fileURLToPath(new URL('./user/RecommendView.vue', import.meta.url)), 'utf-8')
const profileSource = readFileSync(fileURLToPath(new URL('./user/ProfileView.vue', import.meta.url)), 'utf-8')
const onboardingSource = readFileSync(
  fileURLToPath(new URL('./user/ProfileOnboardingView.vue', import.meta.url)),
  'utf-8',
)

describe('profile onboarding flow wiring', () => {
  it('routes new user registration into onboarding', () => {
    expect(routerSource).toContain("path: 'onboarding'")
    expect(routerSource).toContain("name: 'profile-onboarding'")
    expect(registerSource).toContain("'/user/onboarding'")
  })

  it('models profile completion on profile and summary payloads', () => {
    expect(typesSource).toContain('profileCompleted: boolean')
    expect(typesSource).toContain('profileCompleted?: boolean')
  })

  it('presents a three-step health profile onboarding page', () => {
    expect(onboardingSource).toContain('profile-onboarding')
    expect(onboardingSource).toContain('基础身体信息')
    expect(onboardingSource).toContain('饮食目标')
    expect(onboardingSource).toContain('口味与限制')
    expect(onboardingSource).toContain('保存并开始使用')
    expect(onboardingSource).toContain('稍后完善')
    expect(onboardingSource).toContain('profileCompleted: true')
  })

  it('shows lightweight reminders when profile is incomplete', () => {
    expect(homeSource).toContain('完善健康档案后，推荐会更准确')
    expect(homeSource).toContain('/user/onboarding')
    expect(recommendSource).toContain('当前使用默认档案')
    expect(recommendSource).toContain('/user/onboarding')
  })

  it('keeps completion state when users save a complete profile later', () => {
    expect(profileSource).toContain('profileCompleted: completion.value >= 100')
  })
})
