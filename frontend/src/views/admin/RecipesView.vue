<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { BookOpen, Pencil, Plus, Search, Trash2 } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import {
  createAdminRecipe,
  deleteAdminRecipe,
  getAdminRecipe,
  listAdminRecipes,
  updateAdminRecipe,
} from '@/api/admin'
import type { DietGoal, Difficulty, RecipeSaveRequest, RecipeSummary } from '@/types'

const message = useMessage()
const loading = ref(true)
const saving = ref(false)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const error = ref('')
const recipes = ref<RecipeSummary[]>([])

const filters = reactive({
  keyword: '',
  dietGoal: null as DietGoal | null,
  status: 1 as 0 | 1,
})

const form = reactive({
  name: '',
  description: '',
  imageUrl: '/images/recipes/new-recipe.jpg',
  cookingTime: 30,
  difficulty: 'EASY' as Difficulty,
  servings: 1,
  calories: 420,
  protein: 25,
  fat: 10,
  carbs: 45,
  tasteTags: [] as string[],
  healthTags: [] as string[],
  targetGoals: ['BALANCED'] as DietGoal[],
  steps: [] as string[],
  ingredientsJson: '[{"ingredientId":1,"quantity":100,"unit":"g","core":true}]',
})

const goalOptions = [
  { label: '减脂控热量', value: 'FAT_LOSS' },
  { label: '日常均衡', value: 'BALANCED' },
  { label: '健身增肌', value: 'MUSCLE_GAIN' },
]

const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' },
]

