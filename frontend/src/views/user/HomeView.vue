<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, ClipboardList, Flame, Heart, Leaf, Sparkles, Target, Timer } from '@lucide/vue'
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

const heroRecipe = computed(() => recipes.value[0])
const dailyCalorieTarget = computed(
  () => profileSummary.value?.dailyCalorieTarget ?? auth.profile.dailyCalorieTarget ?? 1600,
)

const bmiValue = computed(() => profileSummary.value?.bmi ?? auth.profile.bmi ?? 21.4)
const bmiStatus = computed(() => {
  if (bmiValue.value < 18.5) return '偏低'
  if (bmiValue.value > 24) return '偏高'
  return '正常'
})

const featuredTags = computed(() => {
  const tags = heroRecipe.value?.tags ?? []
  return tags.length ? tags.slice(0, 3) : ['高蛋白 低脂肪', '富含膳食纤维', '适合减脂人群']
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
    <section class="hero">
      <div class="hero-copy">
        <p class="hero-eyebrow">膳哉智能轻饮食助手</p>
        <h1>今天想吃得更合适一点</h1>
        <p>
          根据你的健康档案、已有食材和烹饪时间，先匹配营养目标，再给出清楚的推荐理由。
        </p>
        <div class="hero-actions">
          <n-button class="primary-action" type="primary" size="large" @click="router.push('/user/recommend')">
            <template #icon>
              <n-icon><Sparkles /></n-icon>
            </template>
            开始推荐
          </n-button>
          <n-button class="secondary-action" secondary size="large" @click="router.push('/user/profile')">
            完善档案
          </n-button>
        </div>
      </div>
      <div v-if="heroRecipe" class="featured-recipe">
        <img
          :src="cardImage(heroRecipe)"
          :alt="heroRecipe.name"
          :style="{ objectPosition: cardImagePosition(heroRecipe) }"
          @error="replaceImageWithFallback($event)"
        />
        <div class="featured-copy">
          <div class="featured-head">
            <span>今日推荐</span>
            <small>{{ goalLabel }}</small>
          </div>
          <h2>{{ heroRecipe.name }}</h2>
          <ul>
            <li v-for="tag in featuredTags" :key="tag"><Flame /> {{ tag }}</li>
          </ul>
          <div class="featured-meta">
            <span><Flame /> {{ heroRecipe.calories }} kcal</span>
            <span><Timer /> {{ heroRecipe.time ?? 25 }} 分钟</span>
            <button type="button" @click="openRecipeDetail(heroRecipe.id)">查看详情</button>
          </div>
        </div>
      </div>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="3" />

    <section class="summary-grid">
      <article class="metric-card bmi-card">
        <div class="metric-icon green"><Leaf /></div>
        <div>
          <span>BMI</span>
          <strong>{{ bmiValue.toFixed(1) }}</strong>
          <em>{{ bmiStatus }}</em>
          <small>健康范围 18.5 - 24.0</small>
        </div>
      </article>
      <article class="metric-card">
        <div class="metric-icon orange"><Flame /></div>
        <div>
          <span>今日目标热量</span>
          <strong>{{ dailyCalorieTarget }} <small>kcal</small></strong>
          <small>根据你的目标自动计算</small>
        </div>
      </article>
      <article class="metric-card">
        <div class="metric-icon lime"><Target /></div>
        <div>
          <span>今日目标</span>
          <strong>{{ goalLabel }}</strong>
          <small>保持均衡，维持健康体重</small>
        </div>
      </article>
      <article class="metric-card">
        <div class="metric-icon gold"><Sparkles /></div>
        <div>
          <span>推荐次数</span>
          <strong>{{ recommendationCount }} <small>次</small></strong>
          <small>最近 7 天生成推荐</small>
        </div>
      </article>
    </section>

    <section class="quick-panel">
      <h2 class="sz-section-title">快速入口</h2>
      <div class="quick-actions">
        <button type="button" @click="router.push('/user/recommend')">
          <span class="quick-icon leaf"><Leaf /></span>
          <strong>快速输入食材</strong>
          <small>告诉我你有什么食材</small>
          <ArrowRight />
        </button>
        <button type="button" @click="router.push('/user/shopping-lists')">
          <span class="quick-icon grain"><ClipboardList /></span>
          <strong>我的菜单</strong>
          <small>查看菜单与购物清单</small>
          <ArrowRight />
        </button>
        <button type="button" @click="router.push('/user/favorites')">
          <span class="quick-icon tomato"><Heart /></span>
          <strong>收藏菜谱</strong>
          <small>查看我收藏的菜谱</small>
          <ArrowRight />
        </button>
      </div>
    </section>

    <section class="recipes-panel">
      <div class="section-head">
        <h2 class="sz-section-title">最近推荐</h2>
        <router-link to="/user/recommend/result">查看全部</router-link>
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

.hero {
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

.featured-recipe {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(228px, 0.82fr);
  justify-self: end;
  width: 100%;
  max-width: 600px;
  height: 272px;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  background: rgba(255, 253, 248, 0.94);
  box-shadow: 0 18px 34px rgba(23, 37, 31, 0.14);
}

.featured-recipe img {
  width: 100%;
  height: 100%;
  min-height: 0;
  object-fit: cover;
}

.featured-copy {
  display: grid;
  align-content: start;
  gap: 9px;
  min-width: 0;
  padding: 18px 22px;
}

.featured-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.featured-head small {
  color: var(--sz-muted);
  font-size: 12px;
  font-weight: 800;
}

.featured-copy h2 {
  margin: 0;
  color: var(--sz-ink);
  font-size: 21px;
  line-height: 1.32;
}

.featured-copy > span {
  justify-self: start;
  padding: 5px 12px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-green-dark);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 800;
}

.featured-copy ul {
  display: grid;
  gap: 5px;
  padding: 0;
  margin: 0;
  list-style: none;
  color: var(--sz-text);
  font-size: 14px;
}

.featured-copy li,
.featured-meta,
.featured-meta span {
  display: inline-flex;
  align-items: center;
}

.featured-copy li {
  gap: 8px;
}

.featured-copy li svg,
.featured-meta svg {
  width: 16px;
  height: 16px;
  color: var(--sz-muted);
}

.featured-meta {
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 2px;
  color: var(--sz-muted);
  font-size: 13px;
}

.featured-meta span {
  gap: 5px;
}

.featured-meta button {
  flex-basis: 100%;
  width: fit-content;
  min-height: 34px;
  padding: 0 16px;
  border: 0;
  border-radius: 10px;
  color: #ffffff;
  background: var(--sz-green-dark);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  min-height: 116px;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px;
  border: 1px solid rgba(227, 218, 203, 0.88);
  border-radius: 16px;
  background: var(--sz-surface);
  box-shadow: 0 10px 24px rgba(23, 37, 31, 0.07);
}

.metric-card span {
  display: block;
  color: var(--sz-muted);
  font-weight: 700;
}

.metric-card strong {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  margin-top: 5px;
  color: var(--sz-deep-green);
  font-size: 28px;
  line-height: 1.1;
}

.metric-card strong small {
  color: var(--sz-text);
  font-size: 15px;
  font-weight: 500;
}

.metric-card small {
  display: block;
  margin-top: 7px;
  color: var(--sz-muted);
}

.bmi-card em {
  display: inline-block;
  margin-left: 8px;
  padding: 3px 9px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-green-dark);
  background: var(--sz-mint);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  vertical-align: text-bottom;
}

.metric-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 58px;
  height: 58px;
  border-radius: 50%;
}

