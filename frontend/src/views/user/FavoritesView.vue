<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Check, Heart, ListPlus, Trash2, X } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { listFavorites, unfavoriteRecipe } from '@/api/favorite'
import { createShoppingList } from '@/api/shopping'
import { backendAssetUrl } from '@/api/http'
import type { FavoriteRecipe } from '@/types'

const message = useMessage()
const loading = ref(true)
const creating = ref(false)
const error = ref('')
const favorites = ref<FavoriteRecipe[]>([])
const selectedRecipeIds = ref<number[]>([])
const fallbackImage =
  'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=80'

const selectedCount = computed(() => selectedRecipeIds.value.length)
const allSelected = computed(() => favorites.value.length > 0 && selectedCount.value === favorites.value.length)

function isSelected(recipeId: number) {
  return selectedRecipeIds.value.includes(recipeId)
}

function toggleSelected(recipeId: number, checked: boolean) {
  if (checked) {
    if (!selectedRecipeIds.value.includes(recipeId)) selectedRecipeIds.value.push(recipeId)
  } else {
    selectedRecipeIds.value = selectedRecipeIds.value.filter((id) => id !== recipeId)
  }
}

function onSelectionChange(recipeId: number, value: boolean) {
  toggleSelected(recipeId, value)
}

function toggleSelection(recipeId: number) {
  toggleSelected(recipeId, !isSelected(recipeId))
}

function toggleAll() {
  selectedRecipeIds.value = allSelected.value ? [] : favorites.value.map((favorite) => favorite.recipeId)
}

function clearSelection() {
  selectedRecipeIds.value = []
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    favorites.value = await listFavorites()
    selectedRecipeIds.value = selectedRecipeIds.value.filter((id) =>
      favorites.value.some((favorite) => favorite.recipeId === id),
    )
  } catch (err) {
    error.value = err instanceof Error ? err.message : '收藏列表加载失败'
  } finally {
    loading.value = false
  }
}

async function removeFavorite(recipeId: number) {
  try {
    await unfavoriteRecipe(recipeId)
    message.success('已取消收藏')
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '取消收藏失败')
  }
}

async function makeShoppingList() {
  if (!selectedRecipeIds.value.length) {
    message.warning('请先选择菜谱')
    return
  }
  creating.value = true
  try {
    await createShoppingList({
      recipeIds: selectedRecipeIds.value,
      availableIngredients: [],
      title: `收藏菜谱采购清单`,
    })
    message.success('购物清单已生成')
  } catch (err) {
    message.error(err instanceof Error ? err.message : '生成购物清单失败')
  } finally {
    creating.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="favorites-view">
    <section class="title-block">
      <p class="sz-chip"><Heart /> 收藏菜谱</p>
      <h1>把想复吃的菜谱收进一张短清单</h1>
      <p>勾选收藏菜谱后可直接生成购物清单。</p>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>

    <section class="toolbar sz-panel">
      <span>已选择 {{ selectedCount }} 道菜</span>
      <div class="toolbar-actions">
        <button type="button" class="ghost-action" @click="toggleAll">
          <Check />
          {{ allSelected ? '取消全选' : '全选' }}
        </button>
        <button type="button" class="ghost-action" :disabled="selectedCount === 0" @click="clearSelection">
          <X />
          清空
        </button>
        <n-button type="primary" :loading="creating" @click="makeShoppingList">
          <template #icon><n-icon><ListPlus /></n-icon></template>
          生成购物清单
        </n-button>
      </div>
    </section>

    <n-skeleton v-if="loading" text :repeat="4" />

    <section class="favorite-grid">
      <article
        v-for="favorite in favorites"
        :key="favorite.favoriteId"
        class="favorite-card"
        :class="{ selected: isSelected(favorite.recipeId) }"
        tabindex="0"
        role="checkbox"
        :aria-checked="isSelected(favorite.recipeId)"
        @click="toggleSelection(favorite.recipeId)"
        @keydown.enter.prevent="toggleSelection(favorite.recipeId)"
        @keydown.space.prevent="toggleSelection(favorite.recipeId)"
      >
        <img
          :src="backendAssetUrl(favorite.imageUrl) || fallbackImage"
          :alt="favorite.recipeName"
          @error="($event.target as HTMLImageElement).src = fallbackImage"
        />
        <div>
          <button
            type="button"
            class="select-pill"
            :class="{ selected: isSelected(favorite.recipeId) }"
            @click.stop="onSelectionChange(favorite.recipeId, !isSelected(favorite.recipeId))"
          >
            <Check />
            {{ isSelected(favorite.recipeId) ? '已选择' : '选择' }}
          </button>
          <h2>{{ favorite.recipeName }}</h2>
          <p>{{ favorite.description }}</p>
          <span>{{ favorite.calories }} kcal · {{ favorite.protein }}g 蛋白质</span>
          <n-button tertiary type="error" size="small" @click.stop="removeFavorite(favorite.recipeId)">
            <template #icon><n-icon><Trash2 /></n-icon></template>
            取消收藏
          </n-button>
        </div>
      </article>
    </section>

    <n-empty v-if="!loading && favorites.length === 0" description="暂无收藏菜谱" />
  </div>
</template>

<style scoped>
.favorites-view,
.title-block {
  display: grid;
  gap: 18px;
}

.title-block {
  justify-items: start;
  gap: 10px;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  font-size: 32px;
}

.title-block p:last-child,
.favorite-card p,
.favorite-card span,
.toolbar span {
  color: var(--sz-muted);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.ghost-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-surface);
  font-weight: 800;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    opacity 0.18s ease;
}

.ghost-action:hover:not(:disabled) {
  border-color: var(--sz-green);
  background: var(--sz-mint);
}

.ghost-action:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.ghost-action svg {
  width: 16px;
  height: 16px;
}

.favorite-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.favorite-card {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-card);
  background: var(--sz-surface);
  box-shadow: var(--sz-shadow-soft);
  cursor: pointer;
  outline: 0;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.favorite-card:hover,
.favorite-card:focus-visible {
  border-color: var(--sz-green);
  box-shadow: var(--sz-shadow);
  transform: translateY(-2px);
}

.favorite-card.selected {
  border-color: var(--sz-green);
  background: linear-gradient(180deg, var(--sz-surface) 0%, rgba(215, 236, 224, 0.58) 100%);
  box-shadow: 0 16px 34px rgba(35, 107, 75, 0.16);
}

.favorite-card.selected::after {
  position: absolute;
  inset: 0;
  border: 2px solid rgba(35, 107, 75, 0.28);
  border-radius: inherit;
  content: '';
  pointer-events: none;
}

.favorite-card img {
  width: 100%;
  aspect-ratio: 4 / 3;
  display: block;
  object-fit: cover;
}

.favorite-card div {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.select-pill {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-muted);
  background: rgba(23, 37, 31, 0.04);
  font-weight: 800;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease,
    border-color 0.18s ease;
}

.select-pill svg {
  width: 15px;
  height: 15px;
}

.select-pill.selected {
  border-color: transparent;
  color: #ffffff;
  background: var(--sz-green-dark);
}

h2 {
  font-size: 18px;
}

.favorite-card p {
  line-height: 1.7;
}

@media (max-width: 980px) {
  .favorite-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .favorite-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: stretch;
  }

  .toolbar-actions > * {
    flex: 1 1 auto;
  }
}
</style>
