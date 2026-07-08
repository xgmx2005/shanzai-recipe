<script setup lang="ts">
import type { GlobalThemeOverrides } from 'naive-ui'

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#236B4B',
    primaryColorHover: '#2C8B57',
    primaryColorPressed: '#123D2D',
    borderRadius: '14px',
    fontFamily:
      'Inter, "PingFang SC", "Microsoft YaHei", "Noto Sans SC", Arial, sans-serif',
  },
  Button: {
    borderRadiusMedium: '999px',
    borderRadiusLarge: '999px',
    heightMedium: '42px',
    heightLarge: '50px',
  },
  Card: {
    borderRadius: '16px',
  },
  Input: {
    borderRadius: '14px',
  },
  Select: {
    peers: {
      InternalSelection: {
        borderRadius: '14px',
      },
    },
  },
}
</script>

<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <n-message-provider>
      <router-view v-slot="{ Component, route }">
        <transition :name="route.meta.authPage ? 'auth-route' : undefined" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </router-view>
    </n-message-provider>
  </n-config-provider>
</template>

<style>
.auth-route-enter-active,
.auth-route-leave-active {
  transition:
    opacity 0.24s ease,
    transform 0.24s ease,
    filter 0.24s ease;
}

.auth-route-enter-from {
  opacity: 0;
  filter: blur(8px);
  transform: translateY(14px) scale(0.985);
}

.auth-route-leave-to {
  opacity: 0;
  filter: blur(6px);
  transform: translateY(-10px) scale(0.99);
}

@media (prefers-reduced-motion: reduce) {
  .auth-route-enter-active,
  .auth-route-leave-active {
    transition: opacity 0.01s linear;
  }

  .auth-route-enter-from,
  .auth-route-leave-to {
    filter: none;
    transform: none;
  }
}
</style>
