<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getAdminDashboard, listDietGoalStats, listPopularRecipes } from '@/api/admin'
import type { AdminDashboard, DietGoalStat, PopularRecipeStat } from '@/types'

const loading = ref(true)
const error = ref('')
const dashboard = ref<AdminDashboard | null>(null)
const popularRecipes = ref<PopularRecipeStat[]>([])
const dietGoalStats = ref<DietGoalStat[]>([])

const stats = computed(() => [
  { label: '用户总数', value: dashboard.value?.userCount ?? 0, note: '已注册用户' },
  { label: '菜谱数量', value: dashboard.value?.recipeCount ?? 0, note: '当前菜谱库' },
  { label: '食材数量', value: dashboard.value?.ingredientCount ?? 0, note: '可维护食材' },
  { label: '推荐次数', value: dashboard.value?.recommendationCount ?? 0, note: '累计生成' },
])

const goalLabels = {
  FAT_LOSS: '减脂控热量',
  BALANCED: '日常均衡',
  MUSCLE_GAIN: '健身增肌',
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const [dashboardData, popularData, goalData] = await Promise.all([
      getAdminDashboard(),
      listPopularRecipes(5),
      listDietGoalStats(),
    ])
    dashboard.value = dashboardData
    popularRecipes.value = popularData
    dietGoalStats.value = goalData
  } catch (err) {
    error.value = err instanceof Error ? err.message : '维护端数据加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="dashboard">
    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <n-skeleton v-if="loading" text :repeat="3" />
    <div class="stat-grid">
      <article v-for="stat in stats" :key="stat.label">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <em>{{ stat.note }}</em>
      </article>
    </div>
    <div class="dashboard-grid">
      <div class="admin-card">
        <h1>热门菜谱</h1>
        <ul>
          <li v-for="recipe in popularRecipes" :key="recipe.recipeId">
            <span>{{ recipe.recipeName }}</span>
            <strong>{{ recipe.recommendationCount }}</strong>
          </li>
        </ul>
        <n-empty v-if="!loading && popularRecipes.length === 0" description="暂无推荐统计" />
      </div>
      <div class="admin-card">
        <h1>饮食目标分布</h1>
        <ul>
          <li v-for="goal in dietGoalStats" :key="goal.dietGoal">
            <span>{{ goalLabels[goal.dietGoal] }}</span>
            <strong>{{ goal.count }}</strong>
          </li>
        </ul>
        <n-empty v-if="!loading && dietGoalStats.length === 0" description="暂无目标数据" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

article,
.admin-card {
  border: 1px solid var(--sz-line);
  border-radius: 18px;
  background: var(--sz-surface);
}

article {
  display: grid;
  gap: 8px;
  padding: 18px;
}

span {
  color: var(--sz-muted);
}

strong {
  font-size: 26px;
}

em {
  color: var(--sz-green-dark);
  font-style: normal;
  font-weight: 800;
}

.admin-card {
  padding: 24px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

h1,
p {
  margin: 0;
}

ul {
  display: grid;
  gap: 10px;
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}

li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 0 12px;
  border-radius: 12px;
  background: var(--sz-bg-soft);
}

li span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

li strong {
  color: var(--sz-deep-green);
  font-size: 18px;
}

p {
  margin-top: 10px;
  color: var(--sz-muted);
}

@media (max-width: 920px) {
  .stat-grid,
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .stat-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
