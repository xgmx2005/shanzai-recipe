<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, ChevronDown, Clock, Heart, Home, ListChecks, LogOut, Sparkles, UserRound } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const navItems = [
  { label: '首页', to: '/user/home', icon: Home },
  { label: '健康档案', to: '/user/profile', icon: UserRound },
  { label: '智能推荐', to: '/user/recommend', icon: Sparkles },
  { label: '购物清单', to: '/user/shopping-lists', icon: ListChecks },
  { label: '收藏菜谱', to: '/user/favorites', icon: Heart },
  { label: '推荐历史', to: '/user/history', icon: Clock },
]

const nickname = computed(() => auth.user?.nickname ?? '小膳用户')
const isHomePage = computed(() => route.name === 'user-home')

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
        <div class="avatar" aria-hidden="true">{{ nickname.slice(0, 1) }}</div>
        <div class="user-copy">
          <span>{{ nickname }}</span>
          <small>日常健康</small>
        </div>
        <ChevronDown class="chevron" />
        <button type="button" class="notice-button" aria-label="通知">
          <Bell />
        </button>
        <button type="button" class="logout-button" aria-label="退出登录" @click="logout">
          <LogOut />
        </button>
      </div>
    </header>
    <main class="sz-page page-content" :class="{ 'is-home-page': isHomePage }">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.user-shell {
  --user-page-inline: clamp(32px, 3.75vw, 60px);
  min-height: 100vh;
  padding: 0 0 52px;
  background:
    radial-gradient(circle at 8% 0%, rgba(220, 239, 228, 0.46), transparent 30%),
    linear-gradient(180deg, #fffdf8 0%, var(--sz-bg-soft) 100%);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 24px;
  align-items: center;
  width: 100%;
  max-width: none;
  min-height: 72px;
  padding: 0 max(var(--user-page-inline), calc((100vw - 1800px) / 2));
  border-bottom: 1px solid rgba(223, 210, 191, 0.88);
  background: rgba(255, 253, 248, 0.94);
  box-shadow: 0 8px 24px rgba(23, 37, 31, 0.05);
  backdrop-filter: blur(18px);
}

.brand {
  display: grid;
  gap: 2px;
}

.brand strong {
  color: var(--sz-deep-green);
  font-size: 32px;
  line-height: 1;
}

.brand span {
  color: var(--sz-muted);
  font-size: 12px;
}

nav {
  display: flex;
  justify-content: center;
  gap: 22px;
}

nav a {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 72px;
  padding: 0 2px;
  color: var(--sz-muted);
  font-weight: 700;
  transition:
    color 0.18s ease;
}

nav svg {
  width: 17px;
  height: 17px;
}

nav a.router-link-active {
  color: var(--sz-deep-green);
}

nav a.router-link-active::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  border-radius: 999px 999px 0 0;
  background: var(--sz-deep-green);
  content: '';
}

.user-menu {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--sz-text);
  font-weight: 700;
}

.avatar {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 2px solid #fff4e4;
  border-radius: 50%;
  color: #ffffff;
  background: linear-gradient(135deg, var(--sz-green-dark), var(--sz-grain));
  box-shadow: 0 7px 16px rgba(23, 37, 31, 0.14);
  font-size: 15px;
}

.user-copy {
  display: grid;
  gap: 1px;
  min-width: 74px;
}

.user-copy small {
  color: var(--sz-muted);
  font-size: 12px;
  font-weight: 500;
}

.chevron {
  width: 15px;
  height: 15px;
  color: var(--sz-muted);
}

.notice-button,
.logout-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  color: var(--sz-muted);
  background: transparent;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease;
}

.notice-button:hover,
.logout-button:hover {
  color: var(--sz-deep-green);
  background: var(--sz-mint);
}

.notice-button svg,
.logout-button svg {
  width: 17px;
  height: 17px;
}

.page-content {
  width: min(1180px, calc(100vw - 40px));
  max-width: 1180px;
  padding-top: 18px;
}

.page-content.is-home-page {
  width: min(1800px, calc(100vw - (var(--user-page-inline) * 2)));
  max-width: none;
}

@media (max-width: 920px) {
  .topbar {
    grid-template-columns: 1fr auto;
    padding: 12px 18px;
  }

  .page-content {
    width: min(100% - 24px, 1180px);
  }

  .brand {
    grid-column: 1;
    grid-row: 1;
  }

  .user-menu {
    grid-column: 2;
    grid-row: 1;
    justify-self: end;
  }

  nav {
    grid-column: 1 / -1;
    grid-row: 2;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
  }
}

@media (max-width: 560px) {
  .user-copy,
  .brand span,
  nav a span {
    display: none;
  }

  nav {
    gap: 14px;
  }

  nav a {
    min-height: 34px;
  }
}
</style>
