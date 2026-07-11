<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, CalendarClock, ChefHat, Clock, Flame, ListChecks, Salad, Sparkles, UsersRound } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { createShoppingList } from '@/api/shopping'
import IngredientIcon from '@/components/IngredientIcon.vue'
import { getRecommendationHistory, listRecommendationHistory } from '@/api/recommendation'
import type { RecommendationHistoryDetail, RecommendationHistorySummary } from '@/types'
import { replaceImageWithFallback, resolveRecipeImage } from '@/utils/assets'
import { shoppingListRoute } from '@/utils/navigation'

const message = useMessage()
const route = useRoute()
const router = useRouter()
const loading = ref(true)
const detailLoading = ref(false)
const creating = ref(false)
const error = ref('')
const histories = ref<RecommendationHistorySummary[]>([])
const detail = ref<RecommendationHistoryDetail | null>(null)

const latest = computed(() => histories.value[0])
const totalRecipeCount = computed(() => histories.value.reduce((sum, item) => sum + item.resultRecipeIds.length, 0))
const latestTimeLabel = computed(() => latest.value ? formatDate(latest.value.createdAt) : '暂无记录')
const selectedRecipes = computed(() => detail.value?.recipes ?? [])

const goalLabels = {
  FAT_LOSS: '减脂控热量',
  BALANCED: '日常均衡',
  MUSCLE_GAIN: '健身增肌',
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function fullDate(value: string) {
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
  try {
    histories.value = await listRecommendationHistory()
    const requestedHistoryId = Number(route.query.historyId)
    if (Number.isFinite(requestedHistoryId) && requestedHistoryId > 0) {
      await openDetail(requestedHistoryId)
    } else if (latest.value) {
      await openDetail(latest.value.id)
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '推荐历史加载失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  detailLoading.value = true
  error.value = ''
  try {
    detail.value = await getRecommendationHistory(id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '推荐详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

async function makeShoppingList() {
  if (!detail.value?.resultRecipeIds.length) return
  creating.value = true
  try {
    const list = await createShoppingList({
      recipeIds: detail.value.resultRecipeIds,
      availableIngredients: detail.value.inputIngredients,
      title: `推荐 #${detail.value.id} 采购清单`,
    })
    message.success('购物清单已生成')
    await router.push(shoppingListRoute(list.id, 'recommendation-history'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : '生成购物清单失败')
  } finally {
    creating.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="history-view">
    <section class="page-heading">
      <div>
        <p class="sz-chip"><Clock :size="15" /> 推荐历史</p>
        <h1>回看每一次推荐，快速复用成下一餐</h1>
      </div>
      <p>历史记录保留输入食材、目标和命中菜谱，可直接生成购物清单。</p>
    </section>

    <section class="stats-strip">
      <article>
        <span class="stat-icon"><CalendarClock :size="19" /></span>
        <small>历史记录</small>
        <strong>{{ histories.length }}</strong>
        <em>次推荐</em>
      </article>
      <article>
        <span class="stat-icon is-warm"><ChefHat :size="19" /></span>
        <small>累计菜谱</small>
        <strong>{{ totalRecipeCount }}</strong>
        <em>道命中</em>
      </article>
      <article>
        <span class="stat-icon is-red"><Sparkles :size="19" /></span>
        <small>最近一次</small>
        <strong>{{ latestTimeLabel }}</strong>
        <em>已保存</em>
      </article>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="4" />

    <section class="history-grid">
      <aside class="history-list sz-panel">
        <div class="panel-heading">
          <div>
            <p class="sz-chip">记录列表</p>
            <h2>最近生成</h2>
          </div>
          <span>{{ histories.length }} 条</span>
        </div>
        <button
          v-for="item in histories"
          :key="item.id"
          type="button"
          :class="{ active: detail?.id === item.id }"
          @click="openDetail(item.id)"
        >
          <span>#{{ item.id }} {{ goalLabels[item.dietGoal] }}</span>
          <strong>{{ item.inputIngredients.join('、') || '未填写食材' }}</strong>
          <small>{{ formatDate(item.createdAt) }} · {{ item.cookingTime }} 分钟 · {{ item.servings }} 人</small>
          <ArrowRight :size="17" />
        </button>
        <n-empty v-if="!loading && histories.length === 0" description="暂无推荐历史" />
      </aside>

      <section class="detail-panel sz-panel">
        <n-spin :show="detailLoading">
          <template v-if="detail">
            <div class="detail-content">
              <div class="detail-head">
                <div>
                  <p class="sz-chip is-warm"><Sparkles :size="15" /> 推荐 #{{ detail.id }}</p>
                  <h2>{{ goalLabels[detail.dietGoal] }}</h2>
                  <span>{{ fullDate(detail.createdAt) }}</span>
                </div>
                <button class="shopping-button" type="button" :disabled="creating" @click="makeShoppingList">
                  <ListChecks :size="18" />
                  {{ creating ? '正在生成' : '生成购物清单' }}
                </button>
              </div>

              <div class="detail-metrics">
                <article>
                  <Clock :size="19" />
                  <span>烹饪时间</span>
                  <strong>{{ detail.cookingTime }} 分钟</strong>
                </article>
                <article>
                  <UsersRound :size="19" />
                  <span>用餐人数</span>
                  <strong>{{ detail.servings }} 人</strong>
                </article>
                <article>
                  <Salad :size="19" />
                  <span>推荐菜谱</span>
                  <strong>{{ selectedRecipes.length }} 道</strong>
                </article>
              </div>

              <article class="ai-summary">
                <p class="sz-chip">
                  <Sparkles :size="15" />
                  {{ detail.aiGenerated ? 'AI 推荐分析' : '规则推荐分析' }}
                </p>
                <strong>{{ detail.aiSummary }}</strong>
                <div class="ai-summary-grid">
                  <section>
                    <span>健康提示</span>
                    <p>{{ detail.aiHealthTip }}</p>
                  </section>
                  <section>
                    <span>购物清单提示</span>
                    <p>{{ detail.aiShoppingTip }}</p>
                  </section>
                </div>
              </article>

              <div class="input-row">
                <article>
                  <span>已有食材</span>
                  <div class="ingredient-tags">
                    <span v-for="name in detail.inputIngredients" :key="`in-${name}`">
                      <IngredientIcon :name="name" :size="26" />
                      {{ name }}
                    </span>
                    <strong v-if="detail.inputIngredients.length === 0">无</strong>
                  </div>
                </article>
                <article>
                  <span>排除食材</span>
                  <div class="ingredient-tags">
                    <span v-for="name in detail.excludedIngredients" :key="`out-${name}`">
                      <IngredientIcon :name="name" :size="26" />
                      {{ name }}
                    </span>
                    <strong v-if="detail.excludedIngredients.length === 0">无</strong>
                  </div>
                </article>
              </div>

              <div class="recipe-list">
                <article v-for="recipe in detail.recipes" :key="recipe.id">
                  <img
                    :src="resolveRecipeImage(recipe.imageUrl)"
                    :alt="recipe.name"
                    @error="replaceImageWithFallback($event)"
                  />
                  <div class="recipe-copy">
                    <strong>{{ recipe.name }}</strong>
                    <span><Flame :size="15" /> {{ recipe.calories }} kcal · {{ recipe.protein }}g 蛋白质</span>
                  </div>
                  <button type="button" @click="router.push(`/user/recipes/${recipe.id}`)">
                    查看详情
                  </button>
                </article>
              </div>
            </div>
          </template>
          <n-empty v-else-if="!loading" description="选择一条推荐历史查看详情" />
        </n-spin>
      </section>
    </section>
  </div>
</template>

<style scoped>
.history-view,
.history-list,
.detail-panel,
.recipe-list {
  display: grid;
}

.history-view {
  gap: 22px;
}

h1,
h2,
p {
  margin: 0;
}

.page-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.page-heading > div {
  display: grid;
  justify-items: start;
  gap: 10px;
}

h1 {
  color: var(--sz-evergreen);
  font-size: 32px;
  line-height: 1.2;
  letter-spacing: 0;
}

.page-heading > p {
  max-width: 410px;
  color: var(--sz-muted);
  line-height: 1.8;
  text-align: right;
}

.stats-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.stats-strip article {
  display: grid;
  grid-template-columns: 48px 1fr auto;
  column-gap: 14px;
  align-items: center;
  min-height: 92px;
  padding: 16px;
  border: 1px solid var(--sz-line);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 250, 241, 0.96), rgba(250, 244, 234, 0.9));
  box-shadow: var(--sz-shadow-soft);
}

.stat-icon {
  grid-row: span 2;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: var(--sz-green-dark);
  background: var(--sz-mint);
}

.stat-icon.is-warm {
  color: #b16b18;
  background: var(--sz-grain-soft);
}

.stat-icon.is-red {
  color: var(--sz-tomato);
  background: var(--sz-tomato-soft);
}

.stats-strip small {
  color: var(--sz-muted);
  font-weight: 800;
}

.stats-strip strong {
  color: var(--sz-evergreen);
  font-size: 24px;
}

.stats-strip em {
  grid-column: 3;
  grid-row: 1 / span 2;
  padding: 5px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.history-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.history-list,
.detail-panel {
  padding: 22px;
}

.history-list {
  gap: 12px;
}

.panel-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 4px;
}

.panel-heading > div {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.panel-heading h2 {
  color: var(--sz-evergreen);
  font-size: 24px;
}

.panel-heading > span {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 6px 12px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 900;
}

.history-list button {
  position: relative;
  display: grid;
  gap: 7px;
  padding: 14px 42px 14px 14px;
  border: 1px solid var(--sz-line);
  border-radius: 16px;
  color: var(--sz-text);
  background: rgba(255, 253, 247, 0.86);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease;
}

.history-list button.active {
  border-color: rgba(72, 168, 106, 0.5);
  background: linear-gradient(135deg, var(--sz-mint), #fffdf7);
}

.history-list button:hover {
  border-color: var(--sz-line-strong);
  transform: translateY(-1px);
}

.history-list button > svg {
  position: absolute;
  right: 14px;
  top: 50%;
  color: var(--sz-muted);
  transform: translateY(-50%);
}

.history-list span,
.history-list small,
.recipe-list span {
  color: var(--sz-muted);
}

.detail-panel {
  gap: 18px;
  background: linear-gradient(180deg, rgba(255, 250, 241, 0.98), rgba(251, 247, 239, 0.94));
}

.detail-content {
  display: grid;
  gap: 18px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.detail-head div {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.detail-head h2 {
  color: var(--sz-evergreen);
  font-size: 26px;
}

.detail-head span {
  color: var(--sz-muted);
  font-weight: 800;
}

.shopping-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 44px;
  padding: 0 18px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 12px 22px rgba(35, 107, 75, 0.2);
  font-weight: 900;
  cursor: pointer;
}

.shopping-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.detail-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-metrics article {
  display: grid;
  gap: 8px;
  min-height: 92px;
  padding: 14px;
  border: 1px solid rgba(223, 210, 191, 0.86);
  border-radius: 16px;
  background: rgba(255, 253, 247, 0.86);
}

.detail-metrics svg {
  color: var(--sz-green-dark);
}

.detail-metrics span {
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 800;
}

.detail-metrics strong {
  color: var(--sz-evergreen);
  font-size: 20px;
}

.ai-summary {
  display: grid;
  justify-items: start;
  gap: 12px;
  padding: 18px;
  border: 1px solid rgba(72, 168, 106, 0.24);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(220, 239, 228, 0.78), rgba(255, 253, 247, 0.9));
}

.ai-summary strong {
  color: var(--sz-text);
  line-height: 1.75;
}

.ai-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.ai-summary-grid section {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(223, 210, 191, 0.72);
  border-radius: 12px;
  background: rgba(255, 253, 247, 0.72);
}

.ai-summary-grid span {
  color: var(--sz-evergreen);
  font-size: 13px;
  font-weight: 900;
}

.ai-summary-grid p {
  color: var(--sz-text);
  line-height: 1.7;
}

.input-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.input-row article,
.recipe-list article {
  border: 1px solid var(--sz-line);
  border-radius: 16px;
  background: rgba(255, 253, 247, 0.86);
}

.input-row article {
  display: grid;
  gap: 8px;
  padding: 14px;
}

.input-row span {
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 800;
}

.input-row strong {
  color: var(--sz-evergreen);
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.ingredient-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ingredient-tags span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 4px 10px 4px 4px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 800;
}

.recipe-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.recipe-list article {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 12px;
}

.recipe-list img {
  width: 82px;
  height: 68px;
  border-radius: 12px;
  object-fit: cover;
}

.recipe-copy {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.recipe-copy strong {
  overflow: hidden;
  color: var(--sz-evergreen);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipe-copy span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
}

.recipe-list button {
  grid-column: 1 / -1;
  min-height: 34px;
  border: 1px solid rgba(35, 107, 75, 0.2);
  border-radius: 10px;
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-weight: 900;
  cursor: pointer;
}

@media (max-width: 900px) {
  .history-grid,
  .stats-strip,
  .detail-metrics,
  .ai-summary-grid,
  .input-row,
  .recipe-list {
    grid-template-columns: 1fr;
  }

  .page-heading {
    display: grid;
  }

  .page-heading > p {
    max-width: none;
    text-align: left;
  }
}

@media (max-width: 640px) {
  .detail-head,
  .panel-heading {
    display: grid;
  }

  .stats-strip article {
    grid-template-columns: 44px 1fr;
  }

  .stats-strip em {
    grid-column: 2;
    grid-row: auto;
    justify-self: start;
  }
}
</style>
