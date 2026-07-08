<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Flame, Salad, Sparkles } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { favoriteRecipe, listFavorites, unfavoriteRecipe } from '@/api/favorite'
import { getProfileSummary } from '@/api/profile'
import { listRecipes } from '@/api/recipe'
import { listRecommendationHistory } from '@/api/recommendation'
import HealthSummaryCard from '@/components/HealthSummaryCard.vue'
import RecipeCard from '@/components/RecipeCard.vue'
import { useAuthStore } from '@/stores/auth'
import type { ProfileSummary, RecipeCardModel, RecipeSummary } from '@/types'

const router = useRouter()
const message = useMessage()
const auth = useAuthStore()
const loading = ref(true)
const error = ref('')
const profileSummary = ref<ProfileSummary | null>(null)
const recipes = ref<RecipeCardModel[]>([])
const recommendationCount = ref(0)
const favoriteRecipeIds = ref<number[]>([])
const favoritePendingRecipeIds = ref<number[]>([])

const goalLabel = computed(() => {
  const labels = {
    FAT_LOSS: '减脂控热量',
    BALANCED: '日常健康',
    MUSCLE_GAIN: '健身增肌',
  }
  return labels[profileSummary.value?.dietGoal ?? auth.profile.dietGoal]
})

const heroRecipe = computed(() => recipes.value[0])
const dailyCalorieTarget = computed(
  () => profileSummary.value?.dailyCalorieTarget ?? auth.profile.dailyCalorieTarget ?? 1600,
)

function toCard(recipe: RecipeSummary): RecipeCardModel {
  return {
    id: recipe.id,
    name: recipe.name,
    imageUrl: recipe.imageUrl,
    calories: recipe.calories,
    protein: recipe.protein,
    time: recipe.cookingTime,
    tags: [...recipe.healthTags, ...recipe.tasteTags],
    reason: recipe.description,
  }
}

function setFavoriteId(id: number, favorite: boolean) {
  if (favorite) {
    if (!favoriteRecipeIds.value.includes(id)) {
      favoriteRecipeIds.value = [...favoriteRecipeIds.value, id]
    }
  } else {
    favoriteRecipeIds.value = favoriteRecipeIds.value.filter((recipeId) => recipeId !== id)
  }
}

function setFavoritePending(id: number, pending: boolean) {
  if (pending) {
    if (!favoritePendingRecipeIds.value.includes(id)) {
      favoritePendingRecipeIds.value = [...favoritePendingRecipeIds.value, id]
    }
  } else {
    favoritePendingRecipeIds.value = favoritePendingRecipeIds.value.filter((recipeId) => recipeId !== id)
  }
}

function recipeNameOf(id: number) {
  return recipes.value.find((recipe) => recipe.id === id)?.name ?? '这道菜'
}

function isRecipeFavorite(id: number) {
  return favoriteRecipeIds.value.includes(id)
}

function isFavoritePending(id: number) {
  return favoritePendingRecipeIds.value.includes(id)
}

async function syncFavorites() {
  const favoriteList = await listFavorites()
  favoriteRecipeIds.value = favoriteList.map((favorite) => favorite.recipeId)
}

async function handleFavorite(id: number, nextFavorite: boolean) {
  if (isFavoritePending(id)) return
  setFavoritePending(id, true)
  try {
    if (nextFavorite) {
      const favorite = await favoriteRecipe(id)
      setFavoriteId(id, true)
      message.success(`已收藏「${favorite.recipeName}」`)
    } else {
      await unfavoriteRecipe(id)
      setFavoriteId(id, false)
      message.success(`已取消收藏「${recipeNameOf(id)}」`)
    }
    await syncFavorites()
  } catch (err) {
    message.error(err instanceof Error ? err.message : nextFavorite ? '收藏失败' : '取消收藏失败')
  } finally {
    setFavoritePending(id, false)
  }
}

