import { http } from './http'
import type {
  ShoppingList,
  ShoppingListCreateRequest,
  ShoppingListItem,
  ShoppingListSummary,
} from '@/types'

export function createShoppingList(payload: ShoppingListCreateRequest) {
  return http.post<ShoppingList>('/shopping-lists', payload).then((res) => res.data)
}

export function listShoppingLists() {
  return http.get<ShoppingListSummary[]>('/shopping-lists').then((res) => res.data)
}

export function getShoppingList(id: number) {
  return http.get<ShoppingList>(`/shopping-lists/${id}`).then((res) => res.data)
}

export function updateShoppingListItem(listId: number, itemId: number, checked: boolean) {
  return http
    .patch<ShoppingListItem>(`/shopping-lists/${listId}/items/${itemId}`, { checked })
    .then((res) => res.data)
}

export function deleteShoppingList(id: number) {
  return http.delete<null>(`/shopping-lists/${id}`).then((res) => res.data)
}
