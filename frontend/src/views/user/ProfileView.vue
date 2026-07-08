<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { CheckCircle2, ChefHat, Clock3, Flame, HeartPulse, Save, Scale, ShieldCheck, SlidersHorizontal, UserRound } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import GoalSegment from '@/components/GoalSegment.vue'
import HealthSummaryCard from '@/components/HealthSummaryCard.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import { useAuthStore } from '@/stores/auth'
import type { DietGoal, Profile, ProfileRequest } from '@/types'

const auth = useAuthStore()
const message = useMessage()
const saving = ref(false)
const loading = ref(true)
const error = ref('')
const genderOptions = ['女', '男'] as const
const goalLabels: Record<DietGoal, string> = {
  FAT_LOSS: '减脂控热量',
  BALANCED: '日常健康',
  MUSCLE_GAIN: '健身增肌',
}

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
const liveProfile = computed<Profile>(() => ({ ...previewProfile, ...form }))

const bmi = computed(() => {
  const meters = form.heightCm / 100
  return Number((form.weightKg / (meters * meters)).toFixed(2))
})

const bmiStatus = computed(() => {
  if (bmi.value < 18.5) return '偏瘦'
  if (bmi.value >= 24) return '偏高'
  return '正常'
})

const calorieTarget = computed(() => {
  if (previewProfile.dailyCalorieTarget) return previewProfile.dailyCalorieTarget
  const base = form.gender === '女' ? 1450 : 1700
  const offset = form.dietGoal === 'FAT_LOSS' ? -120 : form.dietGoal === 'MUSCLE_GAIN' ? 220 : 0
  return base + offset
})

const preferenceCount = computed(
  () => form.tastePreferences.length + form.avoidIngredients.length + form.allergyIngredients.length,
)

