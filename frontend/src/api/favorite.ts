import { http } from './http'
import type { FavoriteRecipe } from '@/types'

export function favoriteRecipe(recipeId: number) {
  return http.post<FavoriteRecipe>(`/recipes/${recipeId}/favorite`).then((res) => res.data)
}

export function unfavoriteRecipe(recipeId: number) {
  return http.delete<null>(`/recipes/${recipeId}/favorite`).then((res) => res.data)
}

export function listFavorites() {
  return http.get<FavoriteRecipe[]>('/favorites').then((res) => res.data)
}
