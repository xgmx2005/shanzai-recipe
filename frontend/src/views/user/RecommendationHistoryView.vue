<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, CalendarClock, ChefHat, Clock, Flame, ListChecks, Salad, Sparkles, UsersRound } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { createShoppingList } from '@/api/shopping'
import { backendAssetUrl } from '@/api/http'
import { getRecommendationHistory, listRecommendationHistory } from '@/api/recommendation'
import type { RecommendationHistoryDetail, RecommendationHistorySummary } from '@/types'

const message = useMessage()
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
    if (latest.value) {
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
    await createShoppingList({
      recipeIds: detail.value.resultRecipeIds,
      availableIngredients: detail.value.inputIngredients,
      title: `推荐 #${detail.value.id} 采购清单`,
    })
    message.success('购物清单已生成')
  } catch (err) {
    message.error(err instanceof Error ? err.message : '生成购物清单失败')
  } finally {
    creating.value = false
  }
}

function recipeImage(seed: number, imageUrl?: string) {
  const fallback = [
    'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80',
    'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80',
    'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=80',
  ]
  return backendAssetUrl(imageUrl) || fallback[Math.abs(seed) % fallback.length]
}

onMounted(load)
</script>

<template>
  <div class="history-view">
    <section class="history-hero">
      <div class="hero-copy">
        <p class="sz-chip"><Clock :size="15" /> 推荐历史</p>
        <h1>把每一次推荐，沉淀成下一餐计划</h1>
        <p>回看输入食材、推荐目标和命中的菜谱，从历史记录里快速生成购物清单。</p>
      </div>
      <div class="hero-stats">
        <article>
          <span class="stat-icon"><CalendarClock :size="20" /></span>
          <small>历史记录</small>
          <strong>{{ histories.length }}</strong>
          <em>次推荐</em>
        </article>
        <article>
          <span class="stat-icon is-warm"><ChefHat :size="20" /></span>
          <small>累计菜谱</small>
          <strong>{{ totalRecipeCount }}</strong>
          <em>道命中</em>
        </article>
        <article>
          <span class="stat-icon is-red"><Sparkles :size="20" /></span>
          <small>最近一次</small>
          <strong>{{ latestTimeLabel }}</strong>
          <em>已保存</em>
        </article>
      </div>
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
                <p class="sz-chip"><Sparkles :size="15" /> AI 摘要</p>
                <strong>{{ detail.aiSummary }}</strong>
              </article>

              <div class="input-row">
                <article>
                  <span>已有食材</span>
                  <strong>{{ detail.inputIngredients.join('、') || '无' }}</strong>
                </article>
                <article>
                  <span>排除食材</span>
                  <strong>{{ detail.excludedIngredients.join('、') || '无' }}</strong>
                </article>
              </div>

              <div class="recipe-list">
                <article v-for="recipe in detail.recipes" :key="recipe.id">
                  <img :src="recipeImage(recipe.id, recipe.imageUrl)" :alt="recipe.name" />
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
.history-hero,
.history-list,
.detail-panel,
.recipe-list {
  display: grid;
}

.history-view {
  gap: 22px;
}

.history-hero {
  position: relative;
  overflow: hidden;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 38px;
  align-items: center;
  min-height: 260px;
  padding: 42px 52px;
  border: 1px solid rgba(184, 220, 199, 0.82);
  border-radius: 30px;
  background:
    radial-gradient(circle at 8% 58%, rgba(72, 168, 106, 0.16) 0 88px, transparent 90px),
    linear-gradient(105deg, #dceee4 0%, #edf6e9 55%, #f8f1e7 100%);
  box-shadow: var(--sz-shadow-soft);
}

.history-hero::after {
  content: "";
  position: absolute;
  right: 34px;
  bottom: 24px;
  width: 126px;
  height: 126px;
  border: 1px solid rgba(35, 107, 75, 0.12);
  border-radius: 48% 52% 44% 56%;
  background: rgba(255, 250, 241, 0.24);
  transform: rotate(-8deg);
}

.hero-copy,
.hero-stats {
  position: relative;
  z-index: 1;
}

.hero-copy {
  display: grid;
  justify-items: start;
  gap: 18px;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  max-width: 720px;
  color: var(--sz-evergreen);
  font-size: 50px;
  line-height: 1.08;
  letter-spacing: 0;
}

.hero-copy > p:not(.sz-chip) {
  max-width: 650px;
  color: var(--sz-muted);
  font-size: 17px;
  line-height: 1.9;
}

.hero-stats {
  display: grid;
  gap: 14px;
}

.hero-stats article {
  display: grid;
  grid-template-columns: 48px 1fr auto;
  column-gap: 14px;
  align-items: center;
  min-height: 86px;
  padding: 16px;
  border: 1px solid rgba(255, 250, 241, 0.72);
  border-radius: 18px;
  background: rgba(255, 250, 241, 0.78);
  box-shadow: 0 12px 24px rgba(23, 37, 31, 0.06);
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

.hero-stats small {
  color: var(--sz-muted);
  font-weight: 800;
}

.hero-stats strong {
  color: var(--sz-evergreen);
  font-size: 24px;
}

.hero-stats em {
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
  .history-hero,
  .history-grid,
  .detail-metrics,
  .input-row,
  .recipe-list {
    grid-template-columns: 1fr;
  }

  .history-hero {
    padding: 32px;
  }

  h1 {
    font-size: 40px;
  }
}

@media (max-width: 640px) {
  .history-hero {
    padding: 26px 20px;
    border-radius: 24px;
  }

  h1 {
    font-size: 32px;
  }

  .detail-head,
  .panel-heading {
    display: grid;
  }

  .hero-stats article {
    grid-template-columns: 44px 1fr;
  }

  .hero-stats em {
    grid-column: 2;
    grid-row: auto;
    justify-self: start;
  }
}
</style>
