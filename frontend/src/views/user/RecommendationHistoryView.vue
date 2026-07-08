<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, ListChecks, Sparkles } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { createShoppingList } from '@/api/shopping'
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

const goalLabels = {
  FAT_LOSS: '减脂控热量',
  BALANCED: '日常均衡',
  MUSCLE_GAIN: '健身增肌',
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

onMounted(load)
</script>

<template>
  <div class="history-view">
    <section class="title-block">
      <p class="sz-chip"><Clock /> 推荐历史</p>
      <h1>把每一次推荐沉淀成下一餐计划</h1>
      <p>查看输入食材、AI 摘要和命中的菜谱，并可直接生成采购清单。</p>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="4" />

    <section class="history-grid">
      <aside class="history-list sz-panel">
        <button
          v-for="item in histories"
          :key="item.id"
          type="button"
          :class="{ active: detail?.id === item.id }"
          @click="openDetail(item.id)"
        >
          <span>#{{ item.id }} {{ goalLabels[item.dietGoal] }}</span>
          <strong>{{ item.inputIngredients.join('、') || '未填写食材' }}</strong>
          <small>{{ new Date(item.createdAt).toLocaleString() }}</small>
        </button>
        <n-empty v-if="!loading && histories.length === 0" description="暂无推荐历史" />
      </aside>

      <section class="detail-panel sz-panel">
        <n-spin :show="detailLoading">
          <template v-if="detail">
            <div class="detail-head">
              <div>
                <p class="sz-chip"><Sparkles /> #{{ detail.id }}</p>
                <h2>{{ goalLabels[detail.dietGoal] }} · {{ detail.cookingTime }} 分钟 · {{ detail.servings }} 人</h2>
              </div>
              <n-button type="primary" :loading="creating" @click="makeShoppingList">
                <template #icon><n-icon><ListChecks /></n-icon></template>
                生成清单
              </n-button>
            </div>

            <n-alert type="success" :bordered="false">{{ detail.aiSummary }}</n-alert>

            <div class="input-row">
              <span>已有食材：{{ detail.inputIngredients.join('、') || '无' }}</span>
              <span>排除食材：{{ detail.excludedIngredients.join('、') || '无' }}</span>
            </div>

            <div class="recipe-list">
              <article v-for="recipe in detail.recipes" :key="recipe.id">
                <div>
                  <strong>{{ recipe.name }}</strong>
                  <span>{{ recipe.calories }} kcal · {{ recipe.protein }}g 蛋白质</span>
                </div>
                <n-button secondary type="primary" size="small" @click="router.push(`/user/recipes/${recipe.id}`)">
                  查看详情
                </n-button>
              </article>
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
.title-block,
.history-list,
.detail-panel,
.recipe-list {
  display: grid;
}

.history-view {
  gap: 18px;
}

.title-block {
  justify-items: start;
  gap: 10px;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  font-size: 32px;
}

.title-block p:last-child {
  color: var(--sz-muted);
}

.history-grid {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.history-list,
.detail-panel {
  padding: 18px;
}

.history-list {
  gap: 10px;
}

.history-list button {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--sz-line);
  border-radius: 14px;
  color: var(--sz-text);
  background: var(--sz-surface);
  text-align: left;
  cursor: pointer;
}

.history-list button.active {
  border-color: var(--sz-green);
  background: var(--sz-mint);
}

.history-list span,
.history-list small,
.input-row,
.recipe-list span {
  color: var(--sz-muted);
}

.detail-panel {
  gap: 16px;
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

.input-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.input-row span,
.recipe-list article {
  border: 1px solid var(--sz-line);
  border-radius: 14px;
  background: var(--sz-surface);
}

.input-row span {
  padding: 8px 12px;
}

.recipe-list {
  gap: 10px;
}

.recipe-list article {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
}

.recipe-list div {
  display: grid;
  gap: 6px;
}

@media (max-width: 900px) {
  .history-grid {
    grid-template-columns: 1fr;
  }
}
</style>
