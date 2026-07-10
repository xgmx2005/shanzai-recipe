<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, CheckCircle2, Clock3, Flame, ListPlus, Salad, Sparkles, UsersRound } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { createRecommendation } from '@/api/recommendation'
import { createShoppingList } from '@/api/shopping'
import GoalSegment from '@/components/GoalSegment.vue'
import IngredientIcon from '@/components/IngredientIcon.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import { useAuthStore } from '@/stores/auth'
import type { RecommendForm, RecommendationResponse } from '@/types'
import { replaceImageWithFallback, resolveRecipeImage, resolveRecipeImagePosition } from '@/utils/assets'
import { shoppingListRoute } from '@/utils/navigation'

const auth = useAuthStore()
const message = useMessage()
const router = useRouter()
const loading = ref(false)
const creatingList = ref(false)
const error = ref('')
const result = ref<RecommendationResponse | null>(null)
const quickIngredients = ['鸡胸肉', '番茄', '牛肉', '藜麦', '鸡蛋', '豆腐', '西兰花', '虾仁']
const goalLabels = {
  FAT_LOSS: '减脂控热量',
  BALANCED: '日常健康',
  MUSCLE_GAIN: '健身增肌',
}

const form = reactive<RecommendForm>({
  availableIngredients: ['鸡胸肉', '西兰花', '鸡蛋'],
  excludedIngredients: [...auth.profile.avoidIngredients],
  dietGoal: auth.profile.dietGoal,
  cookingTime: auth.profile.cookingTimePreference,
  servings: 1,
})

function addIngredient(name: string) {
  if (!form.availableIngredients.includes(name)) {
    form.availableIngredients.push(name)
  }
}

const activeInputCount = computed(() => form.availableIngredients.length + form.excludedIngredients.length)
const primaryRecipe = computed(() => result.value?.recipes[0] ?? null)
const secondaryRecipes = computed(() => result.value?.recipes.slice(1) ?? [])
const resultRecipeIds = computed(() => result.value?.recipes.map((recipe) => recipe.id) ?? [])
const canSubmit = computed(() => form.availableIngredients.length > 0 && !loading.value)
const resultTotalCalories = computed(() => result.value?.recipes.reduce((sum, recipe) => sum + recipe.calories, 0) ?? 0)
const resultAverageProtein = computed(() => {
  const recipes = result.value?.recipes ?? []
  if (!recipes.length) return 0
  return Math.round(recipes.reduce((sum, recipe) => sum + recipe.protein, 0) / recipes.length)
})

function isQuickSelected(name: string) {
  return form.availableIngredients.includes(name)
}

function recipeImagePosition(imageUrl?: string) {
  return resolveRecipeImagePosition(imageUrl)
}

async function submit() {
  error.value = ''
  if (!form.availableIngredients.length) {
    message.warning('请先添加至少一种已有食材')
    return
  }

  loading.value = true
  try {
    result.value = await createRecommendation({ ...form })
    message.success('推荐已生成')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '生成推荐失败'
    message.error(error.value)
  } finally {
    loading.value = false
  }
}

