<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import {
  CheckCircle2,
  ChefHat,
  Clock3,
  Flame,
  HeartPulse,
  ImageUp,
  Palette,
  Save,
  Scale,
  ShieldCheck,
  SlidersHorizontal,
  UserRound,
} from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { backendAssetUrl } from '@/api/http'
import GoalSegment from '@/components/GoalSegment.vue'
import HealthSummaryCard from '@/components/HealthSummaryCard.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import { useAuthStore } from '@/stores/auth'
import type { DietGoal, Profile, ProfileRequest } from '@/types'

const auth = useAuthStore()
const message = useMessage()
const saving = ref(false)
const accountSaving = ref(false)
const avatarUploading = ref(false)
const loading = ref(true)
const error = ref('')
const saveSuccess = ref(false)
const accountSaveSuccess = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)
let successNoticeTimer: ReturnType<typeof window.setTimeout> | null = null
const genderOptions = ['女', '男'] as const
const avatarOptions = [
  { label: '青叶', value: 'leaf' },
  { label: '薄荷', value: 'mint' },
  { label: '番茄', value: 'tomato' },
  { label: '谷物', value: 'grain' },
  { label: '湖蓝', value: 'blue' },
]
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

const accountForm = reactive({
  username: auth.user?.username ?? '',
  nickname: auth.user?.nickname ?? '',
  avatarTheme: auth.user?.avatarTheme ?? 'leaf',
  avatarUrl: auth.user?.avatarUrl ?? '',
})

const accountAvatarText = computed(() => (accountForm.nickname || accountForm.username || '膳').slice(0, 1))
const accountAvatarClass = computed(() => `theme-${accountForm.avatarTheme}`)
const accountAvatarUrl = computed(() => backendAssetUrl(accountForm.avatarUrl))

function syncAccountForm() {
  accountForm.username = auth.user?.username ?? ''
  accountForm.nickname = auth.user?.nickname ?? ''
  accountForm.avatarTheme = auth.user?.avatarTheme ?? 'leaf'
  accountForm.avatarUrl = auth.user?.avatarUrl ?? ''
}

function clearSuccessNoticeTimer() {
  if (!successNoticeTimer) return
  window.clearTimeout(successNoticeTimer)
  successNoticeTimer = null
}

function showSuccessNotice(type: 'account' | 'profile') {
  clearSuccessNoticeTimer()
  accountSaveSuccess.value = type === 'account'
  saveSuccess.value = type === 'profile'
  successNoticeTimer = window.setTimeout(() => {
    accountSaveSuccess.value = false
    saveSuccess.value = false
    successNoticeTimer = null
  }, 3000)
}

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
  saveSuccess.value = false
  saving.value = true
  try {
    const saved = await auth.saveProfile({ ...form, profileCompleted: completion.value >= 100 })
    Object.assign(previewProfile, saved)
    applyProfile(saved)
    showSuccessNotice('profile')
    message.success('健康档案已保存')
  } catch (err) {
    saveSuccess.value = false
    error.value = err instanceof Error ? err.message : '保存健康档案失败'
    message.error(error.value)
  } finally {
    saving.value = false
  }
}

async function saveAccount() {
  error.value = ''
  accountSaveSuccess.value = false
  const username = accountForm.username.trim()
  const nickname = accountForm.nickname.trim()
  if (!username || !nickname) {
    message.warning('用户名和显示名称不能为空')
    return
  }

  accountSaving.value = true
  try {
    await auth.updateAccount({
      username,
      nickname,
      avatarTheme: accountForm.avatarTheme,
    })
    syncAccountForm()
    showSuccessNotice('account')
    message.success('账号资料已保存')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存账号资料失败'
    message.error(error.value)
  } finally {
    accountSaving.value = false
  }
}

function chooseAvatar() {
  avatarInput.value?.click()
}

async function uploadAvatar(event: Event) {
  const input = event.target
  if (!(input instanceof HTMLInputElement)) return
  const file = input.files?.[0]
  if (!file) return

  avatarUploading.value = true
  accountSaveSuccess.value = false
  error.value = ''
  try {
    await auth.uploadAvatar(file)
    syncAccountForm()
    showSuccessNotice('account')
    message.success('头像已更新')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '上传头像失败'
    message.error(error.value)
  } finally {
    avatarUploading.value = false
    input.value = ''
  }
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    await auth.initSession()
    syncAccountForm()
    const profile = await auth.loadProfile()
    applyProfile(profile)
    Object.assign(previewProfile, profile)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '健康档案加载失败'
  } finally {
    loading.value = false
  }
})

onUnmounted(clearSuccessNoticeTimer)
</script>

