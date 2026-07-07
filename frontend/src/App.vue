<script setup lang="ts">
import type { GlobalThemeOverrides } from 'naive-ui'

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#2F9E63',
    primaryColorHover: '#248955',
    primaryColorPressed: '#1F4D3A',
    borderRadius: '16px',
    fontFamily:
      'Inter, "PingFang SC", "Microsoft YaHei", "Noto Sans SC", Arial, sans-serif',
  },
  Button: {
    borderRadiusMedium: '999px',
    borderRadiusLarge: '999px',
    heightMedium: '40px',
    heightLarge: '46px',
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
