<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  placeholder?: string
}>()

const tags = defineModel<string[]>({ required: true })
const labelId = computed(() => `tag-field-${props.label.replace(/\s+/g, '-')}`)
</script>

<template>
  <div class="tag-field">
    <span :id="labelId" class="tag-field-label">{{ label }}</span>
    <n-dynamic-tags
      v-model:value="tags"
      :aria-labelledby="labelId"
      :input-props="{ placeholder: placeholder ?? '输入后回车添加', 'aria-label': `${label}，输入后回车添加` }"
    />
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
</style>
