import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const source = readFileSync(fileURLToPath(new URL('./UserLayout.vue', import.meta.url)), 'utf-8')

describe('UserLayout navigation', () => {
  it('keeps primary navigation focused and moves secondary pages into the account menu', () => {
    expect(source).toContain("label: '首页'")
    expect(source).toContain("label: '智能推荐'")
    expect(source).toContain("label: '购物清单'")
    expect(source).toContain('accountMenuItems')
    expect(source).toContain('accountMenuOpen')
    expect(source).toContain("label: '健康档案'")
    expect(source).toContain("label: '收藏菜谱'")
    expect(source).toContain("label: '推荐历史'")
    expect(source).toContain('logout-button')
    expect(source).not.toContain('Bell')
    expect(source).not.toContain('notice-button')
    expect(source).not.toContain('n-dropdown')
  })

  it('uses a dedicated route transition when entering the recommendation result page', () => {
    expect(source).toContain('pageTransitionName')
    expect(source).toContain('<router-view v-slot')
    expect(source).toContain('<transition :name="pageTransitionName" mode="out-in">')
    expect(source).toContain('viewRoute.fullPath')
    expect(source).toContain('recommend-result-transition')
    expect(source).toContain('user-page-transition')
  })
})