async function makeShoppingList() {
  if (!resultRecipeIds.value.length || creatingList.value) return

  creatingList.value = true
  try {
    const list = await createShoppingList({
      recipeIds: resultRecipeIds.value,
      availableIngredients: form.availableIngredients,
      title: `智能推荐 #${result.value?.historyId} 采购清单`,
    })
    message.success('购物清单已生成')
    await router.push(shoppingListRoute(list.id, 'recommendation'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : '生成购物清单失败')
  } finally {
    creatingList.value = false
  }
}

function openRecipeDetail(id: number) {
  void router.push({
    path: `/user/recipes/${id}`,
    query: result.value?.historyId
      ? {
          from: 'recommendation',
          historyId: String(result.value.historyId),
        }
      : undefined,
  })
}

onMounted(async () => {
  try {
    const profile = await auth.loadProfile()
    form.dietGoal = profile.dietGoal
    form.excludedIngredients = [...profile.avoidIngredients, ...profile.allergyIngredients]
    form.cookingTime = profile.cookingTimePreference
  } catch {
    // 未建档用户仍可直接填写条件生成推荐。
  }
})
</script>

<template>
  <div class="recommend-view">
    <section class="recommend-header">
      <div class="header-copy">
        <p class="sz-chip"><Sparkles /> 智能推荐</p>
        <h1>把现有食材匹配成可执行的一餐</h1>
        <p>结合健康档案、已有食材、排除食材和烹饪时间，生成清晰可执行的菜谱推荐。</p>
        <div class="header-footnote">
          <span>已有食材 {{ form.availableIngredients.length }} 种</span>
          <span>排除食材 {{ form.excludedIngredients.length }} 种</span>
        </div>
      </div>
      <div class="header-summary">
        <article class="summary-card">
          <span class="summary-icon"><Salad :size="20" /></span>
          <small>当前目标</small>
          <strong>{{ goalLabels[form.dietGoal] }}</strong>
        </article>
        <article class="summary-card">
          <span class="summary-icon is-warm"><Clock3 :size="20" /></span>
          <small>烹饪时间</small>
          <strong>{{ form.cookingTime }} 分钟内</strong>
        </article>
        <article class="summary-card">
          <span class="summary-icon is-red"><UsersRound :size="20" /></span>
          <small>用餐人数</small>
          <strong>{{ form.servings }} 人</strong>
        </article>
      </div>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>

    <section class="recommend-grid">
      <form class="recommend-form sz-panel" @submit.prevent="submit">
        <div class="panel-heading">
          <div>
            <p class="sz-chip">输入条件</p>
            <h2>告诉膳哉你现在有什么</h2>
          </div>
          <span>{{ activeInputCount }} 个条件</span>
        </div>

        <div class="form-hint-grid">
          <article>
            <strong>先填已有食材</strong>
            <small>系统优先命中你现在就能做的一餐。</small>
          </article>
          <article>
            <strong>排除不想吃的食材</strong>
            <small>会直接参与过滤，避免推荐看起来对但做不了。</small>
          </article>
        </div>

        <IngredientTagInput v-model="form.availableIngredients" label="已有食材" placeholder="例如 鸡胸肉" />

        <div class="quick-wrap">
          <span>常用食材快速选择</span>
          <div>
            <button
              v-for="item in quickIngredients"
              :key="item"
              type="button"
              :class="{ active: isQuickSelected(item) }"
              :disabled="isQuickSelected(item)"
              @click="addIngredient(item)"
            >
              <IngredientIcon :name="item" :size="20" />
              <CheckCircle2 v-if="isQuickSelected(item)" :size="14" />
              {{ item }}
            </button>
          </div>
        </div>

        <IngredientTagInput
          v-model="form.excludedIngredients"
          label="排除食材"
          placeholder="例如 香菜"
        />

        <div class="form-section">
          <h2>本次目标</h2>
          <GoalSegment v-model="form.dietGoal" />
        </div>

        <div class="condition-grid">
          <label>
            <span>烹饪时间</span>
            <n-select
              v-model:value="form.cookingTime"
              :options="[
                { label: '15 分钟以内', value: 15 },
                { label: '25 分钟以内', value: 25 },
                { label: '40 分钟以内', value: 40 },
                { label: '60 分钟以内', value: 60 },
              ]"
            />
          </label>
          <label>
            <span>人数</span>
            <n-select
              v-model:value="form.servings"
              :options="[
                { label: '1 人', value: 1 },
                { label: '2 人', value: 2 },
                { label: '3 人', value: 3 },
                { label: '4 人', value: 4 },
              ]"
            />
          </label>
        </div>

        <button class="submit-button" type="submit" :disabled="!canSubmit">
          <Sparkles :size="18" />
          {{ loading ? '正在匹配菜谱、营养目标和已有食材' : '生成推荐' }}
          <ArrowRight :size="18" />
        </button>
      </form>

      <aside class="preview-panel sz-panel">
        <div class="panel-heading">
          <div>
            <p class="sz-chip is-warm">推荐结果</p>
            <h2>{{ result ? `本次推荐 #${result.historyId}` : '等待生成推荐' }}</h2>
          </div>
          <span v-if="result">{{ result.recipes.length }} 道</span>
        </div>

        <article v-if="result?.aiSummary" class="ai-insight-panel">
          <div class="ai-insight-head">
            <span><Sparkles :size="15" /> {{ result.aiGenerated ? 'AI 已生成分析' : '规则推荐分析' }}</span>
          </div>
          <p>{{ result.aiSummary }}</p>
          <div class="ai-insight-grid">
            <section>
              <strong>健康提示</strong>
              <span>{{ result.aiHealthTip }}</span>
            </section>
            <section>
              <strong>购物清单</strong>
              <span>{{ result.aiShoppingTip }}</span>
            </section>
          </div>
        </article>

        <div v-if="result" class="result-stats">
          <article>
            <small>命中菜谱</small>
            <strong>{{ result.recipes.length }} 道</strong>
          </article>
          <article>
            <small>累计热量</small>
            <strong>{{ resultTotalCalories }} kcal</strong>
          </article>
          <article>
            <small>平均蛋白质</small>
            <strong>{{ resultAverageProtein }} g</strong>
          </article>
        </div>

        <div v-if="result" class="result-actions">
          <button type="button" class="result-primary" :disabled="creatingList" @click="makeShoppingList">
            <ListPlus :size="17" />
            {{ creatingList ? '正在生成清单' : '生成购物清单' }}
          </button>
        </div>

        <article v-if="primaryRecipe" class="primary-recipe">
          <img
            :src="resolveRecipeImage(primaryRecipe.imageUrl)"
            :alt="primaryRecipe.name"
            :style="{ objectPosition: recipeImagePosition(primaryRecipe.imageUrl) }"
            @error="replaceImageWithFallback($event)"
          />
          <div class="match-badge">匹配度 {{ primaryRecipe.score }}%</div>
          <div class="primary-info">
            <h3>{{ primaryRecipe.name }}</h3>
            <p>{{ primaryRecipe.reason }}</p>
            <div>
              <span><Flame :size="15" /> {{ primaryRecipe.calories }} kcal</span>
              <span>{{ primaryRecipe.protein }}g 蛋白质</span>
            </div>
            <button type="button" @click="openRecipeDetail(primaryRecipe.id)">查看详情</button>
          </div>
        </article>

        <div class="preview-list">
          <article v-for="recipe in secondaryRecipes" :key="recipe.id">
            <img
              :src="resolveRecipeImage(recipe.imageUrl)"
              :alt="recipe.name"
              :style="{ objectPosition: recipeImagePosition(recipe.imageUrl) }"
              @error="replaceImageWithFallback($event)"
            />
            <div>
              <strong>{{ recipe.name }}</strong>
              <span>{{ recipe.score }}% 匹配 | {{ recipe.calories }} kcal | {{ recipe.protein }}g 蛋白质</span>
              <small>{{ recipe.reason }}</small>
            </div>
            <button type="button" @click="openRecipeDetail(recipe.id)">详情</button>
          </article>
        </div>

        <div v-if="loading" class="empty-state is-loading">
          <span class="loading-mark" />
          <strong>正在匹配最合适的一餐</strong>
          <span>膳哉正在计算食材命中、健康目标、烹饪时间和营养得分。</span>
        </div>

        <div v-else-if="!result" class="empty-state">
          <Sparkles :size="26" />
          <strong>填写左侧条件后生成推荐</strong>
          <span>系统会返回推荐菜谱、匹配理由和本次推荐记录。</span>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.recommend-view {
  display: grid;
  gap: 22px;
}

