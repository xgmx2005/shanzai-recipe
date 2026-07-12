import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('HomeView workbench design', () => {
  const source = readFileSync(fileURLToPath(new URL('./HomeView.vue', import.meta.url)), 'utf-8')

  it('presents the homepage as a daily diet workbench', () => {
    expect(source).toContain('home-workbench')
    expect(source).toContain('daily-status-panel')
    expect(source).toContain('workbench-flow')
    expect(source).toContain('inspiration-recipes')
    expect(source).toContain('今天这顿，交给膳哉来配')
    expect(source).toContain('开始智能推荐')
    expect(source).toContain('完善健康档案')
    expect(source).not.toContain('最近 7 天生成推荐')
  })

  it('uses a stable random sampler instead of always taking the first four recipes', () => {
    expect(source).toContain('function sampleRecipes')
    expect(source).toContain('sampleRecipes(recipeList.map(toCard), 4)')
    expect(source).not.toContain('recipeList.slice(0, 4).map(toCard)')
  })
})
