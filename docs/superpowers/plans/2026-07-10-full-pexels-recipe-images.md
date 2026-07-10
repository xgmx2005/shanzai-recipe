# Full Pexels Recipe Images Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace every existing recipe image with a local Pexels-backed photo while preserving stable local runtime loading and source traceability.

**Architecture:** Use Pexels only as a build-time asset source. Download images into `frontend/public/images/recipes/`, point seed SQL and update migrations to those local files, and keep a Markdown source ledger for attribution and review.

**Tech Stack:** Spring Boot seed SQL, MySQL migration SQL, Vue static assets, Pexels API, Maven tests, Vite build.

---

### Task 1: Add Full-Coverage Asset Tests

**Files:**
- Modify: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecipeKnowledgeBaseSeedDataTest.java`

- [ ] **Step 1: Write the failing test**

Add a test asserting every seed recipe image path starts with `/images/recipes/pexels-recipe-`, ends with `.jpg`, exists under `frontend/public`, and is unique.

- [ ] **Step 2: Run focused test**

Run: `mvn -B -ntp -Dtest=RecipeKnowledgeBaseSeedDataTest test`

Expected before implementation: failure because many recipes still reference older Commons image names.

### Task 2: Download Pexels Photos And Generate Preview

**Files:**
- Create: `frontend/public/images/recipes/pexels-recipe-*.jpg`
- Create: `docs/ui/screenshots/pexels-full-recipe-contact-sheet.jpg`

- [ ] **Step 1: Search Pexels by recipe-specific English keywords**

Use the temporary Pexels API key only in the shell process. Do not write it to any file.

- [ ] **Step 2: Download one local image per recipe**

Save files using stable names: `pexels-recipe-01-chicken-broccoli-bowl.jpg` through `pexels-recipe-45-shrimp-poke-bowl.jpg`.

- [ ] **Step 3: Generate a contact sheet**

Generate `docs/ui/screenshots/pexels-full-recipe-contact-sheet.jpg` containing all 45 images for visual review.

### Task 3: Update Data And Frontend Mapping

**Files:**
- Modify: `backend/src/main/resources/db/data.sql`
- Modify: `backend/src/main/resources/db/migrations/2026-07-09-expand-recipe-knowledge-base.sql`
- Create: `backend/src/main/resources/db/migrations/2026-07-10-update-recipe-images-to-pexels.sql`
- Modify: `frontend/src/utils/assets.ts`

- [ ] **Step 1: Update seed SQL**

Set every recipe `image_url` to its corresponding `pexels-recipe-*.jpg`.

- [ ] **Step 2: Update expansion migration**

Keep recipes 19-45 consistent with seed SQL for fresh database setup.

- [ ] **Step 3: Add update migration**

Add update statements for recipes 1-45 so existing local databases can migrate image paths without recreating data.

- [ ] **Step 4: Add crop position mappings**

Add every new Pexels path to `recipeImagePositionMap`.

### Task 4: Update Source Documentation

**Files:**
- Modify: `docs/recipe-image-sources.md`

- [ ] **Step 1: Add full Pexels library table**

Record local filename, recipe id, recipe name, Pexels photographer, and source page.

- [ ] **Step 2: Note runtime behavior**

Document that the app serves local images and does not call Pexels at runtime.

### Task 5: Verify And Integrate

**Files:**
- All files from previous tasks

- [ ] **Step 1: Run backend tests**

Run: `mvn -B -ntp test`

Expected: 39 tests, 0 failures.

- [ ] **Step 2: Run frontend build**

Run: `npm run build`

Expected: Vite production build succeeds.

- [ ] **Step 3: Scan secrets**

Run a local repository scan for the temporary Pexels key prefix, Pexels API URLs, and Pexels CDN URLs.

Expected: no API key; no Pexels API or CDN direct runtime URLs outside documentation.

- [ ] **Step 4: Commit, merge, push**

Commit with Chinese message, merge into `main`, rerun verification on `main`, push to GitHub, and remove the worktree.
