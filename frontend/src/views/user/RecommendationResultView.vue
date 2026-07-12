<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  ChefHat,
  Clock3,
  Flame,
  ListPlus,
  RefreshCw,
  ShieldCheck,
  ShoppingBasket,
  Sparkles,
  Target,
  UsersRound,
  X,
} from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { favoriteRecipe, listFavorites, unfavoriteRecipe } from '@/api/favorite'
import { getRecommendationHistory } from '@/api/recommendation'
import { createShoppingList } from '@/api/shopping'
import RecipeRecommendationCard from '@/components/recommendation/RecipeRecommendationCard.vue'
import type { DietGoal, RecommendationHistoryDetail } from '@/types'
import { shoppingListRoute } from '@/utils/navigation'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const loading = ref(true)
const creatingList = ref(false)
const favoriteRecipeId = ref<number | null>(null)
const favoriteRecipeIds = ref<number[]>([])
const selectedRecipeIds = ref<number[]>([])
const menuBasketExpanded = ref(true)
const error = ref('')
const detail = ref<RecommendationHistoryDetail | null>(null)

const goalLabels: Record<DietGoal, string> = {
  FAT_LOSS: '减脂控热量',
  BALANCED: '日常均衡',
  MUSCLE_GAIN: '健身增肌',
}

const historyId = computed(() => {
  const value = Number(route.query.historyId)
  return Number.isInteger(value) && value > 0 ? value : null
})
const recipes = computed(() => detail.value?.recipes ?? [])
const resultCalories = computed(() => recipes.value.reduce((sum, recipe) => sum + recipe.calories, 0))
const resultProtein = computed(() => recipes.value.reduce((sum, recipe) => sum + recipe.protein, 0))
const selectedMenuRecipes = computed(() =>
  recipes.value.filter((recipe) => selectedRecipeIds.value.includes(recipe.id)),
)
const selectedMenuCalories = computed(() =>
  selectedMenuRecipes.value.reduce((sum, recipe) => sum + recipe.calories, 0),
)
const hasInputIngredients = computed(() => Boolean(detail.value?.inputIngredients.length))
const conditionModeText = computed(() =>
  hasInputIngredients.value ? '优先利用已有食材' : '未指定已有食材，按目标和约束推荐',
)
const aiExplanationTitle = computed(() =>
  detail.value?.aiGenerated ? 'AI 推荐讲解' : '知识库推荐讲解',
)

function isFavorite(recipeId: number) {
  return favoriteRecipeIds.value.includes(recipeId)
}

