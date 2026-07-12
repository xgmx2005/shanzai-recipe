<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Flame, Heart, Sparkles, Timer } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { favoriteRecipe, listFavorites, unfavoriteRecipe } from '@/api/favorite'
import { getProfileSummary } from '@/api/profile'
import { listRecipes } from '@/api/recipe'
import { listRecommendationHistory } from '@/api/recommendation'
import { useAuthStore } from '@/stores/auth'
import type { ProfileSummary, RecipeCardModel, RecipeSummary } from '@/types'
import { replaceImageWithFallback, resolveRecipeImage, resolveRecipeImagePosition } from '@/utils/assets'

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

const dailyCalorieTarget = computed(
  () => profileSummary.value?.dailyCalorieTarget ?? auth.profile.dailyCalorieTarget ?? 1600,
)

const bmiValue = computed(() => profileSummary.value?.bmi ?? auth.profile.bmi ?? 21.4)
const bmiStatus = computed(() => {
  if (bmiValue.value < 18.5) return '偏低'
  if (bmiValue.value > 24) return '偏高'
  return '正常'
})

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

function sampleRecipes(items: RecipeCardModel[], limit: number) {
  if (items.length <= limit) return [...items]
  return [...items]
    .map((item) => ({ item, rank: Math.random() }))
    .sort((left, right) => left.rank - right.rank)
    .slice(0, limit)
    .map(({ item }) => item)
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

function cardImage(recipe?: RecipeCardModel) {
  return resolveRecipeImage(recipe?.imageUrl)
}

function cardImagePosition(recipe?: RecipeCardModel) {
  return resolveRecipeImagePosition(recipe?.imageUrl)
}

function matchScore(index: number) {
  return Math.max(83, 92 - index * 4)
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
    recipes.value = sampleRecipes(recipeList.map(toCard), 4)
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
  <div class="home-view home-workbench">
    <section class="workbench-hero">
      <div class="hero-copy">
        <p class="hero-eyebrow">膳哉智能轻饮食助手</p>
        <h1>今天这顿，交给膳哉来配</h1>
        <p>
          结合你的健康档案、饮食目标、烹饪时间和忌口，膳哉会把想法整理成一份清楚可执行的推荐。
        </p>
        <div class="hero-actions">
          <n-button class="primary-action" type="primary" size="large" @click="router.push('/user/recommend')">
            <template #icon>
              <n-icon><Sparkles /></n-icon>
            </template>
            开始智能推荐
          </n-button>
          <n-button class="secondary-action" secondary size="large" @click="router.push('/user/profile')">
            完善健康档案
          </n-button>
        </div>
      </div>
      <div class="daily-status-panel" aria-label="今日饮食状态">
        <article>
          <span>BMI</span>
          <strong>{{ bmiValue.toFixed(1) }}</strong>
          <small>{{ bmiStatus }}</small>
        </article>
        <article>
          <span>目标热量</span>
          <strong>{{ dailyCalorieTarget }}</strong>
          <small>kcal / 日</small>
        </article>
        <article>
          <span>饮食目标</span>
          <strong>{{ goalLabel }}</strong>
          <small>来自健康档案</small>
        </article>
        <article>
          <span>累计推荐记录</span>
          <strong>{{ recommendationCount }}</strong>
          <small>次</small>
        </article>
      </div>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="3" />

    <section class="workbench-flow">
      <div class="section-head">
        <h2 class="sz-section-title">从想法到采购清单</h2>
        <p>膳哉把“想吃什么”拆成可以执行的三步。</p>
      </div>
      <div class="flow-steps">
        <button type="button" @click="router.push('/user/recommend')">
          <span class="flow-index">01</span>
          <strong>说出想吃什么</strong>
          <small>用对话告诉膳哉你的食材、口味、时间和人数。</small>
          <ArrowRight />
        </button>
        <button type="button" @click="router.push('/user/recommend')">
          <span class="flow-index">02</span>
          <strong>生成推荐菜谱</strong>
          <small>结合健康档案和知识库，生成匹配理由清楚的菜谱。</small>
          <ArrowRight />
        </button>
        <button type="button" @click="router.push('/user/shopping-lists')">
          <span class="flow-index">03</span>
          <strong>整理购物清单</strong>
          <small>从推荐、详情或收藏页，把缺少食材整理成清单。</small>
          <ArrowRight />
        </button>
      </div>
    </section>

    <section class="recipes-panel inspiration-recipes">
      <div class="section-head">
        <h2 class="sz-section-title">今天也可以这样吃</h2>
        <router-link to="/user/history">查看推荐历史</router-link>
      </div>
      <div class="recipe-grid">
        <article
          v-for="recipe in recipes"
          :key="recipe.id"
          class="home-recipe-card"
          @click="openRecipeDetail(recipe.id)"
        >
          <div class="home-recipe-image">
            <img
              :src="cardImage(recipe)"
              :alt="recipe.name"
              :style="{ objectPosition: cardImagePosition(recipe) }"
              @error="replaceImageWithFallback($event)"
            />
            <span>匹配度 {{ matchScore(recipes.indexOf(recipe)) }}%</span>
            <button
              type="button"
              :class="{ 'is-favorite': isRecipeFavorite(recipe.id) }"
              :disabled="isFavoritePending(recipe.id)"
              :aria-label="isRecipeFavorite(recipe.id) ? '取消收藏' : '收藏'"
              @click.stop="handleFavorite(recipe.id, !isRecipeFavorite(recipe.id))"
            >
              <Heart />
            </button>
          </div>
          <div class="home-recipe-body">
            <h3>{{ recipe.name }}</h3>
            <div class="recipe-meta">
              <span><Flame /> {{ recipe.calories }} kcal</span>
              <span><Timer /> {{ recipe.time ?? 25 }} 分钟</span>
            </div>
            <div class="tags">
              <span v-for="tag in (recipe.tags ?? []).slice(0, 2)" :key="tag">{{ tag }}</span>
            </div>
          </div>
        </article>
        <n-empty v-if="!loading && recipes.length === 0" description="暂无可展示菜谱" />
      </div>
    </section>

  </div>
</template>

<style scoped>
.home-view {
  display: grid;
  gap: 22px;
  min-width: 0;
}

.workbench-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.92fr);
  gap: clamp(22px, 3.2vw, 54px);
  align-items: center;
  min-height: 314px;
  padding: 28px clamp(24px, 5vw, 92px);
  border: 1px solid rgba(201, 221, 205, 0.92);
  border-radius: 24px;
  background:
    radial-gradient(circle at 7% 48%, rgba(47, 153, 99, 0.12) 0 88px, transparent 90px),
    linear-gradient(100deg, #dcefe4 0%, #edf5e6 48%, #f1f4e8 100%);
  box-shadow: 0 18px 34px rgba(31, 77, 58, 0.11);
}

.hero-copy {
  display: grid;
  justify-items: start;
  gap: 14px;
  min-width: 0;
}

h1 {
  margin: 0;
  max-width: 620px;
  color: var(--sz-evergreen);
  font-size: 44px;
  line-height: 1.16;
  letter-spacing: 0;
}

.hero-copy > p:not(.sz-chip) {
  max-width: 620px;
  margin: 0;
  color: var(--sz-text);
  font-size: 16px;
  line-height: 1.8;
}

.hero-eyebrow {
  color: var(--sz-green-dark);
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.06em;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
}

.primary-action {
  --n-color: var(--sz-green-dark) !important;
  --n-color-hover: var(--sz-deep-green) !important;
  --n-color-pressed: var(--sz-evergreen) !important;
  --n-border: 1px solid var(--sz-green-dark) !important;
  --n-border-hover: 1px solid var(--sz-deep-green) !important;
  --n-border-pressed: 1px solid var(--sz-evergreen) !important;
  --n-border-radius: 10px !important;
  min-width: 160px;
  box-shadow: 0 10px 20px rgba(35, 107, 75, 0.22);
}

.secondary-action {
  --n-text-color: var(--sz-deep-green) !important;
  --n-text-color-hover: var(--sz-evergreen) !important;
  --n-border: 1px solid rgba(35, 107, 75, 0.16) !important;
  --n-border-hover: 1px solid rgba(35, 107, 75, 0.28) !important;
  --n-border-radius: 10px !important;
  min-width: 132px;
  background: rgba(255, 255, 255, 0.66);
}

.daily-status-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: min(100%, 520px);
  justify-self: end;
}

