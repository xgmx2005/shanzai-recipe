<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Heart, ListPlus, Tags, Timer, Utensils } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { favoriteRecipe, listFavorites, unfavoriteRecipe } from '@/api/favorite'
import { getRecipe } from '@/api/recipe'
import { createShoppingList } from '@/api/shopping'
import IngredientIcon from '@/components/IngredientIcon.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import type { DietGoal, Difficulty, RecipeDetail } from '@/types'
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
const favoriteRecipeIds = ref<number[]>([])

const recipeId = computed(() => Number(route.params.id))
const isFavorite = computed(() => favoriteRecipeIds.value.includes(recipeId.value))
const fromRecommendation = computed(() => route.query.from === 'recommendation')
const heroImageUrl = computed(() => resolveRecipeImage(recipe.value?.imageUrl))
const heroImagePosition = computed(() => resolveRecipeImagePosition(recipe.value?.imageUrl))
const mainIngredients = computed(() => recipe.value?.ingredients.filter((item) => item.core) ?? [])
const otherIngredients = computed(() => recipe.value?.ingredients.filter((item) => !item.core) ?? [])
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
  try {
    const [detail] = await Promise.all([getRecipe(recipeId.value), syncFavorites()])
    recipe.value = detail
  } catch (err) {
    error.value = err instanceof Error ? err.message : '菜谱详情加载失败'
  } finally {
    loading.value = false
  }
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
      <section class="detail-hero sz-panel">
        <div class="hero-copy">
          <p class="sz-chip"><Utensils /> {{ difficultyLabels[recipe.difficulty] }}</p>
          <h1>{{ recipe.name }}</h1>
          <p>{{ recipe.description }}</p>
          <div v-if="recipeTags.length" class="hero-tags">
            <span v-for="tag in recipeTags.slice(0, 5)" :key="tag">{{ tag }}</span>
          </div>
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
        <img
          :src="heroImageUrl"
          :alt="recipe.name"
          :style="{ objectPosition: heroImagePosition }"
          @error="replaceImageWithFallback($event)"
        />
      </section>

      <section class="nutrition-grid">
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
          <span>脂肪</span>
          <strong>{{ recipe.fat }}</strong>
          <small>g</small>
        </article>
        <article>
          <span>碳水</span>
          <strong>{{ recipe.carbs }}</strong>
          <small>g</small>
        </article>
        <article>
          <span><Timer /> 用时</span>
          <strong>{{ recipe.cookingTime }}</strong>
          <small>分钟</small>
        </article>
      </section>

      <section class="content-grid">
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
            <h2 class="sz-section-title">做法步骤</h2>
            <ol>
              <li v-for="(step, index) in recipe.steps" :key="`${index}-${step}`">
                <span>{{ index + 1 }}</span>
                <p>{{ step }}</p>
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
          <p>填写你已经有的食材，系统生成清单时会自动排除。</p>
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

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 44%);
  gap: 24px;
  align-items: stretch;
  padding: 22px;
  background: linear-gradient(135deg, #f8fbf5 0%, #eef7ee 46%, #fff7e7 100%);
}

.hero-copy {
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 16px;
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
  font-size: 38px;
  line-height: 1.18;
}

.hero-copy > p:not(.sz-chip) {
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

.detail-hero img {
  width: 100%;
  min-height: 300px;
  border-radius: 18px;
  object-fit: cover;
  box-shadow: 0 18px 36px rgba(31, 42, 36, 0.16);
}

.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.nutrition-grid article {
  display: grid;
  gap: 6px;
  padding: 16px;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-card);
  background: var(--sz-surface);
  box-shadow: var(--sz-shadow-soft);
}

.nutrition-grid span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--sz-muted);
  font-weight: 800;
}

.nutrition-grid svg {
  width: 15px;
  height: 15px;
}

.nutrition-grid strong {
  color: var(--sz-deep-green);
  font-size: 28px;
}

.nutrition-grid small,
.section-head span {
  color: var(--sz-muted);
}

.content-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
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
  .detail-hero,
  .content-grid,
  .meta-panel {
    grid-template-columns: 1fr;
  }

  .nutrition-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  h1 {
    font-size: 30px;
  }

  .detail-hero {
    padding: 18px;
  }

  .detail-hero img {
    min-height: 220px;
  }

  .nutrition-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