async function openRecipeDetail(id: number) {
  await router.push(`/user/recipes/${id}`)
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  const warnings: string[] = []
  try {
    const profile = await auth.loadProfile()
    auth.profile = { ...profile }
  } catch (err) {
    warnings.push(err instanceof Error ? err.message : '健康档案加载失败')
  }

  try {
    profileSummary.value = await getProfileSummary()
  } catch {
    warnings.push('健康摘要加载失败')
  }

  try {
    const recipeList = await listRecipes()
    recipes.value = recipeList.slice(0, 4).map(toCard)
  } catch {
    warnings.push('菜谱推荐加载失败')
  }

  try {
    const history = await listRecommendationHistory()
    recommendationCount.value = history.length
  } catch {
    warnings.push('推荐历史加载失败')
  }

  try {
    const favoriteList = await listFavorites()
    favoriteRecipeIds.value = favoriteList.map((favorite) => favorite.recipeId)
  } catch {
    warnings.push('收藏状态加载失败')
  }

  if (warnings.length === 1) {
    message.warning(warnings[0])
  } else if (warnings.length > 1) {
    message.warning('部分首页数据暂时不可用')
  }
  if (warnings.length && recipes.value.length === 0) {
    error.value = warnings.join('；')
  }
  loading.value = false
})
</script>

<template>
  <div class="home-view">
    <section class="hero sz-panel">
      <div class="hero-copy">
        <p class="sz-chip"><Sparkles /> AI 健康膳食助手</p>
        <h1>今天想吃得更合适一点</h1>
        <p>
          根据你的健康档案、已有食材和烹饪时间，先匹配营养目标，再给出清楚的推荐理由。
        </p>
        <div class="hero-actions">
          <n-button type="primary" size="large" @click="router.push('/user/recommend')">
            开始推荐
            <template #icon>
              <n-icon><ArrowRight /></n-icon>
            </template>
          </n-button>
          <n-button class="hero-secondary" size="large" secondary type="primary" @click="router.push('/user/profile')">
            完善档案
          </n-button>
        </div>
      </div>
      <div v-if="heroRecipe" class="hero-plate">
        <RecipeCard
          :recipe="heroRecipe"
          :is-favorite="isRecipeFavorite(heroRecipe.id)"
          :favorite-pending="isFavoritePending(heroRecipe.id)"
          dense
          @favorite="handleFavorite"
          @detail="openRecipeDetail"
        />
        <div>
          <span>健康评分</span>
          <strong>{{ Math.round((profileSummary?.bmi ?? auth.profile.bmi ?? 21) * 38) }}</strong>
          <em>分</em>
        </div>
      </div>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="3" />

    <section class="summary-grid">
      <HealthSummaryCard :profile="auth.profile" compact />
      <article class="metric-card">
        <span><Flame /> 今日目标热量</span>
        <strong>{{ dailyCalorieTarget }} kcal</strong>
        <small>来自健康档案摘要</small>
      </article>
      <article class="metric-card">
        <span><Salad /> 今日目标</span>
        <strong>{{ goalLabel }}</strong>
        <small>默认读取健康档案</small>
      </article>
      <article class="metric-card">
        <span><Sparkles /> 推荐次数</span>
        <strong>{{ recommendationCount }} 次</strong>
        <small>历史推荐记录</small>
      </article>
    </section>

    <section class="quick-panel sz-panel">
      <div class="section-head">
        <div>
          <h2 class="sz-section-title">快速入口</h2>
          <p class="sz-muted">从档案、食材和收藏开始都可以。</p>
        </div>
      </div>
      <div class="quick-actions">
        <button type="button" @click="router.push('/user/recommend')">快速输入食材</button>
        <button type="button" @click="router.push('/user/shopping-lists')">我的菜单</button>
        <button type="button" @click="router.push('/user/favorites')">收藏菜谱</button>
      </div>
    </section>

    <section class="recipes-panel">
      <div class="section-head">
        <div>
          <h2 class="sz-section-title">最近推荐</h2>
          <p class="sz-muted">优先展示食材命中高、热量更稳定的菜谱。</p>
        </div>
        <router-link to="/user/recommend/result">查看全部</router-link>
      </div>
      <div class="recipe-grid">
        <RecipeCard
          v-for="recipe in recipes"
          :key="recipe.id"
          :recipe="recipe"
          :is-favorite="isRecipeFavorite(recipe.id)"
          :favorite-pending="isFavoritePending(recipe.id)"
          dense
          @favorite="handleFavorite"
          @detail="openRecipeDetail"
        />
        <n-empty v-if="!loading && recipes.length === 0" description="暂无可展示菜谱" />
      </div>
    </section>

  </div>
