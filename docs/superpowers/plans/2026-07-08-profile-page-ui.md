# 健康档案页界面优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将健康档案页升级为与首页、智能推荐页统一的现代轻健康个人健康控制台。

**Architecture:** 不改后端接口和数据模型，只在 Vue 页面层重构信息层级和样式。`ProfileView.vue` 负责页面布局、表单和实时摘要；`HealthSummaryCard.vue` 负责 BMI/热量摘要卡视觉表现。

**Tech Stack:** Vue 3、TypeScript、Naive UI、Lucide Vue、现有 CSS variables。

---

### Task 1: 重构健康档案页结构

**Files:**
- Modify: `frontend/src/views/user/ProfileView.vue`

- [ ] **Step 1: 增加页面摘要计算**

在 `<script setup>` 中引入 `computed` 和图标，增加 BMI、热量、目标文案、偏好完整度等计算属性。

- [ ] **Step 2: 重写顶部引导区**

把 `title-block` 替换为 `profile-hero`，右侧展示 BMI、每日目标热量、推荐目标三个摘要卡。

- [ ] **Step 3: 重写主体表单分区**

把身体信息改成卡片化参数输入区，保留 `GoalSegment` 和 `IngredientTagInput`，保存按钮改为统一自定义主按钮。

- [ ] **Step 4: 重写右侧洞察区**

保留 `HealthSummaryCard`，新增推荐影响说明、偏好完整度和标签预览。

### Task 2: 调整健康摘要卡视觉

**Files:**
- Modify: `frontend/src/components/HealthSummaryCard.vue`

- [ ] **Step 1: 增强卡片层级**

保持计算逻辑不变，调整为浅色卡片、BMI 数字突出、热量说明更像指标摘要。

- [ ] **Step 2: 保持 compact 兼容**

保留 `compact` class，避免影响其他页面复用。

### Task 3: 验证和提交

**Files:**
- Verify: `frontend/src/views/user/ProfileView.vue`
- Verify: `frontend/src/components/HealthSummaryCard.vue`

- [ ] **Step 1: 构建验证**

Run: `npm run build` in `frontend`

Expected: exit code 0.

- [ ] **Step 2: 浏览器验证**

打开 `/user/profile`，验证页面渲染、无横向溢出、可编辑字段、保存按钮触发接口。

- [ ] **Step 3: 提交**

Commit message: `style: 优化健康档案页界面`
