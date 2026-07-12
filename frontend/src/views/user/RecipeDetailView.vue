<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CheckCircle2, Heart, ListPlus, SearchCheck, ShoppingBag, Tags, Timer, Utensils } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { favoriteRecipe, listFavorites, unfavoriteRecipe } from '@/api/favorite'
import { getRecipe } from '@/api/recipe'
import { getRecommendationHistory } from '@/api/recommendation'
import { createShoppingList } from '@/api/shopping'
import IngredientIcon from '@/components/IngredientIcon.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import type { DietGoal, Difficulty, RecipeDetail, RecommendationHistoryDetail, RecommendedRecipe } from '@/types'
import { replaceImageWithFallback, resolveRecipeImage, resolveRecipeImagePosition } from '@/utils/assets'
import { shoppingListRoute } from '@/utils/navigation'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(true)
const favoriteLoading = ref(false)
const shoppingLoading = ref(false)
const shoppingModalOpen = ref(false)
const availableIngredients = ref<string[]>([])
const error = ref('')
const recipe = ref<RecipeDetail | null>(null)
const recommendationDetail = ref<RecommendationHistoryDetail | null>(null)
const favoriteRecipeIds = ref<number[]>([])

const recipeId = computed(() => Number(route.params.id))
const historyId = computed(() => {
  const value = Number(route.query.historyId)
  return Number.isInteger(value) && value > 0 ? value : null
})
const isFavorite = computed(() => favoriteRecipeIds.value.includes(recipeId.value))
const fromRecommendation = computed(() => route.query.from === 'recommendation')
const heroImageUrl = computed(() => resolveRecipeImage(recipe.value?.imageUrl))
const heroImagePosition = computed(() => resolveRecipeImagePosition(recipe.value?.imageUrl))
const mainIngredients = computed(() => recipe.value?.ingredients.filter((item) => item.core) ?? [])
const otherIngredients = computed(() => recipe.value?.ingredients.filter((item) => !item.core) ?? [])
const recommendedRecipe = computed<RecommendedRecipe | null>(() =>
  recommendationDetail.value?.recipes.find((item) => item.id === recipeId.value) ?? null,
)
const hasRecommendationContext = computed(() => fromRecommendation.value && Boolean(recommendedRecipe.value))
const matchedIngredients = computed(() => recommendedRecipe.value?.matchedIngredients ?? [])
const missingIngredients = computed(() => recommendedRecipe.value?.missingIngredients ?? [])
const recommendationMode = computed(() =>
  recommendationDetail.value?.inputIngredients.length ? '结合已有食材推荐' : '按目标和约束推荐',
)
const difficultyLabels: Record<Difficulty, string> = {
  EASY: '简单',
  MEDIUM: '适中',
  HARD: '复杂',
}
const goalLabels: Record<DietGoal, string> = {
  FAT_LOSS: '减脂控热量',
  BALANCED: '日常健康',
  MUSCLE_GAIN: '健身增肌',
}
const recipeTags = computed(() => {
  if (!recipe.value) return []
  return [
    ...recipe.value.healthTags,
    ...recipe.value.tasteTags,
    ...recipe.value.targetGoals.map((goal) => goalLabels[goal]),
  ]
})

async function syncFavorites() {
  const favorites = await listFavorites()
  favoriteRecipeIds.value = favorites.map((favorite) => favorite.recipeId)
}

async function load() {
  if (!Number.isFinite(recipeId.value)) {
    error.value = '菜谱编号无效'
    loading.value = false
    return
  }

  loading.value = true
  error.value = ''
  recommendationDetail.value = null
  try {
    const [detail, history] = await Promise.all([
      getRecipe(recipeId.value),
      fromRecommendation.value && historyId.value ? getRecommendationHistory(historyId.value) : Promise.resolve(null),
      syncFavorites(),
    ])
    recipe.value = detail
    recommendationDetail.value = history
    if (history?.inputIngredients.length) {
      availableIngredients.value = history.inputIngredients
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '菜谱详情加载失败'
  } finally {
    loading.value = false
  }
}

function stepHint(index: number) {
  const hints = ['准备与清洗', '控制火候', '调味成型', '装盘检查']
  return hints[Math.min(index, hints.length - 1)]
}

async function toggleFavorite() {
  if (!recipe.value || favoriteLoading.value) return
  favoriteLoading.value = true
  try {
    if (isFavorite.value) {
      await unfavoriteRecipe(recipe.value.id)
      message.success('已取消收藏')
    } else {
      await favoriteRecipe(recipe.value.id)
      message.success('已收藏菜谱')
    }
    await syncFavorites()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '收藏操作失败')
  } finally {
    favoriteLoading.value = false
  }
}

