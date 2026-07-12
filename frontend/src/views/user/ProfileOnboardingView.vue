<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, CheckCircle2, Clock3, HeartPulse, ShieldCheck, Sparkles } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import GoalSegment from '@/components/GoalSegment.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import { useAuthStore } from '@/stores/auth'
import type { ProfileRequest } from '@/types'

const router = useRouter()
const message = useMessage()
const auth = useAuthStore()
const currentStep = ref(0)
const loading = ref(false)
const error = ref('')

const steps = [
  {
    title: '基础身体信息',
    text: '用于估算 BMI 和每日热量范围，帮助推荐更贴合你的身体状态。',
  },
  {
    title: '饮食目标',
    text: '饮食目标会影响推荐排序，烹饪时间会过滤不适合当前时间的菜谱。',
  },
  {
    title: '口味与限制',
    text: '忌口和过敏会优先过滤，口味偏好会帮助 AI 更准确解释推荐理由。',
  },
]

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
  profileCompleted: false,
})

const progress = computed(() => Math.round(((currentStep.value + 1) / steps.length) * 100))
const activeStep = computed(() => steps[currentStep.value])

function validateCurrentStep() {
  if (currentStep.value === 0) {
    if (!form.heightCm || !form.weightKg || !form.age || !form.gender) {
      throw new Error('请先补全身高、体重、年龄和性别')
    }
  }
  if (currentStep.value === 1) {
    if (!form.dietGoal || !form.cookingTimePreference) {
      throw new Error('请选择饮食目标和烹饪时间')
    }
  }
}

function nextStep() {
  error.value = ''
  try {
    validateCurrentStep()
    currentStep.value = Math.min(currentStep.value + 1, steps.length - 1)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '请补全当前步骤'
    message.warning(error.value)
  }
}

function previousStep() {
  error.value = ''
  currentStep.value = Math.max(currentStep.value - 1, 0)
}

async function skip() {
  await router.push('/user/home')
}

async function finish() {
  error.value = ''
  loading.value = true
  try {
    await auth.saveProfile({ ...form, profileCompleted: true })
    message.success('健康档案已完成')
    await router.push('/user/home')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '健康档案保存失败'
    message.error(error.value)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const profile = await auth.loadProfile()
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
      profileCompleted: profile.profileCompleted,
    })
  } catch {
    message.warning('暂时使用默认档案，你可以先完成引导')
  }
})
</script>

<template>
  <div class="profile-onboarding">
    <section class="onboarding-shell">
      <aside class="onboarding-story">
        <p class="sz-chip"><Sparkles :size="15" /> 新用户引导</p>
        <h1>先让膳哉认识你的饮食节奏</h1>
        <p>三步完成基础档案。之后智能推荐会结合目标热量、忌口、口味和可接受烹饪时间，不再只是随机给菜。</p>

        <div class="step-track">
          <button
            v-for="(step, index) in steps"
            :key="step.title"
            type="button"
            :class="{ active: currentStep === index, done: currentStep > index }"
            @click="currentStep = index"
          >
            <span>{{ index + 1 }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <small>{{ step.text }}</small>
            </div>
          </button>
        </div>
      </aside>

      <section class="onboarding-card">
        <div class="progress-head">
          <span>{{ currentStep + 1 }} / {{ steps.length }}</span>
          <strong>{{ activeStep.title }}</strong>
          <div class="progress-line"><i :style="{ width: `${progress}%` }" /></div>
        </div>

        <n-alert v-if="error" type="warning" :bordered="false">{{ error }}</n-alert>

        <div v-if="currentStep === 0" class="step-form">
          <p class="step-note"><HeartPulse :size="17" /> {{ activeStep.text }}</p>
          <div class="field-grid">
            <label>
              <span>身高</span>
              <n-input-number v-model:value="form.heightCm" :min="120" :max="230">
                <template #suffix>cm</template>
              </n-input-number>
            </label>
            <label>
              <span>体重</span>
              <n-input-number v-model:value="form.weightKg" :min="30" :max="180">
                <template #suffix>kg</template>
              </n-input-number>
            </label>
            <label>
              <span>年龄</span>
              <n-input-number v-model:value="form.age" :min="12" :max="90">
                <template #suffix>岁</template>
              </n-input-number>
            </label>
            <label>
              <span>性别</span>
              <div class="gender-toggle" role="radiogroup" aria-label="性别">
                <button type="button" :class="{ active: form.gender === '女' }" @click="form.gender = '女'">女</button>
                <button type="button" :class="{ active: form.gender === '男' }" @click="form.gender = '男'">男</button>
              </div>
            </label>
          </div>
        </div>

        <div v-else-if="currentStep === 1" class="step-form">
          <p class="step-note"><Clock3 :size="17" /> {{ activeStep.text }}</p>
          <GoalSegment v-model="form.dietGoal" />
          <label class="time-field">
            <span>通常希望多久做好一餐</span>
            <n-input-number v-model:value="form.cookingTimePreference" :min="10" :max="120">
              <template #suffix>分钟</template>
            </n-input-number>
          </label>
        </div>

        <div v-else class="step-form">
          <p class="step-note"><ShieldCheck :size="17" /> {{ activeStep.text }}</p>
          <div class="quick-tags">
            <button type="button" @click="form.tastePreferences = ['清淡', '高蛋白', '少油']">想吃清淡高蛋白</button>
            <button type="button" @click="form.avoidIngredients = ['辣椒']">不要辣</button>
            <button type="button" @click="form.tastePreferences = ['家常', '盖饭']">推荐家常菜</button>
          </div>
          <IngredientTagInput v-model="form.tastePreferences" label="口味偏好" />
          <IngredientTagInput v-model="form.avoidIngredients" label="忌口/过滤食材" />
          <IngredientTagInput v-model="form.allergyIngredients" label="过敏食材" />
        </div>

        <footer class="onboarding-actions">
          <button class="ghost-action" type="button" @click="skip">稍后完善</button>
          <button v-if="currentStep > 0" class="secondary-action" type="button" @click="previousStep">
            <ArrowLeft :size="17" />
            上一步
          </button>
          <button v-if="currentStep < steps.length - 1" class="primary-action" type="button" @click="nextStep">
            下一步
            <ArrowRight :size="17" />
          </button>
          <button v-else class="primary-action" type="button" :disabled="loading" @click="finish">
            <CheckCircle2 :size="17" />
            {{ loading ? '正在保存' : '保存并开始使用' }}
          </button>
        </footer>
      </section>
    </section>
  </div>
</template>

<style scoped>
.profile-onboarding {
  display: grid;
  min-height: calc(100vh - 140px);
  place-items: center;
  padding: 18px;
}

.onboarding-shell {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(420px, 1.1fr);
  gap: 18px;
  width: min(1120px, 100%);
}

.onboarding-story,
.onboarding-card {
  border: 1px solid rgba(223, 210, 191, 0.88);
  border-radius: 24px;
  background: rgba(255, 253, 247, 0.94);
  box-shadow: var(--sz-shadow-soft);
}

.onboarding-story {
  display: grid;
  align-content: start;
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at 18% 22%, rgba(72, 168, 106, 0.16), transparent 32%),
    linear-gradient(135deg, rgba(220, 239, 228, 0.94), rgba(255, 253, 247, 0.96));
}

