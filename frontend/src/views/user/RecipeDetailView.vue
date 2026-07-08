<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Heart, ListPlus, Timer, Utensils } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { backendAssetUrl } from '@/api/http'
import { favoriteRecipe, listFavorites, unfavoriteRecipe } from '@/api/favorite'
import { getRecipe } from '@/api/recipe'
import { createShoppingList } from '@/api/shopping'
import type { RecipeDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(true)
const actionLoading = ref(false)
const error = ref('')
const recipe = ref<RecipeDetail | null>(null)
const favoriteRecipeIds = ref<number[]>([])
const fallbackImage =
  'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=1200&q=80'

const recipeId = computed(() => Number(route.params.id))
const isFavorite = computed(() => favoriteRecipeIds.value.includes(recipeId.value))
const heroImageUrl = computed(() => backendAssetUrl(recipe.value?.imageUrl) || fallbackImage)
const mainIngredients = computed(() => recipe.value?.ingredients.filter((item) => item.core) ?? [])
const otherIngredients = computed(() => recipe.value?.ingredients.filter((item) => !item.core) ?? [])

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
  if (!recipe.value || actionLoading.value) return
  actionLoading.value = true
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
    actionLoading.value = false
  }
}

async function makeShoppingList() {
  if (!recipe.value || actionLoading.value) return
  actionLoading.value = true
  try {
    await createShoppingList({
      recipeIds: [recipe.value.id],
      availableIngredients: [],
      title: `${recipe.value.name}采购清单`,
    })
    message.success('购物清单已生成')
  } catch (err) {
    message.error(err instanceof Error ? err.message : '生成购物清单失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="recipe-detail-view">
    <button type="button" class="back-button" @click="router.back()">
      <ArrowLeft />
      返回
    </button>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="6" />

    <template v-if="recipe">
      <section class="detail-hero sz-panel">
        <div class="hero-copy">
          <p class="sz-chip"><Utensils /> {{ recipe.difficulty }}</p>
          <h1>{{ recipe.name }}</h1>
          <p>{{ recipe.description }}</p>
          <div class="hero-actions">
            <n-button type="primary" :loading="actionLoading" @click="makeShoppingList">
              <template #icon><n-icon><ListPlus /></n-icon></template>
              生成购物清单
            </n-button>
            <n-button secondary type="primary" :loading="actionLoading" @click="toggleFavorite">
              <template #icon><n-icon><Heart /></n-icon></template>
              {{ isFavorite ? '取消收藏' : '收藏菜谱' }}
            </n-button>
          </div>
        </div>
        <img :src="heroImageUrl" :alt="recipe.name" @error="($event.target as HTMLImageElement).src = fallbackImage" />
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
            <p v-for="item in mainIngredients" :key="item.ingredientId">
              <strong>{{ item.name }}</strong>
              <span>{{ item.quantity }}{{ item.unit }}</span>
            </p>
          </div>
          <div v-if="otherIngredients.length" class="ingredient-group">
            <h3>辅助食材</h3>
            <p v-for="item in otherIngredients" :key="item.ingredientId">
              <strong>{{ item.name }}</strong>
              <span>{{ item.quantity }}{{ item.unit }}</span>
            </p>
          </div>
        </article>

        <article class="sz-panel steps-panel">
          <h2 class="sz-section-title">做法步骤</h2>
          <ol>
            <li v-for="(step, index) in recipe.steps" :key="`${index}-${step}`">
              <span>{{ index + 1 }}</span>
              <p>{{ step }}</p>
            </li>
          </ol>
        </article>
      </section>
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

.ingredient-panel,
.steps-panel {
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

.ingredient-group p {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: var(--sz-mint);
}

.ingredient-group span {
  color: var(--sz-deep-green);
  font-weight: 800;
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

@media (max-width: 980px) {
  .detail-hero,
  .content-grid {
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
