<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Flame, Salad, Sparkles } from '@lucide/vue'
import HealthSummaryCard from '@/components/HealthSummaryCard.vue'
import RecipeCard from '@/components/RecipeCard.vue'
import { recipes } from '@/mock/data'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const goalLabel = computed(() => {
  const labels = {
    FAT_LOSS: '减脂控热量',
    BALANCED: '日常健康',
    MUSCLE_GAIN: '健身增肌',
  }
  return labels[auth.profile.dietGoal]
})
</script>

<template>
  <div class="home-view">
    <section class="hero sz-panel">
      <div class="hero-copy">
        <p class="sz-chip"><Sparkles /> AI 健康膳食助手</p>
        <h1>今天想吃得更合适一点</h1>
        <p>
          根据你的健康档案、已有食材和烹饪时间，先匹配营养目标，再给出清楚的推荐理由。
        </p>
        <div class="hero-actions">
          <n-button type="primary" size="large" @click="router.push('/user/recommend')">
            开始推荐
            <template #icon>
              <n-icon><ArrowRight /></n-icon>
            </template>
          </n-button>
          <n-button size="large" secondary type="primary" @click="router.push('/user/profile')">
            完善档案
          </n-button>
        </div>
      </div>
      <div class="hero-plate">
        <img :src="recipes[0].imageUrl" alt="健康轻食碗" />
        <div>
          <span>健康评分</span>
          <strong>820</strong>
          <em>分</em>
        </div>
      </div>
    </section>

    <section class="summary-grid">
      <HealthSummaryCard :profile="auth.profile" compact />
      <article class="metric-card">
        <span><Flame /> 今日目标热量</span>
        <strong>1600 kcal</strong>
        <small>已摄入 620 kcal</small>
      </article>
      <article class="metric-card">
        <span><Salad /> 今日目标</span>
        <strong>{{ goalLabel }}</strong>
        <small>默认读取健康档案</small>
      </article>
      <article class="metric-card">
        <span><Sparkles /> 推荐次数</span>
        <strong>3 次</strong>
        <small>本周</small>
      </article>
    </section>

    <section class="quick-panel sz-panel">
      <div class="section-head">
        <div>
          <h2 class="sz-section-title">快速入口</h2>
          <p class="sz-muted">从档案、食材和收藏开始都可以。</p>
        </div>
      </div>
      <div class="quick-actions">
        <button type="button" @click="router.push('/user/recommend')">快速输入食材</button>
        <button type="button" @click="router.push('/user/shopping-lists')">我的菜单</button>
        <button type="button" @click="router.push('/user/favorites')">收藏菜谱</button>
      </div>
    </section>

    <section class="recipes-panel">
      <div class="section-head">
        <div>
          <h2 class="sz-section-title">最近推荐</h2>
          <p class="sz-muted">优先展示食材命中高、热量更稳定的菜谱。</p>
        </div>
        <router-link to="/user/recommend/result">查看全部</router-link>
      </div>
      <div class="recipe-grid">
        <RecipeCard v-for="recipe in recipes" :key="recipe.id" :recipe="recipe" dense />
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-view {
  display: grid;
  gap: 18px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  align-items: center;
  padding: 30px;
}

.hero-copy {
  display: grid;
  justify-items: start;
  gap: 16px;
}

h1 {
  margin: 0;
  max-width: 620px;
  color: var(--sz-ink);
  font-size: 38px;
  line-height: 1.18;
  letter-spacing: 0;
}

.hero-copy > p:not(.sz-chip) {
  max-width: 620px;
  margin: 0;
  color: var(--sz-text);
  font-size: 16px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
}

.hero-plate {
  position: relative;
  min-height: 250px;
}

.hero-plate img {
  width: 100%;
  height: 250px;
  display: block;
  object-fit: cover;
  border-radius: 22px;
}

.hero-plate div {
  position: absolute;
  right: 16px;
  bottom: 16px;
  min-width: 136px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 253, 247, 0.9);
  box-shadow: 0 12px 24px rgba(31, 42, 36, 0.16);
}

.hero-plate span,
.hero-plate em {
  color: var(--sz-muted);
  font-style: normal;
}

.hero-plate strong {
  display: inline-block;
  margin: 4px 4px 0 0;
  color: var(--sz-deep-green);
  font-size: 30px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  min-height: 116px;
  display: grid;
  align-content: center;
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(227, 218, 203, 0.88);
  border-radius: 18px;
  background: var(--sz-surface);
}

.metric-card span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--sz-muted);
  font-weight: 700;
}

.metric-card svg {
  width: 16px;
  height: 16px;
}

.metric-card strong {
  font-size: 24px;
}

.metric-card small {
  color: var(--sz-muted);
}

.quick-panel {
  padding: 20px;
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
}

.section-head p {
  margin: 6px 0 0;
}

.section-head a {
  color: var(--sz-green-dark);
  font-weight: 800;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.quick-actions button {
  min-height: 46px;
  border: 1px solid var(--sz-line);
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-surface);
  font-weight: 800;
  cursor: pointer;
}

.quick-actions button:first-child {
  border-color: transparent;
  color: #ffffff;
  background: linear-gradient(135deg, var(--sz-green), var(--sz-deep-green));
}

.recipes-panel {
  display: grid;
  gap: 14px;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 980px) {
  .hero,
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }

  .recipe-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero,
  .summary-grid,
  .quick-actions,
  .recipe-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    padding: 22px;
  }

  h1 {
    font-size: 30px;
  }
}
</style>