function resetForm() {
  Object.assign(form, {
    name: '',
    description: '',
    imageUrl: '/images/recipes/new-recipe.jpg',
    cookingTime: 30,
    difficulty: 'EASY',
    servings: 1,
    calories: 420,
    protein: 25,
    fat: 10,
    carbs: 45,
    tasteTags: [],
    healthTags: [],
    targetGoals: ['BALANCED'],
    steps: [],
    ingredientsJson: '[{"ingredientId":1,"quantity":100,"unit":"g","core":true}]',
  })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    recipes.value = await listAdminRecipes({
      keyword: filters.keyword || undefined,
      dietGoal: filters.dietGoal || undefined,
      status: filters.status,
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '菜谱列表加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

async function openEdit(id: number) {
  saving.value = true
  try {
    const detail = await getAdminRecipe(id)
    editingId.value = id
    Object.assign(form, {
      name: detail.name,
      description: detail.description,
      imageUrl: detail.imageUrl,
      cookingTime: detail.cookingTime,
      difficulty: detail.difficulty,
      servings: detail.servings,
      calories: detail.calories,
      protein: detail.protein,
      fat: detail.fat,
      carbs: detail.carbs,
      tasteTags: [...detail.tasteTags],
      healthTags: [...detail.healthTags],
      targetGoals: [...detail.targetGoals],
      steps: [...detail.steps],
      ingredientsJson: JSON.stringify(
        detail.ingredients.map((item) => ({
          ingredientId: item.ingredientId,
          quantity: item.quantity,
          unit: item.unit,
          core: item.core,
        })),
        null,
        2,
      ),
    })
    modalOpen.value = true
  } catch (err) {
    message.error(err instanceof Error ? err.message : '菜谱详情加载失败')
  } finally {
    saving.value = false
  }
}

function buildPayload(): RecipeSaveRequest {
  const ingredients = JSON.parse(form.ingredientsJson) as RecipeSaveRequest['ingredients']
  return {
    name: form.name,
    description: form.description,
    imageUrl: form.imageUrl,
    cookingTime: form.cookingTime,
    difficulty: form.difficulty,
    servings: form.servings,
    calories: form.calories,
    protein: form.protein,
    fat: form.fat,
    carbs: form.carbs,
    tasteTags: form.tasteTags,
    healthTags: form.healthTags,
    targetGoals: form.targetGoals,
    steps: form.steps,
    ingredients,
  }
}

async function save() {
  if (!form.name.trim()) {
    message.warning('请填写菜谱名称')
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (editingId.value) {
      await updateAdminRecipe(editingId.value, payload)
      message.success('菜谱已更新')
    } else {
      await createAdminRecipe(payload)
      message.success('菜谱已新增')
    }
    modalOpen.value = false
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '保存菜谱失败，请检查食材 JSON')
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  try {
    await deleteAdminRecipe(id)
    message.success('菜谱已下架')
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '下架菜谱失败')
  }
}

onMounted(load)
</script>

<template>
  <section class="recipes-admin">
    <div class="page-head">
      <div>
        <p class="sz-chip"><BookOpen /> 菜谱库</p>
        <h1>维护推荐系统的菜谱燃料</h1>
      </div>
      <n-button type="primary" @click="openCreate">
        <template #icon><n-icon><Plus /></n-icon></template>
        新增菜谱
      </n-button>
    </div>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>

    <div class="filter-bar sz-panel">
      <n-input v-model:value="filters.keyword" placeholder="搜索菜谱名称/描述" clearable>
        <template #prefix><n-icon><Search /></n-icon></template>
      </n-input>
      <n-select v-model:value="filters.dietGoal" :options="goalOptions" clearable placeholder="饮食目标" />
      <n-select
        v-model:value="filters.status"
        :options="[
          { label: '上架', value: 1 },
          { label: '下架', value: 0 },
        ]"
      />
      <n-button type="primary" @click="load">查询</n-button>
    </div>

    <div class="table-wrap sz-panel">
      <n-skeleton v-if="loading" text :repeat="5" />
      <table v-else>
        <thead>
          <tr>
            <th>菜谱</th>
            <th>目标</th>
            <th>营养</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="recipe in recipes" :key="recipe.id">
            <td>
              <strong>{{ recipe.name }}</strong>
              <span>{{ recipe.description }}</span>
            </td>
            <td>{{ recipe.targetGoals.join(' / ') }}</td>
            <td>{{ recipe.calories }} kcal · {{ recipe.protein }}g 蛋白质</td>
            <td>
              <span class="status" :class="{ off: recipe.status === 0 }">{{ recipe.status === 1 ? '上架' : '下架' }}</span>
            </td>
            <td>
              <div class="actions">
                <n-button size="small" tertiary @click="openEdit(recipe.id)">
                  <template #icon><n-icon><Pencil /></n-icon></template>
                  编辑
                </n-button>
                <n-button size="small" tertiary type="error" @click="remove(recipe.id)">
                  <template #icon><n-icon><Trash2 /></n-icon></template>
                  下架
                </n-button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <n-empty v-if="!loading && recipes.length === 0" description="暂无菜谱" />
    </div>

    <n-modal v-model:show="modalOpen" preset="card" :title="editingId ? '编辑菜谱' : '新增菜谱'" class="recipe-modal">
      <div class="recipe-form">
        <n-form-item label="名称"><n-input v-model:value="form.name" /></n-form-item>
        <n-form-item label="描述"><n-input v-model:value="form.description" type="textarea" /></n-form-item>
        <n-form-item label="图片路径"><n-input v-model:value="form.imageUrl" /></n-form-item>
        <div class="form-grid">
          <n-form-item label="时间"><n-input-number v-model:value="form.cookingTime" :min="1" /></n-form-item>
          <n-form-item label="难度"><n-select v-model:value="form.difficulty" :options="difficultyOptions" /></n-form-item>
          <n-form-item label="份数"><n-input-number v-model:value="form.servings" :min="1" /></n-form-item>
          <n-form-item label="热量"><n-input-number v-model:value="form.calories" :min="1" /></n-form-item>
          <n-form-item label="蛋白质"><n-input-number v-model:value="form.protein" :min="0" /></n-form-item>
          <n-form-item label="脂肪"><n-input-number v-model:value="form.fat" :min="0" /></n-form-item>
          <n-form-item label="碳水"><n-input-number v-model:value="form.carbs" :min="0" /></n-form-item>
        </div>
        <n-form-item label="口味标签"><n-dynamic-tags v-model:value="form.tasteTags" /></n-form-item>
        <n-form-item label="健康标签"><n-dynamic-tags v-model:value="form.healthTags" /></n-form-item>
        <n-form-item label="目标"><n-select v-model:value="form.targetGoals" multiple :options="goalOptions" /></n-form-item>
        <n-form-item label="步骤"><n-dynamic-tags v-model:value="form.steps" /></n-form-item>
        <n-form-item label="食材 JSON">
          <n-input v-model:value="form.ingredientsJson" type="textarea" :autosize="{ minRows: 4, maxRows: 8 }" />
        </n-form-item>
        <n-button block type="primary" :loading="saving" @click="save">保存</n-button>
      </div>
    </n-modal>
  </section>
</template>

<style scoped>
.recipes-admin,
.recipe-form {
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

.status {
  display: inline-flex;
  margin: 0;
  padding: 4px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-weight: 800;
}

.status.off {
  color: #8a5e0f;
  background: var(--sz-grain-soft);
}

.recipe-modal {
  max-width: 760px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 760px) {
  .page-head,
  .filter-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
