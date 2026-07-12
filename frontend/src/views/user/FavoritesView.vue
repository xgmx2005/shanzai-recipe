<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CalendarClock, Check, Heart, ListPlus, X } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { listFavorites, unfavoriteRecipe } from '@/api/favorite'
import { createShoppingList } from '@/api/shopping'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import type { FavoriteRecipe } from '@/types'
import { replaceImageWithFallback, resolveRecipeImage } from '@/utils/assets'
import { shoppingListRoute } from '@/utils/navigation'

const message = useMessage()
const router = useRouter()
const loading = ref(true)
const creating = ref(false)
const error = ref('')
const favorites = ref<FavoriteRecipe[]>([])
const selectedRecipeIds = ref<number[]>([])
const shoppingModalOpen = ref(false)
const removeConfirmOpen = ref(false)
const removeFavoriteTarget = ref<FavoriteRecipe | null>(null)
const availableIngredients = ref<string[]>([])

const selectedCount = computed(() => selectedRecipeIds.value.length)
const allSelected = computed(() => favorites.value.length > 0 && selectedCount.value === favorites.value.length)
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

function requestRemoveFavorite(favorite: FavoriteRecipe) {
  removeFavoriteTarget.value = favorite
  removeConfirmOpen.value = true
}

function closeRemoveConfirm() {
  removeConfirmOpen.value = false
  removeFavoriteTarget.value = null
}

async function confirmRemoveFavorite() {
  if (!removeFavoriteTarget.value) return
  const recipeId = removeFavoriteTarget.value.recipeId
  closeRemoveConfirm()
  await removeFavorite(recipeId)
}

