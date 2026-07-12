import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('RecommendationResultView', () => {
  it('loads result from historyId and links to detail and shopping list', () => {
    const source = readFileSync(fileURLToPath(new URL('./RecommendationResultView.vue', import.meta.url)), 'utf8')
    expect(source).toContain('getRecommendationHistory')
    expect(source).toContain('listFavorites')
    expect(source).toContain('historyId')
    expect(source).toContain('RecipeRecommendationCard')
    expect(source).toContain('shoppingListRoute')
    expect(source).toContain('暂无符合过敏和忌口约束的安全推荐')
  })

  it('reveals generated result sections in staged order after navigation', () => {
    const source = readFileSync(fileURLToPath(new URL('./RecommendationResultView.vue', import.meta.url)), 'utf8')
    expect(source).toContain('result-enter-shell')
    expect(source).toContain('result-reveal')
    expect(source).toContain('--reveal-index')
    expect(source).toContain('@keyframes result-reveal')
    expect(source).toContain('prefers-reduced-motion')
  })

  it('explains recommendation quality and avoids negative wording when no ingredients are provided', () => {
    const source = readFileSync(fileURLToPath(new URL('./RecommendationResultView.vue', import.meta.url)), 'utf8')
    const cardSource = readFileSync(
      fileURLToPath(new URL('../../components/recommendation/RecipeRecommendationCard.vue', import.meta.url)),
      'utf8',
    )

    expect(source).toContain('推荐逻辑')
    expect(source).toContain('安全过滤')
    expect(source).toContain('未指定，按目标推荐')
    expect(source).toContain('可进入详情查看步骤，或直接生成采购清单')
    expect(cardSource).toContain('按目标推荐')
    expect(cardSource).toContain('未指定已有食材，已按饮食目标、时间和忌口筛选')
    expect(cardSource).not.toContain('未命中已有食材')
  })

  it('keeps recommendation card heart filled after a recipe is favorited', () => {
    const source = readFileSync(fileURLToPath(new URL('./RecommendationResultView.vue', import.meta.url)), 'utf8')
    const cardSource = readFileSync(
      fileURLToPath(new URL('../../components/recommendation/RecipeRecommendationCard.vue', import.meta.url)),
      'utf8',
    )

    expect(source).toContain('favoriteRecipeIds')
    expect(source).toContain('isFavorite(recipe.id)')
    expect(source).toContain(':favorite-active="isFavorite(recipe.id)"')
    expect(source).toContain('favoriteRecipeIds.value = [...new Set([...favoriteRecipeIds.value, recipeId])]')
    expect(cardSource).toContain('favoriteActive')
    expect(cardSource).toContain('fill="favoriteActive ?')
    expect(cardSource).toContain('.icon-button.active')
  })

  it('toggles an active recommendation heart back to unfavorite', () => {
    const source = readFileSync(fileURLToPath(new URL('./RecommendationResultView.vue', import.meta.url)), 'utf8')
    const cardSource = readFileSync(
      fileURLToPath(new URL('../../components/recommendation/RecipeRecommendationCard.vue', import.meta.url)),
      'utf8',
    )

    expect(source).toContain('unfavoriteRecipe')
    expect(source).toContain('async function toggleFavorite')
    expect(source).toContain('await unfavoriteRecipe(recipeId)')
    expect(source).toContain('favoriteRecipeIds.value = favoriteRecipeIds.value.filter((id) => id !== recipeId)')
    expect(source).toContain("message.success('已取消收藏')")
    expect(source).toContain('@favorite="toggleFavorite"')
    expect(cardSource).toContain(':title="favoriteActive ?')
    expect(cardSource).toContain("favoriteActive ? '取消收藏' : '收藏菜谱'")
  })

  it('lets users collect multiple recommended recipes in a floating menu basket', () => {
    const source = readFileSync(fileURLToPath(new URL('./RecommendationResultView.vue', import.meta.url)), 'utf8')
    const cardSource = readFileSync(
      fileURLToPath(new URL('../../components/recommendation/RecipeRecommendationCard.vue', import.meta.url)),
      'utf8',
    )

    expect(source).toContain('selectedRecipeIds')
    expect(source).toContain('selectedMenuRecipes')
    expect(source).toContain('toggleMenuRecipe')
    expect(source).toContain('clearMenuBasket')
    expect(source).toContain('makeSelectedShoppingList')
    expect(source).toContain('recommendation-result-body')
    expect(source).toContain('menu-basket-rail')
    expect(source).toContain('生成所选购物清单')
    expect(source).toContain('菜单篮')
    expect(source).not.toContain('position: fixed')
    expect(source).toContain('position: sticky')
    expect(cardSource).toContain('menuSelected')
    expect(cardSource).toContain('menu-toggle')
    expect(cardSource).toContain('加入菜单篮')
    expect(cardSource).toContain('已加入')
    expect(cardSource).not.toContain('单独生成清单')
  })
})