<template>
  <div class="profile-view personal-diet-center">
    <section class="profile-command-center sz-panel">
      <div class="identity-stack">
        <span class="account-avatar is-large" :class="accountAvatarClass">
          <img v-if="accountAvatarUrl" :src="accountAvatarUrl" alt="当前头像" />
          <template v-else>{{ accountAvatarText }}</template>
        </span>
        <div>
          <p class="sz-chip"><HeartPulse :size="15" /> 个人饮食设置中心</p>
          <h1>{{ accountForm.nickname || '设置显示名称' }}</h1>
          <span>@{{ accountForm.username || 'username' }}</span>
          <p>推荐会使用这份档案：身体参数决定热量范围，饮食目标影响排序，忌口和过敏会参与安全过滤。</p>
          <button class="avatar-upload-button" type="button" :disabled="avatarUploading" @click="chooseAvatar">
            <ImageUp :size="16" />
            {{ avatarUploading ? '正在上传头像' : '上传头像' }}
          </button>
          <input
            ref="avatarInput"
            class="avatar-file-input"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            @change="uploadAvatar"
          />
        </div>
      </div>

      <div class="profile-summary-strip">
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
          <small>饮食目标</small>
          <strong>{{ goalLabels[form.dietGoal] }}</strong>
          <em>{{ preferenceCount }} 个偏好</em>
        </article>
        <article class="metric-card">
          <span class="metric-icon"><CheckCircle2 :size="20" /></span>
          <small>档案完整度</small>
          <strong>{{ completion }}%</strong>
          <em>实时预览</em>
        </article>
      </div>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <article v-if="accountSaveSuccess" class="save-success" role="status" aria-live="polite">
      <CheckCircle2 :size="22" />
      <div>
        <strong>账号资料已更新</strong>
        <span>右上角头像和对话头像会同步使用新的显示名称与头像主题。</span>
      </div>
    </article>
    <article v-if="saveSuccess" class="save-success" role="status" aria-live="polite">
      <CheckCircle2 :size="22" />
      <div>
        <strong>保存完成</strong>
        <span>健康档案已更新，新的推荐会使用这份最新信息。</span>
      </div>
    </article>

    <section class="profile-settings-layout">
      <div class="settings-main">
        <section class="account-panel sz-panel">
          <div class="section-heading">
            <div>
              <p class="sz-chip"><Palette :size="15" /> 身份与头像</p>
              <h2>同步右上角和对话头像</h2>
            </div>
            <span>账号资料</span>
          </div>
          <form class="account-form" @submit.prevent="saveAccount">
            <label>
              <span>用户名</span>
              <n-input v-model:value="accountForm.username" placeholder="用于登录和账号识别" />
            </label>
            <label>
              <span>显示名称</span>
              <n-input v-model:value="accountForm.nickname" placeholder="显示在右上角和对话头像中" />
            </label>
            <section class="avatar-picker">
              <span>默认头像主题</span>
              <div>
                <button
                  v-for="item in avatarOptions"
                  :key="item.value"
                  type="button"
                  :class="[`theme-${item.value}`, { active: accountForm.avatarTheme === item.value }]"
                  @click="accountForm.avatarTheme = item.value"
                >
                  {{ item.label }}
                </button>
              </div>
              <small>未上传图片时，会使用这里的主题色作为头像兜底。</small>
            </section>
            <button class="account-save-button" type="submit" :disabled="accountSaving">
              <Save :size="17" />
              {{ accountSaving ? '正在保存账号资料' : '保存账号资料' }}
            </button>
          </form>
        </section>

        <form class="profile-form sz-panel" @submit.prevent="save">
          <n-skeleton v-if="loading" text :repeat="4" />

          <div class="form-section">
            <div class="section-heading">
              <div>
                <p class="sz-chip">身体参数</p>
                <h2>校准基础数据</h2>
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
                <p class="sz-chip">推荐偏好配置</p>
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
            {{ saving ? '正在保存健康档案' : '保存档案并影响下一次推荐' }}
          </button>
        </form>
      </div>

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

.profile-command-center {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 520px;
  gap: 26px;
  align-items: center;
  padding: 28px;
  border: 1px solid rgba(184, 220, 199, 0.82);
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(220, 239, 228, 0.86), rgba(255, 253, 247, 0.96)),
    var(--sz-surface);
  box-shadow: var(--sz-shadow-soft);
}

.identity-stack,
.profile-summary-strip {
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
  font-size: clamp(34px, 4vw, 46px);
  line-height: 1.08;
  letter-spacing: 0;
}

.identity-stack {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.identity-stack > div {
  display: grid;
  justify-items: start;
  gap: 9px;
  min-width: 0;
}

.identity-stack span:not(.account-avatar) {
  color: var(--sz-muted);
  font-weight: 900;
}

.identity-stack p:not(.sz-chip) {
  max-width: 620px;
  color: var(--sz-text);
  line-height: 1.8;
}

.profile-summary-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
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

.profile-settings-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  align-items: start;
}

