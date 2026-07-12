<script setup lang="ts">
import { useRouter } from 'vue-router'
import { BarChart3, BookOpen, Carrot, LogOut } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const navItems = [
  { label: '概览看板', to: '/admin/dashboard', icon: BarChart3 },
  { label: '菜谱管理', to: '/admin/recipes', icon: BookOpen },
  { label: '食材管理', to: '/admin/ingredients', icon: Carrot },
]

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="admin-shell knowledge-admin-shell">
    <aside>
      <div class="admin-brand">
        <strong>膳哉</strong>
        <span>知识库维护中心</span>
      </div>
      <nav>
        <router-link v-for="item in navItems" :key="item.to" :to="item.to">
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>
    <section class="admin-main">
      <header>
        <div>
          <strong>数据维护工作台</strong>
          <span>当前维护员 · {{ auth.user?.nickname ?? '维护员' }}</span>
        </div>
        <n-button size="small" quaternary round @click="logout">
          <template #icon>
            <n-icon><LogOut /></n-icon>
          </template>
          退出
        </n-button>
      </header>
      <router-view />
    </section>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  background:
    radial-gradient(circle at 78% 4%, rgba(220, 239, 228, 0.74), transparent 30%),
    var(--sz-bg);
}

aside {
  min-height: 100vh;
  padding: 28px 16px;
  color: #eaf7ee;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent 38%),
    linear-gradient(160deg, #173b2d, #081f19);
  box-shadow: 16px 0 38px rgba(23, 37, 31, 0.12);
}

.admin-brand {
  display: grid;
  gap: 6px;
  margin-bottom: 32px;
  padding: 0 12px 18px;
  border-bottom: 1px solid rgba(234, 247, 238, 0.14);
}

.admin-brand strong {
  font-size: 32px;
}

.admin-brand span {
  color: rgba(234, 247, 238, 0.72);
}

nav {
  display: grid;
  gap: 8px;
}

nav a {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 12px;
  border-radius: 12px;
  color: rgba(234, 247, 238, 0.78);
  font-weight: 700;
  transition:
    background 0.18s ease,
    color 0.18s ease;
}

nav a.router-link-active {
  color: #ffffff;
  background: rgba(47, 158, 99, 0.38);
}

nav a:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}

nav svg {
  width: 18px;
  height: 18px;
}

.admin-main {
  min-width: 0;
  padding: 22px clamp(18px, 3vw, 34px);
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid var(--sz-line);
  border-radius: 18px;
  background: rgba(255, 253, 247, 0.9);
  box-shadow: var(--sz-shadow-soft);
}

header div {
  display: grid;
  gap: 4px;
}

header strong {
  font-size: 20px;
  color: var(--sz-evergreen);
}

header span {
  color: var(--sz-muted);
}

@media (max-width: 760px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  aside {
    min-height: auto;
  }
}
</style>
