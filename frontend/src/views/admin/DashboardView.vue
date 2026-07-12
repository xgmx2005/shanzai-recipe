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

const knowledgeHealthScore = computed(() => {
  const recipeReady = Math.min((dashboard.value?.recipeCount ?? 0) / 40, 1) * 45
  const ingredientReady = Math.min((dashboard.value?.ingredientCount ?? 0) / 80, 1) * 35
  const recommendationReady = Math.min((dashboard.value?.recommendationCount ?? 0) / 30, 1) * 20
  return Math.round(recipeReady + ingredientReady + recommendationReady)
})

const maintenanceAdvice = computed(() => {
  const recipeCount = dashboard.value?.recipeCount ?? 0
  const ingredientCount = dashboard.value?.ingredientCount ?? 0
  const recommendationCount = dashboard.value?.recommendationCount ?? 0

  return [
    {
      title: recipeCount >= 40 ? '菜谱库规模稳定' : '继续扩充菜谱库',
      detail: recipeCount >= 40 ? '当前菜谱数量可以支撑基础推荐展示。' : '建议优先补充轻食、家常和高蛋白菜谱。',
    },
    {
      title: ingredientCount >= 80 ? '食材字典较完整' : '补齐常用食材字典',
      detail: ingredientCount >= 80 ? '食材数量能覆盖多数购物清单计算。' : '建议补充调料、主食、蔬菜和蛋白质食材。',
    },
    {
      title: recommendationCount > 0 ? '推荐链路已有数据' : '等待推荐数据沉淀',
      detail: recommendationCount > 0 ? '可以从热门菜谱观察用户偏好。' : '完成几次智能推荐后，看板会更有参考价值。',
    },
  ]
})

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
  <section class="dashboard maintenance-dashboard">
    <div class="maintenance-hero">
      <div>
        <p class="sz-chip">知识库健康度</p>
        <h1>维护推荐系统的基础数据</h1>
        <p>管理员端负责菜谱、食材和推荐统计的稳定维护，保证用户端推荐结果可解释、可执行。</p>
      </div>
      <div class="health-card">
        <span>当前健康度</span>
        <strong>{{ knowledgeHealthScore }}%</strong>
        <small>由菜谱规模、食材字典和推荐数据综合估算</small>
      </div>
    </div>

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
        <div class="card-head">
          <h2>热门菜谱</h2>
          <span>按推荐次数排序</span>
        </div>
        <ul>
          <li v-for="recipe in popularRecipes" :key="recipe.recipeId">
            <span>{{ recipe.recipeName }}</span>
            <strong>{{ recipe.recommendationCount }}</strong>
          </li>
        </ul>
        <n-empty v-if="!loading && popularRecipes.length === 0" description="暂无推荐统计" />
      </div>
      <div class="admin-card">
        <div class="card-head">
          <h2>饮食目标分布</h2>
          <span>用户健康目标画像</span>
        </div>
        <ul>
          <li v-for="goal in dietGoalStats" :key="goal.dietGoal">
            <span>{{ goalLabels[goal.dietGoal] }}</span>
            <strong>{{ goal.count }}</strong>
          </li>
        </ul>
        <n-empty v-if="!loading && dietGoalStats.length === 0" description="暂无目标数据" />
      </div>
    </div>

    <div class="admin-card maintenance-advice">
      <div class="card-head">
        <h2>维护建议</h2>
        <span>帮助知识库保持可用和可信</span>
      </div>
      <div class="advice-grid">
        <article v-for="item in maintenanceAdvice" :key="item.title">
          <strong>{{ item.title }}</strong>
          <p>{{ item.detail }}</p>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 18px;
}

.maintenance-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 18px;
  align-items: stretch;
  padding: 22px;
  border: 1px solid rgba(184, 220, 199, 0.78);
  border-radius: 22px;
  background:
    radial-gradient(circle at 12% 18%, rgba(220, 239, 228, 0.88), transparent 34%),
    linear-gradient(135deg, rgba(255, 253, 247, 0.96), rgba(246, 251, 247, 0.9));
  box-shadow: var(--sz-shadow-soft);
}

.maintenance-hero > div:first-child {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.maintenance-hero h1,
.card-head h2,
.maintenance-hero p,
.maintenance-advice p {
  margin: 0;
}

.maintenance-hero h1 {
  color: var(--sz-evergreen);
  font-size: 30px;
  line-height: 1.18;
}

.maintenance-hero p {
  max-width: 760px;
  color: var(--sz-muted);
  line-height: 1.8;
}

.health-card {
  display: grid;
  align-content: center;
  gap: 7px;
  padding: 18px;
  border: 1px solid rgba(223, 210, 191, 0.78);
  border-radius: 18px;
  background: rgba(255, 250, 241, 0.88);
}

.health-card span,
.health-card small,
.card-head span,
.stat-grid span {
  color: var(--sz-muted);
  font-weight: 800;
}

.health-card strong {
  color: var(--sz-evergreen);
  font-size: 38px;
  line-height: 1;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-grid article,
.admin-card {
  border: 1px solid var(--sz-line);
  border-radius: 18px;
  background: rgba(255, 253, 247, 0.92);
  box-shadow: var(--sz-shadow-soft);
}

.stat-grid article {
  display: grid;
  gap: 8px;
  padding: 18px;
}

.stat-grid strong {
  color: var(--sz-evergreen);
  font-size: 26px;
  line-height: 1;
}

.stat-grid em {
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

.card-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.card-head h2 {
  color: var(--sz-evergreen);
  font-size: 22px;
  line-height: 1.25;
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

.maintenance-advice {
  display: grid;
  gap: 16px;
}

.advice-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.advice-grid article {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(184, 220, 199, 0.74);
  border-radius: 16px;
  background: rgba(220, 239, 228, 0.34);
}

.advice-grid strong {
  color: var(--sz-evergreen);
  font-size: 17px;
}

.maintenance-advice p {
  color: var(--sz-muted);
  line-height: 1.7;
}

@media (max-width: 920px) {
  .maintenance-hero,
  .stat-grid,
  .dashboard-grid,
  .advice-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .maintenance-hero,
  .stat-grid,
  .dashboard-grid,
  .advice-grid {
    grid-template-columns: 1fr;
  }
}
</style>
