<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronDown, Clock, Heart, Home, ListChecks, LogOut, Sparkles, UserRound } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const navItems = [
  { label: '首页', to: '/user/home', icon: Home },
  { label: '智能推荐', to: '/user/recommend', icon: Sparkles },
  { label: '购物清单', to: '/user/shopping-lists', icon: ListChecks },
]

const accountMenuItems = [
  { label: '健康档案', to: '/user/profile', icon: UserRound },
  { label: '收藏菜谱', to: '/user/favorites', icon: Heart },
  { label: '推荐历史', to: '/user/history', icon: Clock },
]

const accountMenuOpen = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)
const nickname = computed(() => auth.user?.nickname ?? '小膳用户')
const avatarText = computed(() => nickname.value.slice(0, 1))
const avatarThemeClass = computed(() => `theme-${auth.user?.avatarTheme ?? 'leaf'}`)
const isHomePage = computed(() => route.name === 'user-home')

function logout() {
  auth.logout()
  router.push('/login')
}

function toggleAccountMenu() {
  accountMenuOpen.value = !accountMenuOpen.value
}

function openAccountPage(to: string) {
  accountMenuOpen.value = false
  router.push(to)
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target
  if (!(target instanceof Node)) return
  if (!userMenuRef.value?.contains(target)) {
    accountMenuOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', handleDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', handleDocumentClick))
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
      <div class="user-actions">
        <div ref="userMenuRef" class="user-menu-wrap">
          <button
            type="button"
            class="user-menu"
            aria-label="打开用户菜单"
            :aria-expanded="accountMenuOpen"
            @click="toggleAccountMenu"
          >
            <span class="avatar" :class="avatarThemeClass" aria-hidden="true">{{ avatarText }}</span>
            <span class="user-copy">
              <span>{{ nickname }}</span>
              <small>日常健康</small>
            </span>
            <ChevronDown class="chevron" :class="{ open: accountMenuOpen }" />
          </button>

          <div v-if="accountMenuOpen" class="account-dropdown" role="menu">
            <button
              v-for="item in accountMenuItems"
              :key="item.to"
              type="button"
              role="menuitem"
              @click="openAccountPage(item.to)"
            >
              <component :is="item.icon" />
              {{ item.label }}
            </button>
          </div>
        </div>

        <button type="button" class="logout-button" aria-label="退出登录" title="退出登录" @click="logout">
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
  white-space: nowrap;
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

.user-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.user-menu-wrap {
  position: relative;
}

.user-menu {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 48px;
  padding: 4px 8px;
  border: 1px solid transparent;
  border-radius: 14px;
  color: var(--sz-text);
  background: transparent;
  font-weight: 700;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease;
}

.user-menu:hover {
  border-color: rgba(35, 107, 75, 0.14);
  background: rgba(220, 239, 228, 0.42);
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

.avatar.theme-leaf {
  background: linear-gradient(135deg, var(--sz-green-dark), var(--sz-grain));
}

.avatar.theme-mint {
  background: linear-gradient(135deg, #2c8b57, #8bcf9b);
}

.avatar.theme-tomato {
  background: linear-gradient(135deg, #e65b3e, #e6b85c);
}

.avatar.theme-grain {
  background: linear-gradient(135deg, #b16b18, #e6b85c);
}

.avatar.theme-blue {
  background: linear-gradient(135deg, #2c6f86, #8fc4d1);
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
  transition: transform 0.18s ease;
}

.chevron.open {
  transform: rotate(180deg);
}

.account-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 30;
  display: grid;
  gap: 4px;
  min-width: 168px;
  padding: 8px;
  border: 1px solid rgba(223, 210, 191, 0.92);
  border-radius: 14px;
  background: rgba(255, 253, 248, 0.98);
  box-shadow: 0 18px 36px rgba(23, 37, 31, 0.14);
}

.account-dropdown button {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 38px;
  padding: 0 10px;
  border: 0;
  border-radius: 10px;
  color: var(--sz-text);
  background: transparent;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
}

.account-dropdown button:hover {
  color: var(--sz-deep-green);
  background: var(--sz-mint);
}

.account-dropdown svg,
.logout-button svg {
  width: 17px;
  height: 17px;
}

.logout-button {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  color: var(--sz-muted);
  background: transparent;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease;
}

.logout-button:hover {
  color: var(--sz-deep-green);
  background: var(--sz-mint);
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

@media (max-width: 1180px) {
  .topbar {
    grid-template-columns: 1fr auto;
    padding: 12px 18px;
  }

  .page-content {
    width: min(100% - 24px, 1180px);
  }

  .page-content.is-home-page {
    width: min(100% - 24px, 1800px);
  }

  .brand {
    grid-column: 1;
    grid-row: 1;
  }

  .user-actions {
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

@media (max-width: 920px) {
  .user-shell {
    --user-page-inline: 18px;
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
