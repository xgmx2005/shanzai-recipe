<script setup lang="ts">
import { computed } from 'vue'
import type { Profile } from '@/types'

const props = defineProps<{
  profile: Profile
  compact?: boolean
}>()

const bmi = computed(() => {
  if (props.profile.bmi) return props.profile.bmi
  const meters = props.profile.heightCm / 100
  return Number((props.profile.weightKg / (meters * meters)).toFixed(2))
})

const status = computed(() => {
  if (bmi.value < 18.5) return '偏瘦'
  if (bmi.value >= 24) return '偏高'
  return '正常'
})

const calories = computed(() => {
  if (props.profile.dailyCalorieTarget) return props.profile.dailyCalorieTarget
  const base = props.profile.gender === '女' ? 1450 : 1700
  const goalOffset = props.profile.dietGoal === 'FAT_LOSS' ? -120 : props.profile.dietGoal === 'MUSCLE_GAIN' ? 220 : 0
  return base + goalOffset
})
</script>

<template>
  <article class="summary-card" :class="{ compact }">
    <div class="summary-head">
      <div>
        <span class="label">BMI</span>
        <strong>{{ bmi }}</strong>
      </div>
      <em>{{ status }}</em>
    </div>
    <div class="calorie-row">
      <span>每日目标热量</span>
      <strong>{{ calories }} <small>kcal</small></strong>
    </div>
    <p>膳哉会结合你的饮食目标、BMI 和热量范围，调整菜谱推荐优先级。</p>
  </article>
</template>

<style scoped>
.summary-card {
  display: grid;
  gap: 16px;
  min-height: 168px;
  padding: 20px;
  border: 1px solid rgba(189, 229, 204, 0.92);
  border-radius: 18px;
  background:
    radial-gradient(circle at 100% 0, rgba(72, 168, 106, 0.12), transparent 92px),
    linear-gradient(135deg, #eff9f1, #fffdf7);
  box-shadow: var(--sz-shadow-soft);
}

.summary-card.compact {
  min-height: 116px;
}

.summary-head,
.calorie-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.summary-head > div {
  display: grid;
  gap: 5px;
}

.label {
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 800;
}

strong {
  color: var(--sz-evergreen);
  font-size: 34px;
  line-height: 1;
}

em {
  padding: 5px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.calorie-row {
  min-height: 58px;
  padding: 12px;
  border: 1px solid rgba(223, 210, 191, 0.74);
  border-radius: 14px;
  background: rgba(255, 253, 247, 0.78);
}

.calorie-row span {
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 800;
}

.calorie-row strong {
  font-size: 24px;
}

.calorie-row small {
  color: var(--sz-muted);
  font-size: 12px;
}

p {
  margin: 0;
  color: var(--sz-muted);
  line-height: 1.7;
}
</style>
