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
    <div>
      <span class="label">BMI</span>
      <strong>{{ bmi }}</strong>
      <em>{{ status }}</em>
    </div>
    <p>每日目标热量约 {{ calories }} kcal，结合你的饮食目标进行推荐。</p>
  </article>
</template>

<style scoped>
.summary-card {
  min-height: 138px;
  padding: 18px;
  border: 1px solid rgba(189, 229, 204, 0.9);
  border-radius: 18px;
  background: linear-gradient(135deg, #eff9f1, #fffdf7);
}

.summary-card.compact {
  min-height: 116px;
}

.summary-card div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.label {
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 700;
}

strong {
  color: var(--sz-ink);
  font-size: 28px;
}

em {
  padding: 3px 9px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

p {
  margin: 14px 0 0;
  color: var(--sz-muted);
  line-height: 1.7;
}
</style>
