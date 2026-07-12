export type UserRole = 'USER' | 'MAINTAINER'

export type DietGoal = 'FAT_LOSS' | 'BALANCED' | 'MUSCLE_GAIN'

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

export interface AuthUser {
  userId: number
  username: string
  nickname: string
  avatarTheme: string
  avatarUrl: string
  role: UserRole
}

export interface AuthSession extends AuthUser {
  token: string
}

export interface Profile {
  id?: number
  userId?: number
  heightCm: number
  weightKg: number
  age: number
  gender: '女' | '男'
  dietGoal: DietGoal
  tastePreferences: string[]
  avoidIngredients: string[]
  allergyIngredients: string[]
  cookingTimePreference: number
  profileCompleted: boolean
  bmi?: number
  dailyCalorieTarget?: number
  updatedAt?: string
}

export type ProfileRequest = Pick<
  Profile,
  | 'heightCm'
  | 'weightKg'
  | 'age'
  | 'gender'
  | 'dietGoal'
  | 'tastePreferences'
  | 'avoidIngredients'
  | 'allergyIngredients'
  | 'cookingTimePreference'
> & {
  profileCompleted?: boolean
}

export interface ProfileSummary {
  hasProfile: boolean
  dietGoal: DietGoal
  bmi: number | null
  bmiStatus: string
  dailyCalorieTarget: number | null
  cookingTimePreference: number | null
  profileCompleted: boolean
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface RecipeSummary {
  id: number
  name: string
  imageUrl: string
  description: string
  cookingTime: number
  difficulty: Difficulty
  servings: number
  calories: number
  protein: number
  fat: number
  carbs: number
  tasteTags: string[]
  healthTags: string[]
  targetGoals: DietGoal[]
  status: number
}

export interface RecipeIngredient {
  ingredientId: number
  name: string
  category: string
  quantity: number
  unit: string
  core: boolean
}

export interface RecipeDetail extends RecipeSummary {
  steps: string[]
  ingredients: RecipeIngredient[]
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface RecipeCardModel {
  id: number
  name: string
  imageUrl?: string
  score?: number
  calories: number
  protein: number
  time?: number
  tags?: string[]
  reason?: string
}

export interface RecommendForm {
  availableIngredients: string[]
  excludedIngredients: string[]
  dietGoal: DietGoal
  cookingTime: number
  servings: number
}

export interface RecommendationRequest extends RecommendForm {}

export interface RecommendedRecipe {
  id: number
  name: string
  score: number
  reason: string
  calories: number
  protein: number
  imageUrl: string
  matchedIngredients: string[]
  missingIngredients: string[]
}

export interface RecommendationResponse {
  historyId: number
  aiSummary: string
  aiHealthTip: string
  aiShoppingTip: string
  aiGenerated: boolean
  recipes: RecommendedRecipe[]
}

export type ConversationStage = 'INTENT' | 'INGREDIENTS' | 'RESTRICTIONS' | 'CONTEXT' | 'CONFIRM'

export type ConversationStatus = 'ACTIVE' | 'READY_TO_CONFIRM' | 'COMPLETED' | 'CANCELLED'

export interface AvailableIngredientInput {
  name: string
  quantity: number | null
  unit: string | null
  quantityKnown: boolean
}

export interface RecommendationConversationContext {
  intentText: string | null
  dietGoal: DietGoal | null
  availableIngredients: AvailableIngredientInput[]
  excludedIngredients: string[]
  allergyIngredients: string[]
  cookingTime: number | null
  servings: number | null
  unknownTerms: string[]
  conflicts: string[]
  restrictionsConfirmed: boolean
}

export interface ConversationMessage {
  id: number
  role: 'USER' | 'ASSISTANT'
  content: string
  clientMessageId: string | null
  createdAt: string
}

export interface ConversationResponse {
  id: number
  stage: ConversationStage
  status: ConversationStatus
  invalidAnswerCount: number
  context: RecommendationConversationContext
  messages: ConversationMessage[]
  showConfirmation: boolean
  quickOptions: string[]
}

export interface ConversationMessageRequest {
  content: string
  clientMessageId: string
}

export interface ConversationContextPatchRequest {
  intentText?: string | null
  dietGoal?: DietGoal | null
  availableIngredients?: AvailableIngredientInput[] | null
  excludedIngredients?: string[] | null
  allergyIngredients?: string[] | null
  cookingTime?: number | null
  servings?: number | null
}

export interface RecommendationHistorySummary {
  id: number
  inputIngredients: string[]
  excludedIngredients: string[]
  dietGoal: DietGoal
  cookingTime: number
  servings: number
  resultRecipeIds: number[]
  aiSummary: string
  aiHealthTip: string
  aiShoppingTip: string
  aiGenerated: boolean
  createdAt: string
}

export interface RecommendationHistoryDetail extends RecommendationHistorySummary {
  recipes: RecommendedRecipe[]
  conversationContext?: RecommendationConversationContext | null
}

export interface ShoppingListCreateRequest {
  recipeIds: number[]
  availableIngredients: string[]
  title: string
}

export interface ShoppingListItem {
  id: number
  ingredientId: number
  ingredientName: string
  category: string
  quantity: number
  unit: string
  checked: boolean
}

export interface ShoppingList {
  id: number
  title: string
  sourceRecipeIds: number[]
  status: string
  items: ShoppingListItem[]
  createdAt: string
  updatedAt: string
}

export interface ShoppingListSummary {
  id: number
  title: string
  sourceRecipeIds: number[]
  status: string
  itemCount: number
  checkedCount: number
  createdAt: string
}

export interface FavoriteRecipe {
  favoriteId: number
  recipeId: number
  recipeName: string
  description: string
  imageUrl: string
  calories: number
  protein: number
  createdAt: string
}

export interface AdminDashboard {
  userCount: number
  recipeCount: number
  ingredientCount: number
  recommendationCount: number
}

export interface PopularRecipeStat {
  recipeId: number
  recipeName: string
  recommendationCount: number
}

export interface DietGoalStat {
  dietGoal: DietGoal
  count: number
}

export interface Ingredient {
  id: number
  name: string
  category: string
  unit: string
  caloriesPer100g: number
  proteinPer100g: number
  fatPer100g: number
  carbsPer100g: number
  aliases: string[]
  createdAt: string
  updatedAt: string
}

export type IngredientSaveRequest = Omit<Ingredient, 'id' | 'createdAt' | 'updatedAt'>

export interface RecipeSaveRequest {
  name: string
  description: string
  imageUrl: string
  cookingTime: number
  difficulty: Difficulty
  servings: number
  calories: number
  protein: number
  fat: number
  carbs: number
  tasteTags: string[]
  healthTags: string[]
  targetGoals: DietGoal[]
  steps: string[]
  ingredients: Array<{
    ingredientId: number
    quantity: number
    unit: string
    core: boolean
  }>
}
