<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ChefHat, Clock3, ListPlus, RefreshCw, Sparkles, UsersRound } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { favoriteRecipe } from '@/api/favorite'
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
const creatingRecipeId = ref<number | null>(null)
const favoriteRecipeId = ref<number | null>(null)
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
    detail.value = await getRecommendationHistory(historyId.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '推荐结果加载失败'
  } finally {
    loading.value = false
  }
}

async function makeShoppingList(recipeIds = recipes.value.map((recipe) => recipe.id)) {
  if (!detail.value || recipeIds.length === 0 || creatingList.value) return
  creatingList.value = true
  if (recipeIds.length === 1) creatingRecipeId.value = recipeIds[0]

  try {
    const list = await createShoppingList({
      recipeIds,
      availableIngredients: detail.value.inputIngredients,
      title: recipeIds.length === 1
        ? `推荐 #${detail.value.id} 单菜采购清单`
        : `推荐 #${detail.value.id} 采购清单`,
    })
    message.success('购物清单已生成')
    await router.push(shoppingListRoute(list.id, 'recommendation'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : '生成购物清单失败')
  } finally {
    creatingList.value = false
    creatingRecipeId.value = null
  }
}

async function addFavorite(recipeId: number) {
  if (favoriteRecipeId.value) return
  favoriteRecipeId.value = recipeId
  try {
    await favoriteRecipe(recipeId)
    message.success('已收藏菜谱')
  } catch (err) {
    message.error(err instanceof Error ? err.message : '收藏失败')
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
  <div class="result-view">
    <section class="result-heading">
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

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="5" />

    <template v-else-if="detail">
      <section class="result-summary sz-panel">
        <div class="summary-main">
          <p class="sz-chip is-warm"><Sparkles :size="15" /> {{ detail.aiGenerated ? 'AI 推荐讲解' : '规则推荐讲解' }}</p>
          <h2>{{ goalLabels[detail.dietGoal] }}</h2>
          <p>{{ detail.aiSummary }}</p>
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
            <span>总热量 / 蛋白质</span>
            <strong>{{ resultCalories }} kcal / {{ resultProtein }}g</strong>
          </article>
        </aside>
      </section>

      <section class="condition-strip">
        <article>
          <span>已有食材</span>
          <div>
            <small v-for="name in detail.inputIngredients" :key="`in-${name}`">{{ name }}</small>
            <strong v-if="detail.inputIngredients.length === 0">无</strong>
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

      <section class="toolbar sz-panel">
        <div>
          <h2>推荐菜谱</h2>
          <span>结果来自知识库菜谱，AI 只负责解释和排序，不凭空生成新菜。</span>
        </div>
        <button type="button" :disabled="recipes.length === 0 || creatingList" @click="makeShoppingList()">
          <ListPlus :size="17" />
          {{ creatingList && !creatingRecipeId ? '正在生成' : '生成全部购物清单' }}
        </button>
      </section>

      <section v-if="recipes.length" class="recipe-list">
        <RecipeRecommendationCard
          v-for="recipe in recipes"
          :key="recipe.id"
          :recipe="recipe"
          :available-ingredients="detail.inputIngredients"
          :favorite-pending="favoriteRecipeId === recipe.id"
          :shopping-pending="creatingRecipeId === recipe.id"
          @detail="openDetail"
          @favorite="addFavorite"
          @shopping="(id) => makeShoppingList([id])"
        />
      </section>

      <section v-else class="empty-safe sz-panel">
        <Sparkles :size="26" />
        <strong>暂无符合过敏和忌口约束的安全推荐</strong>
        <span>可以修改条件、减少排除食材，或补充更多可用食材后重新生成。</span>
        <div>
          <button type="button" class="ghost-button" @click="router.push('/user/history')">查看历史</button>
          <button type="button" class="primary-button" @click="restartRecommendation">
            <RefreshCw :size="16" />
            重新开始推荐
          </button>
        </div>
      </section>
    </template>

    <section v-else class="empty-safe sz-panel">
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
  .tip-grid,
  .condition-strip {
    grid-template-columns: 1fr;
  }

  .toolbar button {
    width: 100%;
  }
}
</style>
