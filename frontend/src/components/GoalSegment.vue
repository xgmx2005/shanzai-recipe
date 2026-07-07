<script setup lang="ts">
import type { DietGoal } from '@/types'

const model = defineModel<DietGoal>({ required: true })

const goals: Array<{ value: DietGoal; label: string; hint: string }> = [
  { value: 'FAT_LOSS', label: '减脂控热量', hint: '低热量 高蛋白' },
  { value: 'BALANCED', label: '日常健康', hint: '均衡 清爽' },
  { value: 'MUSCLE_GAIN', label: '健身增肌', hint: '高蛋白 饱腹' },
]
</script>

<template>
  <div class="goal-segment" role="radiogroup" aria-label="饮食目标">
    <button
      v-for="goal in goals"
      :key="goal.value"
      type="button"
      class="goal-option"
      :class="{ active: model === goal.value }"
      @click="model = goal.value"
    >
      <strong>{{ goal.label }}</strong>
      <span>{{ goal.hint }}</span>
    </button>
  </div>
</template>

<style scoped>
.goal-segment {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.goal-option {
  min-height: 70px;
  padding: 12px 14px;
  border: 1px solid var(--sz-line);
  border-radius: 16px;
  color: var(--sz-text);
  background: var(--sz-surface);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease;
}

.goal-option strong,
.goal-option span {
  display: block;
}

.goal-option span {
  margin-top: 6px;
  color: var(--sz-muted);
  font-size: 12px;
}

.goal-option.active {
  border-color: rgba(47, 158, 99, 0.55);
  color: var(--sz-deep-green);
  background: linear-gradient(135deg, #ecf8ef, #fffdf7);
}

.goal-option:active {
  transform: scale(0.98);
}

@media (max-width: 720px) {
  .goal-segment {
    grid-template-columns: 1fr;
  }
}
</style>