.metric-icon svg {
  width: 28px;
  height: 28px;
}

.metric-icon.green {
  color: var(--sz-green-dark);
  background: var(--sz-mint);
}

.metric-icon.orange {
  color: #f47722;
  background: #fae4dc;
}

.metric-icon.lime {
  color: #73a72f;
  background: #e8f3d7;
}

.metric-icon.gold {
  color: #e79d23;
  background: #fff0ce;
}

.quick-panel {
  display: grid;
  gap: 12px;
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
  gap: 34px;
}

.quick-actions button {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr auto;
  grid-template-rows: auto auto;
  column-gap: 16px;
  align-items: center;
  min-height: 86px;
  padding: 18px 24px;
  border: 1px solid rgba(227, 218, 203, 0.88);
  border-radius: 14px;
  color: var(--sz-ink);
  background: var(--sz-surface);
  box-shadow: var(--sz-shadow-soft);
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.quick-actions button:hover {
  border-color: var(--sz-line-strong);
  box-shadow: var(--sz-shadow);
  transform: translateY(-2px);
}

.quick-actions strong {
  font-size: 18px;
}

.quick-actions small {
  grid-column: 2;
  color: var(--sz-muted);
  font-size: 14px;
}

.quick-actions button > svg {
  grid-column: 3;
  grid-row: 1 / 3;
  width: 22px;
  height: 22px;
  color: var(--sz-muted);
}

.quick-icon {
  display: grid;
  grid-row: 1 / 3;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: 50%;
}

.quick-icon svg {
  width: 28px;
  height: 28px;
}

.quick-icon.leaf {
  color: #ffffff;
  background: #65b991;
}

.quick-icon.grain {
  color: #ffffff;
  background: var(--sz-grain);
}

.quick-icon.tomato {
  color: #ffffff;
  background: #f26369;
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
  aspect-ratio: 2.48 / 1;
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
  padding: 16px;
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
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }

  .hero {
    grid-template-columns: 1fr;
    padding: 26px;
  }

  .featured-recipe {
    justify-self: stretch;
    max-width: none;
    height: 260px;
  }

  .featured-copy {
    align-content: center;
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

  .featured-recipe {
    grid-template-columns: 1fr;
    height: auto;
  }

  .featured-recipe img {
    height: 190px;
  }
}
</style>
