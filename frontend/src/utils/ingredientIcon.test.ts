import { describe, expect, it } from 'vitest'
import { resolveIngredientIcon } from './assets'

describe('resolveIngredientIcon', () => {
  it('maps common recipe ingredients to food icons instead of generic fallback', () => {
    for (const name of ['青菜', '葱', '姜', '芦笋', '橄榄油', '柠檬', '彩椒']) {
      expect(resolveIngredientIcon(name), name).not.toContain('ingredient-default')
    }
  })
})
