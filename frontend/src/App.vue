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
      <router-view />
    </n-message-provider>
  </n-config-provider>
</template>