const completion = computed(() => {
  const points = [
    form.heightCm,
    form.weightKg,
    form.age,
    form.gender,
    form.dietGoal,
    form.cookingTimePreference,
    form.tastePreferences.length > 0,
  ].filter(Boolean).length
  return Math.round((points / 7) * 100)
})

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
    <section class="profile-hero">
      <div class="hero-copy">
        <p class="sz-chip"><HeartPulse :size="15" /> 健康档案</p>
        <h1>让推荐更懂你的身体</h1>
        <p>校准身体数据、饮食目标和口味限制后，膳哉会把每一次推荐算得更贴近你的日常。</p>
        <div class="hero-note">
          <CheckCircle2 :size="16" />
          更新档案后会同步影响智能推荐、购物清单和健康提示
        </div>
      </div>

      <div class="hero-metrics">
        <article class="metric-card">
          <span class="metric-icon"><Scale :size="20" /></span>
          <small>BMI</small>
          <strong>{{ bmi }}</strong>
          <em>{{ bmiStatus }}</em>
        </article>
        <article class="metric-card">
          <span class="metric-icon is-warm"><Flame :size="20" /></span>
          <small>目标热量</small>
          <strong>{{ calorieTarget }}</strong>
          <em>kcal / 日</em>
        </article>
        <article class="metric-card">
          <span class="metric-icon is-red"><SlidersHorizontal :size="20" /></span>
          <small>推荐目标</small>
          <strong>{{ goalLabels[form.dietGoal] }}</strong>
          <em>{{ preferenceCount }} 个偏好</em>
        </article>
      </div>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>

    <section class="profile-grid">
      <form class="profile-form sz-panel" @submit.prevent="save">
        <n-skeleton v-if="loading" text :repeat="4" />

        <div class="form-section">
          <div class="section-heading">
            <div>
              <p class="sz-chip">身体参数</p>
              <h2>先校准基础数据</h2>
            </div>
            <span>用于 BMI 和热量估算</span>
          </div>

          <div class="field-grid">
            <article class="field-card">
              <span><UserRound :size="17" /> 身高</span>
              <n-input-number v-model:value="form.heightCm" :min="120" :max="230">
                <template #suffix>cm</template>
              </n-input-number>
            </article>
            <article class="field-card">
              <span><Scale :size="17" /> 体重</span>
              <n-input-number v-model:value="form.weightKg" :min="30" :max="180">
                <template #suffix>kg</template>
              </n-input-number>
            </article>
            <article class="field-card">
              <span><HeartPulse :size="17" /> 年龄</span>
              <n-input-number v-model:value="form.age" :min="12" :max="90">
                <template #suffix>岁</template>
              </n-input-number>
            </article>
            <article class="field-card">
              <span><ShieldCheck :size="17" /> 性别</span>
              <div class="gender-toggle" role="radiogroup" aria-label="性别">
                <button
                  v-for="gender in genderOptions"
                  :key="gender"
                  type="button"
                  role="radio"
                  :aria-checked="form.gender === gender"
                  :class="{ active: form.gender === gender }"
                  @click="form.gender = gender"
                >
                  {{ gender }}
                </button>
              </div>
            </article>
            <article class="field-card is-wide">
              <span><Clock3 :size="17" /> 烹饪时间偏好</span>
              <n-input-number v-model:value="form.cookingTimePreference" :min="10" :max="120">
                <template #suffix>分钟</template>
              </n-input-number>
            </article>
          </div>
        </div>

        <div class="form-section">
          <div class="section-heading">
            <div>
              <p class="sz-chip is-warm">饮食目标</p>
              <h2>选择这阶段的身体方向</h2>
            </div>
            <span>{{ goalLabels[form.dietGoal] }}</span>
          </div>
          <GoalSegment v-model="form.dietGoal" />
        </div>

        <div class="form-section">
          <div class="section-heading">
            <div>
              <p class="sz-chip">口味限制</p>
              <h2>告诉膳哉什么适合你</h2>
            </div>
            <span>{{ preferenceCount }} 项偏好</span>
          </div>
          <div class="tag-grid">
            <IngredientTagInput v-model="form.tastePreferences" label="口味偏好" />
            <IngredientTagInput v-model="form.avoidIngredients" label="忌口/过滤食材" />
            <IngredientTagInput v-model="form.allergyIngredients" label="过敏食材" />
          </div>
        </div>

        <button class="save-button" type="submit" :disabled="saving">
          <Save :size="18" />
          {{ saving ? '正在保存健康档案' : '保存档案' }}
        </button>
      </form>

      <aside class="insight-column">
        <HealthSummaryCard :profile="liveProfile" />
        <article class="completion-card">
          <div>
            <p class="sz-chip">档案完整度</p>
            <strong>{{ completion }}%</strong>
          </div>
          <div class="progress-track">
            <span :style="{ width: `${completion}%` }" />
          </div>
          <p>至少补充身体参数、饮食目标和口味偏好，推荐会更稳定。</p>
        </article>
        <article class="tip-card">
          <p class="sz-chip is-warm"><ChefHat :size="15" /> 推荐逻辑</p>
          <h3>档案会如何影响推荐？</h3>
          <div class="tip-list">
            <span><ShieldCheck :size="16" /> 过滤忌口和过敏食材</span>
            <span><Flame :size="16" /> 调整热量和蛋白质权重</span>
            <span><Clock3 :size="16" /> 匹配你的可接受烹饪时间</span>
          </div>
        </article>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.profile-view {
  display: grid;
  gap: 22px;
}

.profile-hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 38px;
  align-items: center;
  min-height: 268px;
  padding: 42px 52px;
  border: 1px solid rgba(184, 220, 199, 0.82);
  border-radius: 30px;
  background:
    radial-gradient(circle at 9% 64%, rgba(72, 168, 106, 0.16) 0 88px, transparent 90px),
    linear-gradient(105deg, #dceee4 0%, #edf6e9 55%, #f8f1e7 100%);
  box-shadow: var(--sz-shadow-soft);
}

.profile-hero::after {
  content: "";
  position: absolute;
  right: 34px;
  bottom: 24px;
  width: 126px;
  height: 126px;
  border: 1px solid rgba(35, 107, 75, 0.12);
  border-radius: 46% 54% 48% 52%;
  background: rgba(255, 250, 241, 0.24);
  transform: rotate(-8deg);
}

.hero-copy,
.hero-metrics {
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
  max-width: 680px;
  color: var(--sz-evergreen);
  font-size: clamp(38px, 5vw, 54px);
  line-height: 1.08;
  letter-spacing: 0;
}

.hero-copy {
  display: grid;
  justify-items: start;
  gap: 18px;
}

.hero-copy > p:not(.sz-chip) {
  max-width: 650px;
  color: var(--sz-muted);
  font-size: 17px;
  line-height: 1.9;
}