async function makeShoppingList() {
  if (!selectedRecipeIds.value.length) {
    message.warning('请先选择菜谱')
    return
  }
  if (creating.value) return

  creating.value = true
  try {
    const list = await createShoppingList({
      recipeIds: [...selectedRecipeIds.value],
      availableIngredients: availableIngredients.value,
      title: `收藏菜谱采购清单（${selectedCount.value}道）`,
    })
    shoppingModalOpen.value = false
    availableIngredients.value = []
    selectedRecipeIds.value = []
    message.success('购物清单已生成')
    await router.push(shoppingListRoute(list.id, 'favorites'))
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
    <section class="recipe-library-hero sz-panel">
      <div>
        <p class="sz-chip"><Heart /> 私人菜谱库</p>
        <h1>把真正想复吃的菜留下来</h1>
        <p>收藏页负责复用，不负责堆满屏幕。选中几道菜后，可以直接生成一份采购清单。</p>
      </div>
      <div class="library-overview" aria-label="收藏概览">
        <article>
          <strong>{{ favorites.length }}</strong>
          <span>收藏</span>
        </article>
        <article>
          <strong>{{ selectedCount }}</strong>
          <span>已选</span>
        </article>
      </div>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>

    <section class="library-toolbar sz-panel">
      <div>
        <strong>{{ selectedCount ? `已选 ${selectedCount} 道菜` : '选择菜谱生成采购清单' }}</strong>
        <span>{{ selectedCount ? `预计 ${selectedCalories} kcal` : '保留复吃菜谱，按需组合成一餐或一周菜单。' }}</span>
      </div>
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

    <div v-if="loading" class="favorites-loading-shell">
      <n-skeleton text :repeat="5" />
    </div>

    <section class="favorite-card-grid">
      <article
        v-for="favorite in favorites"
        :key="favorite.favoriteId"
        class="library-recipe-card"
        :class="{ selected: isSelected(favorite.recipeId) }"
        tabindex="0"
        role="checkbox"
        :aria-checked="isSelected(favorite.recipeId)"
        @click="toggleSelection(favorite.recipeId)"
        @keydown.enter.prevent="toggleSelection(favorite.recipeId)"
        @keydown.space.prevent="toggleSelection(favorite.recipeId)"
      >
        <button
          type="button"
          class="select-marker"
          :class="{ selected: isSelected(favorite.recipeId) }"
          :aria-label="isSelected(favorite.recipeId) ? `取消选择 ${favorite.recipeName}` : `选择 ${favorite.recipeName}`"
          @click.stop="onSelectionChange(favorite.recipeId, !isSelected(favorite.recipeId))"
        >
          <Check />
        </button>
        <div class="favorite-image">
          <img
            :src="favoriteImage(favorite.imageUrl)"
            :alt="favorite.recipeName"
            @error="replaceImageWithFallback($event)"
          />
        </div>
        <div class="favorite-card-body">
          <div class="card-topline">
            <small><CalendarClock :size="14" /> {{ formatDate(favorite.createdAt) }}</small>
          </div>
          <h2>{{ favorite.recipeName }}</h2>
          <p>{{ favorite.description }}</p>
          <div class="nutrition-chips">
            <span>{{ favorite.calories }} kcal</span>
            <span>{{ favorite.protein }}g 蛋白质</span>
          </div>
          <div class="card-actions">
            <n-button secondary type="primary" size="small" @click.stop="router.push(`/user/recipes/${favorite.recipeId}`)">
              查看详情
            </n-button>
            <button
              type="button"
              class="favorite-heart-button"
              :aria-label="`取消收藏 ${favorite.recipeName}`"
              @click.stop="requestRemoveFavorite(favorite)"
            >
              <Heart />
            </button>
          </div>
        </div>
      </article>
    </section>

    <section v-if="!loading && favorites.length === 0" class="empty-library sz-panel">
      <p class="sz-chip"><Heart /> 暂无收藏</p>
      <h2>先去挑几道真正想吃的菜</h2>
      <p>收藏后的菜谱会出现在这里，之后可以多选生成购物清单。</p>
      <n-button type="primary" @click="router.push('/user/home')">去首页看看</n-button>
    </section>

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

    <n-modal
      v-model:show="removeConfirmOpen"
      preset="card"
      title="确认取消收藏"
      class="remove-modal"
      :style="{ width: '390px', maxWidth: 'calc(100vw - 32px)' }"
      @after-leave="removeFavoriteTarget = null"
    >
      <div class="remove-modal-body">
        <p>
          确定要取消收藏
          <strong>「{{ removeFavoriteTarget?.recipeName }}」</strong>
          吗？取消后可在菜谱详情页重新收藏。
        </p>
        <div class="remove-modal-actions">
          <button type="button" class="ghost-action" @click="closeRemoveConfirm">再想想</button>
          <button type="button" class="danger-action" @click="confirmRemoveFavorite">确认取消</button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<style scoped>
.favorites-view,
.recipe-library-hero {
  display: grid;
  gap: 18px;
}

.recipe-library-hero {
  grid-template-columns: minmax(0, 1fr) 520px;
  align-items: center;
  padding: 24px;
  border-color: rgba(184, 220, 199, 0.82);
  background:
    radial-gradient(circle at 12% 22%, rgba(220, 239, 228, 0.92), transparent 34%),
    linear-gradient(135deg, rgba(255, 253, 247, 0.98), rgba(246, 251, 247, 0.94));
}

.recipe-library-hero > div:first-child {
  display: grid;
  justify-items: start;
  gap: 10px;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  max-width: 680px;
  color: var(--sz-evergreen);
  font-size: clamp(32px, 4vw, 44px);
  line-height: 1.08;
}

.recipe-library-hero p:last-child,
.library-recipe-card p,
.library-toolbar span {
  color: var(--sz-muted);
}

.library-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.library-overview article {
  display: grid;
  align-content: center;
  min-height: 86px;
  padding: 14px 16px;
  border: 1px solid rgba(223, 210, 191, 0.78);
  border-radius: 18px;
  background: rgba(255, 250, 241, 0.82);
}

.library-overview strong {
  overflow: hidden;
  color: var(--sz-evergreen);
  font-size: 25px;
  line-height: 1.18;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.library-overview span {
  color: var(--sz-muted);
  font-weight: 800;
}

.library-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.library-toolbar > div:first-child {
  display: grid;
  gap: 4px;
}

.library-toolbar strong {
  color: var(--sz-evergreen);
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
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: space-between;
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

.favorites-loading-shell {
  display: grid;
  min-height: 260px;
  padding: 20px;
  border: 1px solid var(--sz-line);
  border-radius: 20px;
  background: rgba(255, 250, 241, 0.78);
}

.favorite-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.library-recipe-card {
  position: relative;
  display: grid;
  grid-template-rows: auto 1fr;
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(223, 210, 191, 0.92);
  border-radius: 18px;
  background: rgba(255, 253, 247, 0.95);
  box-shadow: var(--sz-shadow-soft);
  cursor: pointer;
  outline: 0;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.library-recipe-card:hover,
.library-recipe-card:focus-visible {
  border-color: var(--sz-green);
  box-shadow: var(--sz-shadow);
  transform: translateY(-2px);
}

.library-recipe-card.selected {
  border-color: var(--sz-green);
  background: linear-gradient(180deg, rgba(255, 253, 247, 0.96), rgba(220, 239, 228, 0.56));
  box-shadow: 0 16px 34px rgba(35, 107, 75, 0.16);
}

.library-recipe-card.selected::after {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(35, 107, 75, 0.18);
  border-radius: inherit;
  content: '';
  pointer-events: none;
}

.select-marker {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 1px solid rgba(255, 250, 241, 0.8);
  border-radius: 50%;
  color: transparent;
  background: rgba(255, 250, 241, 0.62);
  box-shadow: 0 8px 16px rgba(23, 37, 31, 0.1);
  cursor: pointer;
  transition:
    background 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.select-marker.selected {
  color: #ffffff;
  background: rgba(35, 107, 75, 0.92);
}

.select-marker:hover {
  transform: scale(1.04);
}

.select-marker svg {
  width: 15px;
  height: 15px;
}

.favorite-image {
  position: relative;
  overflow: hidden;
}

.favorite-image img {
  width: 100%;
  aspect-ratio: 16 / 10;
  display: block;
  object-fit: cover;
}

.favorite-heart-button {
  position: absolute;
  right: 14px;
  bottom: 14px;
  z-index: 2;
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(217, 63, 72, 0.08);
  border-radius: 50%;
  color: #d93f48;
  background: rgba(217, 63, 72, 0.1);
  cursor: pointer;
  transition:
    background 0.18s ease,
    transform 0.18s ease;
}

.favorite-heart-button:hover {
  color: #ffffff;
  background: #d93f48;
  transform: translateY(-1px);
}

.favorite-heart-button svg {
  width: 20px;
  height: 20px;
  fill: currentColor;
}

.favorite-card-body {
  display: grid;
  grid-template-rows: auto auto 1fr auto auto;
  gap: 9px;
  padding: 13px 52px 13px 13px;
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

h2 {
  overflow: hidden;
  color: var(--sz-evergreen);
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.library-recipe-card p {
  display: -webkit-box;
  overflow: hidden;
  min-height: 44px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.nutrition-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nutrition-chips span {
  min-height: 28px;
  padding: 5px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 900;
}

.empty-library {
  justify-items: start;
  gap: 12px;
  padding: 28px;
  border-style: dashed;
  background: linear-gradient(135deg, rgba(255, 253, 247, 0.96), rgba(220, 239, 228, 0.54));
}

.empty-library h2 {
  font-size: 24px;
}

.empty-library p:not(.sz-chip) {
  color: var(--sz-muted);
  line-height: 1.7;
}

.shopping-modal-body {
  display: grid;
  gap: 16px;
}

.shopping-modal-body p {
  color: var(--sz-muted);
  line-height: 1.7;
}

.remove-modal {
  max-width: 420px;
}

.remove-modal-body {
  display: grid;
  gap: 18px;
}

.remove-modal-body p {
  color: var(--sz-text);
  line-height: 1.8;
}

.remove-modal-body strong {
  color: var(--sz-evergreen);
}

.remove-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.danger-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 16px;
  border: 0;
  border-radius: var(--sz-radius-pill);
  color: #ffffff;
  background: #d93f48;
  font-weight: 900;
  cursor: pointer;
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
  .recipe-library-hero {
    grid-template-columns: 1fr;
  }

  .favorite-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .library-overview,
  .favorite-card-grid {
    grid-template-columns: 1fr;
  }

  .library-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: stretch;
  }

  .toolbar-actions > * {
    flex: 1 1 auto;
  }

  .favorite-heart-button {
    right: 12px;
    bottom: 12px;
    width: 30px;
    height: 30px;
  }
}
</style>
