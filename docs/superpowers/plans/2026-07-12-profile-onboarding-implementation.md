# Profile Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a three-step health profile onboarding flow after user registration, then use completion state to show lightweight guidance on home and recommendation pages.

**Architecture:** Extend the existing `user_profile` model with `profile_completed`, reuse the existing profile save endpoint, and add a new Vue route `/user/onboarding`. Registration sends normal users to onboarding; existing login behavior stays lightweight and only shows reminders when the profile is incomplete.

**Tech Stack:** Spring Boot, MyBatis Plus, MySQL SQL migrations, Vue 3, Pinia, Vue Router, Naive UI, Vitest, JUnit.

---

### Task 1: Backend Profile Completion State

**Files:**
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/profile/ProfileEntity.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/profile/ProfileRequest.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/profile/ProfileResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/profile/ProfileSummaryResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/profile/ProfileService.java`
- Modify: `backend/src/main/resources/db/schema.sql`
- Modify: `backend/src/main/resources/db/data.sql`
- Create: `backend/src/main/resources/db/migrations/2026-07-12-add-profile-completed.sql`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/profile/ProfileServiceTest.java`

- [ ] **Step 1: Write failing backend tests**

Add assertions to `ProfileServiceTest`:

```java
@Test
void saveProfileMarksProfileCompletedWhenRequested() {
    when(profileMapper.selectOne(any())).thenReturn(null);
    when(profileMapper.insert(any(ProfileEntity.class))).thenAnswer(invocation -> {
        ProfileEntity profile = invocation.getArgument(0);
        profile.setId(10L);
        return 1;
    });

    ProfileResponse response = profileService.saveProfile(
        7L,
        new ProfileRequest(
            "FEMALE",
            20,
            new BigDecimal("170"),
            new BigDecimal("65"),
            DietGoal.BALANCED,
            List.of("清淡"),
            List.of(),
            List.of(),
            30,
            true
        )
    );

    ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
    verify(profileMapper).insert(captor.capture());
    assertEquals(true, captor.getValue().getProfileCompleted());
    assertEquals(true, response.profileCompleted());
}
```

Update the existing `ProfileRequest` constructor calls to pass `false` where completion is not under test.

- [ ] **Step 2: Run backend test and verify RED**

Run:

```bash
cd backend
mvn -B -ntp -Dtest=ProfileServiceTest test
```

Expected: compile failure or test failure because `profileCompleted` is not defined yet.

- [ ] **Step 3: Implement backend completion field**

Add `profileCompleted` to entity, request, response, summary response, schema, seed data, and migration:

```sql
ALTER TABLE user_profile
    ADD COLUMN profile_completed TINYINT NOT NULL DEFAULT 0 AFTER daily_calorie_target;
```

In `ProfileService.applyRequest`, set:

```java
profile.setProfileCompleted(Boolean.TRUE.equals(request.profileCompleted()));
```

In `getSummary`, include the stored completion state.

- [ ] **Step 4: Run backend tests and verify GREEN**

Run:

```bash
cd backend
mvn -B -ntp -Dtest=ProfileServiceTest test
```

Expected: tests pass.

### Task 2: Frontend Types, Store, and Registration Routing

**Files:**
- Modify: `frontend/src/types.ts`
- Modify: `frontend/src/api/profile.ts`
- Modify: `frontend/src/stores/auth.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/RegisterView.vue`
- Test: `frontend/src/views/profileOnboardingFlow.test.ts`

- [ ] **Step 1: Write failing frontend routing/type test**

Create `frontend/src/views/profileOnboardingFlow.test.ts`:

```ts
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const routerSource = readFileSync(fileURLToPath(new URL('../router/index.ts', import.meta.url)), 'utf-8')
const registerSource = readFileSync(fileURLToPath(new URL('./RegisterView.vue', import.meta.url)), 'utf-8')
const typesSource = readFileSync(fileURLToPath(new URL('../types.ts', import.meta.url)), 'utf-8')

describe('profile onboarding flow wiring', () => {
  it('routes new user registration into onboarding', () => {
    expect(routerSource).toContain("path: 'onboarding'")
    expect(routerSource).toContain("name: 'profile-onboarding'")
    expect(registerSource).toContain("'/user/onboarding'")
  })

  it('models profile completion on profile and summary payloads', () => {
    expect(typesSource).toContain('profileCompleted: boolean')
    expect(typesSource).toContain('profileCompleted?: boolean')
  })
})
```

