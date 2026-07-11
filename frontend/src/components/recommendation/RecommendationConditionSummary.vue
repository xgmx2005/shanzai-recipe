<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { AlertTriangle, CheckCircle2, Clock3, Pencil, Save, ShieldAlert, Sparkles, UsersRound, X } from '@lucide/vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import type {
  ConversationContextPatchRequest,
  ConversationStatus,
  DietGoal,
  RecommendationConversationContext,
} from '@/types'

const props = defineProps<{
  context: RecommendationConversationContext
  status: ConversationStatus
  showConfirmation: boolean
  confirming?: boolean
  saving?: boolean
}>()

const emit = defineEmits<{
  (event: 'confirm'): void
  (event: 'save', payload: ConversationContextPatchRequest): void
}>()

const goalLabels: Record<DietGoal, string> = {
  FAT_LOSS: '减脂控热量',
  BALANCED: '日常均衡',
  MUSCLE_GAIN: '健身增肌',
}

const ingredientNames = computed(() => props.context.availableIngredients.map((item) => item.name))
const canConfirm = computed(() => props.showConfirmation && props.status === 'READY_TO_CONFIRM' && props.context.conflicts.length === 0)
const editing = ref(false)
const draft = reactive({
  dietGoal: 'BALANCED' as DietGoal,
  cookingTime: 30,
  servings: 1,
  availableIngredients: [] as string[],
  excludedIngredients: [] as string[],
  allergyIngredients: [] as string[],
})

watch(
  () => props.context,
  () => {
    if (!editing.value) syncDraft()
  },
  { immediate: true, deep: true },
)

function syncDraft() {
  draft.dietGoal = props.context.dietGoal ?? 'BALANCED'
  draft.cookingTime = props.context.cookingTime ?? 30
  draft.servings = props.context.servings ?? 1
  draft.availableIngredients = props.context.availableIngredients.map((item) => item.name)
  draft.excludedIngredients = [...props.context.excludedIngredients]
  draft.allergyIngredients = [...props.context.allergyIngredients]
}

function startEdit() {
  syncDraft()
  editing.value = true
}

function cancelEdit() {
  syncDraft()
  editing.value = false
}

function positiveInteger(value: number, fallback: number) {
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : fallback
}

function saveConditions() {
  emit('save', {
    dietGoal: draft.dietGoal,
    cookingTime: positiveInteger(draft.cookingTime, 30),
    servings: positiveInteger(draft.servings, 1),
    availableIngredients: draft.availableIngredients.map((name) => ({
      name,
      quantity: null,
      unit: null,
      quantityKnown: false,
    })),
    excludedIngredients: draft.excludedIngredients,
    allergyIngredients: draft.allergyIngredients,
  })
  editing.value = false
}
</script>