function openShoppingModal() {
  shoppingModalOpen.value = true
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.push(fromRecommendation.value ? '/user/recommend' : '/user/home')
}

async function makeShoppingList() {
  if (!recipe.value || shoppingLoading.value) return
  shoppingLoading.value = true
  try {
    const list = await createShoppingList({
      recipeIds: [recipe.value.id],
      availableIngredients: availableIngredients.value,
      title: `${recipe.value.name}采购清单`,
    })
    shoppingModalOpen.value = false
    availableIngredients.value = []
    message.success('购物清单已生成')
    await router.push(shoppingListRoute(list.id, fromRecommendation.value ? 'recommendation-detail' : 'recipe-detail'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : '生成购物清单失败')
  } finally {
    shoppingLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="recipe-detail-view">
    <button type="button" class="back-button" @click="goBack">
      <ArrowLeft />
      {{ fromRecommendation ? '返回推荐结果' : '返回' }}
    </button>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="6" />

    <template v-if="recipe">
      <section class="detail-hero-shell sz-panel">
        <div class="hero-image-panel">
          <img
            :src="heroImageUrl"
            :alt="recipe.name"
            :style="{ objectPosition: heroImagePosition }"
            @error="replaceImageWithFallback($event)"
          />
        </div>
        <div class="recipe-overview-panel">
          <p class="sz-chip"><Utensils /> {{ difficultyLabels[recipe.difficulty] }}</p>
          <h1>{{ recipe.name }}</h1>
          <p>{{ recipe.description }}</p>
          <div class="quick-meta">
            <span><Timer /> {{ recipe.cookingTime }} 分钟</span>
            <span>{{ recipe.servings }} 人份</span>
            <span>{{ difficultyLabels[recipe.difficulty] }}</span>
          </div>
          <div v-if="recipeTags.length" class="hero-tags">
            <span v-for="tag in recipeTags.slice(0, 5)" :key="tag">{{ tag }}</span>
          </div>
          <section class="nutrition-facts">
            <article>
              <span>热量</span>
              <strong>{{ recipe.calories }}</strong>
              <small>kcal</small>
            </article>
            <article>
              <span>蛋白质</span>
              <strong>{{ recipe.protein }}</strong>
              <small>g</small>
            </article>
            <article>
              <span>碳水</span>
              <strong>{{ recipe.carbs }}</strong>
              <small>g</small>
            </article>
            <article>
              <span>脂肪</span>
              <strong>{{ recipe.fat }}</strong>
              <small>g</small>
            </article>
          </section>
          <div class="hero-actions">
            <n-button type="primary" :loading="shoppingLoading" @click="openShoppingModal">
              <template #icon><n-icon><ListPlus /></n-icon></template>
              生成购物清单
            </n-button>
            <n-button secondary type="primary" :loading="favoriteLoading" @click="toggleFavorite">
              <template #icon><n-icon><Heart /></n-icon></template>
              {{ isFavorite ? '取消收藏' : '收藏菜谱' }}
            </n-button>
          </div>
        </div>
      </section>

      <section v-if="hasRecommendationContext && recommendedRecipe" class="recommend-fit-panel sz-panel">
        <div class="section-head">
          <div>
            <p class="sz-chip is-warm"><CheckCircle2 :size="15" /> 适合你的原因</p>
            <h2>{{ recommendationMode }}</h2>
          </div>
          <strong>{{ recommendedRecipe.score }}% 适配</strong>
        </div>
        <p>{{ recommendedRecipe.reason }}</p>
        <div class="fit-grid">
          <article>
            <span><SearchCheck :size="16" /> 已利用食材</span>
            <div>
              <small v-for="name in matchedIngredients" :key="`matched-${name}`">{{ name }}</small>
              <strong v-if="matchedIngredients.length === 0">未指定已有食材，按目标筛选</strong>
            </div>
          </article>
          <article>
            <span><ShoppingBag :size="16" /> 建议采购</span>
            <div>
              <small v-for="name in missingIngredients" :key="`missing-${name}`" class="is-warm">{{ name }}</small>
              <strong v-if="missingIngredients.length === 0">暂无缺少食材明细</strong>
            </div>
          </article>
        </div>
      </section>

      <section class="recipe-body-grid">
        <article class="sz-panel ingredient-panel">
          <div class="section-head">
            <h2 class="sz-section-title">食材清单</h2>
            <span>{{ recipe.servings }} 人份</span>
          </div>
          <div v-if="mainIngredients.length" class="ingredient-group">
            <h3>核心食材</h3>
            <article v-for="item in mainIngredients" :key="item.ingredientId">
              <div class="ingredient-copy">
                <IngredientIcon :name="item.name" :size="42" />
                <strong>{{ item.name }}</strong>
              </div>
              <span>{{ item.quantity }}{{ item.unit }}</span>
            </article>
          </div>
          <div v-if="otherIngredients.length" class="ingredient-group">
            <h3>辅助食材</h3>
            <article v-for="item in otherIngredients" :key="item.ingredientId">
              <div class="ingredient-copy">
                <IngredientIcon :name="item.name" :size="38" />
                <strong>{{ item.name }}</strong>
              </div>
              <span>{{ item.quantity }}{{ item.unit }}</span>
            </article>
          </div>
          <button type="button" class="panel-action" @click="openShoppingModal">
            <ListPlus :size="17" />
            按这道菜生成采购清单
          </button>
        </article>

        <div class="detail-column">
          <article class="sz-panel steps-panel">
            <div class="section-head">
              <h2 class="sz-section-title">做法步骤</h2>
              <span>{{ recipe.steps.length }} 步完成</span>
            </div>
            <ol>
              <li v-for="(step, index) in recipe.steps" :key="`${index}-${step}`">
                <span>{{ index + 1 }}</span>
                <div>
                  <strong>{{ stepHint(index) }}</strong>
                  <p>{{ step }}</p>
                </div>
              </li>
            </ol>
          </article>

          <section class="meta-panel sz-panel">
            <div>
              <p class="sz-chip"><Tags :size="15" /> 菜谱标签</p>
              <h2>适合场景</h2>
            </div>
            <div class="tag-list">
              <span v-for="tag in recipeTags" :key="tag">{{ tag }}</span>
            </div>
          </section>
        </div>
      </section>

      <n-modal
        v-model:show="shoppingModalOpen"
        preset="card"
        title="生成购物清单"
        class="shopping-modal"
        :style="{ width: '420px', maxWidth: 'calc(100vw - 32px)' }"
      >
        <div class="shopping-form">
          <p>填写你已经有的食材，系统生成清单时会自动排除。若从推荐结果进入，会默认带入本次输入食材。</p>
          <IngredientTagInput v-model="availableIngredients" label="已有食材" placeholder="例如 鸡蛋" />
          <n-button block type="primary" :loading="shoppingLoading" @click="makeShoppingList">
            确认生成
          </n-button>
        </div>
      </n-modal>
    </template>
  </div>
</template>

<style scoped>
.recipe-detail-view {
  display: grid;
  gap: 18px;
}

.back-button {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-surface);
  font-weight: 800;
  cursor: pointer;
}

.back-button svg {
  width: 17px;
  height: 17px;
}

.detail-hero-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.16fr) minmax(360px, 0.84fr);
  gap: clamp(24px, 3.2vw, 42px);
  align-items: stretch;
  padding: clamp(18px, 2.6vw, 30px);
  background:
    linear-gradient(90deg, rgba(255, 253, 247, 0.98), rgba(255, 253, 247, 0.72)),
    radial-gradient(circle at 86% 12%, rgba(220, 239, 228, 0.86), transparent 34%),
    linear-gradient(135deg, #fffaf1 0%, #eef7ee 100%);
}

.hero-image-panel {
  min-height: 460px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.86);
  border-radius: 18px;
  background: var(--sz-mint);
  box-shadow: 0 22px 44px rgba(31, 42, 36, 0.15);
}