- [ ] **Step 2: Run frontend test and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- --run src/views/profileOnboardingFlow.test.ts --reporter=dot
```

Expected: test fails because onboarding route and completion fields do not exist.

- [ ] **Step 3: Add frontend model and route wiring**

Update `Profile` and `ProfileSummary` with `profileCompleted: boolean`; update `ProfileRequest` with `profileCompleted?: boolean`; add route:

```ts
{
  path: 'onboarding',
  name: 'profile-onboarding',
  component: () => import('@/views/user/ProfileOnboardingView.vue'),
}
```

Change `RegisterView.vue` registration success:

```ts
await router.push('/user/onboarding')
```

- [ ] **Step 4: Run frontend wiring test and verify GREEN**

Run:

```bash
cd frontend
npm run test:unit -- --run src/views/profileOnboardingFlow.test.ts --reporter=dot
```

Expected: test passes.

### Task 3: Three-Step Onboarding Page

**Files:**
- Create: `frontend/src/views/user/ProfileOnboardingView.vue`
- Modify: `frontend/src/views/profileOnboardingFlow.test.ts`

- [ ] **Step 1: Extend failing test for onboarding page content**

Append assertions:

```ts
const onboardingSource = readFileSync(
  fileURLToPath(new URL('./user/ProfileOnboardingView.vue', import.meta.url)),
  'utf-8',
)

it('presents a three-step health profile onboarding page', () => {
  expect(onboardingSource).toContain('profile-onboarding')
  expect(onboardingSource).toContain('基础身体信息')
  expect(onboardingSource).toContain('饮食目标')
  expect(onboardingSource).toContain('口味与限制')
  expect(onboardingSource).toContain('保存并开始使用')
  expect(onboardingSource).toContain('稍后完善')
  expect(onboardingSource).toContain('profileCompleted: true')
})
```

- [ ] **Step 2: Run frontend test and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- --run src/views/profileOnboardingFlow.test.ts --reporter=dot
```

Expected: failure because `ProfileOnboardingView.vue` does not exist.

- [ ] **Step 3: Implement onboarding page**

Create `ProfileOnboardingView.vue` with:

- step state `0 | 1 | 2`
- form based on `auth.profile`
- `nextStep`, `previousStep`, `skip`, `finish`
- `finish` calls `auth.saveProfile({ ...form, profileCompleted: true })`
- `skip` routes to `/user/home`

- [ ] **Step 4: Run frontend test and verify GREEN**

Run:

```bash
cd frontend
npm run test:unit -- --run src/views/profileOnboardingFlow.test.ts --reporter=dot
```

Expected: test passes.

### Task 4: Lightweight Incomplete Profile Guidance

**Files:**
- Modify: `frontend/src/views/user/HomeView.vue`
- Modify: `frontend/src/views/user/RecommendView.vue`
- Modify: `frontend/src/views/profileOnboardingFlow.test.ts`

- [ ] **Step 1: Add failing source tests for reminders**

Add assertions:

```ts
const homeSource = readFileSync(fileURLToPath(new URL('./user/HomeView.vue', import.meta.url)), 'utf-8')
const recommendSource = readFileSync(fileURLToPath(new URL('./user/RecommendView.vue', import.meta.url)), 'utf-8')

it('shows lightweight reminders when profile is incomplete', () => {
  expect(homeSource).toContain('完善健康档案后，推荐会更准确')
  expect(homeSource).toContain('/user/onboarding')
  expect(recommendSource).toContain('当前使用默认档案')
  expect(recommendSource).toContain('/user/onboarding')
})
```

- [ ] **Step 2: Run frontend test and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- --run src/views/profileOnboardingFlow.test.ts --reporter=dot
```

Expected: reminder assertions fail.

- [ ] **Step 3: Implement reminders**

Home page:

```vue
<section v-if="profileSummary && !profileSummary.profileCompleted" class="profile-reminder">
  <span>完善健康档案后，推荐会更准确</span>
  <button type="button" @click="router.push('/user/onboarding')">去完善</button>
</section>
```

Recommendation page:

```vue
<n-alert v-if="!auth.profile.profileCompleted" type="info" :bordered="false">
  当前使用默认档案，完善后会结合热量、忌口和口味偏好。
  <router-link to="/user/onboarding">去完善</router-link>
</n-alert>
```

- [ ] **Step 4: Run frontend test and verify GREEN**

Run:

```bash
cd frontend
npm run test:unit -- --run src/views/profileOnboardingFlow.test.ts --reporter=dot
```

Expected: test passes.

### Task 5: Full Verification

**Files:** all modified files.

- [ ] **Step 1: Run backend regression**

Run:

```bash
cd backend
mvn -B -ntp test
```

Expected: all backend tests pass.

- [ ] **Step 2: Run frontend regression**

Run:

```bash
cd frontend
npm run test:unit -- --run --reporter=dot
```

Expected: all frontend tests pass.

- [ ] **Step 3: Run frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: production build succeeds.

## Self-Review

- Spec coverage: registration redirect, three-step onboarding, completion state, skip behavior, and reminder banners are covered.
- Placeholder scan: no TBD or vague implementation-only placeholders remain.
- Type consistency: frontend uses `profileCompleted`; backend uses Java `profileCompleted` and SQL `profile_completed`.
