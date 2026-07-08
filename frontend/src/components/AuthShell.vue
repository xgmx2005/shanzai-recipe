<script setup lang="ts">
import { CheckCircle2, Leaf, Sparkles } from '@lucide/vue'

defineProps<{
  eyebrow: string
  title: string
  description: string
  imageUrl?: string
}>()

const defaultImage =
  'https://images.unsplash.com/photo-1498837167922-ddd27525d352?auto=format&fit=crop&w=1200&q=85'
</script>

<template>
  <main class="auth-page">
    <section class="auth-shell">
      <aside class="brand-panel">
        <img :src="imageUrl || defaultImage" alt="膳哉健康餐食" />
        <div class="brand-overlay" />
        <div class="brand-content">
          <div class="logo-row">
            <Leaf />
            <strong>膳哉</strong>
          </div>
          <div class="brand-copy">
            <p><Sparkles /> AI 健康膳食助手</p>
            <h1>让每一餐都更适合<br />今天的你</h1>
          </div>
          <ol>
            <li><CheckCircle2 /> 健康档案驱动推荐</li>
            <li><CheckCircle2 /> 匹配食材、目标和时间</li>
            <li><CheckCircle2 /> 自动生成购物清单</li>
          </ol>
        </div>
      </aside>

      <section class="form-panel">
        <div class="form-card">
          <p class="eyebrow">{{ eyebrow }}</p>
          <h2>{{ title }}</h2>
          <p class="description">{{ description }}</p>
          <slot />
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background:
    linear-gradient(135deg, rgba(248, 251, 245, 0.92), rgba(255, 247, 231, 0.96)),
    var(--sz-surface-soft);
}

.auth-shell {
  width: min(1080px, 100%);
  min-height: 680px;
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(420px, 1.05fr);
  gap: 18px;
  padding: 18px;
  border: 1px solid rgba(227, 218, 203, 0.9);
  border-radius: 28px;
  background: rgba(255, 250, 241, 0.86);
  box-shadow: 0 24px 54px rgba(31, 42, 36, 0.14);
}

.brand-panel {
  position: relative;
  overflow: hidden;
  min-height: 640px;
  border-radius: 22px;
  background: var(--sz-evergreen);
}

.brand-panel img,
.brand-overlay {
  position: absolute;
  inset: 0;
}

.brand-panel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.brand-overlay {
  background:
    linear-gradient(180deg, rgba(18, 61, 45, 0.2) 0%, rgba(18, 61, 45, 0.88) 100%),
    rgba(18, 61, 45, 0.34);
}

.brand-content {
  position: relative;
  z-index: 1;
  min-height: 100%;
  display: grid;
  align-content: space-between;
  gap: 32px;
  padding: 34px;
  color: #ffffff;
}

.logo-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-row svg {
  width: 28px;
  height: 28px;
}

.logo-row strong {
  font-size: 40px;
  letter-spacing: 0;
}

.brand-copy {
  display: grid;
  gap: 16px;
}

.brand-copy p,
ol,
h1,
h2,
.description {
  margin: 0;
}

.brand-copy p {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.86);
  font-weight: 800;
}

.brand-copy p svg {
  width: 17px;
  height: 17px;
}

h1 {
  max-width: 420px;
  font-size: 42px;
  line-height: 1.16;
}

ol {
  display: grid;
  gap: 12px;
  padding: 0;
  list-style: none;
}

li {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid rgba(255, 250, 241, 0.18);
  border-radius: var(--sz-radius-pill);
  background: rgba(255, 255, 255, 0.16);
  font-weight: 800;
}

li svg {
  width: 18px;
  height: 18px;
}

.form-panel {
  display: grid;
  place-items: center;
  padding: 30px;
}

.form-card {
  width: min(420px, 100%);
  display: grid;
  gap: 18px;
  padding: 34px;
  border: 1px solid rgba(227, 218, 203, 0.9);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 44px rgba(31, 42, 36, 0.1);
}

.eyebrow {
  margin: 0;
  color: var(--sz-green-dark);
  font-weight: 900;
}

h2 {
  margin: 0;
  color: var(--sz-deep-green);
  font-size: 32px;
  line-height: 1.2;
}

.description {
  color: var(--sz-muted);
  line-height: 1.75;
}

@media (max-width: 900px) {
  .auth-page {
    place-items: stretch;
    padding: 16px;
  }

  .auth-shell {
    min-height: 0;
    grid-template-columns: 1fr;
  }

  .form-panel {
    order: 1;
    padding: 12px;
  }

  .brand-panel {
    order: 2;
    min-height: 360px;
  }

  .brand-content {
    padding: 24px;
  }

  h1 {
    font-size: 32px;
  }

  .form-card {
    padding: 26px 20px;
  }
}
</style>