function isRecipeInMenu(recipeId: number) {
  return selectedRecipeIds.value.includes(recipeId)
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function load() {
  loading.value = true
  error.value = ''
  detail.value = null

  if (!historyId.value) {
    error.value = '推荐结果地址缺少有效 historyId'
    loading.value = false
    return
  }

  try {
    const [history, favorites] = await Promise.all([
      getRecommendationHistory(historyId.value),
      listFavorites(),
    ])
    detail.value = history
    favoriteRecipeIds.value = favorites.map((favorite) => favorite.recipeId)
    selectedRecipeIds.value = []
    menuBasketExpanded.value = true
  } catch (err) {
    error.value = err instanceof Error ? err.message : '推荐结果加载失败'
  } finally {
    loading.value = false
  }
}

async function makeShoppingList(recipeIds = recipes.value.map((recipe) => recipe.id)) {
  if (!detail.value || recipeIds.length === 0 || creatingList.value) return
  creatingList.value = true

  try {
    const list = await createShoppingList({
      recipeIds,
      availableIngredients: detail.value.inputIngredients,
      title: recipeIds.length === 1
        ? `推荐 #${detail.value.id} 所选采购清单`
        : `推荐 #${detail.value.id} 采购清单`,
    })
    message.success('购物清单已生成')
    await router.push(shoppingListRoute(list.id, 'recommendation'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : '生成购物清单失败')
  } finally {
    creatingList.value = false
  }
}

function toggleMenuRecipe(recipeId: number) {
  if (isRecipeInMenu(recipeId)) {
    selectedRecipeIds.value = selectedRecipeIds.value.filter((id) => id !== recipeId)
  } else {
    selectedRecipeIds.value = [...selectedRecipeIds.value, recipeId]
    menuBasketExpanded.value = true
  }

  if (selectedRecipeIds.value.length === 0) {
    menuBasketExpanded.value = false
  }
}

function clearMenuBasket() {
  selectedRecipeIds.value = []
  menuBasketExpanded.value = false
}

async function makeSelectedShoppingList() {
  if (selectedRecipeIds.value.length === 0) {
    message.warning('请先加入想做的菜')
    return
  }
  await makeShoppingList([...selectedRecipeIds.value])
}

async function toggleFavorite(recipeId: number) {
  if (favoriteRecipeId.value) return
  favoriteRecipeId.value = recipeId
  try {
    if (isFavorite(recipeId)) {
      await unfavoriteRecipe(recipeId)
      favoriteRecipeIds.value = favoriteRecipeIds.value.filter((id) => id !== recipeId)
      message.success('已取消收藏')
    } else {
      await favoriteRecipe(recipeId)
      favoriteRecipeIds.value = [...new Set([...favoriteRecipeIds.value, recipeId])]
      message.success('已收藏菜谱')
    }
  } catch (err) {
    message.error(err instanceof Error ? err.message : '收藏操作失败')
  } finally {
    favoriteRecipeId.value = null
  }
}

function openDetail(recipeId: number) {
  void router.push({
    path: `/user/recipes/${recipeId}`,
    query: detail.value?.id
      ? {
          from: 'recommendation',
          historyId: String(detail.value.id),
        }
      : undefined,
  })
}

function restartRecommendation() {
  void router.push('/user/recommend')
}

onMounted(load)
</script>

<template>
  <div class="result-view result-enter-shell">
    <section class="result-heading result-reveal" style="--reveal-index: 0">
      <div>
        <button type="button" class="back-button" @click="router.push('/user/recommend')">
          <ArrowLeft :size="16" />
          返回推荐
        </button>
        <p class="sz-chip"><Sparkles :size="15" /> 推荐结果</p>
        <h1>{{ detail ? `本次推荐 #${detail.id}` : '推荐结果' }}</h1>
      </div>
      <p>这里保留本次 AI 讲解、输入条件和知识库菜谱结果，后续可继续进入详情或生成购物清单。</p>
    </section>

    <n-alert v-if="error" class="result-reveal" style="--reveal-index: 1" type="error" :bordered="false">
      {{ error }}
    </n-alert>
    <n-skeleton v-if="loading" class="result-reveal" style="--reveal-index: 1" text :repeat="5" />

    <template v-else-if="detail">
      <section class="result-summary sz-panel result-reveal" style="--reveal-index: 1">
        <div class="summary-main">
          <p class="sz-chip is-warm"><Sparkles :size="15" /> {{ aiExplanationTitle }}</p>
          <h2>{{ goalLabels[detail.dietGoal] }}，为你筛出 {{ recipes.length }} 道可执行菜谱</h2>
          <p>{{ detail.aiSummary }}</p>
          <div class="explain-points">
            <article>
              <Target :size="17" />
              <strong>推荐逻辑</strong>
              <span>{{ conditionModeText }}</span>
            </article>
            <article>
              <ShieldCheck :size="17" />
              <strong>安全过滤</strong>
              <span>{{ detail.excludedIngredients.length ? `已避开 ${detail.excludedIngredients.join('、')}` : '无明确忌口限制' }}</span>
            </article>
          </div>
          <div class="tip-grid">
            <section>
              <strong>健康提示</strong>
              <span>{{ detail.aiHealthTip }}</span>
            </section>
            <section>
              <strong>购物提示</strong>
              <span>{{ detail.aiShoppingTip }}</span>
            </section>
          </div>
        </div>

        <aside class="summary-side">
          <article>
            <ChefHat :size="18" />
            <span>推荐菜谱</span>
            <strong>{{ recipes.length }} 道</strong>
          </article>
          <article>
            <Flame :size="18" />
            <span>总热量</span>
            <strong>{{ resultCalories }} kcal</strong>
          </article>
          <article>
            <Clock3 :size="18" />
            <span>烹饪时间</span>
            <strong>{{ detail.cookingTime }} 分钟</strong>
          </article>
          <article>
            <UsersRound :size="18" />
            <span>用餐人数</span>
            <strong>{{ detail.servings }} 人</strong>
          </article>
          <article>
            <span>总蛋白质</span>
            <strong>{{ resultProtein }}g</strong>
          </article>
        </aside>
      </section>

      <section class="condition-strip result-reveal" style="--reveal-index: 2">
        <article>
          <span>已有食材</span>
          <div>
            <small v-for="name in detail.inputIngredients" :key="`in-${name}`">{{ name }}</small>
            <strong v-if="detail.inputIngredients.length === 0">未指定，按目标推荐</strong>
          </div>
        </article>
        <article>
          <span>排除食材</span>
          <div>
            <small v-for="name in detail.excludedIngredients" :key="`out-${name}`" class="is-danger">{{ name }}</small>
            <strong v-if="detail.excludedIngredients.length === 0">无</strong>
          </div>
        </article>
        <article>
          <span>生成时间</span>
          <strong>{{ formatDate(detail.createdAt) }}</strong>
        </article>
      </section>

      <section class="toolbar sz-panel result-reveal" style="--reveal-index: 3">
        <div>
          <h2>推荐菜谱</h2>
          <span>结果来自知识库菜谱，AI 负责解释和排序；可进入详情查看步骤，或直接生成采购清单。</span>
        </div>
        <button type="button" :disabled="recipes.length === 0 || creatingList" @click="makeShoppingList()">
          <ListPlus :size="17" />
          {{ creatingList ? '正在生成' : '生成全部购物清单' }}
        </button>
      </section>

      <section v-if="recipes.length" class="recommendation-result-body result-reveal" style="--reveal-index: 4">
        <div class="recipe-list">
          <RecipeRecommendationCard
            v-for="recipe in recipes"
            :key="recipe.id"
            :recipe="recipe"
            :available-ingredients="detail.inputIngredients"
            :favorite-pending="favoriteRecipeId === recipe.id"
            :favorite-active="isFavorite(recipe.id)"
            :menu-selected="isRecipeInMenu(recipe.id)"
            @detail="openDetail"
            @favorite="toggleFavorite"
            @menu-toggle="toggleMenuRecipe"
          />
        </div>

        <aside
          v-if="selectedMenuRecipes.length"
          class="menu-basket-rail"
          :class="{ expanded: menuBasketExpanded }"
        >
          <button type="button" class="basket-toggle" @click="menuBasketExpanded = !menuBasketExpanded">
            <ShoppingBasket :size="18" />
            <span>菜单篮</span>
            <strong>{{ selectedMenuRecipes.length }} 道</strong>
          </button>

          <div v-if="menuBasketExpanded" class="basket-panel">
            <div class="basket-head">
              <div>
                <strong>已选菜单</strong>
                <span>{{ selectedMenuRecipes.length }} 道 · 约 {{ selectedMenuCalories }} kcal</span>
              </div>
              <button type="button" aria-label="清空菜单篮" @click="clearMenuBasket">
                <X :size="16" />
              </button>
            </div>
            <ul>
              <li v-for="recipe in selectedMenuRecipes" :key="recipe.id">
                <span>{{ recipe.name }}</span>
                <button type="button" @click="toggleMenuRecipe(recipe.id)">移除</button>
              </li>
            </ul>
            <button type="button" class="basket-primary" :disabled="creatingList" @click="makeSelectedShoppingList">
              <ListPlus :size="16" />
              {{ creatingList ? '正在生成' : '生成所选购物清单' }}
            </button>
          </div>
        </aside>

        <aside v-else class="menu-basket-rail is-empty">
          <ShoppingBasket :size="18" />
          <strong>菜单篮</strong>
          <span>从左侧菜谱中加入想做的几道菜，再统一生成购物清单。</span>
        </aside>
      </section>

      <section v-else class="empty-safe sz-panel result-reveal" style="--reveal-index: 4">
        <Sparkles :size="26" />
        <strong>暂无符合过敏和忌口约束的安全推荐</strong>
        <span>可以修改条件、减少排除食材，或放宽时间要求后重新生成。</span>
        <div>
          <button type="button" class="ghost-button" @click="router.push('/user/history')">查看历史</button>
          <button type="button" class="primary-button" @click="restartRecommendation">
            <RefreshCw :size="16" />
            重新开始推荐
          </button>
        </div>
      </section>
    </template>

    <section v-else class="empty-safe sz-panel result-reveal" style="--reveal-index: 1">
      <Sparkles :size="26" />
      <strong>没有找到推荐结果</strong>
      <span>请从智能推荐页重新生成一次，或在推荐历史中选择记录。</span>
      <div>
        <button type="button" class="ghost-button" @click="router.push('/user/history')">查看历史</button>
        <button type="button" class="primary-button" @click="restartRecommendation">
          <RefreshCw :size="16" />
          重新开始推荐
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.result-view {
  display: grid;
  gap: 20px;
}

.result-enter-shell {
  --result-reveal-distance: 14px;
}

.result-reveal {
  opacity: 0;
  transform: translateY(var(--result-reveal-distance));
  animation: result-reveal 460ms cubic-bezier(0.2, 0.9, 0.18, 1) forwards;
  animation-delay: calc(var(--reveal-index, 0) * 70ms);
  will-change: opacity, transform;
}

@keyframes result-reveal {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

h1,
h2,
p {
  margin: 0;
}

.result-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.result-heading > div {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.result-heading h1 {
  color: var(--sz-evergreen);
  font-size: 32px;
  line-height: 1.2;
  letter-spacing: 0;
}

.result-heading > p {
  max-width: 450px;
  color: var(--sz-muted);
  line-height: 1.8;
  text-align: right;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 11px;
  border: 1px solid rgba(35, 107, 75, 0.14);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: rgba(255, 253, 247, 0.86);
  font-weight: 900;
  cursor: pointer;
}

.result-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(220, 239, 228, 0.72), rgba(255, 250, 241, 0.96));
}

.summary-main {
  display: grid;
  justify-items: start;
  gap: 12px;
}

.summary-main h2 {
  color: var(--sz-evergreen);
  font-size: 28px;
}

.summary-main > p:not(.sz-chip) {
  color: var(--sz-text);
  line-height: 1.85;
}

.explain-points {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.explain-points article {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 3px 8px;
  align-items: center;
  padding: 12px;
  border: 1px solid rgba(35, 107, 75, 0.12);
  border-radius: 14px;
  background: rgba(255, 253, 247, 0.72);
}

.explain-points svg {
  grid-row: 1 / span 2;
  color: var(--sz-green-dark);
}

.explain-points strong {
  color: var(--sz-evergreen);
  font-size: 13px;
}

.explain-points span {
  color: var(--sz-text);
  line-height: 1.55;
  font-size: 13px;
}

.tip-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.tip-grid section {
  display: grid;
  gap: 7px;
  padding: 13px;
  border: 1px solid rgba(223, 210, 191, 0.78);
  border-radius: 14px;
  background: rgba(255, 253, 247, 0.82);
}

.tip-grid strong {
  color: var(--sz-evergreen);
}

.tip-grid span {
  color: var(--sz-text);
  line-height: 1.7;
}

.summary-side {
  display: grid;
  gap: 10px;
}

.summary-side article {
  display: grid;
  gap: 6px;
  padding: 13px;
  border: 1px solid rgba(223, 210, 191, 0.78);
  border-radius: 14px;
  background: rgba(255, 253, 247, 0.86);
}

.summary-side svg {
  color: var(--sz-green-dark);
}

.summary-side span,
.condition-strip article > span,
.toolbar span {
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 900;
}

.summary-side strong {
  color: var(--sz-evergreen);
  font-size: 18px;
}

.condition-strip {
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 0.8fr;
  gap: 12px;
}

.condition-strip article {
  display: grid;
  gap: 9px;
  padding: 14px;
  border: 1px solid var(--sz-line);
  border-radius: 16px;
  background: rgba(255, 253, 247, 0.86);
}

.condition-strip div {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.condition-strip small,
.condition-strip strong {
  min-height: 28px;
  padding: 5px 9px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 850;
}

.condition-strip small.is-danger {
  color: #9b3f2d;
  background: var(--sz-tomato-soft);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
}

.toolbar div {
  display: grid;
  gap: 6px;
}

.toolbar h2 {
  color: var(--sz-evergreen);
  font-size: 24px;
}

.toolbar button,
.primary-button,
.ghost-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 12px;
  font-weight: 900;
  cursor: pointer;
}

.toolbar button,
.primary-button {
  border: 0;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 10px 18px rgba(35, 107, 75, 0.18);
}

.ghost-button {
  border: 1px solid rgba(35, 107, 75, 0.18);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
}

.toolbar button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.recommendation-result-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
}

.recipe-list {
  display: grid;
  gap: 14px;
}

.empty-safe {
  display: grid;
  justify-items: center;
  gap: 12px;
  min-height: 300px;
  padding: 30px;
  color: var(--sz-muted);
  text-align: center;
}

.empty-safe svg {
  color: var(--sz-green-dark);
}

.empty-safe strong {
  color: var(--sz-evergreen);
  font-size: 20px;
}

.empty-safe span {
  max-width: 520px;
  line-height: 1.8;
}

.empty-safe div {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.menu-basket-rail {
  position: sticky;
  top: 92px;
  display: grid;
  gap: 10px;
}

.basket-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 46px;
  width: 100%;
  padding: 0 16px;
  border: 0;
  border-radius: var(--sz-radius-pill);
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 16px 34px rgba(35, 107, 75, 0.26);
  font-weight: 900;
  cursor: pointer;
}

.basket-toggle strong {
  padding: 4px 8px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: #ffffff;
  font-size: 12px;
}

.basket-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(35, 107, 75, 0.16);
  border-radius: 18px;
  background: rgba(255, 253, 247, 0.96);
  box-shadow: 0 20px 42px rgba(23, 37, 31, 0.18);
}

.basket-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.basket-head div {
  display: grid;
  gap: 4px;
}

.basket-head strong {
  color: var(--sz-evergreen);
}

.basket-head span {
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 800;
}

.basket-head button {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid rgba(35, 107, 75, 0.14);
  border-radius: 50%;
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  cursor: pointer;
}

.basket-panel ul {
  display: grid;
  gap: 8px;
  max-height: 190px;
  margin: 0;
  padding: 0;
  overflow: auto;
  list-style: none;
}

.basket-panel li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 12px;
  background: var(--sz-mint);
}

