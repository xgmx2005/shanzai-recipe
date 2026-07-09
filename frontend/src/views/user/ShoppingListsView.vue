<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ListChecks, Plus, Trash2 } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import {
  createShoppingList,
  deleteShoppingList,
  getShoppingList,
  listShoppingLists,
  updateShoppingListItem,
} from '@/api/shopping'
import { listRecipes } from '@/api/recipe'
import IngredientIcon from '@/components/IngredientIcon.vue'
import IngredientTagInput from '@/components/IngredientTagInput.vue'
import type { RecipeSummary, ShoppingList, ShoppingListSummary } from '@/types'

const message = useMessage()
const route = useRoute()
const router = useRouter()
const loading = ref(true)
const detailLoading = ref(false)
const saving = ref(false)
const error = ref('')
const lists = ref<ShoppingListSummary[]>([])
const recipes = ref<RecipeSummary[]>([])
const activeList = ref<ShoppingList | null>(null)
const createOpen = ref(false)
const form = reactive({
  title: '本周健康采购',
  recipeIds: [] as number[],
  availableIngredients: [] as string[],
})

const progress = computed(() => {
  if (!activeList.value?.items.length) return 0
  const checked = activeList.value.items.filter((item) => item.checked).length
  return Math.round((checked / activeList.value.items.length) * 100)
})
const activeCheckedCount = computed(() => activeList.value?.items.filter((item) => item.checked).length ?? 0)
const activeItemCount = computed(() => activeList.value?.items.length ?? 0)

const recipeOptions = computed(() =>
  recipes.value.map((recipe) => ({
    label: `${recipe.name} · ${recipe.calories} kcal · ${recipe.cookingTime} 分钟`,
    value: recipe.id,
  })),
)

function parseListId(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value
  const id = Number(raw)
  return Number.isInteger(id) && id > 0 ? id : undefined
}

function preferredListIdFromRoute() {
  return parseListId(route.query.listId)
}

async function loadRecipes() {
  try {
    recipes.value = await listRecipes()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '菜谱列表加载失败')
  }
}

async function openCreate() {
  createOpen.value = true
  if (!recipes.value.length) {
    await loadRecipes()
  }
}

