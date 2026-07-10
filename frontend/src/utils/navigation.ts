export type ShoppingListSource =
  | 'recommendation'
  | 'recommendation-detail'
  | 'recommendation-history'
  | 'recipe-detail'
  | 'favorites'

export function shoppingListRoute(listId: number, from: ShoppingListSource) {
  return {
    path: '/user/shopping-lists',
    query: {
      listId: String(listId),
      from,
    },
  }
}
