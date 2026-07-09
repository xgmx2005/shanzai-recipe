<script setup lang="ts">
import { computed } from 'vue'
import { DEFAULT_INGREDIENT_ICON, replaceImageWithFallback, resolveIngredientIcon } from '@/utils/assets'

const props = withDefaults(defineProps<{
  name?: string
  size?: number
}>(), {
  size: 36,
})

const iconUrl = computed(() => resolveIngredientIcon(props.name))
</script>

<template>
  <span class="ingredient-icon" :style="{ width: `${size}px`, height: `${size}px` }">
    <img
      :src="iconUrl"
      :alt="name || '食材图标'"
      @error="replaceImageWithFallback($event, DEFAULT_INGREDIENT_ICON)"
    />
  </span>
</template>

<style scoped>
.ingredient-icon {
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(201, 221, 205, 0.92);
  border-radius: 12px;
  background: rgba(237, 247, 238, 0.96);
}

.ingredient-icon img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}
</style>