async function load(preferredListId?: number) {
  loading.value = true
  error.value = ''
  try {
    lists.value = await listShoppingLists()
    const requestedId = preferredListId ?? preferredListIdFromRoute()
    const nextListId = lists.value.some((item) => item.id === requestedId) ? requestedId : lists.value[0]?.id
    if (nextListId) {
      await openList(nextListId)
    } else {
      activeList.value = null
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '购物清单加载失败'
  } finally {
    loading.value = false
  }
}

async function openList(id: number) {
  detailLoading.value = true
  try {
    activeList.value = await getShoppingList(id)
    if (route.query.listId !== String(id)) {
      void router.replace({
        query: {
          ...route.query,
          listId: String(id),
        },
      })
    }
  } catch (err) {
    message.error(err instanceof Error ? err.message : '购物清单详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function toggleItem(itemId: number, checked: boolean) {
  if (!activeList.value) return
  try {
    const item = await updateShoppingListItem(activeList.value.id, itemId, checked)
    const target = activeList.value.items.find((value) => value.id === itemId)
    if (target) Object.assign(target, item)
    syncActiveSummary()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '更新勾选状态失败')
  }
}

function syncActiveSummary() {
  if (!activeList.value) return
  const target = lists.value.find((item) => item.id === activeList.value?.id)
  if (!target) return
  target.checkedCount = activeCheckedCount.value
  target.itemCount = activeItemCount.value
}

function onItemChecked(itemId: number, value: boolean) {
  void toggleItem(itemId, value)
}

async function createList() {
  if (!form.recipeIds.length) {
    message.warning('请至少选择一道菜谱')
    return
  }

  saving.value = true
  try {
    const list = await createShoppingList({
      title: form.title,
      recipeIds: form.recipeIds,
      availableIngredients: form.availableIngredients,
    })
    createOpen.value = false
    form.recipeIds = []
    form.availableIngredients = []
    await load(list.id)
    message.success('购物清单已创建')
  } catch (err) {
    message.error(err instanceof Error ? err.message : '创建购物清单失败')
  } finally {
    saving.value = false
  }
}

async function removeList(id: number) {
  try {
    await deleteShoppingList(id)
    message.success('购物清单已删除')
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '删除购物清单失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="shopping-view">
    <section class="title-block">
      <p class="sz-chip"><ListChecks /> 购物清单</p>
      <h1>把推荐结果变成可以照着买的清单</h1>
      <p>同名同单位食材会由后端自动合并，已有食材不会进入采购项。</p>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>

    <section class="shopping-grid">
      <aside class="list-column sz-panel">
        <div class="column-head">
          <strong>我的清单</strong>
          <button type="button" class="create-button" aria-label="创建购物清单" @click="openCreate">
            <Plus />
          </button>
        </div>
        <n-skeleton v-if="loading" text :repeat="4" />
        <button
          v-for="item in lists"
          :key="item.id"
          type="button"
          :class="{ active: activeList?.id === item.id }"
          @click="openList(item.id)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.checkedCount }}/{{ item.itemCount }} 已完成</span>
          <small>{{ new Date(item.createdAt).toLocaleString() }}</small>
        </button>
        <n-empty v-if="!loading && lists.length === 0" description="暂无购物清单" />
      </aside>

      <section class="detail-panel sz-panel">
        <n-spin :show="detailLoading">
          <template v-if="activeList">
            <div class="detail-head">
              <div>
                <p class="sz-chip">当前清单</p>
                <h2>{{ activeList.title }}</h2>
                <span>{{ activeCheckedCount }}/{{ activeItemCount }} 已完成</span>
              </div>
              <n-button tertiary type="error" @click="removeList(activeList.id)">
                <template #icon><n-icon><Trash2 /></n-icon></template>
                删除
              </n-button>
            </div>
            <n-progress type="line" :percentage="progress" />
            <div class="items">
              <label v-for="item in activeList.items" :key="item.id" :class="{ checked: item.checked }">
                <n-checkbox :checked="item.checked" @update:checked="(value: boolean) => onItemChecked(item.id, value)" />
                <IngredientIcon :name="item.ingredientName" :size="38" />
                <span class="item-name">{{ item.ingredientName }}</span>
                <em>{{ item.quantity }}{{ item.unit }}</em>
                <small>{{ item.category }}</small>
              </label>
            </div>
          </template>
          <n-empty v-else-if="!loading" description="暂无可查看清单" />
        </n-spin>
      </section>
    </section>

    <n-modal
      v-model:show="createOpen"
      preset="card"
      title="创建购物清单"
      class="create-modal"
      :style="{ width: '460px', maxWidth: 'calc(100vw - 32px)' }"
    >
      <div class="create-form">
        <n-form-item label="标题">
          <n-input v-model:value="form.title" />
        </n-form-item>
        <n-form-item label="选择菜谱">
          <n-select
            v-model:value="form.recipeIds"
            multiple
            filterable
            :options="recipeOptions"
            placeholder="搜索菜名后选择，可多选"
          />
        </n-form-item>
        <n-form-item>
          <IngredientTagInput v-model="form.availableIngredients" label="已有食材" placeholder="输入已有食材后回车" />
        </n-form-item>
        <n-button block type="primary" :loading="saving" @click="createList">创建</n-button>
      </div>
    </n-modal>
  </div>
</template>

<style scoped>
.shopping-view,
.title-block,
.list-column,
.detail-panel,
.items,
.create-form {
  display: grid;
}

.shopping-view {
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
.list-column span,
.list-column small,
.items small {
  color: var(--sz-muted);
}

.shopping-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
}

.list-column,
.detail-panel {
  gap: 12px;
  padding: 18px;
}

.column-head,
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-head > div {
  display: grid;
  justify-items: start;
  gap: 7px;
}

.detail-head span {
  color: var(--sz-muted);
  font-weight: 800;
}

.create-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 10px 18px rgba(35, 107, 75, 0.2);
  cursor: pointer;
  line-height: 0;
  transition:
    transform 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.create-button:hover {
  background: var(--sz-green);
  transform: translateY(-1px);
}

.create-button:active {
  transform: scale(0.96);
}

.create-button svg {
  position: absolute;
  inset: 50% auto auto 50%;
  width: 20px;
  height: 20px;
  display: block;
  stroke-width: 2.4;
  transform: translate(-50%, -50%);
}

.list-column button,
.items label {
  border: 1px solid var(--sz-line);
  border-radius: 14px;
  background: var(--sz-surface);
}

.list-column button {
  display: grid;
  gap: 6px;
  padding: 14px;
  text-align: left;
  cursor: pointer;
}

.list-column button.active {
  border-color: var(--sz-green);
  background: var(--sz-mint);
}

.items {
  gap: 10px;
}

.items label {
  display: grid;
  grid-template-columns: auto auto 1fr auto auto;
  gap: 10px;
  align-items: center;
  min-height: 48px;
  padding: 0 12px;
}

.items label.checked .item-name,
.items label.checked em {
  color: var(--sz-muted);
  text-decoration: line-through;
}

.items em {
  color: var(--sz-deep-green);
  font-style: normal;
  font-weight: 800;
}

.create-modal {
  max-width: 520px;
}

.create-form {
  gap: 4px;
}

@media (max-width: 880px) {
  .shopping-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .items label {
    grid-template-columns: auto auto 1fr;
  }

  .items small,
  .items em {
    grid-column: 3;
    justify-self: start;
  }
}
</style>
