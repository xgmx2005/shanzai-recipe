import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const pageRoot = resolve(__dirname)

function readPage(name: string) {
  return readFileSync(resolve(pageRoot, name), 'utf8')
}

describe('shopping list creation navigation', () => {
  it('uses the shared route helper in every page that creates shopping lists', () => {
    const pages = [
      'RecipeDetailView.vue',
      'RecommendationHistoryView.vue',
      'FavoritesView.vue',
    ]

    for (const page of pages) {
      const source = readPage(page)
      expect(source, `${page} should import shoppingListRoute`).toContain('shoppingListRoute')
      expect(source, `${page} should not push a raw shopping list path`).not.toContain("router.push('/user/shopping-lists')")
      expect(source, `${page} should not build a raw shopping list route object`).not.toContain(
        "path: '/user/shopping-lists'",
      )
    }
  })

  it('keeps the recommendation input page focused on creating a recommendation result', () => {
    const source = readPage('RecommendView.vue')
    expect(source).toContain('recommendationResultRoute')
    expect(source).not.toContain('shoppingListRoute')
    expect(source).not.toContain("path: '/user/shopping-lists'")
  })
})
