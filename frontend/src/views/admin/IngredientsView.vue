<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Carrot, Pencil, Plus, Search, Trash2 } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import { createIngredient, deleteIngredient, listIngredients, updateIngredient } from '@/api/admin'
import type { Ingredient, IngredientSaveRequest } from '@/types'

const message = useMessage()
const loading = ref(true)
const saving = ref(false)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const error = ref('')
const ingredients = ref<Ingredient[]>([])

const filters = reactive({
  keyword: '',
  category: '',
})

const form = reactive<IngredientSaveRequest>({
  name: '',
  category: '其他',
  unit: 'g',
  caloriesPer100g: 0,
  proteinPer100g: 0,
  fatPer100g: 0,
  carbsPer100g: 0,
  aliases: [],
})

function resetForm() {
  Object.assign(form, {
    name: '',
    category: '其他',
    unit: 'g',
    caloriesPer100g: 0,
    proteinPer100g: 0,
    fatPer100g: 0,
    carbsPer100g: 0,
    aliases: [],
  })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    ingredients.value = await listIngredients({
      keyword: filters.keyword || undefined,
      category: filters.category || undefined,
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '食材列表加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(item: Ingredient) {
  editingId.value = item.id
  Object.assign(form, {
    name: item.name,
    category: item.category,
    unit: item.unit,
    caloriesPer100g: item.caloriesPer100g,
    proteinPer100g: item.proteinPer100g,
    fatPer100g: item.fatPer100g,
    carbsPer100g: item.carbsPer100g,
    aliases: [...item.aliases],
  })
  modalOpen.value = true
}

async function save() {
  if (!form.name.trim()) {
    message.warning('请填写食材名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateIngredient(editingId.value, { ...form })
      message.success('食材已更新')
    } else {
      await createIngredient({ ...form })
      message.success('食材已新增')
    }
    modalOpen.value = false
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '保存食材失败')
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  try {
    await deleteIngredient(id)
    message.success('食材已删除')
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '删除食材失败')
  }
}

onMounted(load)
</script>

<template>
  <section class="ingredients-admin">
    <div class="page-head">
      <div>
        <p class="sz-chip"><Carrot /> 食材库</p>
        <h1>维护营养计算的基础字典</h1>
      </div>
      <n-button type="primary" @click="openCreate">
        <template #icon><n-icon><Plus /></n-icon></template>
        新增食材
      </n-button>
    </div>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>

    <div class="filter-bar sz-panel">
      <n-input v-model:value="filters.keyword" placeholder="搜索食材名称/别名" clearable>
        <template #prefix><n-icon><Search /></n-icon></template>
      </n-input>
      <n-input v-model:value="filters.category" placeholder="分类" clearable />
      <n-button type="primary" @click="load">查询</n-button>
    </div>

    <div class="table-wrap sz-panel">
      <n-skeleton v-if="loading" text :repeat="5" />
      <table v-else>
        <thead>
          <tr>
            <th>食材</th>
            <th>单位</th>
            <th>每 100g 热量</th>
            <th>蛋白/脂肪/碳水</th>
            <th>别名</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in ingredients" :key="item.id">
            <td>
              <strong>{{ item.name }}</strong>
              <span>{{ item.category }}</span>
            </td>
            <td>{{ item.unit }}</td>
            <td>{{ item.caloriesPer100g }} kcal</td>
            <td>{{ item.proteinPer100g }} / {{ item.fatPer100g }} / {{ item.carbsPer100g }}</td>
            <td>{{ item.aliases.join('、') || '-' }}</td>
            <td>
              <div class="actions">
                <n-button size="small" tertiary @click="openEdit(item)">
                  <template #icon><n-icon><Pencil /></n-icon></template>
                  编辑
                </n-button>
                <n-button size="small" tertiary type="error" @click="remove(item.id)">
                  <template #icon><n-icon><Trash2 /></n-icon></template>
                  删除
                </n-button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <n-empty v-if="!loading && ingredients.length === 0" description="暂无食材" />
    </div>

    <n-modal v-model:show="modalOpen" preset="card" :title="editingId ? '编辑食材' : '新增食材'" class="ingredient-modal">
      <div class="ingredient-form">
        <div class="form-grid">
          <n-form-item label="名称"><n-input v-model:value="form.name" /></n-form-item>
          <n-form-item label="分类"><n-input v-model:value="form.category" /></n-form-item>
          <n-form-item label="单位"><n-input v-model:value="form.unit" /></n-form-item>
          <n-form-item label="热量"><n-input-number v-model:value="form.caloriesPer100g" :min="0" /></n-form-item>
          <n-form-item label="蛋白质"><n-input-number v-model:value="form.proteinPer100g" :min="0" /></n-form-item>
          <n-form-item label="脂肪"><n-input-number v-model:value="form.fatPer100g" :min="0" /></n-form-item>
          <n-form-item label="碳水"><n-input-number v-model:value="form.carbsPer100g" :min="0" /></n-form-item>
        </div>
        <n-form-item label="别名"><n-dynamic-tags v-model:value="form.aliases" /></n-form-item>
        <n-button block type="primary" :loading="saving" @click="save">保存</n-button>
      </div>
    </n-modal>
  </section>
</template>

<style scoped>
.ingredients-admin,
.ingredient-form {
  display: grid;
  gap: 16px;
}

.page-head,
.filter-bar,
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-head {
  justify-content: space-between;
}

.page-head div {
  display: grid;
  justify-items: start;
  gap: 10px;
}

h1 {
  margin: 0;
  font-size: 28px;
}

.filter-bar {
  padding: 14px;
}

.filter-bar .n-input {
  min-width: 220px;
}

.table-wrap {
  overflow-x: auto;
  padding: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 14px;
  border-bottom: 1px solid var(--sz-line);
  text-align: left;
  vertical-align: top;
}

th {
  color: var(--sz-muted);
  font-size: 13px;
}

td strong,
td span {
  display: block;
}

td span {
  margin-top: 5px;
  color: var(--sz-muted);
}

.ingredient-modal {
  max-width: 680px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 760px) {
  .page-head,
  .filter-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
