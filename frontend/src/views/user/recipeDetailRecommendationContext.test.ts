import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const source = readFileSync(fileURLToPath(new URL('./RecipeDetailView.vue', import.meta.url)), 'utf8')

describe('RecipeDetailView recommendation context', () => {
  it('loads recommendation history context when opened from recommendation results', () => {
    expect(source).toContain('getRecommendationHistory')
    expect(source).toContain('historyId')
    expect(source).toContain('recommendedRecipe')
    expect(source).toContain('recommendationDetail')
    expect(source).toContain('availableIngredients.value = history.inputIngredients')
  })

  it('shows why the recipe fits the user and strengthens cooking steps', () => {
    expect(source).toContain('适合你的原因')
    expect(source).toContain('结合已有食材推荐')
    expect(source).toContain('按目标和约束推荐')
    expect(source).toContain('建议采购')
    expect(source).toContain('stepHint')
    expect(source).toContain('步完成')
  })

  it('uses an editorial recipe detail layout with hero image and compact nutrition facts', () => {
    expect(source).toContain('detail-hero-shell')
    expect(source).toContain('hero-image-panel')
    expect(source).toContain('recipe-overview-panel')
    expect(source).toContain('nutrition-facts')
    expect(source).toContain('quick-meta')
    expect(source).toContain('recipe-body-grid')
  })
})
