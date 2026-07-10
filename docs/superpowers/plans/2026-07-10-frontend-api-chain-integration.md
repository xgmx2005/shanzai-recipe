# Frontend API Chain Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the user-facing recipe recommendation chain consistently land on real backend data: recommendation result, recipe detail, shopping list creation, favorites, and recommendation history.

**Architecture:** Keep API calls in existing `src/api/*` modules and keep page state in current Vue views. Add a tiny route helper so every “create shopping list then navigate” flow uses the same `listId` query behavior, which lets `ShoppingListsView` reliably open the newly created list.

**Tech Stack:** Vue 3, TypeScript, Vue Router, Naive UI, Vitest for focused frontend logic tests, existing Spring Boot backend APIs.

---

### Task 1: Add route helper test coverage

**Files:**
- Create: `frontend/src/utils/navigation.test.ts`
- Create: `frontend/src/utils/navigation.ts`
- Modify: `frontend/package.json`

- [x] **Step 1: Add a failing test for shopping-list navigation**

```ts
import { describe, expect, it } from 'vitest'
import { shoppingListRoute } from './navigation'

describe('shoppingListRoute', () => {
  it('opens the newly created shopping list by id and source', () => {
    expect(shoppingListRoute(18, 'recommendation-history')).toEqual({
      path: '/user/shopping-lists',
      query: {
        listId: '18',
        from: 'recommendation-history',
      },
    })
  })
})
```

- [x] **Step 2: Run the test and verify RED**

Run: `npm run test:unit -- src/utils/navigation.test.ts --run`

Expected: FAIL because `./navigation` does not exist or `shoppingListRoute` is not exported.

- [x] **Step 3: Implement minimal helper**

```ts
export type ShoppingListSource =
  | 'recommendation'
  | 'recommendation-detail'
  | 'recommendation-history'
  | 'recipe-detail'
  | 'favorites'

export function shoppingListRoute(listId: number, from: ShoppingListSource) {
  return {
    path: '/user/shopping-lists',
    query: {
      listId: String(listId),
      from,
    },
  }
}
```

- [x] **Step 4: Run the test and verify GREEN**

Run: `npm run test:unit -- src/utils/navigation.test.ts --run`

Expected: PASS.

### Task 2: Use route helper in all shopping-list creation flows

**Files:**
- Modify: `frontend/src/views/user/RecommendView.vue`
- Modify: `frontend/src/views/user/RecipeDetailView.vue`
- Modify: `frontend/src/views/user/RecommendationHistoryView.vue`
- Modify: `frontend/src/views/user/FavoritesView.vue`

- [x] **Step 1: Write a failing test for valid source names**

Extend `frontend/src/utils/navigation.test.ts`:

```ts
it('supports every current shopping-list creation source', () => {
  expect(shoppingListRoute(1, 'recommendation').query.from).toBe('recommendation')
  expect(shoppingListRoute(2, 'recommendation-detail').query.from).toBe('recommendation-detail')
  expect(shoppingListRoute(3, 'recommendation-history').query.from).toBe('recommendation-history')
  expect(shoppingListRoute(4, 'recipe-detail').query.from).toBe('recipe-detail')
  expect(shoppingListRoute(5, 'favorites').query.from).toBe('favorites')
})
```

- [x] **Step 2: Run the test and verify RED**

Run: `npm run test:unit -- src/utils/navigation.test.ts --run`

Expected: FAIL until the helper type and implementation include all source names.

- [x] **Step 3: Update pages to navigate through `shoppingListRoute`**

Each successful `createShoppingList(...)` call must store the returned `list` and route through `router.push(shoppingListRoute(list.id, '<source>'))`.

- [x] **Step 4: Run unit test and frontend build**

Run: `npm run test:unit -- src/utils/navigation.test.ts --run`

Expected: PASS.

Run: `npm run build`

Expected: PASS.

### Task 3: Verify backend contract remains stable

**Files:**
- No production backend changes expected.

- [x] **Step 1: Run backend tests**

Run: `mvn -B -ntp test`

Expected: `Tests run: 41, Failures: 0, Errors: 0`.

- [x] **Step 2: Final integration check**

Run: `git diff --check`

Expected: no whitespace errors.
