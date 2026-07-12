<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronDown, Clock, Heart, Home, ListChecks, LogOut, Sparkles, UserRound } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { useRecommendationTransitionStore } from '@/stores/recommendationTransition'
import { backendAssetUrl } from '@/api/http'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const routeTransition = useRecommendationTransitionStore()

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
const avatarUrl = computed(() => backendAssetUrl(auth.user?.avatarUrl))
const isHomePage = computed(() => route.name === 'user-home')
const pageTransitionName = computed(() =>
  route.name === 'recommend-result' ? 'recommend-result-transition' : 'user-page-transition',
)
let routeTransitionTimer: number | undefined

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
onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  if (routeTransitionTimer) window.clearTimeout(routeTransitionTimer)
})

watch(
  () => route.name,
  (name) => {
    if (name !== 'recommend-result' || !routeTransition.active) return
    routeTransition.markServing()
    if (routeTransitionTimer) window.clearTimeout(routeTransitionTimer)
    routeTransitionTimer = window.setTimeout(() => routeTransition.finish(), 980)
  },
)
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
            <span class="avatar" :class="avatarThemeClass" aria-hidden="true">
              <img v-if="avatarUrl" :src="avatarUrl" alt="" />
              <template v-else>{{ avatarText }}</template>
            </span>
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
      <router-view v-slot="{ Component, route: viewRoute }">
        <transition :name="pageTransitionName" mode="out-in">
          <component :is="Component" :key="viewRoute.fullPath" />
        </transition>
      </router-view>
    </main>
    <transition name="cooking-route-transition">
      <div
        v-if="routeTransition.active"
        class="route-cooking-overlay"
        :class="{ 'is-serving': routeTransition.phase === 'serving' }"
        role="status"
        aria-live="polite"
      >
        <div class="route-cooking-scene">
          <div class="route-cooking-copy">
            <span>膳哉 Agent</span>
            <strong>{{ routeTransition.phase === 'serving' ? '正在打开推荐结果' : '正在为你规划这一餐' }}</strong>
            <small>{{ routeTransition.summary || '整理本次推荐条件' }}</small>
          </div>

          <div class="cooking-bench" aria-hidden="true">
            <div class="prep-board">
              <i class="ingredient-piece piece-leaf" />
              <i class="ingredient-piece piece-grain" />
              <i class="ingredient-piece piece-tomato" />
              <i class="knife-line" />
            </div>
            <div class="pan-stage">
              <i class="steam-line steam-one" />
              <i class="steam-line steam-two" />
              <i class="pan-handle" />
              <i class="pan-body">
                <span />
                <span />
                <span />
              </i>
            </div>
            <div class="plate-stage">
              <i class="plate">
                <span />
                <span />
              </i>
            </div>
          </div>

          <div class="route-cooking-steps" aria-hidden="true">
            <span class="generating-step">理解饮食目标</span>
            <span class="generating-step">匹配知识库菜谱</span>
            <span class="generating-step">生成推荐理由</span>
            <span :class="{ active: routeTransition.phase === 'serving' }">呈现结果</span>
          </div>
          <div class="route-cooking-progress" aria-hidden="true">
            <span />
          </div>
        </div>
      </div>
    </transition>
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
  overflow: hidden;
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

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  position: relative;
  width: min(1180px, calc(100vw - 40px));
  max-width: 1180px;
  padding-top: 18px;
  perspective: 1200px;
}

.page-content.is-home-page {
  width: min(1800px, calc(100vw - (var(--user-page-inline) * 2)));
  max-width: none;
}