.onboarding-story h1,
.progress-head strong {
  margin: 0;
  color: var(--sz-evergreen);
  letter-spacing: 0;
}

.onboarding-story h1 {
  font-size: 36px;
  line-height: 1.16;
}

.onboarding-story p {
  margin: 0;
  color: var(--sz-text);
  line-height: 1.8;
}

.step-track {
  display: grid;
  gap: 10px;
  margin-top: 8px;
}

.step-track button {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 12px;
  padding: 13px;
  border: 1px solid rgba(223, 210, 191, 0.76);
  border-radius: 16px;
  color: var(--sz-text);
  background: rgba(255, 253, 247, 0.72);
  cursor: pointer;
  text-align: left;
}

.step-track button.active {
  border-color: rgba(35, 107, 75, 0.36);
  background: rgba(220, 239, 228, 0.82);
}

.step-track button.done span,
.step-track button.active span {
  color: #ffffff;
  background: var(--sz-green-dark);
}

.step-track span {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-weight: 900;
}

.step-track strong,
label > span {
  color: var(--sz-evergreen);
  font-weight: 900;
}

.step-track small {
  display: block;
  margin-top: 4px;
  color: var(--sz-muted);
  line-height: 1.55;
}

.onboarding-card {
  display: grid;
  gap: 18px;
  min-height: 620px;
  padding: 28px;
}

.progress-head {
  display: grid;
  gap: 9px;
}

.progress-head > span {
  color: var(--sz-muted);
  font-weight: 900;
}

.progress-head strong {
  font-size: 28px;
}

.progress-line {
  overflow: hidden;
  height: 8px;
  border-radius: var(--sz-radius-pill);
  background: var(--sz-surface-soft);
}

.progress-line i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--sz-green-dark), var(--sz-green-fresh));
  transition: width 0.24s ease;
}

.step-form {
  display: grid;
  align-content: start;
  gap: 16px;
}

.step-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  color: var(--sz-text);
  background: var(--sz-mint);
  line-height: 1.7;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

label,
.time-field {
  display: grid;
  gap: 9px;
  padding: 13px;
  border: 1px solid rgba(223, 210, 191, 0.82);
  border-radius: 16px;
  background: rgba(255, 250, 241, 0.78);
}

.gender-toggle {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--sz-line);
  border-radius: 12px;
  background: #f8f1e7;
}

.gender-toggle button {
  min-height: 34px;
  border: 0;
  border-radius: 9px;
  color: var(--sz-muted);
  background: transparent;
  font-weight: 900;
  cursor: pointer;
}

.gender-toggle button.active {
  color: #ffffff;
  background: var(--sz-green-dark);
}

.quick-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-tags button,
.ghost-action,
.secondary-action,
.primary-action {
  min-height: 40px;
  border-radius: 12px;
  font-weight: 900;
  cursor: pointer;
}

.quick-tags button,
.secondary-action {
  border: 1px solid rgba(35, 107, 75, 0.16);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
}

.onboarding-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: auto;
}

.ghost-action {
  margin-right: auto;
  border: 0;
  color: var(--sz-muted);
  background: transparent;
}

.secondary-action,
.primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
}

.primary-action {
  border: 0;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 12px 22px rgba(35, 107, 75, 0.18);
}

.primary-action:disabled {
  cursor: wait;
  opacity: 0.72;
}

@media (max-width: 900px) {
  .profile-onboarding {
    place-items: stretch;
    padding: 0;
  }

  .onboarding-shell,
  .field-grid {
    grid-template-columns: 1fr;
  }

  .onboarding-card {
    min-height: 0;
  }
}
</style>
