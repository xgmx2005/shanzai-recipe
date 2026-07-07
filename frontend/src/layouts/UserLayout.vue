<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Heart, Home, ListChecks, LogOut, Sparkles, UserRound } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const navItems = [
  { label: '首页', to: '/user/home', icon: Home },
  { label: '健康档案', to: '/user/profile', icon: UserRound },
  { label: '智能推荐', to: '/user/recommend', icon: Sparkles },
  { label: '购物清单', to: '/user/shopping-lists', icon: ListChecks },
  { label: '收藏与历史', to: '/user/favorites', icon: Heart },
]

const nickname = computed(() => auth.user?.nickname ?? '小膳用户')

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="user-shell">
    <header class="topbar sz-page">
      <router-link class="brand" to="/user/home" aria-label="膳哉首页">
        <strong>膳哉</strong>
        <span>让每一餐更懂你的身体</span>
      </router-link>
      <nav>
        <router-link v-for="item in navItems" :key="item.to" :to="item.to">
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <div class="user-menu">
        <span>{{ nickname }}</span>
        <n-button size="small" quaternary circle aria-label="退出登录" @click="logout">
          <template #icon>
            <n-icon><LogOut /></n-icon>
          </template>
        </n-button>
      </div>
    </header>
    <main class="sz-page page-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.user-shell {
  min-height: 100vh;
  padding: 18px 0 42px;
}

.topbar {
  position: sticky;
  top: 12px;
  z-index: 20;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 24px;
  align-items: center;
  min-height: 72px;
  padding: 12px 18px;
  border: 1px solid rgba(227, 218, 203, 0.92);
  border-radius: var(--sz-radius-panel);
  background: rgba(255, 253, 247, 0.88);
  backdrop-filter: blur(18px);
}

.brand {
  display: grid;
  gap: 2px;
}

.brand strong {
  color: var(--sz-deep-green);
  font-size: 28px;
  line-height: 1;
}

.brand span {
  color: var(--sz-muted);
  font-size: 12px;
}

nav {
  display: flex;
  justify-content: center;
  gap: 6px;
}

nav a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 13px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-muted);
  font-weight: 700;
  transition:
    color 0.18s ease,
    background 0.18s ease;
}

nav svg {
  width: 17px;
  height: 17px;
}

nav a.router-link-active {
  color: var(--sz-deep-green);
  background: var(--sz-mint);
}

.user-menu {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--sz-text);
  font-weight: 700;
}

.page-content {
  padding-top: 22px;
}

@media (max-width: 920px) {
  .topbar {
    grid-template-columns: 1fr auto;
  }

  nav {
    grid-column: 1 / -1;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
  }
}

@media (max-width: 560px) {
  .user-menu span,
  .brand span,
  nav a span {
    display: none;
  }
}
</style>
