<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { Plus, X } from '@lucide/vue'

const props = defineProps<{
  label: string
  placeholder?: string
}>()

const tags = defineModel<string[]>({ required: true })
const labelId = computed(() => `tag-field-${props.label.replace(/\s+/g, '-')}`)
const adding = ref(false)
const draft = ref('')
const inputRef = ref<HTMLInputElement | null>(null)

async function startAdd() {
  adding.value = true
  await nextTick()
  inputRef.value?.focus()
}

function cancelAdd() {
  adding.value = false
  draft.value = ''
}

function addTag() {
  const value = draft.value.trim()
  if (!value) {
    cancelAdd()
    return
  }
  if (!tags.value.includes(value)) {
    tags.value = [...tags.value, value]
  }
  draft.value = ''
  adding.value = false
}

function removeTag(value: string) {
  tags.value = tags.value.filter((tag) => tag !== value)
}
</script>

<template>
  <div class="tag-field">
    <span :id="labelId" class="tag-field-label">{{ label }}</span>
    <div class="tag-list" :aria-labelledby="labelId">
      <span v-for="tag in tags" :key="tag" class="tag-item">
        {{ tag }}
        <button type="button" :aria-label="`移除${tag}`" @click="removeTag(tag)">
          <X :size="13" />
        </button>
      </span>

      <input
        v-if="adding"
        ref="inputRef"
        v-model="draft"
        class="tag-input"
        :placeholder="placeholder ?? '输入后回车添加'"
        :aria-label="`${label}，输入后回车添加`"
        @keydown.enter.prevent="addTag"
        @keydown.esc.prevent="cancelAdd"
        @blur="addTag"
      >

      <button v-else type="button" class="add-tag" :aria-label="`添加${label}`" @click="startAdd">
        <Plus :size="15" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.tag-field {
  display: grid;
  gap: 10px;
}

.tag-field-label {
  color: var(--sz-text);
  font-size: 14px;
  font-weight: 700;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-height: 34px;
}

.tag-item,
.add-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  border-radius: var(--sz-radius-pill);
}

.tag-item {
  gap: 5px;
  padding: 0 4px 0 11px;
  border: 1px solid rgba(35, 107, 75, 0.14);
  color: var(--sz-deep-green);
  background: rgba(220, 239, 228, 0.74);
  font-size: 13px;
  font-weight: 800;
}

.tag-item button,
.add-tag {
  border: 0;
  cursor: pointer;
}

.tag-item button {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: var(--sz-muted);
  background: transparent;
}

.tag-item button:hover {
  color: var(--sz-deep-green);
  background: rgba(35, 107, 75, 0.1);
}

.add-tag {
  width: 32px;
  border: 1px dashed rgba(35, 107, 75, 0.38);
  color: var(--sz-deep-green);
  background: rgba(255, 253, 247, 0.82);
}

.add-tag:hover {
  border-color: var(--sz-green);
  background: var(--sz-mint);
}

.tag-input {
  width: min(180px, 100%);
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--sz-green);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-text);
  background: #fffdf7;
  outline: 0;
}
</style>
