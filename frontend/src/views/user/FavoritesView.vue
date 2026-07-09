<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CalendarClock, Check, Heart, ListPlus, Trash2, X } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { listFavorites, unfavoriteRecipe } from '@/api/favorite'
import { createShoppingList } from '@/api/shopping'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import type { FavoriteRecipe } from '@/types'
import { replaceImageWithFallback, resolveRecipeImage } from '@/utils/assets'

const message = useMessage()
const router = useRouter()
const loading = ref(true)
const creating = ref(false)
const error = ref('')
const favorites = ref<FavoriteRecipe[]>([])
const selectedRecipeIds = ref<number[]>([])
const shoppingModalOpen = ref(false)
const availableIngredients = ref<string[]>([])

const selectedCount = computed(() => selectedRecipeIds.value.length)
const allSelected = computed(() => favorites.value.length > 0 && selectedCount.value === favorites.value.length)
const latestFavoriteTime = computed(() => favorites.value[0]?.createdAt ? formatDate(favorites.value[0].createdAt) : '暂无')
const selectedRecipeIdSet = computed(() => new Set(selectedRecipeIds.value))
const selectedCalories = computed(() =>
  favorites.value
    .filter((favorite) => selectedRecipeIdSet.value.has(favorite.recipeId))
    .reduce((sum, favorite) => sum + favorite.calories, 0),
)

function favoriteImage(imageUrl?: string) {
  return resolveRecipeImage(imageUrl)
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

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

function openShoppingModal() {
  if (!selectedRecipeIds.value.length) {
    message.warning('请先选择菜谱')
    return
  }
  shoppingModalOpen.value = true
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
    selectedRecipeIds.value = selectedRecipeIds.value.filter((id) => id !== recipeId)
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
  if (creating.value) return

  creating.value = true
  try {
    await createShoppingList({
      recipeIds: [...selectedRecipeIds.value],
      availableIngredients: availableIngredients.value,
      title: `收藏菜谱采购清单（${selectedCount.value}道）`,
    })
    shoppingModalOpen.value = false
    availableIngredients.value = []
    selectedRecipeIds.value = []
    message.success('购物清单已生成')
    await router.push('/user/shopping-lists')
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

    <section class="summary-strip">
      <article>
        <strong>{{ favorites.length }}</strong>
        <span>收藏菜谱</span>
      </article>
      <article>
        <strong>{{ selectedCount }}</strong>
        <span>已选择</span>
      </article>
      <article>
        <strong>{{ selectedCalories }}</strong>
        <span>已选热量 kcal</span>
      </article>
      <article>
        <strong>{{ latestFavoriteTime }}</strong>
        <span>最近收藏</span>
      </article>
    </section>

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
        <n-button type="primary" :disabled="selectedCount === 0" :loading="creating" @click="openShoppingModal">
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
          :src="favoriteImage(favorite.imageUrl)"
          :alt="favorite.recipeName"
          @error="replaceImageWithFallback($event)"
        />
        <div class="favorite-card-body">
          <div class="card-topline">
            <button
              type="button"
              class="select-pill"
              :class="{ selected: isSelected(favorite.recipeId) }"
              @click.stop="onSelectionChange(favorite.recipeId, !isSelected(favorite.recipeId))"
            >
              <Check />
              {{ isSelected(favorite.recipeId) ? '已选择' : '选择' }}
            </button>
            <small><CalendarClock :size="14" /> {{ formatDate(favorite.createdAt) }}</small>
          </div>
          <h2>{{ favorite.recipeName }}</h2>
          <p>{{ favorite.description }}</p>
          <span class="nutrition-line">{{ favorite.calories }} kcal · {{ favorite.protein }}g 蛋白质</span>
          <div class="card-actions">
            <n-button secondary type="primary" size="small" @click.stop="router.push(`/user/recipes/${favorite.recipeId}`)">
              查看详情
            </n-button>
            <n-button tertiary type="error" size="small" @click.stop="removeFavorite(favorite.recipeId)">
              <template #icon><n-icon><Trash2 /></n-icon></template>
              取消收藏
            </n-button>
          </div>
        </div>
      </article>
    </section>

    <n-empty v-if="!loading && favorites.length === 0" description="暂无收藏菜谱" />

    <n-modal
      v-model:show="shoppingModalOpen"
      preset="card"
      title="生成购物清单"
      class="shopping-modal"
      :style="{ width: '420px', maxWidth: 'calc(100vw - 32px)' }"
    >
      <div class="shopping-modal-body">
        <section class="modal-summary">
          <article>
            <strong>{{ selectedCount }}</strong>
            <span>道菜谱</span>
          </article>
          <article>
            <strong>{{ selectedCalories }}</strong>
            <span>kcal</span>
          </article>
        </section>
        <p>填写你已经有的食材，生成清单时会自动排除。</p>
        <IngredientTagInput
          v-model="availableIngredients"
          label="已有食材"
          placeholder="输入已有食材后回车"
        />
        <n-button block type="primary" :loading="creating" @click="makeShoppingList">
          <template #icon><n-icon><ListPlus /></n-icon></template>
          确认生成
        </n-button>
      </div>
    </n-modal>
  </div>
</template>

<style scoped>
.favorites-view,
.title-block,
.summary-strip {
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
.favorite-card .nutrition-line,
.toolbar span {
  color: var(--sz-muted);
}

.summary-strip {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-strip article {
  display: grid;
  gap: 5px;
  min-height: 82px;
  padding: 15px;
  border: 1px solid var(--sz-line);
  border-radius: 16px;
  background: rgba(255, 250, 241, 0.94);
  box-shadow: var(--sz-shadow-soft);
}

.summary-strip strong {
  overflow: hidden;
  color: var(--sz-evergreen);
  font-size: 24px;
  line-height: 1.18;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-strip span {
  color: var(--sz-muted);
  font-weight: 800;
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

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
  display: grid;
  grid-template-rows: auto 1fr;
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

.favorite-card-body {
  display: grid;
  grid-template-rows: auto auto 1fr auto auto;
  gap: 10px;
  padding: 14px;
}

.card-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-topline small {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--sz-muted);
  font-weight: 800;
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
  overflow: hidden;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.favorite-card p {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.nutrition-line {
  font-weight: 800;
}

.shopping-modal-body {
  display: grid;
  gap: 16px;
}

.shopping-modal-body p {
  color: var(--sz-muted);
  line-height: 1.7;
}

.modal-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.modal-summary article {
  display: grid;
  gap: 4px;
  padding: 14px;
  border: 1px solid var(--sz-line);
  border-radius: 14px;
  background: var(--sz-mint);
}

.modal-summary strong {
  color: var(--sz-evergreen);
  font-size: 24px;
  line-height: 1;
}

.modal-summary span {
  color: var(--sz-muted);
  font-weight: 800;
}

@media (max-width: 980px) {
  .favorite-grid,
  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .favorite-grid,
  .summary-strip {
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
