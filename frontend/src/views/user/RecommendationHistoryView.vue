<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Clock, Flame, ListChecks, Sparkles, UsersRound } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { createShoppingList } from '@/api/shopping'
import IngredientIcon from '@/components/IngredientIcon.vue'
import { getRecommendationHistory, listRecommendationHistory } from '@/api/recommendation'
import type { RecommendationHistoryDetail, RecommendationHistorySummary } from '@/types'
import { replaceImageWithFallback, resolveRecipeImage } from '@/utils/assets'
import { shoppingListRoute } from '@/utils/navigation'
import { recommendationResultRoute } from '@/utils/recommendationConversation'

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

function openResult(id: number) {
  void router.push(recommendationResultRoute(id))
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
      <p>
        {{ histories.length }} 次推荐 · {{ totalRecipeCount }} 道命中菜谱 · 最近 {{ latestTimeLabel }}
      </p>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="4" />

    <section class="history-grid">
      <aside class="history-record-book sz-panel">
        <div class="panel-heading">
          <div>
            <p class="sz-chip">记录列表</p>
            <h2>推荐记录册</h2>
          </div>
          <span>{{ histories.length }} 条</span>
        </div>
        <button
          v-for="item in histories"
          :key="item.id"
          type="button"
          class="record-card"
          :class="{ active: detail?.id === item.id }"
          @click="openDetail(item.id)"
        >
          <span class="record-number">#{{ item.id }}</span>
          <span class="record-top">
            <em>{{ goalLabels[item.dietGoal] }}</em>
            <small>{{ formatDate(item.createdAt) }}</small>
          </span>
          <strong>{{ item.inputIngredients.join('、') || '未填写食材' }}</strong>
          <span class="record-meta">
            <small><Clock :size="14" /> {{ item.cookingTime }} 分钟</small>
            <small><UsersRound :size="14" /> {{ item.servings }} 人</small>
            <small>{{ item.resultRecipeIds.length }} 道菜</small>
          </span>
          <ArrowRight :size="17" />
        </button>
        <div v-if="!loading && histories.length === 0" class="empty-history">
          <strong>还没有推荐历史</strong>
          <p>先完成一次智能推荐，系统会把条件、结果和购物清单入口保存在这里。</p>
          <button type="button" @click="router.push('/user/recommend')">去智能推荐</button>
        </div>
      </aside>

      <section class="recommendation-report sz-panel">
        <n-spin :show="detailLoading">
          <template v-if="detail">
            <div class="report-content">
              <div class="report-header">
                <div>
                  <p class="sz-chip is-warm"><Sparkles :size="15" /> 推荐 #{{ detail.id }}</p>
                  <h2>{{ goalLabels[detail.dietGoal] }}</h2>
                  <span>{{ fullDate(detail.createdAt) }}</span>
                </div>
                <div class="report-actions">
                  <button class="result-button" type="button" @click="openResult(detail.id)">
                    <ArrowRight :size="18" />
                    查看本次结果
                  </button>
                  <button class="shopping-button" type="button" :disabled="creating" @click="makeShoppingList">
                    <ListChecks :size="18" />
                    {{ creating ? '正在生成' : '生成购物清单' }}
                  </button>
                </div>
              </div>

              <section class="report-section report-condition-grid">
                <article>
                  <span>本次条件</span>
                  <strong>{{ detail.cookingTime }} 分钟 · {{ detail.servings }} 人 · {{ selectedRecipes.length }} 道菜</strong>
                </article>
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
              </section>

              <article class="report-section ai-summary">
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

              <section class="report-section">
                <div class="report-section-head">
                  <span>推荐菜谱</span>
                  <strong>{{ selectedRecipes.length }} 道</strong>
                </div>
                <div class="report-recipe-grid">
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
              </section>
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
.history-record-book,
.recommendation-report,
.report-recipe-grid,
.report-condition-grid {
  display: grid;
}

.history-view {
  gap: 20px;
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
  font-size: 34px;
  line-height: 1.2;
  letter-spacing: 0;
}

.page-heading > p {
  max-width: 410px;
  color: var(--sz-muted);
  line-height: 1.8;
  text-align: right;
}

.history-grid {
  display: grid;
  grid-template-columns: minmax(310px, 0.42fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.history-record-book,
.recommendation-report {
  padding: 20px;
}

.history-record-book {
  gap: 12px;
  background:
    linear-gradient(180deg, rgba(255, 253, 247, 0.96), rgba(250, 244, 234, 0.88)),
    radial-gradient(circle at 8% 12%, rgba(220, 239, 228, 0.82), transparent 34%);
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

.record-card {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 15px 42px 15px 16px;
  border: 1px solid var(--sz-line);
  border-radius: 14px;
  color: var(--sz-text);
  background: rgba(255, 253, 247, 0.86);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease;
}

.record-card.active {
  border-color: rgba(35, 107, 75, 0.48);
  background:
    linear-gradient(90deg, rgba(35, 107, 75, 0.08), transparent 46%),
    linear-gradient(135deg, var(--sz-mint), #fffdf7);
  box-shadow: 0 14px 28px rgba(35, 107, 75, 0.12);
}

.record-card:hover {
  border-color: var(--sz-line-strong);
  transform: translateY(-1px);
}

.record-card > svg {
  position: absolute;
  right: 14px;
  top: 50%;
  color: var(--sz-muted);
  transform: translateY(-50%);
}

.record-number {
  justify-self: start;
  min-height: 28px;
  padding: 5px 10px;
  border-radius: var(--sz-radius-pill);
  color: #ffffff;
  background: var(--sz-green-dark);
  font-size: 12px;
  font-weight: 900;
}

.record-top,
.record-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.record-top {
  justify-content: space-between;
}

.record-top em {
  color: var(--sz-deep-green);
  font-size: 15px;
  font-style: normal;
  font-weight: 900;
}

.record-meta small {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 26px;
  padding: 0 8px;
  border-radius: var(--sz-radius-pill);
  background: rgba(245, 252, 246, 0.72);
  font-size: 12px;
  font-weight: 850;
}

.history-record-book small,
.report-recipe-grid span {
  color: var(--sz-muted);
}

.empty-history {
  display: grid;
  justify-items: start;
  gap: 10px;
  padding: 18px;
  border: 1px dashed rgba(35, 107, 75, 0.24);
  border-radius: 16px;
  background: rgba(245, 252, 246, 0.72);
}

.empty-history strong {
  color: var(--sz-evergreen);
  font-size: 18px;
}

.empty-history p {
  color: var(--sz-muted);
  line-height: 1.7;
}

.empty-history button {
  min-height: 36px;
  padding: 0 14px;
  border: 0;
  border-radius: var(--sz-radius-pill);
  color: #ffffff;
  background: var(--sz-green-dark);
  font-weight: 900;
  cursor: pointer;
}

.recommendation-report {
  gap: 18px;
  background:
    linear-gradient(180deg, rgba(255, 250, 241, 0.98), rgba(251, 247, 239, 0.94)),
    radial-gradient(circle at 92% 0%, rgba(220, 239, 228, 0.76), transparent 30%);
}

.report-content {
  display: grid;
  gap: 18px;
}

.report-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(223, 210, 191, 0.72);
}

.report-header > div:first-child {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.report-header h2 {
  color: var(--sz-evergreen);
  font-size: 30px;
}

.report-header span {
  color: var(--sz-muted);
  font-weight: 800;
}

.report-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.shopping-button,
.result-button {
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

.result-button {
  border: 1px solid rgba(35, 107, 75, 0.18);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  box-shadow: none;
}

.shopping-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.report-condition-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.report-condition-grid article {
  display: grid;
  align-content: start;
  gap: 9px;
  min-height: 98px;
  padding: 13px;
  border: 1px solid rgba(223, 210, 191, 0.86);
  border-radius: 14px;
  background: rgba(255, 253, 247, 0.86);
}

.report-condition-grid article > span {
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 900;
}

.report-condition-grid strong {
  color: var(--sz-evergreen);
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.report-section {
  display: grid;
  gap: 12px;
}

.report-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-section-head span {
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 900;
}

.report-section-head strong {
  min-height: 30px;
  padding: 5px 11px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
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

.report-recipe-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.report-recipe-grid article {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--sz-line);
  border-radius: 16px;
  background: rgba(255, 253, 247, 0.86);
}

.report-recipe-grid img {
  width: 92px;
  height: 74px;
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

.report-recipe-grid button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(35, 107, 75, 0.2);
  border-radius: 10px;
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-weight: 900;
  cursor: pointer;
}

@media (max-width: 900px) {
  .history-grid,
  .report-condition-grid,
  .ai-summary-grid,
  .report-recipe-grid {
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
  .report-header,
  .panel-heading {
    display: grid;
  }

  .report-actions {
    justify-content: stretch;
  }

  .report-actions button {
    flex: 1 1 auto;
  }

  .report-recipe-grid article {
    grid-template-columns: 82px minmax(0, 1fr);
  }

  .report-recipe-grid button {
    grid-column: 1 / -1;
  }
}
</style>
