<script setup lang="ts">
import { computed } from 'vue'
import { Heart, Timer, Utensils } from '@lucide/vue'
import type { RecipeCardModel } from '@/types'
import { replaceImageWithFallback, resolveRecipeImage, resolveRecipeImagePosition } from '@/utils/assets'

const props = defineProps<{
  recipe: RecipeCardModel
  dense?: boolean
  isFavorite?: boolean
  favoritePending?: boolean
}>()

const emit = defineEmits<{
  detail: [id: number]
  favorite: [id: number, nextFavorite: boolean]
}>()

const imageUrl = computed(() => resolveRecipeImage(props.recipe.imageUrl))
const imagePosition = computed(() => resolveRecipeImagePosition(props.recipe.imageUrl))

const tags = computed(() => props.recipe.tags ?? [])
const scoreText = computed(() => (props.recipe.score ? `${props.recipe.score} 匹配` : '膳食推荐'))
const favoriteLabel = computed(() => (props.isFavorite ? '取消收藏' : '收藏'))
</script>

<template>
  <article class="recipe-card" :class="{ dense }">
    <div class="image-wrap">
      <img
        :src="imageUrl"
        :alt="recipe.name"
        :style="{ objectPosition: imagePosition }"
        @error="replaceImageWithFallback($event)"
      />
      <span>{{ scoreText }}</span>
    </div>
    <div class="recipe-body">
      <h3>{{ recipe.name }}</h3>
      <div class="metrics">
        <span><Utensils /> {{ recipe.calories }} kcal</span>
        <span v-if="recipe.time"><Timer /> {{ recipe.time }} 分钟</span>
      </div>
      <p v-if="!dense">{{ recipe.reason }}</p>
      <div class="tags">
        <span v-for="tag in tags.slice(0, dense ? 2 : 3)" :key="tag" class="sz-chip">
          {{ tag }}
        </span>
      </div>
      <div class="actions">
        <n-button size="small" secondary round type="primary" @click="emit('detail', recipe.id)">详情</n-button>
        <button
          type="button"
          class="favorite-button"
          :class="{ 'is-favorite': isFavorite }"
          :aria-label="favoriteLabel"
          :title="favoriteLabel"
          :disabled="favoritePending"
          @click="emit('favorite', recipe.id, !isFavorite)"
        >
          <Heart />
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.recipe-card {
  overflow: hidden;
  border: 1px solid rgba(227, 218, 203, 0.9);
  border-radius: var(--sz-radius-card);
  background: var(--sz-surface);
  box-shadow: var(--sz-shadow-soft);
  transition:
    border-color 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.recipe-card:hover {
  border-color: var(--sz-line-strong);
  box-shadow: var(--sz-shadow);
  transform: translateY(-2px);
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
  color: #ffffff;
  background: var(--sz-green-dark);
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

.favorite-button {
  position: relative;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  color: var(--sz-text);
  background: rgba(23, 37, 31, 0.08);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.favorite-button:hover {
  color: #ffffff;
  background: var(--sz-green-dark);
  transform: translateY(-1px);
}

.favorite-button.is-favorite {
  color: #ffffff;
  background: #d94d5c;
  box-shadow: 0 8px 18px rgba(217, 77, 92, 0.24);
}

.favorite-button.is-favorite:hover {
  background: #bf3d4b;
}

.favorite-button:active {
  transform: scale(0.95);
}

.favorite-button:disabled {
  cursor: wait;
  opacity: 0.72;
  transform: none;
}

.favorite-button svg {
  width: 19px;
  height: 19px;
  stroke-width: 2.2;
}

.favorite-button.is-favorite svg {
  fill: currentColor;
  animation: heart-pop 0.24s ease;
}

@keyframes heart-pop {
  0% {
    transform: scale(0.72);
  }

  70% {
    transform: scale(1.16);
  }

  100% {
    transform: scale(1);
  }
}
</style>