.daily-status-panel article {
  display: grid;
  align-content: center;
  min-height: 116px;
  padding: 18px;
  border: 1px solid rgba(223, 210, 191, 0.78);
  border-radius: 18px;
  background: rgba(255, 253, 247, 0.86);
  box-shadow: 0 14px 28px rgba(23, 37, 31, 0.08);
}

.daily-status-panel span,
.daily-status-panel small {
  overflow-wrap: anywhere;
  color: var(--sz-muted);
  font-weight: 800;
}

.daily-status-panel strong {
  overflow-wrap: anywhere;
  margin: 6px 0;
  color: var(--sz-evergreen);
  font-size: 28px;
  line-height: 1.15;
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

.workbench-flow {
  display: grid;
  gap: 14px;
}

.flow-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.flow-steps button {
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto auto;
  gap: 8px 14px;
  min-height: 150px;
  padding: 18px;
  border: 1px solid rgba(227, 218, 203, 0.88);
  border-radius: 18px;
  color: var(--sz-ink);
  background: rgba(255, 253, 247, 0.94);
  box-shadow: var(--sz-shadow-soft);
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.flow-steps button:hover {
  border-color: var(--sz-line-strong);
  box-shadow: var(--sz-shadow);
  transform: translateY(-2px);
}

.flow-index {
  width: fit-content;
  padding: 4px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 12px;
  font-weight: 900;
}

.flow-steps strong {
  grid-column: 1 / 3;
  color: var(--sz-evergreen);
  font-size: 20px;
}

.flow-steps small {
  grid-column: 1 / 3;
  color: var(--sz-muted);
  font-size: 14px;
  line-height: 1.7;
}

.flow-steps svg {
  grid-column: 2;
  grid-row: 1;
  width: 20px;
  height: 20px;
  color: var(--sz-muted);
}

.recipes-panel {
  display: grid;
  gap: 14px;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 26px;
}

.home-recipe-card {
  overflow: hidden;
  border: 1px solid rgba(227, 218, 203, 0.88);
  border-radius: 14px;
  background: var(--sz-surface);
  box-shadow: var(--sz-shadow-soft);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.home-recipe-card:hover {
  border-color: var(--sz-line-strong);
  box-shadow: var(--sz-shadow);
  transform: translateY(-2px);
}

.home-recipe-image {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
}

.home-recipe-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-recipe-image span {
  position: absolute;
  bottom: 10px;
  left: 10px;
  padding: 4px 9px;
  border-radius: 8px;
  color: #ffffff;
  background: rgba(35, 107, 75, 0.92);
  font-size: 13px;
  font-weight: 800;
}

.home-recipe-image button {
  position: absolute;
  top: 10px;
  right: 10px;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  color: var(--sz-text);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 6px 14px rgba(23, 37, 31, 0.12);
  cursor: pointer;
}

.home-recipe-image button.is-favorite {
  color: #ffffff;
  background: #e75058;
}

.home-recipe-image button svg {
  width: 20px;
  height: 20px;
}

.home-recipe-image button.is-favorite svg {
  fill: currentColor;
}

.home-recipe-body {
  display: grid;
  gap: 9px;
  padding: 14px;
}

.home-recipe-body h3 {
  margin: 0;
  color: var(--sz-ink);
  font-size: 18px;
  line-height: 1.35;
}

.recipe-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 11px;
  color: var(--sz-muted);
  font-size: 14px;
}

.recipe-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.recipe-meta svg {
  width: 15px;
  height: 15px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tags span {
  padding: 4px 10px;
  border-radius: 7px;
  color: var(--sz-green-dark);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 980px) {
  .workbench-hero,
  .flow-steps {
    grid-template-columns: 1fr;
  }

  .workbench-hero {
    padding: 26px;
  }

  .daily-status-panel {
    justify-self: stretch;
    width: 100%;
  }

  .recipe-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .daily-status-panel,
  .recipe-grid {
    grid-template-columns: 1fr;
  }

  .workbench-hero {
    padding: 22px;
  }

  h1 {
    font-size: 30px;
  }
}
</style>
