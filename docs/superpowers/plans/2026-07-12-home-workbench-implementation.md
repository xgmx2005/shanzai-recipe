# Home Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the user homepage as a focused “今日饮食工作台” with a clear recommendation entry, health status panel, three-step flow, and randomized inspiration recipe cards.

**Architecture:** Keep the change inside the existing Vue page and avoid new backend work. Add a small pure helper for random recipe sampling so the homepage can be tested without rendering the full page. Reuse existing APIs and favorite behavior.

**Tech Stack:** Vue 3 `<script setup>`, TypeScript, Naive UI, lucide-vue icons, Vitest static/source tests.

---

## File Structure

- Modify: `frontend/src/views/user/HomeView.vue`
  - Owns homepage data loading, random recipe selection, routing, favorite actions, and workbench layout.
- Create: `frontend/src/views/user/homeView.test.ts`
  - Locks homepage structure, random sampling behavior, and misleading-copy removal.

## Task 1: Homepage Contract Tests And Random Sampler

**Files:**
- Create: `frontend/src/views/user/homeView.test.ts`
- Modify: `frontend/src/views/user/HomeView.vue`

- [ ] **Step 1: Write the failing test**

Create `frontend/src/views/user/homeView.test.ts` with:

```ts
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('HomeView workbench design', () => {
  const source = readFileSync(fileURLToPath(new URL('./HomeView.vue', import.meta.url)), 'utf-8')

  it('presents the homepage as a daily diet workbench', () => {
    expect(source).toContain('home-workbench')
    expect(source).toContain('daily-status-panel')
    expect(source).toContain('workbench-flow')
    expect(source).toContain('inspiration-recipes')
    expect(source).toContain('今天这顿，交给膳哉来配')
    expect(source).toContain('开始智能推荐')
    expect(source).toContain('完善健康档案')
    expect(source).not.toContain('最近 7 天生成推荐')
  })

  it('uses a stable random sampler instead of always taking the first four recipes', () => {
    expect(source).toContain('function sampleRecipes')
    expect(source).toContain('sampleRecipes(recipeList.map(toCard), 4)')
    expect(source).not.toContain('recipeList.slice(0, 4).map(toCard)')
  })
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
npm run test:unit -- --run src/views/user/homeView.test.ts --reporter=dot
```

Expected: FAIL because `home-workbench`, `daily-status-panel`, `workbench-flow`, `inspiration-recipes`, and `sampleRecipes` do not exist yet.

- [ ] **Step 3: Add the minimal random sampler**

In `frontend/src/views/user/HomeView.vue`, add this helper near `toCard`:

```ts
function sampleRecipes(items: RecipeCardModel[], limit: number) {
  if (items.length <= limit) return [...items]
  return [...items]
    .map((item) => ({ item, rank: Math.random() }))
    .sort((left, right) => left.rank - right.rank)
    .slice(0, limit)
    .map(({ item }) => item)
}
```

Change the recipe loading block from:

```ts
const recipeList = await listRecipes()
recipes.value = recipeList.slice(0, 4).map(toCard)
```

to:

```ts
const recipeList = await listRecipes()
recipes.value = sampleRecipes(recipeList.map(toCard), 4)
```

- [ ] **Step 4: Run the test again**

Run:

```bash
npm run test:unit -- --run src/views/user/homeView.test.ts --reporter=dot
```

Expected: still FAIL if structure classes and new copy are not implemented yet, but the random sampler assertions should now pass.

- [ ] **Step 5: Commit this checkpoint**

```bash
git add frontend/src/views/user/HomeView.vue frontend/src/views/user/homeView.test.ts
git commit -m "test: 添加首页工作台契约测试"
```

## Task 2: Workbench Template Structure

**Files:**
- Modify: `frontend/src/views/user/HomeView.vue`
- Test: `frontend/src/views/user/homeView.test.ts`

- [ ] **Step 1: Replace the page root and hero classes**

Change:

```vue
<div class="home-view">
  <section class="hero">
```

to:

```vue
<div class="home-view home-workbench">
  <section class="workbench-hero">
```

- [ ] **Step 2: Replace hero copy**

Use this hero copy:

```vue
<div class="hero-copy">
  <p class="hero-eyebrow">膳哉智能轻饮食助手</p>
  <h1>今天这顿，交给膳哉来配</h1>
  <p>
    结合你的健康档案、饮食目标、烹饪时间和忌口，膳哉会把想法整理成一份清楚可执行的推荐。
  </p>
  <div class="hero-actions">
    <n-button class="primary-action" type="primary" size="large" @click="router.push('/user/recommend')">
      <template #icon>
        <n-icon><Sparkles /></n-icon>
      </template>
      开始智能推荐
    </n-button>
    <n-button class="secondary-action" secondary size="large" @click="router.push('/user/profile')">
      完善健康档案
    </n-button>
  </div>
</div>
```

- [ ] **Step 3: Replace featured recipe with daily status panel**

Replace the `featured-recipe` block in the hero with:

```vue
<div class="daily-status-panel" aria-label="今日饮食状态">
  <article>
    <span>BMI</span>
    <strong>{{ bmiValue.toFixed(1) }}</strong>
    <small>{{ bmiStatus }}</small>
  </article>
  <article>
    <span>目标热量</span>
    <strong>{{ dailyCalorieTarget }}</strong>
    <small>kcal / 日</small>
  </article>
  <article>
    <span>饮食目标</span>
    <strong>{{ goalLabel }}</strong>
    <small>来自健康档案</small>
  </article>
  <article>
    <span>累计推荐记录</span>
    <strong>{{ recommendationCount }}</strong>
    <small>次</small>
  </article>
</div>
```

