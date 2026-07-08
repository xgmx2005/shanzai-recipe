import { http } from './http'
import type {
  AdminDashboard,
  DietGoal,
  DietGoalStat,
  Ingredient,
  IngredientSaveRequest,
  PopularRecipeStat,
  RecipeDetail,
  RecipeSaveRequest,
  RecipeSummary,
} from '@/types'

export interface AdminRecipeQuery {
  keyword?: string
  dietGoal?: DietGoal
  tag?: string
  status?: 0 | 1
}

export interface IngredientQuery {
  keyword?: string
  category?: string
}

export function getAdminDashboard() {
  return http.get<AdminDashboard>('/admin/dashboard').then((res) => res.data)
}

export function listPopularRecipes(limit = 10) {
  return http
    .get<PopularRecipeStat[]>('/admin/stats/popular-recipes', { params: { limit } })
    .then((res) => res.data)
}

export function listDietGoalStats() {
  return http.get<DietGoalStat[]>('/admin/stats/diet-goals').then((res) => res.data)
}

export function listAdminRecipes(params?: AdminRecipeQuery) {
  return http.get<RecipeSummary[]>('/admin/recipes', { params }).then((res) => res.data)
}

export function getAdminRecipe(id: number) {
  return http.get<RecipeDetail>(`/admin/recipes/${id}`).then((res) => res.data)
}

export function createAdminRecipe(payload: RecipeSaveRequest) {
  return http.post<RecipeDetail>('/admin/recipes', payload).then((res) => res.data)
}

export function updateAdminRecipe(id: number, payload: RecipeSaveRequest) {
  return http.put<RecipeDetail>(`/admin/recipes/${id}`, payload).then((res) => res.data)
}

export function deleteAdminRecipe(id: number) {
  return http.delete<null>(`/admin/recipes/${id}`).then((res) => res.data)
}

export function listIngredients(params?: IngredientQuery) {
  return http.get<Ingredient[]>('/admin/ingredients', { params }).then((res) => res.data)
}

export function createIngredient(payload: IngredientSaveRequest) {
  return http.post<Ingredient>('/admin/ingredients', payload).then((res) => res.data)
}

export function updateIngredient(id: number, payload: IngredientSaveRequest) {
  return http.put<Ingredient>(`/admin/ingredients/${id}`, payload).then((res) => res.data)
}

export function deleteIngredient(id: number) {
  return http.delete<null>(`/admin/ingredients/${id}`).then((res) => res.data)
}