.recommend-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 42%);
  gap: 22px;
  align-items: end;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  max-width: 680px;
  color: var(--sz-evergreen);
  font-size: 34px;
  line-height: 1.18;
  letter-spacing: 0;
}

.header-copy {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.header-copy > p:not(.sz-chip) {
  max-width: 680px;
  color: var(--sz-muted);
  line-height: 1.8;
}

.header-footnote {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.header-footnote span {
  min-height: 30px;
  padding: 5px 11px;
  border: 1px solid rgba(35, 107, 75, 0.12);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  font-weight: 800;
}

.submit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 12px 22px rgba(35, 107, 75, 0.2);
  cursor: pointer;
  transition:
    background 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.submit-button:hover:not(:disabled) {
  background: var(--sz-green);
  box-shadow: 0 14px 28px rgba(35, 107, 75, 0.24);
}

.submit-button:active:not(:disabled) {
  transform: translateY(1px) scale(0.98);
}

.header-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.summary-card {
  display: grid;
  gap: 7px;
  align-items: center;
  min-height: 112px;
  padding: 14px;
  border: 1px solid var(--sz-line);
  border-radius: 16px;
  background: rgba(255, 250, 241, 0.9);
  box-shadow: var(--sz-shadow-soft);
}

.summary-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  color: var(--sz-green-dark);
  background: var(--sz-mint);
}

.summary-icon.is-warm {
  color: #b16b18;
  background: var(--sz-grain-soft);
}

.summary-icon.is-red {
  color: var(--sz-tomato);
  background: var(--sz-tomato-soft);
}

.summary-card small {
  color: var(--sz-muted);
  font-weight: 800;
}

.summary-card strong {
  color: var(--sz-evergreen);
  font-size: 18px;
  line-height: 1.25;
}

.recommend-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 18px;
  align-items: start;
}