:deep(.user-page-transition-enter-active),
:deep(.user-page-transition-leave-active) {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

:deep(.user-page-transition-enter-from),
:deep(.user-page-transition-leave-to) {
  opacity: 0;
  transform: translateY(6px);
}

:deep(.recommend-result-transition-enter-active) {
  transition:
    opacity 0.36s ease,
    transform 0.42s cubic-bezier(0.2, 0.9, 0.18, 1),
    filter 0.36s ease;
  transform-origin: 50% 18%;
}

:deep(.recommend-result-transition-leave-active) {
  transition:
    opacity 0.16s ease,
    transform 0.16s ease,
    filter 0.16s ease;
}

:deep(.recommend-result-transition-enter-from) {
  opacity: 0;
  filter: blur(8px);
  transform: translateY(22px) scale(0.982);
}

:deep(.recommend-result-transition-leave-to) {
  opacity: 0;
  filter: blur(3px);
  transform: translateY(-8px) scale(0.996);
}

.route-cooking-overlay {
  position: fixed;
  z-index: 80;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 28px;
  color: var(--sz-evergreen);
  background:
    radial-gradient(circle at 50% 22%, rgba(220, 239, 228, 0.72), transparent 34%),
    linear-gradient(180deg, rgba(255, 253, 248, 0.98), rgba(246, 241, 231, 0.98));
  will-change: opacity, transform;
}

.route-cooking-scene {
  contain: layout paint style;
  display: grid;
  gap: 22px;
  width: min(760px, 100%);
  padding: clamp(24px, 5vw, 46px);
  border: 1px solid rgba(223, 210, 191, 0.92);
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(255, 253, 248, 0.94), rgba(220, 239, 228, 0.72)),
    var(--sz-surface);
  box-shadow: 0 24px 54px rgba(23, 37, 31, 0.14);
  transform: translate3d(0, 0, 0);
  will-change: transform, opacity;
}

.route-cooking-copy {
  display: grid;
  justify-items: center;
  gap: 8px;
  text-align: center;
}

.route-cooking-copy span {
  min-height: 28px;
  padding: 5px 12px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 12px;
  font-weight: 900;
}

.route-cooking-copy strong {
  color: var(--sz-evergreen);
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.1;
}

.route-cooking-copy small {
  color: var(--sz-muted);
  font-size: 14px;
  font-weight: 800;
}

.cooking-bench {
  position: relative;
  contain: layout paint;
  display: grid;
  grid-template-columns: 1fr 1.15fr 1fr;
  gap: 18px;
  align-items: end;
  min-height: 190px;
  padding: 24px 22px 28px;
  border: 1px solid rgba(223, 210, 191, 0.86);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 250, 241, 0.6), rgba(244, 231, 200, 0.46)),
    rgba(255, 253, 248, 0.72);
}

.cooking-bench::after {
  position: absolute;
  right: 22px;
  bottom: 22px;
  left: 22px;
  height: 5px;
  border-radius: var(--sz-radius-pill);
  background: rgba(31, 77, 58, 0.14);
  content: '';
}

.prep-board,
.pan-stage,
.plate-stage {
  position: relative;
  min-height: 128px;
}

.prep-board {
  align-self: end;
  border: 1px solid rgba(205, 189, 166, 0.86);
  border-radius: 18px;
  background:
    repeating-linear-gradient(90deg, rgba(230, 184, 92, 0.14) 0 10px, transparent 10px 20px),
    rgba(255, 250, 241, 0.9);
  transform: rotate(-2deg);
}

.ingredient-piece {
  position: absolute;
  display: block;
  border-radius: 999px;
  animation: ingredient-float 2.6s ease-in-out infinite;
  will-change: transform;
}

.piece-leaf {
  top: 34px;
  left: 34px;
  width: 42px;
  height: 22px;
  background: var(--sz-green-fresh);
}

.piece-grain {
  top: 74px;
  left: 82px;
  width: 34px;
  height: 34px;
  background: var(--sz-grain);
  animation-delay: 0.15s;
}

.piece-tomato {
  right: 38px;
  bottom: 32px;
  width: 30px;
  height: 30px;
  background: var(--sz-tomato);
  animation-delay: 0.28s;
}

.knife-line {
  position: absolute;
  top: 50px;
  right: 26px;
  width: 74px;
  height: 5px;
  border-radius: var(--sz-radius-pill);
  background: var(--sz-deep-green);
  box-shadow: 22px 9px 0 rgba(31, 77, 58, 0.22);
  transform: rotate(-18deg);
  animation: knife-glide 1.9s ease-in-out infinite;
  will-change: transform;
}

