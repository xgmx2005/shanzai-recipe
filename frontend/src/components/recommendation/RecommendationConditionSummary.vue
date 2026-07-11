<script setup lang="ts">
import { computed } from 'vue'
import { AlertTriangle, CheckCircle2, Clock3, ShieldAlert, Sparkles, UsersRound } from '@lucide/vue'
import type { ConversationStatus, DietGoal, RecommendationConversationContext } from '@/types'

const props = defineProps<{
  context: RecommendationConversationContext
  status: ConversationStatus
  showConfirmation: boolean
  confirming?: boolean
}>()

const emit = defineEmits<{
  (event: 'confirm'): void
  (event: 'edit'): void
}>()

const goalLabels: Record<DietGoal, string> = {
  FAT_LOSS: '减脂控热量',
  BALANCED: '日常均衡',
  MUSCLE_GAIN: '健身增肌',
}

const ingredientNames = computed(() => props.context.availableIngredients.map((item) => item.name))
const canConfirm = computed(() => props.showConfirmation && props.status === 'READY_TO_CONFIRM' && props.context.conflicts.length === 0)
</script>

<template>
  <aside class="condition-summary">
    <div class="summary-head">
      <p class="sz-chip"><Sparkles :size="15" /> 已理解条件</p>
      <button type="button" @click="emit('edit')">修改条件</button>
    </div>

    <div class="summary-grid">
      <article>
        <span><CheckCircle2 :size="17" /> 饮食目标</span>
        <strong>{{ context.dietGoal ? goalLabels[context.dietGoal] : '待确认' }}</strong>
      </article>
      <article>
        <span><Clock3 :size="17" /> 烹饪时间</span>
        <strong>{{ context.cookingTime ? `${context.cookingTime} 分钟内` : '待确认' }}</strong>
      </article>
      <article>
        <span><UsersRound :size="17" /> 用餐人数</span>
        <strong>{{ context.servings ? `${context.servings} 人` : '待确认' }}</strong>
      </article>
    </div>

    <section>
      <span class="section-label">已有食材</span>
      <div class="tag-list">
        <span v-for="name in ingredientNames" :key="`in-${name}`">{{ name }}</span>
        <strong v-if="ingredientNames.length === 0">还没有识别到明确食材</strong>
      </div>
    </section>

    <section>
      <span class="section-label">忌口与排除</span>
      <div class="tag-list">
        <span v-for="name in context.excludedIngredients" :key="`out-${name}`">{{ name }}</span>
        <span v-for="name in context.allergyIngredients" :key="`allergy-${name}`" class="is-danger">{{ name }}</span>
        <strong v-if="context.excludedIngredients.length === 0 && context.allergyIngredients.length === 0">无明确限制</strong>
      </div>
    </section>

    <section v-if="context.unknownTerms.length || context.conflicts.length" class="warning-box">
      <AlertTriangle :size="17" />
      <div>
        <p v-if="context.unknownTerms.length">还没理解：{{ context.unknownTerms.join('、') }}</p>
        <p v-if="context.conflicts.length">需要修正：{{ context.conflicts.join('、') }}</p>
      </div>
    </section>

    <button class="confirm-button" type="button" :disabled="!canConfirm || confirming" @click="emit('confirm')">
      <ShieldAlert v-if="!canConfirm" :size="17" />
      <CheckCircle2 v-else :size="17" />
      {{ confirming ? '正在生成推荐' : canConfirm ? '确认并生成推荐' : '继续补充条件后确认' }}
    </button>
  </aside>
</template>

<style scoped>
.condition-summary {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(223, 210, 191, 0.9);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 250, 241, 0.98), rgba(251, 247, 239, 0.94));
}

.summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.summary-head button {
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(35, 107, 75, 0.16);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: #fffdf7;
  font-weight: 900;
  cursor: pointer;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.summary-grid article {
  display: grid;
  gap: 7px;
  min-height: 78px;
  padding: 11px;
  border: 1px solid rgba(223, 210, 191, 0.76);
  border-radius: 13px;
  background: rgba(255, 253, 247, 0.82);
}

.summary-grid span,
.section-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 900;
}

.summary-grid strong {
  color: var(--sz-evergreen);
  line-height: 1.35;
}

section {
  display: grid;
  gap: 8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.tag-list span,
.tag-list strong {
  min-height: 30px;
  padding: 5px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 850;
}

.tag-list span.is-danger {
  color: #9b3f2d;
  background: var(--sz-tomato-soft);
}

.tag-list strong {
  color: var(--sz-muted);
  background: rgba(255, 253, 247, 0.9);
}

.warning-box {
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: start;
  padding: 11px;
  border: 1px solid rgba(230, 91, 62, 0.18);
  border-radius: 13px;
  color: #9b3f2d;
  background: rgba(248, 222, 213, 0.56);
}

.warning-box p {
  margin: 0;
  line-height: 1.65;
}

.confirm-button {
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
}

.confirm-button:disabled {
  color: var(--sz-muted);
  background: var(--sz-surface-soft);
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .summary-head {
    display: grid;
    justify-items: start;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