.recommend-form {
  display: grid;
  gap: 22px;
  padding: 28px;
}

.panel-heading {
  display: flex;
  gap: 18px;
  align-items: start;
  justify-content: space-between;
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

.form-hint-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.form-hint-grid article {
  display: grid;
  gap: 5px;
  min-height: 74px;
  padding: 12px 14px;
  border: 1px solid rgba(223, 210, 191, 0.82);
  border-radius: 14px;
  background: rgba(255, 250, 241, 0.88);
}

.form-hint-grid strong {
  color: var(--sz-evergreen);
  font-size: 14px;
}

.form-hint-grid small {
  color: var(--sz-muted);
  line-height: 1.6;
}

.quick-wrap {
  display: grid;
  gap: 10px;
}

.quick-wrap > span,
.condition-grid label > span {
  color: var(--sz-text);
  font-size: 14px;
  font-weight: 800;
}

.quick-wrap div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-wrap button {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  gap: 6px;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: #fffdf7;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease;
}

.quick-wrap button:hover {
  border-color: var(--sz-green);
  background: var(--sz-mint);
  transform: translateY(-1px);
}

.quick-wrap button.active,
.quick-wrap button:disabled {
  border-color: rgba(35, 107, 75, 0.18);
  color: var(--sz-muted);
  background: rgba(220, 239, 228, 0.62);
  cursor: default;
  transform: none;
}

.form-section {
  display: grid;
  gap: 14px;
}

h2 {
  font-size: 18px;
}

.condition-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.condition-grid label {
  display: grid;
  gap: 10px;
}

.submit-button {
  min-height: 52px;
  width: 100%;
  padding: 0 22px;
  font-weight: 900;
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.preview-panel {
  display: grid;
  gap: 18px;
  padding: 26px;
  border-color: rgba(223, 210, 191, 0.92);
  background: linear-gradient(180deg, rgba(255, 250, 241, 0.98), rgba(251, 247, 239, 0.94));
}

.preview-panel h2 {
  color: var(--sz-evergreen);
}

.ai-insight-panel {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(72, 168, 106, 0.24);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(220, 239, 228, 0.78), rgba(255, 253, 247, 0.9));
}

.ai-insight-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.ai-insight-head span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 4px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: rgba(255, 253, 247, 0.78);
  font-size: 12px;
  font-weight: 900;
}

