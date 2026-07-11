<script setup lang="ts">
import { computed } from 'vue'
import { Heart, ListPlus, SearchCheck, ShoppingBag, Utensils } from '@lucide/vue'
import type { RecommendedRecipe } from '@/types'
import { replaceImageWithFallback, resolveRecipeImage, resolveRecipeImagePosition } from '@/utils/assets'

const props = defineProps<{
  recipe: RecommendedRecipe
  availableIngredients: string[]
  favoritePending?: boolean
  shoppingPending?: boolean
}>()

const emit = defineEmits<{
  detail: [id: number]
  favorite: [id: number]
  shopping: [id: number]
}>()

const imageUrl = computed(() => resolveRecipeImage(props.recipe.imageUrl))
const imagePosition = computed(() => resolveRecipeImagePosition(props.recipe.imageUrl))
const usedIngredients = computed(() =>
  props.recipe.matchedIngredients?.length ? props.recipe.matchedIngredients : props.availableIngredients,
)
const missingIngredients = computed(() => props.recipe.missingIngredients ?? [])
</script>

<template>
  <article class="recipe-result-card">
    <div class="image-wrap">
      <img
        :src="imageUrl"
        :alt="recipe.name"
        :style="{ objectPosition: imagePosition }"
        @error="replaceImageWithFallback($event)"
      />
      <span class="score-pill">{{ recipe.score }}% 匹配</span>
    </div>

    <div class="recipe-copy">
      <div class="title-row">
        <div>
          <h3>{{ recipe.name }}</h3>
          <p>{{ recipe.reason }}</p>
        </div>
        <button
          type="button"
          class="icon-button"
          title="收藏菜谱"
          aria-label="收藏菜谱"
          :disabled="favoritePending"
          @click="emit('favorite', recipe.id)"
        >
          <Heart :size="18" />
        </button>
      </div>

      <div class="metrics">
        <span><Utensils :size="15" /> {{ recipe.calories }} kcal</span>
        <span>{{ recipe.protein }}g 蛋白质</span>
      </div>

      <section class="ingredient-block">
        <span><SearchCheck :size="15" /> 已利用食材</span>
        <div>
          <small v-for="name in usedIngredients" :key="`used-${recipe.id}-${name}`">{{ name }}</small>
          <strong v-if="usedIngredients.length === 0">未命中已有食材</strong>
        </div>
      </section>

      <section class="ingredient-block">
        <span><ShoppingBag :size="15" /> 仍需购买</span>
        <div>
          <small v-for="name in missingIngredients" :key="`missing-${recipe.id}-${name}`" class="is-warm">
            {{ name }}
          </small>
          <strong v-if="missingIngredients.length === 0">现有食材基本可做</strong>
        </div>
      </section>

      <div class="actions">
        <button type="button" class="ghost-button" @click="emit('detail', recipe.id)">查看详情</button>
        <button type="button" class="primary-button" :disabled="shoppingPending" @click="emit('shopping', recipe.id)">
          <ListPlus :size="16" />
          单独生成清单
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.recipe-result-card {
  overflow: hidden;
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  border: 1px solid rgba(223, 210, 191, 0.9);
  border-radius: 18px;
  background: rgba(255, 253, 247, 0.9);
  box-shadow: var(--sz-shadow-soft);
}

.image-wrap {
  position: relative;
  min-height: 100%;
  background: var(--sz-mint);
}

.image-wrap img {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 250px;
  object-fit: cover;
}

.score-pill {
  position: absolute;
  left: 12px;
  top: 12px;
  padding: 6px 11px;
  border-radius: var(--sz-radius-pill);
  color: #ffffff;
  background: rgba(35, 107, 75, 0.93);
  font-size: 13px;
  font-weight: 900;
}

.recipe-copy {
  display: grid;
  gap: 13px;
  padding: 18px;
}

.title-row,
.actions {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 14px;
}

h3,
p {
  margin: 0;
}

h3 {
  color: var(--sz-evergreen);
  font-size: 22px;
  line-height: 1.35;
}

p {
  margin-top: 7px;
  color: var(--sz-text);
  line-height: 1.75;
}

.icon-button {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 50%;
  color: #9b3f2d;
  background: var(--sz-tomato-soft);
  cursor: pointer;
}

.metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.metrics span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 30px;
  padding: 5px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 850;
}

.ingredient-block {
  display: grid;
  gap: 8px;
}

.ingredient-block > span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--sz-muted);
  font-size: 13px;
  font-weight: 900;
}

.ingredient-block div {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.ingredient-block small,
.ingredient-block strong {
  min-height: 28px;
  padding: 5px 9px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 12px;
  font-weight: 850;
}

.ingredient-block small.is-warm {
  color: #8d5d17;
  background: var(--sz-grain-soft);
}

.ingredient-block strong {
  color: var(--sz-muted);
  background: rgba(255, 250, 241, 0.9);
}

.actions {
  align-items: center;
}

.primary-button,
.ghost-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 11px;
  font-weight: 900;
  cursor: pointer;
}

.primary-button {
  border: 0;
  color: #ffffff;
  background: var(--sz-green-dark);
}

.ghost-button {
  border: 1px solid rgba(35, 107, 75, 0.18);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
}

button:disabled {
  cursor: wait;
  opacity: 0.66;
}

@media (max-width: 760px) {
  .recipe-result-card {
    grid-template-columns: 1fr;
  }

  .image-wrap img {
    min-height: 210px;
  }

  .title-row,
  .actions {
    display: grid;
  }

  .primary-button,
  .ghost-button {
    width: 100%;
  }
}
</style>
