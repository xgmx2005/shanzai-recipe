<script setup lang="ts">
import { ref } from 'vue'
import { RefreshCw, SendHorizontal } from '@lucide/vue'

const props = defineProps<{
  loading?: boolean
  quickOptions: string[]
}>()

const emit = defineEmits<{
  (event: 'submit', content: string): void
  (event: 'restart'): void
}>()

const draft = ref('')

function submit(content = draft.value) {
  const normalized = content.trim()
  if (!normalized || props.loading) return
  emit('submit', normalized)
  draft.value = ''
}

function chooseOption(option: string) {
  if (option === '重新开始') {
    emit('restart')
    return
  }
  submit(option)
}
</script>

<template>
  <form class="composer" @submit.prevent="submit()">
    <label>
      <span>告诉膳哉你的想法</span>
      <textarea
        v-model="draft"
        rows="3"
        :disabled="loading"
        placeholder="例如：我有鸡蛋、番茄和牛肉，想吃健康一点，不要辣，最好 25 分钟内。"
        @keydown.enter.exact.prevent="submit()"
      />
    </label>

    <div class="composer-footer">
      <div v-if="quickOptions.length" class="quick-options">
        <button
          v-for="option in quickOptions"
          :key="option"
          type="button"
          :class="{ 'is-restart': option === '重新开始' }"
          :disabled="loading"
          @click="chooseOption(option)"
        >
          <RefreshCw v-if="option === '重新开始'" :size="14" />
          {{ option }}
        </button>
      </div>

      <button class="send-button" type="submit" :disabled="loading || !draft.trim()">
        {{ loading ? '正在理解' : '发送' }}
        <SendHorizontal :size="17" />
      </button>
    </div>
  </form>
</template>

<style scoped>
.composer {
  display: grid;
  gap: 13px;
  padding: 14px;
  border: 1px solid rgba(35, 107, 75, 0.14);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 253, 247, 0.98), rgba(246, 252, 247, 0.94));
  box-shadow: 0 12px 24px rgba(23, 37, 31, 0.06);
}

.composer-footer {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.quick-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-options button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(35, 107, 75, 0.14);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-weight: 800;
  cursor: pointer;
}

.quick-options button.is-restart {
  color: #9b3f2d;
  background: var(--sz-tomato-soft);
}

.quick-options button:disabled {
  cursor: wait;
  opacity: 0.68;
}

label {
  display: grid;
  gap: 9px;
}

label span {
  color: var(--sz-text);
  font-size: 14px;
  font-weight: 900;
}

textarea {
  width: 100%;
  min-height: 116px;
  resize: vertical;
  padding: 15px 16px;
  border: 1px solid var(--sz-line);
  border-radius: 16px;
  color: var(--sz-text);
  background: #fffdf7;
  font: inherit;
  line-height: 1.65;
  outline: none;
}

textarea:focus {
  border-color: var(--sz-green);
  box-shadow: 0 0 0 3px rgba(72, 168, 106, 0.15);
}

.send-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 120px;
  min-height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 10px 18px rgba(35, 107, 75, 0.18);
  font-weight: 900;
  cursor: pointer;
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

@media (max-width: 640px) {
  .composer-footer {
    display: grid;
  }

  .send-button {
    width: 100%;
  }
}
</style>
