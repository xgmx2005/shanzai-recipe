import { http } from './http'
import type { DietGoal, RecipeDetail, RecipeSummary } from '@/types'

export interface RecipeQuery {
  keyword?: string
  dietGoal?: DietGoal
  tag?: string
}

export function listRecipes(params?: RecipeQuery) {
  return http.get<RecipeSummary[]>('/recipes', { params }).then((res) => res.data)
}

export function getRecipe(id: number) {
  return http.get<RecipeDetail>(`/recipes/${id}`).then((res) => res.data)
}