.hero-note {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 6px 12px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: rgba(255, 250, 241, 0.72);
  font-weight: 800;
}

.hero-metrics {
  display: grid;
  gap: 14px;
}

.metric-card {
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

.metric-icon {
  grid-row: span 2;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: var(--sz-green-dark);
  background: var(--sz-mint);
}

.metric-icon.is-warm {
  color: #b16b18;
  background: var(--sz-grain-soft);
}

.metric-icon.is-red {
  color: var(--sz-tomato);
  background: var(--sz-tomato-soft);
}

.metric-card small {
  color: var(--sz-muted);
  font-weight: 800;
}

.metric-card strong {
  color: var(--sz-evergreen);
  font-size: 24px;
}

.metric-card em {
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

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  align-items: start;
}

.profile-form {
  display: grid;
  gap: 28px;
  padding: 28px;
}

.form-section {
  display: grid;
  gap: 16px;
}

.section-heading {
  display: flex;
  gap: 16px;
  align-items: start;
  justify-content: space-between;
}

.section-heading > div {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.section-heading > span {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 6px 12px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 900;
}

h2 {
  color: var(--sz-evergreen);
  font-size: 24px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.field-card {
  display: grid;
  gap: 10px;
  min-height: 112px;
  padding: 14px;
  border: 1px solid rgba(223, 210, 191, 0.88);
  border-radius: 16px;
  background: rgba(255, 253, 247, 0.86);
}

.field-card.is-wide {
  grid-column: span 2;
}

.field-card > span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--sz-text);
  font-size: 14px;
  font-weight: 900;
}

.gender-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  width: 100%;
  min-height: 34px;
  padding: 3px;
  border: 1px solid var(--sz-line);
  border-radius: 10px;
  background: #f8f1e7;
}

.gender-toggle button {
  border: 0;
  border-radius: 8px;
  color: var(--sz-muted);
  background: transparent;
  font-weight: 800;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.gender-toggle button.active {
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 6px 12px rgba(35, 107, 75, 0.18);
}

.tag-grid {
  display: grid;
  gap: 16px;
}

.save-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 52px;
  width: 100%;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 12px 22px rgba(35, 107, 75, 0.2);
  font-weight: 900;
  cursor: pointer;
  transition:
    background 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.save-button:hover:not(:disabled) {
  background: var(--sz-green);
  box-shadow: 0 14px 28px rgba(35, 107, 75, 0.24);
}

.save-button:active:not(:disabled) {
  transform: translateY(1px) scale(0.98);
}

.save-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.insight-column {
  display: grid;
  gap: 14px;
}

.completion-card,
.tip-card {
  padding: 20px;
  border: 1px solid rgba(223, 210, 191, 0.92);
  border-radius: 18px;
  background: rgba(255, 250, 241, 0.94);
  box-shadow: var(--sz-shadow-soft);
}

.completion-card {
  display: grid;
  gap: 14px;
}

.completion-card > div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.completion-card strong {
  color: var(--sz-evergreen);
  font-size: 28px;
}

.completion-card p {
  color: var(--sz-muted);
  line-height: 1.7;
}

.progress-track {
  overflow: hidden;
  height: 10px;
  border-radius: var(--sz-radius-pill);
  background: var(--sz-surface-soft);
}

.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--sz-green-dark), var(--sz-green-fresh));
}

.tip-card {
  display: grid;
  justify-items: start;
  gap: 14px;
}

.tip-card h3 {
  color: var(--sz-evergreen);
  font-size: 20px;
}

.tip-list {
  display: grid;
  gap: 10px;
  width: 100%;
}

.tip-list span {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 38px;
  padding: 9px 10px;
  border: 1px solid rgba(223, 210, 191, 0.74);
  border-radius: 12px;
  color: var(--sz-text);
  background: rgba(255, 253, 247, 0.76);
  font-weight: 800;
}

@media (max-width: 960px) {
  .profile-hero {
    grid-template-columns: 1fr;
    padding: 32px;
  }

  .hero-metrics,
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .field-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .profile-hero {
    padding: 26px 20px;
    border-radius: 24px;
  }

  .section-heading {
    display: grid;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .field-card.is-wide {
    grid-column: auto;
  }

  .metric-card {
    grid-template-columns: 44px 1fr;
  }

  .metric-card em {
    grid-column: 2;
    grid-row: auto;
    justify-self: start;
  }
}
</style>
