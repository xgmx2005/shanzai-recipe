import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('naive-ui')) return 'vendor-naive-ui'
          if (id.includes('@css-render') || id.includes('vueuc') || id.includes('vooks') || id.includes('date-fns')) {
            return 'vendor-naive-ui'
          }
          if (id.includes('@vue') || id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) {
            return 'vendor-vue'
          }
          if (id.includes('@lucide')) return 'vendor-icons'
          if (id.includes('axios')) return 'vendor-http'
          return 'vendor'
        },
      },
    },
  },
})