</template>

<style scoped>
.home-view {
  display: grid;
  gap: 18px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  align-items: center;
  padding: 30px;
  border-color: rgba(18, 61, 45, 0.12);
  background: var(--sz-evergreen);
  box-shadow: 0 18px 34px rgba(18, 61, 45, 0.16);
}

.hero-copy {
  display: grid;
  justify-items: start;
  gap: 16px;
}

h1 {
  margin: 0;
  max-width: 620px;
  color: #ffffff;
  font-size: 38px;
  line-height: 1.18;
  letter-spacing: 0;
}

.hero-copy > p:not(.sz-chip) {
  max-width: 620px;
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 16px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
}

.hero-secondary {
  --n-color: rgba(255, 255, 255, 0.08) !important;
  --n-color-hover: rgba(255, 255, 255, 0.14) !important;
  --n-color-pressed: rgba(255, 255, 255, 0.18) !important;
  --n-border: 1px solid rgba(255, 255, 255, 0.36) !important;
  --n-border-hover: 1px solid rgba(255, 255, 255, 0.72) !important;
  --n-border-pressed: 1px solid rgba(255, 255, 255, 0.72) !important;
  --n-text-color: #ffffff !important;
  --n-text-color-hover: #ffffff !important;
  --n-text-color-pressed: #ffffff !important;
}

.hero-plate {
  position: relative;
  min-height: 250px;
}

.hero-plate div {
  position: absolute;
  right: 16px;
  bottom: 16px;
  min-width: 136px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 250, 241, 0.94);
  box-shadow: 0 12px 24px rgba(31, 42, 36, 0.16);
  pointer-events: none;
}

.hero-plate span,
.hero-plate em {
  color: var(--sz-muted);
  font-style: normal;
}

.hero-plate strong {
  display: inline-block;
  margin: 4px 4px 0 0;
  color: var(--sz-deep-green);
  font-size: 30px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  min-height: 116px;
  display: grid;
  align-content: center;
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(227, 218, 203, 0.88);
  border-radius: var(--sz-radius-card);
  background: var(--sz-surface);
  box-shadow: var(--sz-shadow-soft);
}

.metric-card span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--sz-muted);
  font-weight: 700;
}

.metric-card svg {
  width: 16px;
  height: 16px;
}

.metric-card strong {
  font-size: 24px;
}

.metric-card small {
  color: var(--sz-muted);
}

.quick-panel {
  padding: 20px;
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
}

.section-head p {
  margin: 6px 0 0;
}

.section-head a {
  color: var(--sz-green-dark);
  font-weight: 800;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.quick-actions button {
  min-height: 50px;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-surface);
  font-weight: 800;
  cursor: pointer;
}

.quick-actions button:first-child {
  border-color: transparent;
  color: #ffffff;
  background: var(--sz-green-dark);
}

.recipes-panel {
  display: grid;
  gap: 14px;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 980px) {
  .hero,
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }

  .recipe-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero,
  .summary-grid,
  .quick-actions,
  .recipe-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    padding: 22px;
  }

  h1 {
    font-size: 30px;
  }
}
</style>