.pan-stage {
  display: grid;
  place-items: end center;
}

.pan-body {
  position: relative;
  display: block;
  width: 190px;
  height: 74px;
  border: 6px solid var(--sz-deep-green);
  border-top: 0;
  border-radius: 0 0 90px 90px;
  background: linear-gradient(180deg, rgba(44, 139, 87, 0.18), rgba(18, 61, 45, 0.12));
  animation: pan-sway 2.15s ease-in-out infinite;
  transform: translate3d(0, 0, 0);
  will-change: transform;
}

.pan-body span {
  position: absolute;
  top: 14px;
  width: 22px;
  height: 14px;
  border-radius: 999px;
  background: var(--sz-grain);
  animation: food-float 2.15s ease-in-out infinite;
  will-change: transform;
}

.pan-body span:nth-child(1) {
  left: 42px;
}

.pan-body span:nth-child(2) {
  left: 82px;
  background: var(--sz-green-fresh);
  animation-delay: 0.08s;
}

.pan-body span:nth-child(3) {
  right: 44px;
  background: var(--sz-tomato);
  animation-delay: 0.16s;
}

.pan-handle {
  position: absolute;
  right: -18px;
  bottom: 40px;
  width: 72px;
  height: 10px;
  border-radius: var(--sz-radius-pill);
  background: var(--sz-deep-green);
  transform: rotate(10deg);
}

.steam-line {
  position: absolute;
  bottom: 106px;
  width: 8px;
  height: 44px;
  border: solid rgba(35, 107, 75, 0.34);
  border-width: 0 0 0 2px;
  border-radius: 999px;
  animation: steam-rise 2.1s ease-in-out infinite;
  will-change: opacity, transform;
}

.steam-one {
  left: calc(50% - 26px);
}

.steam-two {
  left: calc(50% + 20px);
  animation-delay: 0.22s;
}

.plate-stage {
  display: grid;
  place-items: end center;
}

.plate {
  position: relative;
  display: block;
  width: 150px;
  height: 94px;
  border: 8px solid rgba(35, 107, 75, 0.16);
  border-radius: 50%;
  background: rgba(255, 253, 248, 0.94);
  box-shadow: inset 0 0 0 18px rgba(220, 239, 228, 0.62);
}

.plate span {
  position: absolute;
  border-radius: 999px;
  opacity: 0;
}

.plate span:first-child {
  top: 34px;
  left: 38px;
  width: 42px;
  height: 18px;
  background: var(--sz-green-fresh);
}

.plate span:last-child {
  right: 42px;
  bottom: 28px;
  width: 32px;
  height: 32px;
  background: var(--sz-grain);
}

.route-cooking-overlay.is-serving .plate span {
  opacity: 1;
  animation: plate-pop 0.52s ease-out both;
  will-change: opacity, transform;
}

.route-cooking-overlay.is-serving .plate span:last-child {
  animation-delay: 0.08s;
}

.route-cooking-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.route-cooking-steps span {
  min-height: 38px;
  padding: 10px 8px;
  border: 1px solid rgba(35, 107, 75, 0.12);
  border-radius: 13px;
  color: var(--sz-muted);
  background: rgba(255, 253, 248, 0.68);
  font-size: 13px;
  font-weight: 900;
  text-align: center;
  transition:
    color 0.24s ease,
    background-color 0.24s ease,
    border-color 0.24s ease;
}

.route-cooking-steps span.active {
  color: #ffffff;
  background: var(--sz-green-dark);
  border-color: transparent;
}

.route-cooking-overlay:not(.is-serving) .route-cooking-steps .generating-step {
  animation: cooking-step-light 2.4s ease-in-out infinite;
}

.route-cooking-overlay:not(.is-serving) .route-cooking-steps .generating-step:nth-child(2) {
  animation-delay: 0.42s;
}

