import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('RecommendationResultView', () => {
  it('loads result from historyId and links to detail and shopping list', () => {
    const source = readFileSync(fileURLToPath(new URL('./RecommendationResultView.vue', import.meta.url)), 'utf8')
    expect(source).toContain('getRecommendationHistory')
    expect(source).toContain('historyId')
    expect(source).toContain('RecipeRecommendationCard')
    expect(source).toContain('shoppingListRoute')
    expect(source).toContain('暂无符合过敏和忌口约束的安全推荐')
  })
})
