<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowRight, CheckCircle2, Clock3, Flame, Salad, Sparkles, UsersRound } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { backendAssetUrl } from '@/api/http'
import { createRecommendation } from '@/api/recommendation'
import GoalSegment from '@/components/GoalSegment.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import { useAuthStore } from '@/stores/auth'
import type { RecommendForm, RecommendationResponse } from '@/types'

const auth = useAuthStore()
const message = useMessage()
const loading = ref(false)
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

async function submit() {
  error.value = ''
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
    <section class="recommend-hero">
      <div class="hero-copy">
        <p class="sz-chip"><Sparkles /> 智能推荐</p>
        <h1>把冰箱里的食材，变成今天刚好的那一餐</h1>
        <p>结合健康档案、已有食材、排除食材和烹饪时间，生成清晰可执行的菜谱推荐。</p>
        <div class="hero-actions">
          <button class="hero-primary" type="button" :disabled="loading" @click="submit">
            <Sparkles :size="18" />
            {{ loading ? '正在推荐' : '生成推荐' }}
          </button>
          <span><CheckCircle2 :size="16" /> 已同步健康档案偏好</span>
        </div>
      </div>
      <div class="hero-summary">
        <div class="summary-card">
          <span class="summary-icon"><Salad :size="20" /></span>
          <small>当前目标</small>
          <strong>{{ goalLabels[form.dietGoal] }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-icon is-warm"><Clock3 :size="20" /></span>
          <small>烹饪时间</small>
          <strong>{{ form.cookingTime }} 分钟内</strong>
        </div>
        <div class="summary-card">
          <span class="summary-icon is-red"><UsersRound :size="20" /></span>
          <small>用餐人数</small>
          <strong>{{ form.servings }} 人</strong>
        </div>
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

        <IngredientTagInput v-model="form.availableIngredients" label="已有食材" placeholder="例如 鸡胸肉" />

        <div class="quick-wrap">
          <span>常用食材快速选择</span>
          <div>
            <button v-for="item in quickIngredients" :key="item" type="button" @click="addIngredient(item)">
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

        <button class="submit-button" type="submit" :disabled="loading">
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

        <n-alert v-if="result?.aiSummary" type="success" :bordered="false">
          {{ result.aiSummary }}
        </n-alert>

        <article v-if="primaryRecipe" class="primary-recipe">
          <img :src="backendAssetUrl(primaryRecipe.imageUrl)" :alt="primaryRecipe.name" />
          <div class="match-badge">匹配度 {{ primaryRecipe.score }}%</div>
          <div class="primary-info">
            <h3>{{ primaryRecipe.name }}</h3>
            <p>{{ primaryRecipe.reason }}</p>
            <div>
              <span><Flame :size="15" /> {{ primaryRecipe.calories }} kcal</span>
              <span>{{ primaryRecipe.protein }}g 蛋白质</span>
            </div>
          </div>
        </article>

        <div class="preview-list">
          <article v-for="recipe in secondaryRecipes" :key="recipe.id">
            <img :src="backendAssetUrl(recipe.imageUrl)" :alt="recipe.name" />
            <div>
              <strong>{{ recipe.name }}</strong>
              <span>{{ recipe.score }}% 匹配 | {{ recipe.calories }} kcal | {{ recipe.protein }}g 蛋白质</span>
              <small>{{ recipe.reason }}</small>
            </div>
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

.recommend-hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 36px;
  align-items: center;
  min-height: 278px;
  padding: 42px 54px;
  border: 1px solid rgba(184, 220, 199, 0.82);
  border-radius: 30px;
  background:
    radial-gradient(circle at 8% 55%, rgba(72, 168, 106, 0.16) 0 88px, transparent 90px),
    linear-gradient(105deg, #dceee4 0%, #eef5e7 54%, #f8f1e7 100%);
  box-shadow: var(--sz-shadow-soft);
}

.recommend-hero::after {
  content: "";
  position: absolute;
  inset: auto 28px 22px auto;
  width: 120px;
  height: 120px;
  border-radius: 48% 52% 45% 55%;
  border: 1px solid rgba(35, 107, 75, 0.12);
  background: rgba(255, 250, 241, 0.26);
  transform: rotate(-8deg);
}

.hero-copy,
.hero-summary {
  position: relative;
  z-index: 1;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  max-width: 760px;
  color: var(--sz-evergreen);
  font-size: clamp(38px, 5vw, 56px);
  line-height: 1.08;
  letter-spacing: 0;
}

.hero-copy {
  display: grid;
  justify-items: start;
  gap: 18px;
}

.hero-copy > p:not(.sz-chip) {
  max-width: 680px;
  color: var(--sz-muted);
  font-size: 17px;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  margin-top: 6px;
}

.hero-primary,
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

.hero-primary {
  min-width: 172px;
  min-height: 52px;
  padding: 0 24px;
  font-size: 17px;
  font-weight: 900;
}

.hero-primary:hover,
.submit-button:hover:not(:disabled) {
  background: var(--sz-green);
  box-shadow: 0 14px 28px rgba(35, 107, 75, 0.24);
}

.hero-primary:active,
.submit-button:active:not(:disabled) {
  transform: translateY(1px) scale(0.98);
}

.hero-primary:disabled {
  cursor: wait;
  opacity: 0.72;
}

.hero-actions span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--sz-deep-green);
  font-weight: 800;
}

.hero-summary {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.summary-card {
  display: grid;
  grid-template-columns: 48px 1fr;
  column-gap: 14px;
  align-items: center;
  min-height: 88px;
  padding: 16px;
  border: 1px solid rgba(255, 250, 241, 0.72);
  border-radius: 18px;
  background: rgba(255, 250, 241, 0.78);
  box-shadow: 0 12px 24px rgba(23, 37, 31, 0.06);
}

.summary-icon {
  grid-row: span 2;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
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
  margin-top: 4px;
  color: var(--sz-evergreen);
  font-size: 22px;
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
  min-height: 32px;
  padding: 0 12px;
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
  cursor: wait;
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

.preview-list {
  display: grid;
  gap: 12px;
}

.preview-list article {
  display: grid;
  grid-template-columns: 78px 1fr;
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
  .recommend-hero {
    grid-template-columns: 1fr;
    padding: 32px;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .hero-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .recommend-hero {
    padding: 26px 20px;
    border-radius: 24px;
  }

  .hero-summary {
    grid-template-columns: 1fr;
  }

  .panel-heading {
    display: grid;
  }

  .condition-grid {
    grid-template-columns: 1fr;
  }
}
</style>