- [ ] **Step 4: Replace quick panel with workbench flow**

Replace the `quick-panel` section with:

```vue
<section class="workbench-flow">
  <div class="section-head">
    <h2 class="sz-section-title">从想法到采购清单</h2>
    <p>膳哉把“想吃什么”拆成可以执行的三步。</p>
  </div>
  <div class="flow-steps">
    <button type="button" @click="router.push('/user/recommend')">
      <span class="flow-index">01</span>
      <strong>说出想吃什么</strong>
      <small>用对话告诉膳哉你的食材、口味、时间和人数。</small>
      <ArrowRight />
    </button>
    <button type="button" @click="router.push('/user/recommend')">
      <span class="flow-index">02</span>
      <strong>生成推荐菜谱</strong>
      <small>结合健康档案和知识库，生成匹配理由清楚的菜谱。</small>
      <ArrowRight />
    </button>
    <button type="button" @click="router.push('/user/shopping-lists')">
      <span class="flow-index">03</span>
      <strong>整理购物清单</strong>
      <small>从推荐、详情或收藏页，把缺少食材整理成清单。</small>
      <ArrowRight />
    </button>
  </div>
</section>
```

- [ ] **Step 5: Rename recipe section**

Change:

```vue
<section class="recipes-panel">
```

to:

```vue
<section class="recipes-panel inspiration-recipes">
```

Change the title from `最近推荐` to:

```vue
<h2 class="sz-section-title">今天也可以这样吃</h2>
```

Change the link target from `/user/recommend/result` to `/user/history` and copy to:

```vue
<router-link to="/user/history">查看推荐历史</router-link>
```

- [ ] **Step 6: Run the homepage test**

Run:

```bash
npm run test:unit -- --run src/views/user/homeView.test.ts --reporter=dot
```

Expected: PASS.

- [ ] **Step 7: Commit this checkpoint**

```bash
git add frontend/src/views/user/HomeView.vue frontend/src/views/user/homeView.test.ts
git commit -m "feat: 重构首页为今日饮食工作台"
```

## Task 3: Visual Polish And Full Verification

**Files:**
- Modify: `frontend/src/views/user/HomeView.vue`
- Test: `frontend/src/views/user/homeView.test.ts`

- [ ] **Step 1: Update CSS selectors**

Rename the major selectors:

```css
.hero
```

to:

```css
.workbench-hero
```

Rename:

```css
.quick-panel
.quick-actions
.quick-icon
```

to:

```css
.workbench-flow
.flow-steps
.flow-index
```

- [ ] **Step 2: Add daily status panel CSS**

Add:

```css
.daily-status-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: min(100%, 520px);
  justify-self: end;
}

.daily-status-panel article {
  display: grid;
  align-content: center;
  min-height: 116px;
  padding: 18px;
  border: 1px solid rgba(223, 210, 191, 0.78);
  border-radius: 18px;
  background: rgba(255, 253, 247, 0.86);
  box-shadow: 0 14px 28px rgba(23, 37, 31, 0.08);
}

.daily-status-panel span,
.daily-status-panel small {
  color: var(--sz-muted);
  font-weight: 800;
}

.daily-status-panel strong {
  margin: 6px 0;
  color: var(--sz-evergreen);
  font-size: 28px;
  line-height: 1.15;
}
```

- [ ] **Step 3: Add workbench flow CSS**

Add:

```css
.workbench-flow {
  display: grid;
  gap: 14px;
}

.flow-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.flow-steps button {
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto auto;
  gap: 8px 14px;
  min-height: 150px;
  padding: 18px;
  border: 1px solid rgba(227, 218, 203, 0.88);
  border-radius: 18px;
  color: var(--sz-ink);
  background: rgba(255, 253, 247, 0.94);
  box-shadow: var(--sz-shadow-soft);
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.flow-steps button:hover {
  border-color: var(--sz-line-strong);
  box-shadow: var(--sz-shadow);
  transform: translateY(-2px);
}

.flow-index {
  width: fit-content;
  padding: 4px 10px;
  border-radius: var(--sz-radius-pill);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 12px;
  font-weight: 900;
}

.flow-steps strong {
  grid-column: 1 / 3;
  color: var(--sz-evergreen);
  font-size: 20px;
}

.flow-steps small {
  grid-column: 1 / 3;
  color: var(--sz-muted);
  font-size: 14px;
  line-height: 1.7;
}

.flow-steps svg {
  grid-column: 2;
  grid-row: 1;
  width: 20px;
  height: 20px;
  color: var(--sz-muted);
}
```

- [ ] **Step 4: Tighten inspiration cards**

Keep the existing recipe card behavior, but make cards secondary to the hero:

```css
.inspiration-recipes .home-recipe-image {
  aspect-ratio: 16 / 10;
}

.inspiration-recipes .home-recipe-body {
  padding: 14px;
}
```

- [ ] **Step 5: Update responsive CSS**

Ensure these responsive selectors exist:

```css
@media (max-width: 980px) {
  .workbench-hero,
  .flow-steps {
    grid-template-columns: 1fr;
  }

  .daily-status-panel {
    justify-self: stretch;
    width: 100%;
  }
}

@media (max-width: 640px) {
  .daily-status-panel {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 6: Run focused and full verification**

Run:

```bash
npm run test:unit -- --run src/views/user/homeView.test.ts --reporter=dot
npm run test:unit -- --run --reporter=dot
npm run build
```

Expected:

- Homepage test passes.
- Existing frontend tests pass.
- Production build succeeds.

- [ ] **Step 7: Commit this checkpoint**

```bash
git add frontend/src/views/user/HomeView.vue frontend/src/views/user/homeView.test.ts
git commit -m "style: 优化首页工作台视觉细节"
```
