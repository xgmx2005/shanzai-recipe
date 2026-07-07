<script setup lang="ts">
const stats = [
  { label: '用户总数', value: '12,845', trend: '+12.6%' },
  { label: '菜谱数量', value: '1,286', trend: '+8.3%' },
  { label: '食材数量', value: '632', trend: '+6.5%' },
  { label: '今日推荐次数', value: '4,368', trend: '+15.2%' },
]
</script>

<template>
  <section class="dashboard">
    <div class="stat-grid">
      <article v-for="stat in stats" :key="stat.label">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <em>{{ stat.trend }}</em>
      </article>
    </div>
    <div class="admin-card">
      <h1>维护端看板</h1>
      <p>当前第一批只搭建 AdminLayout 和看板基线，后续会继续补菜谱管理、食材管理和统计图。</p>
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

h1,
p {
  margin: 0;
}

p {
  margin-top: 10px;
  color: var(--sz-muted);
}

@media (max-width: 920px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