.ai-insight-panel p,
.ai-insight-grid span {
  color: var(--sz-text);
  line-height: 1.7;
}

.ai-insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.ai-insight-grid section {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(223, 210, 191, 0.72);
  border-radius: 12px;
  background: rgba(255, 253, 247, 0.72);
}

.ai-insight-grid strong {
  color: var(--sz-evergreen);
  font-size: 13px;
}

.result-actions {
  display: grid;
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.result-stats article {
  display: grid;
  gap: 4px;
  min-height: 78px;
  padding: 12px 14px;
  border: 1px solid rgba(223, 210, 191, 0.82);
  border-radius: 14px;
  background: rgba(255, 253, 247, 0.88);
}

.result-stats small {
  color: var(--sz-muted);
  font-weight: 800;
}

.result-stats strong {
  color: var(--sz-evergreen);
  font-size: 18px;
  line-height: 1.3;
}

.result-primary,
.primary-info button,
.preview-list button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 10px;
  font-weight: 900;
  cursor: pointer;
}

.result-primary {
  gap: 8px;
  min-height: 42px;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 10px 18px rgba(35, 107, 75, 0.18);
}

.result-primary:disabled {
  cursor: wait;
  opacity: 0.72;
}

.primary-recipe {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(223, 210, 191, 0.92);
  border-radius: 20px;
  background: #fffdf7;
  box-shadow: var(--sz-shadow-soft);
}

.primary-recipe > img {
  display: block;
  width: 100%;
  height: 218px;
  object-fit: cover;
}

.match-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 7px 12px;
  border-radius: var(--sz-radius-pill);
  color: #ffffff;
  background: rgba(35, 107, 75, 0.92);
  font-size: 13px;
  font-weight: 900;
}

.primary-info {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.primary-info h3 {
  color: var(--sz-evergreen);
  font-size: 22px;
}

.primary-info p {
  color: var(--sz-muted);
  line-height: 1.7;
}

.primary-info div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.primary-info span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 28px;
  padding: 4px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 800;
}

.primary-info button {
  min-height: 38px;
  color: var(--sz-deep-green);
  background: var(--sz-mint);
}

.preview-list {
  display: grid;
  gap: 12px;
}

.preview-list article {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid rgba(223, 210, 191, 0.8);
  border-radius: 16px;
  background: rgba(255, 250, 241, 0.82);
}

.preview-list img {
  width: 78px;
  height: 64px;
  object-fit: cover;
  border-radius: 12px;
}

.preview-list div {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.preview-list strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--sz-evergreen);
}

.preview-list span {
  color: var(--sz-muted);
  font-size: 13px;
}

.preview-list small {
  color: var(--sz-text);
  font-size: 12px;
  line-height: 1.5;
}

.preview-list button {
  min-height: 34px;
  padding: 0 12px;
  color: var(--sz-deep-green);
  background: var(--sz-mint);
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 300px;
  padding: 28px;
  border: 1px dashed rgba(35, 107, 75, 0.24);
  border-radius: 20px;
  color: var(--sz-muted);
  background: rgba(220, 239, 228, 0.38);
  text-align: center;
}

.empty-state svg {
  color: var(--sz-green-dark);
}

.empty-state strong {
  color: var(--sz-evergreen);
  font-size: 18px;
}

.loading-mark {
  width: 30px;
  height: 30px;
  border: 3px solid rgba(35, 107, 75, 0.16);
  border-top-color: var(--sz-green-dark);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 940px) {
  .recommend-header,
  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .header-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .header-summary,
  .form-hint-grid,
  .ai-insight-grid,
  .result-stats,
  .preview-list article {
    grid-template-columns: 1fr;
  }

  .panel-heading {
    display: grid;
  }

  .condition-grid {
    grid-template-columns: 1fr;
  }

  .preview-list button {
    width: 100%;
  }
}
</style>