.route-cooking-overlay:not(.is-serving) .route-cooking-steps .generating-step:nth-child(3) {
  animation-delay: 0.84s;
}

.route-cooking-progress {
  overflow: hidden;
  height: 5px;
  border-radius: var(--sz-radius-pill);
  background: rgba(31, 77, 58, 0.12);
}

.route-cooking-progress span {
  display: block;
  width: 46%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, transparent, var(--sz-green-dark), transparent);
  animation: cooking-progress 1.65s cubic-bezier(0.45, 0, 0.25, 1) infinite;
  transform: translate3d(-100%, 0, 0);
  will-change: transform;
}

.route-cooking-overlay.is-serving .route-cooking-progress span {
  width: 100%;
  background: var(--sz-green-dark);
  animation: none;
  transform: translate3d(0, 0, 0);
}

.cooking-route-transition-enter-active {
  transition:
    opacity 0.32s ease,
    transform 0.38s cubic-bezier(0.2, 0.9, 0.18, 1);
}

.cooking-route-transition-leave-active {
  transition:
    opacity 0.24s ease,
    transform 0.24s ease;
}

.cooking-route-transition-enter-from,
.cooking-route-transition-leave-to {
  opacity: 0;
  transform: translate3d(0, 8px, 0) scale(0.99);
}

@keyframes ingredient-float {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(0deg);
  }

  50% {
    transform: translate3d(0, -6px, 0) rotate(4deg);
  }
}

@keyframes knife-glide {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(-18deg);
  }

  50% {
    transform: translate3d(-8px, 10px, 0) rotate(-18deg);
  }
}

@keyframes pan-sway {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(0deg);
  }

  50% {
    transform: translate3d(0, -3px, 0) rotate(-2deg);
  }
}

@keyframes food-float {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }

  50% {
    transform: translate3d(0, -13px, 0);
  }
}

@keyframes steam-rise {
  0% {
    opacity: 0;
    transform: translate3d(0, 8px, 0);
  }

  45% {
    opacity: 0.86;
  }

  100% {
    opacity: 0;
    transform: translate3d(0, -12px, 0);
  }
}

@keyframes plate-pop {
  from {
    opacity: 0;
    transform: translate3d(0, 8px, 0) scale(0.78);
  }

  to {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
}

@keyframes cooking-progress {
  to {
    transform: translate3d(230%, 0, 0);
  }
}

@keyframes cooking-step-light {
  0%,
  18%,
  100% {
    color: var(--sz-muted);
    background: rgba(255, 253, 248, 0.68);
    border-color: rgba(35, 107, 75, 0.12);
    transform: translate3d(0, 0, 0);
  }

  34%,
  62% {
    color: #ffffff;
    background: var(--sz-green-dark);
    border-color: transparent;
    transform: translate3d(0, -1px, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  :deep(.user-page-transition-enter-active),
  :deep(.user-page-transition-leave-active),
  :deep(.recommend-result-transition-enter-active),
  :deep(.recommend-result-transition-leave-active),
  .cooking-route-transition-enter-active,
  .cooking-route-transition-leave-active {
    transition: none;
  }

  :deep(.user-page-transition-enter-from),
  :deep(.user-page-transition-leave-to),
  :deep(.recommend-result-transition-enter-from),
  :deep(.recommend-result-transition-leave-to),
  .cooking-route-transition-enter-from,
  .cooking-route-transition-leave-to {
    opacity: 1;
    filter: none;
    transform: none;
  }

  .ingredient-piece,
  .knife-line,
  .pan-body,
  .pan-body span,
  .steam-line,
  .route-cooking-overlay.is-serving .plate span,
  .route-cooking-progress span,
  .route-cooking-steps .generating-step {
    animation: none;
  }
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

  .route-cooking-scene {
    padding: 22px;
  }

  .cooking-bench {
    grid-template-columns: 1fr;
    min-height: 420px;
  }

  .route-cooking-steps {
    grid-template-columns: 1fr;
  }
}
</style>
