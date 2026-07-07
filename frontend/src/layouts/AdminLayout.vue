<script setup lang="ts">
import { useRouter } from 'vue-router'
import { BarChart3, BookOpen, Carrot, LogOut, Settings } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const navItems = [
  { label: '概览看板', to: '/admin/dashboard', icon: BarChart3 },
  { label: '菜谱管理', to: '/admin/recipes', icon: BookOpen },
  { label: '食材管理', to: '/admin/ingredients', icon: Carrot },
  { label: '系统设置', to: '/admin/settings', icon: Settings },
]

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside>
      <div class="admin-brand">
        <strong>膳哉</strong>
        <span>维护端</span>
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
          <span>{{ auth.user?.nickname ?? '维护员' }}</span>
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
  grid-template-columns: 248px 1fr;
  background: var(--sz-bg);
}

aside {
  min-height: 100vh;
  padding: 28px 18px;
  color: #eaf7ee;
  background: linear-gradient(160deg, #173b2d, #081f19);
}

.admin-brand {
  display: grid;
  gap: 6px;
  margin-bottom: 36px;
  padding: 0 10px;
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
  min-height: 44px;
  padding: 0 12px;
  border-radius: 14px;
  color: rgba(234, 247, 238, 0.78);
  font-weight: 700;
}

nav a.router-link-active {
  color: #ffffff;
  background: rgba(47, 158, 99, 0.34);
}

nav svg {
  width: 18px;
  height: 18px;
}

.admin-main {
  padding: 22px;
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-panel);
  background: var(--sz-surface);
}

header div {
  display: grid;
  gap: 4px;
}

header strong {
  font-size: 20px;
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
