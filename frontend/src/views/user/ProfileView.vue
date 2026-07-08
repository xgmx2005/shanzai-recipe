<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import GoalSegment from '@/components/GoalSegment.vue'
import HealthSummaryCard from '@/components/HealthSummaryCard.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import { useAuthStore } from '@/stores/auth'
import type { Profile, ProfileRequest } from '@/types'

const auth = useAuthStore()
const message = useMessage()
const saving = ref(false)
const loading = ref(true)
const error = ref('')

const form = reactive<ProfileRequest>({
  heightCm: auth.profile.heightCm,
  weightKg: auth.profile.weightKg,
  age: auth.profile.age,
  gender: auth.profile.gender,
  dietGoal: auth.profile.dietGoal,
  tastePreferences: [...auth.profile.tastePreferences],
  avoidIngredients: [...auth.profile.avoidIngredients],
  allergyIngredients: [...auth.profile.allergyIngredients],
  cookingTimePreference: auth.profile.cookingTimePreference,
})

function applyProfile(profile: Profile) {
  Object.assign(form, {
    heightCm: profile.heightCm,
    weightKg: profile.weightKg,
    age: profile.age,
    gender: profile.gender,
    dietGoal: profile.dietGoal,
    tastePreferences: [...profile.tastePreferences],
    avoidIngredients: [...profile.avoidIngredients],
    allergyIngredients: [...profile.allergyIngredients],
    cookingTimePreference: profile.cookingTimePreference,
  })
}

const previewProfile = reactive<Profile>({ ...auth.profile, ...form })

async function save() {
  error.value = ''
  saving.value = true
  try {
    const saved = await auth.saveProfile({ ...form })
    Object.assign(previewProfile, saved)
    applyProfile(saved)
    message.success('健康档案已保存')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存健康档案失败'
    message.error(error.value)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const profile = await auth.loadProfile()
    applyProfile(profile)
    Object.assign(previewProfile, profile)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '健康档案加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="profile-view">
    <section class="title-block">
      <p class="sz-chip">03 健康档案</p>
      <h1>把推荐的基础信息先校准</h1>
      <p>档案会影响每日热量、推荐目标、忌口过滤和 AI 健康提示。</p>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>

    <section class="profile-grid">
      <form class="profile-form sz-panel" @submit.prevent="save">
        <n-skeleton v-if="loading" text :repeat="4" />
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
            <n-form-item label="烹饪时间偏好">
              <n-input-number v-model:value="form.cookingTimePreference" :min="10" :max="120">
                <template #suffix>分钟</template>
              </n-input-number>
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
        <HealthSummaryCard :profile="{ ...previewProfile, ...form }" />
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
