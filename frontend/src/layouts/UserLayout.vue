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
    <button class="floating-recommend" type="button" aria-label="快速生成推荐" @click="router.push('/user/recommend')">
      <Sparkles />
    </button>
  </div>
</template>

<style scoped>
.user-shell {
  min-height: 100vh;
  padding: 22px 0 52px;
}

.topbar {
  position: sticky;
  top: 14px;
  z-index: 20;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 28px;
  align-items: center;
  min-height: 78px;
  padding: 12px 18px 12px 22px;
  border: 1px solid rgba(223, 210, 191, 0.96);
  border-radius: 26px;
  background: rgba(255, 250, 241, 0.9);
  box-shadow: 0 9px 24px rgba(23, 37, 31, 0.07);
  backdrop-filter: blur(20px);
}

.brand {
  display: grid;
  gap: 2px;
}

.brand strong {
  color: var(--sz-deep-green);
  font-size: 30px;
  line-height: 1;
}

.brand span {
  color: var(--sz-muted);
  font-size: 12px;
}

nav {
  display: flex;
  justify-content: center;
  gap: 4px;
}

nav a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 42px;
  padding: 0 14px;
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
  background: #d8eadf;
}

.user-menu {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--sz-text);
  font-weight: 700;
}

.page-content {
  padding-top: 24px;
}

.floating-recommend {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 30;
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  border: 0;
  border-radius: 50%;
  color: #ffffff;
  background: var(--sz-evergreen);
  box-shadow: 0 14px 28px rgba(18, 61, 45, 0.22);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    background 0.18s ease;
}

.floating-recommend:hover {
  background: var(--sz-green-dark);
  transform: translateY(-2px);
}

.floating-recommend:active {
  transform: scale(0.96);
}

.floating-recommend svg {
  width: 22px;
  height: 22px;
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

  .floating-recommend {
    right: 18px;
    bottom: 18px;
  }
}
</style>
