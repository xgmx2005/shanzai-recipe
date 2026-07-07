<script setup lang="ts">
import { Heart, Timer, Utensils } from '@lucide/vue'
import type { Recipe } from '@/types'

defineProps<{
  recipe: Recipe
  dense?: boolean
}>()
</script>

<template>
  <article class="recipe-card" :class="{ dense }">
    <div class="image-wrap">
      <img :src="recipe.imageUrl" :alt="recipe.name" />
      <span>{{ recipe.score }} 匹配</span>
    </div>
    <div class="recipe-body">
      <h3>{{ recipe.name }}</h3>
      <div class="metrics">
        <span><Utensils /> {{ recipe.calories }} kcal</span>
        <span><Timer /> {{ recipe.time }} 分钟</span>
      </div>
      <p v-if="!dense">{{ recipe.reason }}</p>
      <div class="tags">
        <span v-for="tag in recipe.tags.slice(0, dense ? 2 : 3)" :key="tag" class="sz-chip">
          {{ tag }}
        </span>
      </div>
      <div class="actions">
        <n-button size="small" secondary round type="primary">详情</n-button>
        <n-button size="small" quaternary circle aria-label="收藏">
          <template #icon>
            <n-icon><Heart /></n-icon>
          </template>
        </n-button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.recipe-card {
  overflow: hidden;
  border: 1px solid rgba(227, 218, 203, 0.9);
  border-radius: 18px;
  background: var(--sz-surface);
}

.image-wrap {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
}

img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.image-wrap span {
  position: absolute;
  left: 10px;
  bottom: 10px;
  padding: 4px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: rgba(223, 241, 230, 0.94);
  font-size: 12px;
  font-weight: 800;
}

.recipe-body {
  padding: 14px;
}

h3 {
  margin: 0;
  font-size: 16px;
  line-height: 1.45;
}

.metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: var(--sz-muted);
  font-size: 13px;
}

.metrics span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.metrics svg {
  width: 15px;
  height: 15px;
}

p {
  margin: 10px 0 0;
  color: var(--sz-text);
  font-size: 13px;
  line-height: 1.7;
}

.tags,
.actions {
  display: flex;
  align-items: center;
}

.tags {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.actions {
  justify-content: space-between;
  margin-top: 14px;
}
</style>