.hero-image-panel img {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 460px;
  object-fit: cover;
}

.recipe-overview-panel {
  display: grid;
  align-content: start;
  justify-items: start;
  gap: 14px;
  min-width: 0;
  padding: 8px 0;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  max-width: 680px;
  color: var(--sz-deep-green);
  font-size: clamp(34px, 4vw, 50px);
  line-height: 1.1;
}

.recipe-overview-panel > p:not(.sz-chip) {
  max-width: 660px;
  color: var(--sz-muted);
  font-size: 16px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid rgba(35, 107, 75, 0.12);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: rgba(255, 253, 247, 0.76);
  font-size: 13px;
  font-weight: 850;
}

.quick-meta svg {
  width: 15px;
  height: 15px;
}

.hero-tags,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-tags span,
.tag-list span {
  padding: 5px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 800;
}

.nutrition-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
  width: 100%;
  margin-top: 6px;
  overflow: hidden;
  border: 1px solid rgba(35, 107, 75, 0.12);
  border-radius: 16px;
  background: rgba(255, 253, 247, 0.74);
}

.nutrition-facts article {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 10px;
  min-height: 82px;
  padding: 14px;
  border-right: 1px solid rgba(35, 107, 75, 0.1);
  border-bottom: 1px solid rgba(35, 107, 75, 0.1);
}

