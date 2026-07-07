<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import GoalSegment from '@/components/GoalSegment.vue'
import HealthSummaryCard from '@/components/HealthSummaryCard.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import { useAuthStore } from '@/stores/auth'
import type { Profile } from '@/types'

const auth = useAuthStore()
const message = useMessage()
const saving = ref(false)

const form = reactive<Profile>({ ...auth.profile })

async function save() {
  saving.value = true
  await new Promise((resolve) => window.setTimeout(resolve, 600))
  auth.saveProfile({ ...form })
  saving.value = false
  message.success('健康档案已保存')
}
</script>

<template>
  <div class="profile-view">
    <section class="title-block">
      <p class="sz-chip">03 健康档案</p>
      <h1>把推荐的基础信息先校准</h1>
      <p>档案会影响每日热量、推荐目标、忌口过滤和 AI 健康提示。</p>
    </section>

    <section class="profile-grid">
      <form class="profile-form sz-panel" @submit.prevent="save">
        <div class="form-section">
          <h2>身体信息</h2>
          <div class="field-grid">
            <n-form-item label="身高">
              <n-input-number v-model:value="form.heightCm" :min="120" :max="230">
                <template #suffix>cm</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="体重">
              <n-input-number v-model:value="form.weightKg" :min="30" :max="180">
                <template #suffix>kg</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="年龄">
              <n-input-number v-model:value="form.age" :min="12" :max="90">
                <template #suffix>岁</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="性别">
              <n-segmented v-model:value="form.gender" :options="['女', '男']" />
            </n-form-item>
          </div>
        </div>

        <div class="form-section">
          <h2>饮食目标</h2>
          <GoalSegment v-model="form.dietGoal" />
        </div>

        <div class="form-section">
          <h2>口味与限制</h2>
          <div class="tag-grid">
            <IngredientTagInput v-model="form.tastePreferences" label="口味偏好" />
            <IngredientTagInput v-model="form.avoidIngredients" label="忌口/过滤食材" />
            <IngredientTagInput v-model="form.allergyIngredients" label="过敏食材" />
          </div>
        </div>

        <n-button type="primary" size="large" block :loading="saving" @click="save">保存档案</n-button>
      </form>

      <aside class="insight-column">
        <HealthSummaryCard :profile="form" />
        <article class="tip-card">
          <h3>推荐会如何使用档案？</h3>
          <ul>
            <li>先过滤忌口和过敏食材。</li>
            <li>根据目标调整热量、蛋白质和菜谱标签权重。</li>
            <li>把 BMI 和目标热量放入推荐理由。</li>
          </ul>
        </article>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.profile-view {
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
h3,
p {
  margin: 0;
}

h1 {
  font-size: 32px;
}

.title-block p:last-child {
  color: var(--sz-muted);
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 18px;
  align-items: start;
}

.profile-form {
  display: grid;
  gap: 26px;
  padding: 24px;
}

.form-section {
  display: grid;
  gap: 14px;
}

h2 {
  font-size: 18px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.tag-grid {
  display: grid;
  gap: 16px;
}

.insight-column {
  display: grid;
  gap: 14px;
}

.tip-card {
  padding: 20px;
  border: 1px solid var(--sz-line);
  border-radius: 18px;
  background: var(--sz-surface);
}

ul {
  display: grid;
  gap: 10px;
  margin: 14px 0 0;
  padding-left: 18px;
  color: var(--sz-text);
  line-height: 1.7;
}

@media (max-width: 960px) {
  .profile-grid,
  .field-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .profile-grid,
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
