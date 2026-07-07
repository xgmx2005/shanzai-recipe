<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Sparkles } from '@lucide/vue'
import GoalSegment from '@/components/GoalSegment.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import { quickIngredients, recipes } from '@/mock/data'
import { useAuthStore } from '@/stores/auth'
import type { RecommendForm } from '@/types'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)

const form = reactive<RecommendForm>({
  availableIngredients: ['鸡胸肉', '西兰花', '鸡蛋'],
  avoidIngredients: ['香菜'],
  dietGoal: auth.profile.dietGoal,
  cookingTime: 25,
  servings: 1,
})

function addIngredient(name: string) {
  if (!form.availableIngredients.includes(name)) {
    form.availableIngredients.push(name)
  }
}

async function submit() {
  loading.value = true
  await new Promise((resolve) => window.setTimeout(resolve, 850))
  loading.value = false
  router.push('/user/recommend/result')
}
</script>

<template>
  <div class="recommend-view">
    <section class="title-block">
      <p class="sz-chip"><Sparkles /> 04 智能推荐</p>
      <h1>输入条件，生成今天的合适一餐</h1>
      <p>先用 mock 数据完成交互，后续直接替换成推荐接口返回。</p>
    </section>

    <section class="recommend-grid">
      <form class="recommend-form sz-panel" @submit.prevent="submit">
        <IngredientTagInput
          v-model="form.availableIngredients"
          label="已有食材"
          placeholder="例如 鸡胸肉"
        />

        <div class="quick-wrap">
          <span>常用食材快速选择</span>
          <div>
            <button v-for="item in quickIngredients" :key="item" type="button" @click="addIngredient(item)">
              {{ item }}
            </button>
          </div>
        </div>

        <IngredientTagInput
          v-model="form.avoidIngredients"
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

        <n-button type="primary" size="large" block :loading="loading" @click="submit">
          {{ loading ? '正在匹配菜谱、营养目标和已有食材' : '生成推荐' }}
          <template #icon>
            <n-icon><Sparkles /></n-icon>
          </template>
        </n-button>
      </form>

      <aside class="preview-panel sz-panel">
        <div>
          <p class="sz-chip">推荐预览</p>
          <h2>可能优先命中的菜谱</h2>
        </div>
        <div class="preview-list">
          <article v-for="recipe in recipes.slice(0, 3)" :key="recipe.id">
            <img :src="recipe.imageUrl" :alt="recipe.name" />
            <div>
              <strong>{{ recipe.name }}</strong>
              <span>{{ recipe.calories }} kcal | {{ recipe.protein }}g 蛋白质</span>
            </div>
          </article>
        </div>
        <n-alert type="info" :bordered="false">
          结果页会展示 AI 健康提示、综合评分、已有食材命中和还需补买。
        </n-alert>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.recommend-view {
  display: grid;
  gap: 18px;
}

.title-block {
  display: grid;
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

.recommend-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: start;
}

.recommend-form {
  display: grid;
  gap: 22px;
  padding: 24px;
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
  background: var(--sz-surface);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease;
}

.quick-wrap button:hover {
  border-color: var(--sz-green);
  background: var(--sz-mint);
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

.preview-panel {
  display: grid;
  gap: 18px;
  padding: 22px;
  border-color: rgba(18, 61, 45, 0.18);
  color: #ffffff;
  background: var(--sz-evergreen);
}

.preview-panel > div:first-child {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.preview-panel h2 {
  color: #ffffff;
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
  padding: 10px;
  border: 1px solid rgba(255, 250, 241, 0.16);
  border-radius: 16px;
  background: rgba(255, 250, 241, 0.12);
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
}

.preview-list span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

@media (max-width: 940px) {
  .recommend-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .condition-grid {
    grid-template-columns: 1fr;
  }
}
</style>