.nutrition-facts article:nth-child(2n) {
  border-right: 0;
}

.nutrition-facts article:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.nutrition-facts span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--sz-muted);
  font-weight: 800;
}

.nutrition-facts strong {
  grid-column: 1;
  color: var(--sz-deep-green);
  font-size: 26px;
  line-height: 1;
}

.nutrition-facts small,
.section-head span {
  color: var(--sz-muted);
}

.recommend-fit-panel {
  display: grid;
  gap: 14px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(220, 239, 228, 0.76), rgba(255, 250, 241, 0.96));
}

.recommend-fit-panel .section-head > div {
  display: grid;
  justify-items: start;
  gap: 8px;
}

.recommend-fit-panel .section-head strong {
  min-height: 34px;
  padding: 7px 12px;
  border-radius: var(--sz-radius-pill);
  color: #ffffff;
  background: var(--sz-green-dark);
  font-weight: 900;
}

.recommend-fit-panel h2 {
  color: var(--sz-evergreen);
  font-size: 24px;
}

.recommend-fit-panel > p {
  color: var(--sz-text);
  line-height: 1.8;
}

.fit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.fit-grid article {
  display: grid;
  gap: 9px;
  padding: 13px;
  border: 1px solid rgba(223, 210, 191, 0.78);
  border-radius: 14px;
  background: rgba(255, 253, 247, 0.78);
}

.fit-grid article > span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 900;
}

.fit-grid article > div {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.fit-grid small,
.fit-grid strong {
  min-height: 28px;
  padding: 5px 9px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 12px;
  font-weight: 850;
}

.fit-grid small.is-warm {
  color: #8d5d17;
  background: var(--sz-grain-soft);
}

.fit-grid strong {
  color: var(--sz-muted);
  background: rgba(255, 250, 241, 0.9);
}

.recipe-body-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.7fr) minmax(0, 1.3fr);
  gap: 20px;
  align-items: start;
}

.detail-column {
  display: grid;
  gap: 18px;
  align-content: start;
}

.ingredient-panel,
.steps-panel,
.meta-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ingredient-group {
  display: grid;
  gap: 10px;
}

.ingredient-group h3 {
  color: var(--sz-muted);
  font-size: 14px;
}

.ingredient-group article {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: var(--sz-mint);
}

.ingredient-copy {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ingredient-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ingredient-group span {
  color: var(--sz-deep-green);
  font-weight: 800;
}

.panel-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  width: 100%;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 10px 18px rgba(35, 107, 75, 0.18);
  font-weight: 900;
  cursor: pointer;
}

.steps-panel ol {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.steps-panel li {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.steps-panel li span {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #ffffff;
  background: var(--sz-green-dark);
  font-weight: 900;
}

.steps-panel li div {
  display: grid;
  gap: 5px;
}

.steps-panel li strong {
  color: var(--sz-evergreen);
  font-size: 14px;
}

.steps-panel li p {
  padding-top: 4px;
  color: var(--sz-text);
  line-height: 1.75;
}

.meta-panel {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
}

.meta-panel > div:first-child {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.meta-panel h2 {
  color: var(--sz-evergreen);
  font-size: 22px;
}

.shopping-modal {
  max-width: 520px;
}

.shopping-form {
  display: grid;
  gap: 16px;
}

.shopping-form p {
  color: var(--sz-muted);
  line-height: 1.7;
}

@media (max-width: 980px) {
  .detail-hero-shell,
  .fit-grid,
  .recipe-body-grid,
  .meta-panel {
    grid-template-columns: 1fr;
  }

  .hero-image-panel,
  .hero-image-panel img {
    min-height: 360px;
  }
}

@media (max-width: 640px) {
  h1 {
    font-size: 30px;
  }

  .detail-hero-shell {
    padding: 18px;
  }

  .hero-image-panel,
  .hero-image-panel img {
    min-height: 260px;
  }

  .nutrition-facts {
    grid-template-columns: 1fr;
  }

  .nutrition-facts article,
  .nutrition-facts article:nth-child(2n) {
    border-right: 0;
    border-bottom: 1px solid rgba(35, 107, 75, 0.1);
  }

  .nutrition-facts article:last-child {
    border-bottom: 0;
  }
}
</style>