.basket-panel li span {
  min-width: 0;
  overflow: hidden;
  color: var(--sz-deep-green);
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.basket-panel li button {
  flex: 0 0 auto;
  border: 0;
  color: var(--sz-muted);
  background: transparent;
  font-weight: 850;
  cursor: pointer;
}

.basket-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: var(--sz-green-dark);
  font-weight: 900;
  cursor: pointer;
}

.basket-primary:disabled {
  cursor: wait;
  opacity: 0.66;
}

.menu-basket-rail.is-empty {
  min-height: 154px;
  align-content: center;
  justify-items: start;
  padding: 18px;
  border: 1px dashed rgba(35, 107, 75, 0.2);
  border-radius: 18px;
  color: var(--sz-muted);
  background: rgba(220, 239, 228, 0.36);
}

.menu-basket-rail.is-empty svg {
  color: var(--sz-green-dark);
}

.menu-basket-rail.is-empty strong {
  color: var(--sz-evergreen);
  font-size: 18px;
}

.menu-basket-rail.is-empty span {
  line-height: 1.7;
}

@media (max-width: 860px) {
  .result-heading,
  .toolbar {
    display: grid;
  }

  .result-heading > p {
    max-width: none;
    text-align: left;
  }

  .result-summary,
  .explain-points,
  .tip-grid,
  .condition-strip,
  .recommendation-result-body {
    grid-template-columns: 1fr;
  }

  .toolbar button {
    width: 100%;
  }

  .menu-basket-rail {
    position: static;
  }
}

@media (prefers-reduced-motion: reduce) {
  .result-reveal {
    opacity: 1;
    transform: none;
    animation: none;
  }
}
</style>
