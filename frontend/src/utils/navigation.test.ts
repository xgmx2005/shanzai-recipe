import { describe, expect, it } from 'vitest'
import { shoppingListRoute } from './navigation'

describe('shoppingListRoute', () => {
  it('opens the newly created shopping list by id and source', () => {
    expect(shoppingListRoute(18, 'recommendation-history')).toEqual({
      path: '/user/shopping-lists',
      query: {
        listId: '18',
        from: 'recommendation-history',
      },
    })
  })
})
