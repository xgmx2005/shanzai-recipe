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
    <section class="auth-shell frosted-auth-shell">
      <aside class="brand-panel">
        <img :src="imageUrl || defaultImage" alt="膳哉健康餐食" />
        <div class="brand-overlay" />
        <div class="brand-content">
          <div class="brand-copy">
            <p><Sparkles /> AI 健康膳食助手</p>
            <h1>把健康目标变成今晚能做的一餐</h1>
          </div>
          <ol>
            <li><CheckCircle2 /> 结合健康档案和饮食目标</li>
            <li><CheckCircle2 /> 解释推荐理由，不只给结果</li>
            <li><CheckCircle2 /> 一键生成可采购清单</li>
          </ol>
        </div>
      </aside>

      <section class="form-panel">
        <div class="brand-mark">
          <Leaf />
          <div>
            <strong>膳哉</strong>
            <span>让每一餐更懂你的身体</span>
          </div>
        </div>

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
    radial-gradient(circle at 16% 18%, rgba(190, 225, 206, 0.5), transparent 28%),
    radial-gradient(circle at 88% 78%, rgba(244, 211, 137, 0.34), transparent 30%),
    linear-gradient(135deg, rgba(248, 251, 245, 0.94), rgba(255, 246, 226, 0.94)),
    var(--sz-surface-soft);
}

.auth-shell {
  width: min(1080px, 100%);
  min-height: 660px;
  display: grid;
  grid-template-columns: minmax(480px, 1.08fr) minmax(360px, 0.92fr);
  gap: 16px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 34px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.68), rgba(255, 250, 236, 0.5)),
    rgba(255, 255, 255, 0.42);
  box-shadow:
    0 28px 70px rgba(28, 45, 35, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(26px) saturate(1.12);
}

.frosted-auth-shell {
  position: relative;
  overflow: hidden;
}

.frosted-auth-shell::before {
  content: '';
  position: absolute;
  inset: 16px 16px 16px 52%;
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.48), transparent),
    rgba(255, 253, 245, 0.42);
  pointer-events: none;
}

.brand-panel {
  position: relative;
  overflow: hidden;
  min-height: 628px;
  border-radius: 28px;
  background: var(--sz-evergreen);
  box-shadow: 0 20px 52px rgba(20, 48, 36, 0.18);
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
    linear-gradient(180deg, rgba(18, 61, 45, 0.03) 0%, rgba(18, 61, 45, 0.74) 100%),
    linear-gradient(90deg, rgba(18, 61, 45, 0.18), rgba(18, 61, 45, 0.02));
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

.brand-mark {
  position: absolute;
  top: 30px;
  right: 42px;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  width: fit-content;
  padding: 13px 18px 13px 16px;
  border: 1px solid rgba(31, 78, 58, 0.13);
  border-radius: var(--sz-radius-pill);
  background: rgba(255, 255, 255, 0.42);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(14px);
}

.brand-mark svg {
  width: 32px;
  height: 32px;
  color: var(--sz-green-dark);
}

.brand-mark strong {
  display: block;
  color: var(--sz-deep-green);
  font-size: 28px;
  letter-spacing: 0;
}

.brand-mark span {
  display: block;
  margin-top: 1px;
  color: var(--sz-muted);
  font-size: 12px;
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
  font-size: 40px;
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
  border: 1px solid rgba(255, 250, 241, 0.24);
  border-radius: var(--sz-radius-pill);
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(12px);
  font-weight: 800;
}

li svg {
  width: 18px;
  height: 18px;
}

.form-panel {
  position: relative;
  z-index: 1;
  display: grid;
  padding: 96px 42px 30px;
}

.form-card {
  width: min(430px, 100%);
  display: grid;
  align-content: center;
  gap: 16px;
  justify-self: center;
  align-self: center;
  padding: 18px 4px 10px;
  background: transparent;
}

.form-card :deep(.n-form-item) {
  margin-bottom: 4px;
}

.form-card :deep(.n-form-item-label) {
  min-height: 24px;
  padding-bottom: 4px;
}

.form-card :deep(.n-form-item-feedback-wrapper) {
  min-height: 8px;
}

.form-card :deep(.n-input) {
  --n-border-radius: 999px !important;
  --n-height: 46px !important;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.form-card :deep(.n-button) {
  --n-border-radius: 999px !important;
  min-height: 46px;
  margin-top: 4px;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(31, 119, 78, 0.2);
}

.eyebrow {
  margin: 0;
  color: var(--sz-green-dark);
  font-weight: 900;
}

h2 {
  margin: 0;
  color: var(--sz-deep-green);
  font-size: 34px;
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
    border-radius: 26px;
  }

  .form-panel {
    order: 1;
    padding: 86px 20px 20px;
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
    min-height: 0;
    padding: 8px 0;
  }

}
</style>
