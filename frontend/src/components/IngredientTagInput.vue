<script setup lang="ts">
defineProps<{
  label: string
  placeholder?: string
}>()

const tags = defineModel<string[]>({ required: true })
</script>

<template>
  <label class="tag-field">
    <span>{{ label }}</span>
    <n-dynamic-tags v-model:value="tags" :input-props="{ placeholder: placeholder ?? '输入后回车添加' }" />
  </label>
</template>

<style scoped>
.tag-field {
  display: grid;
  gap: 10px;
}

.tag-field > span {
  color: var(--sz-text);
  font-size: 14px;
  font-weight: 700;
}
</style>
