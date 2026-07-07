export type UserRole = 'USER' | 'MAINTAINER'

export type DietGoal = 'FAT_LOSS' | 'BALANCED' | 'MUSCLE_GAIN'

export interface AuthUser {
  username: string
  nickname: string
  role: UserRole
}

export interface Profile {
  heightCm: number
  weightKg: number
  age: number
  gender: '女' | '男'
  dietGoal: DietGoal
  tastePreferences: string[]
  avoidIngredients: string[]
  allergyIngredients: string[]
}

export interface Recipe {
  id: number
  name: string
  imageUrl: string
  score: number
  calories: number
  protein: number
  time: number
  tags: string[]
  reason: string
}

export interface RecommendForm {
  availableIngredients: string[]
  avoidIngredients: string[]
  dietGoal: DietGoal
  cookingTime: number
  servings: number
}
