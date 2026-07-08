# Auth Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split mixed login/register into polished standalone `/login` and `/register` product entry pages.

**Architecture:** Introduce a shared `AuthShell.vue` layout component, keep auth API/store unchanged, simplify `LoginView.vue`, add `RegisterView.vue`, and register a public `/register` route.

**Tech Stack:** Vue 3, Vite, TypeScript, Naive UI, Pinia, Vue Router.

---

### Task 1: Shared Auth Shell

**Files:**
- Create: `frontend/src/components/AuthShell.vue`

- [ ] Create a reusable shell with slots for form content, props for title/subtitle, and responsive CSS.
- [ ] Verify with `npm run build`.

### Task 2: Standalone Login Page

**Files:**
- Modify: `frontend/src/views/LoginView.vue`

- [ ] Remove register mode state and nickname field.
- [ ] Use `AuthShell.vue`.
- [ ] Keep demo account buttons and role-based redirect.
- [ ] Verify login page compiles with `npm run build`.

### Task 3: Standalone Register Page

**Files:**
- Create: `frontend/src/views/RegisterView.vue`
- Modify: `frontend/src/router/index.ts`

- [ ] Add `/register` public route.
- [ ] Build register form with nickname, username, password, confirm password.
- [ ] Validate required fields, password length, and password equality before calling `auth.register`.
- [ ] Verify with `npm run build`.

### Task 4: Visual And Regression Verification

**Files:**
- None expected.

- [ ] Run `npm run build`.
- [ ] Run `mvn -B -ntp test`.
- [ ] Start Vite if needed and visually inspect `/login` and `/register`.