.settings-main {
  display: grid;
  gap: 18px;
}

.account-panel {
  display: grid;
  gap: 16px;
  padding: 24px;
}

.account-avatar {
  overflow: hidden;
  display: grid;
  box-sizing: border-box;
  flex: 0 0 auto;
  place-items: center;
  width: 76px;
  height: 76px;
  border: 1px solid rgba(255, 250, 241, 0.58);
  border-radius: 24px;
  background: rgba(255, 253, 247, 0.72);
  color: #ffffff;
  font-size: 30px;
  font-weight: 900;
  box-shadow: 0 16px 26px rgba(23, 37, 31, 0.16);
}

.account-avatar.is-large {
  width: 104px;
  height: 104px;
  border-radius: 30px;
  font-size: 38px;
}

.account-avatar img {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: inherit;
  object-fit: cover;
  object-position: center;
}

.avatar-upload-button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(35, 107, 75, 0.16);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-weight: 900;
  cursor: pointer;
  transition:
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.avatar-upload-button:hover:not(:disabled) {
  background: rgba(220, 239, 228, 0.92);
  box-shadow: 0 8px 18px rgba(35, 107, 75, 0.1);
  transform: translateY(-1px);
}

.avatar-upload-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.avatar-file-input {
  display: none;
}

.account-avatar.theme-leaf,
.avatar-picker button.theme-leaf::before {
  background: linear-gradient(135deg, #236b4b, #7fa05a);
}

.account-avatar.theme-mint,
.avatar-picker button.theme-mint::before {
  background: linear-gradient(135deg, #48a86a, #a9d8bc);
}

.account-avatar.theme-tomato,
.avatar-picker button.theme-tomato::before {
  background: linear-gradient(135deg, #d95d45, #ffb09d);
}

.account-avatar.theme-grain,
.avatar-picker button.theme-grain::before {
  background: linear-gradient(135deg, #b98945, #f0d494);
}

.account-avatar.theme-blue,
.avatar-picker button.theme-blue::before {
  background: linear-gradient(135deg, #2f6d7a, #9fc7cb);
}

.account-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.account-form label,
.avatar-picker {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(223, 210, 191, 0.86);
  border-radius: 16px;
  background: rgba(255, 253, 247, 0.86);
}

.account-form label > span,
.avatar-picker > span {
  color: var(--sz-text);
  font-size: 13px;
  font-weight: 900;
}

.avatar-picker {
  grid-column: 1 / -1;
}

.avatar-picker > div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.avatar-picker small {
  color: var(--sz-muted);
  line-height: 1.6;
}

.avatar-picker button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 6px 12px 6px 8px;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-text);
  background: rgba(255, 250, 241, 0.88);
  font-weight: 900;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.avatar-picker button::before {
  content: "";
  width: 18px;
  height: 18px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.52);
}

.avatar-picker button:hover {
  transform: translateY(-1px);
}

.avatar-picker button.active {
  border-color: rgba(35, 107, 75, 0.42);
  background: var(--sz-mint);
  box-shadow: 0 8px 18px rgba(35, 107, 75, 0.12);
}

.account-save-button {
  grid-column: 1 / -1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: var(--sz-green-dark);
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 12px 22px rgba(35, 107, 75, 0.18);
  transition:
    background 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.account-save-button:hover:not(:disabled) {
  background: var(--sz-green);
  box-shadow: 0 14px 28px rgba(35, 107, 75, 0.22);
}

.account-save-button:active:not(:disabled) {
  transform: translateY(1px) scale(0.99);
}

.account-save-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.save-success {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 64px;
  padding: 14px 18px;
  border: 1px solid rgba(72, 168, 106, 0.28);
  border-radius: 18px;
  color: var(--sz-deep-green);
  background: linear-gradient(135deg, rgba(220, 239, 228, 0.92), rgba(255, 250, 241, 0.96));
  box-shadow: var(--sz-shadow-soft);
}

.save-success svg {
  flex: 0 0 auto;
  color: var(--sz-green-dark);
}

.save-success div {
  display: grid;
  gap: 4px;
}

.save-success strong {
  color: var(--sz-evergreen);
  font-size: 18px;
}

.save-success span {
  color: var(--sz-text);
  font-weight: 700;
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
  position: sticky;
  top: 92px;
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
  .profile-command-center {
    grid-template-columns: 1fr;
    padding: 32px;
  }

  .account-panel,
  .profile-settings-layout {
    grid-template-columns: 1fr;
  }

  .insight-column {
    position: static;
  }

  .field-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .profile-command-center {
    padding: 26px 20px;
    border-radius: 24px;
  }

  .identity-stack {
    display: grid;
  }

  .section-heading {
    display: grid;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .account-form {
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

  .profile-summary-strip {
    grid-template-columns: 1fr;
  }
}
</style>