<template>
  <aside class="condition-summary compact-summary">
    <div class="summary-head">
      <p class="sz-chip"><Sparkles :size="15" /> 已理解条件</p>
      <button v-if="!editing" type="button" @click="startEdit">
        <Pencil :size="14" />
        修改条件
      </button>
    </div>

    <form v-if="editing" class="condition-editor" @submit.prevent="saveConditions">
      <label>
        <span>饮食目标</span>
        <select v-model="draft.dietGoal">
          <option value="FAT_LOSS">减脂控热量</option>
          <option value="BALANCED">日常均衡</option>
          <option value="MUSCLE_GAIN">健身增肌</option>
        </select>
      </label>

      <div class="editor-row">
        <label>
          <span>烹饪时间</span>
          <input v-model.number="draft.cookingTime" type="number" min="1" max="240">
        </label>
        <label>
          <span>用餐人数</span>
          <input v-model.number="draft.servings" type="number" min="1" max="20">
        </label>
      </div>

      <IngredientTagInput v-model="draft.availableIngredients" label="已有食材" placeholder="输入食材后回车" />
      <IngredientTagInput v-model="draft.excludedIngredients" label="忌口食材" placeholder="输入忌口后回车" />
      <IngredientTagInput v-model="draft.allergyIngredients" label="过敏食材" placeholder="输入过敏食材后回车" />

      <div class="editor-actions">
        <button type="button" class="ghost-button" :disabled="saving" @click="cancelEdit">
          <X :size="15" />
          取消
        </button>
        <button type="submit" class="save-button" :disabled="saving">
          <Save :size="15" />
          {{ saving ? '保存中' : '保存条件' }}
        </button>
      </div>
    </form>

    <div v-else class="summary-grid">
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

    <section v-if="!editing">
      <span class="section-label">已有食材</span>
      <div class="tag-list">
        <span v-for="name in ingredientNames" :key="`in-${name}`">{{ name }}</span>
        <strong v-if="ingredientNames.length === 0">还没有识别到明确食材</strong>
      </div>
    </section>

    <section v-if="!editing">
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

    <button v-if="!editing" class="confirm-button" type="button" :disabled="!canConfirm || confirming" @click="emit('confirm')">
      <ShieldAlert v-if="!canConfirm" :size="17" />
      <CheckCircle2 v-else :size="17" />
      {{ confirming ? '正在生成推荐' : canConfirm ? '确认并生成推荐' : '继续补充条件后确认' }}
    </button>
  </aside>
</template>

<style scoped>
.condition-summary {
  position: sticky;
  top: 92px;
  display: grid;
  gap: 10px;
  padding: 13px;
  border: 1px solid rgba(223, 210, 191, 0.9);
  border-radius: 15px;
  background: linear-gradient(180deg, rgba(255, 250, 241, 0.98), rgba(251, 247, 239, 0.94));
  box-shadow: 0 10px 22px rgba(23, 37, 31, 0.05);
}

.summary-head {
  display: grid;
  gap: 8px;
  justify-items: start;
}

.summary-head button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid rgba(35, 107, 75, 0.16);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: #fffdf7;
  font-weight: 900;
  cursor: pointer;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.summary-grid article {
  display: grid;
  gap: 4px;
  min-height: 54px;
  padding: 8px 10px;
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
  font-size: 12px;
  font-weight: 900;
}

.summary-grid strong {
  color: var(--sz-evergreen);
  line-height: 1.35;
}

section {
  display: grid;
  gap: 6px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.tag-list span,
.tag-list strong {
  min-height: 24px;
  padding: 3px 8px;
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
  min-height: 38px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: var(--sz-green-dark);
  font-weight: 900;
  cursor: pointer;
}

.condition-editor {
  display: grid;
  gap: 13px;
  padding: 12px;
  border: 1px solid rgba(35, 107, 75, 0.12);
  border-radius: 14px;
  background: rgba(255, 253, 247, 0.74);
}

.condition-editor label {
  display: grid;
  gap: 7px;
}

.condition-editor label > span {
  color: var(--sz-text);
  font-size: 13px;
  font-weight: 850;
}

.condition-editor select,
.condition-editor input {
  width: 100%;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid var(--sz-line);
  border-radius: 11px;
  color: var(--sz-text);
  background: #fffdf7;
  font: inherit;
  outline: 0;
}

.condition-editor select:focus,
.condition-editor input:focus {
  border-color: var(--sz-green);
  box-shadow: 0 0 0 3px rgba(72, 168, 106, 0.13);
}

.editor-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
}

.editor-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
}

.ghost-button,
.save-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 36px;
  border-radius: 11px;
  font-weight: 900;
  cursor: pointer;
}

.ghost-button {
  border: 1px solid rgba(35, 107, 75, 0.16);
  color: var(--sz-deep-green);
  background: #fffdf7;
}

.save-button {
  border: 0;
  color: #ffffff;
  background: var(--sz-green-dark);
}

.ghost-button:disabled,
.save-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.confirm-button:disabled {
  color: var(--sz-muted);
  background: var(--sz-surface-soft);
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .condition-summary {
    position: static;
  }

  .summary-head {
    display: grid;
    justify-items: start;
  }
}
</style>
