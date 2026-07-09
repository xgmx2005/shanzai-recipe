# Recipe Knowledge Base Expansion Design

## Goal

Expand the 膳哉 recipe knowledge base from a small demo seed set into a richer, credible recommendation base while keeping the current product scope unchanged.

The expansion must still serve only the existing three diet goals:

- `FAT_LOSS`: 减脂控热量
- `BALANCED`: 日常健康
- `MUSCLE_GAIN`: 健身增肌

This phase does not add new user groups such as children, seniors, pregnant users, or medical-condition diets. That keeps the health advice scope controllable and aligned with the current profile model.

## Current State

Current seed data:

- 36 ingredients
- 18 recipes
- 18 local recipe photos
- 18 recipe-to-ingredient mappings

The recommendation workflow is already stable:

1. Backend scores database recipes.
2. DeepSeek receives only scored recipe context.
3. DeepSeek returns structured recommendation analysis.
4. Frontend displays recipes, AI status, detail, history, and shopping-list links.

The weak point is breadth. With only 18 recipes, different inputs can repeatedly hit the same recipes, making the AI layer feel more capable than the actual knowledge base.

## Scope

### Target Size

Knowledge base expansion phase 1 targets about 45 recipes total:

- Keep existing 18 recipes.
- Add about 27 new recipes.
- Keep the distribution roughly balanced:
  - 15 recipes suitable for `FAT_LOSS`
  - 15 recipes suitable for `BALANCED`
  - 15 recipes suitable for `MUSCLE_GAIN`

Some recipes can belong to two goals. For example, a low-fat high-protein chicken tofu meal can support both `FAT_LOSS` and `MUSCLE_GAIN`.

### Ingredient Expansion

Add only common, low-risk ingredients that help diversify recommendations:

- Protein: 鸡腿肉, 猪里脊, 巴沙鱼, 牛奶, 无糖酸奶, 毛豆, 鸡腿排
- Vegetables: 菠菜, 白菜, 洋葱, 彩椒, 南瓜, 菌菇, 冬瓜, 西葫芦
- Staples: 红薯, 荞麦面, 杂粮饭, 玉米面, 紫薯
- Seasoning: 姜, 蒜, 料酒, 醋, 番茄酱, 孜然粉

These ingredients should be added only when used by recipes in this phase. Do not add broad ingredient catalogs that are not connected to actual recipes.

## Recipe Selection Principles

### 1. Realistic and Cookable

Every new recipe must look like something a student or young adult could actually make.

Required fields:

- name
- description
- imageUrl
- cookingTime
- difficulty
- servings
- calories
- protein
- fat
- carbs
- tasteTags
- healthTags
- targetGoals
- steps as JSON array text
- status
- createdBy
- recipe_ingredient rows with quantity, unit, and `is_core`

### 2. Recommendation Diversity

The new recipes should improve recommendation variety for common inputs:

- 鸡胸肉 should produce more than one result.
- 鸡蛋 should produce more than one result.
- 豆腐 should support both light and balanced meals.
- 牛肉 should support both balanced and muscle-gain meals.
- 虾仁 should support light, balanced, and muscle-gain meals.
- 番茄, 西兰花, 菠菜, 南瓜, 红薯, 荞麦面 should create useful matches.

### 3. Existing Goal Labels Only

Do not create new diet-goal enums or profile fields.

Use the existing goal combinations:

- `FAT_LOSS`
- `BALANCED`
- `MUSCLE_GAIN`
- `FAT_LOSS,BALANCED`
- `BALANCED,MUSCLE_GAIN`
- `FAT_LOSS,MUSCLE_GAIN`

### 4. Health Claims Stay Mild

Recipe tags and descriptions may say:

- 低脂
- 高蛋白
- 饱腹
- 快手
- 清淡
- 均衡
- 训练后

Avoid medical claims such as:

- 降血糖
- 治疗高血压
- 适合糖尿病
- 排毒
- 减肥必瘦

## Proposed Recipe Additions

### FAT_LOSS Candidates

- 冬瓜虾仁汤
- 菠菜鸡蛋豆腐汤
- 巴沙鱼西兰花轻食盘
- 鸡腿肉南瓜沙拉
- 番茄豆腐菌菇汤
- 西葫芦鸡胸肉炒蛋
- 紫薯酸奶轻食碗
- 黄瓜虾仁鸡蛋杯
- 白菜豆腐鸡肉汤

### BALANCED Candidates

- 洋葱彩椒牛肉饭
- 香菇鸡腿肉杂粮饭
- 番茄牛肉荞麦面
- 南瓜鸡蛋杂粮粥
- 西红柿豆腐炖牛肉
- 白菜猪里脊炒饭
- 菌菇青菜鸡蛋面
- 彩椒豆腐鸡肉盖饭
- 玉米鸡蛋牛奶粥

### MUSCLE_GAIN Candidates

- 孜然牛肉红薯碗
- 鸡腿排杂粮饭
- 虾仁牛油果荞麦面
- 猪里脊鸡蛋蛋白餐
- 牛肉毛豆藜麦饭
- 鸡胸肉酸奶全麦卷
- 巴沙鱼红薯训练餐
- 双蛋牛肉杂粮饭
- 虾仁豆腐高蛋白盖饭

The final implementation may adjust names or ingredients slightly if better matching available images or avoiding duplicate ingredient combinations.

## Image Strategy

Images remain database-backed and local-first.

The AI must not generate image URLs. Every recipe image must be one of:

1. A local real photo under `frontend/public/images/recipes/`.
2. A reused similar local photo when the dish is visually close.
3. `/images/recipes/default-recipe.svg` only when no acceptable real photo is available.

Implementation should prefer adding real photos for visually distinct dishes. Reuse is acceptable for similar bowls, soups, and rice plates as long as it does not obviously misrepresent the dish.

Each new image file should use lowercase English kebab-case names, for example:

- `winter-melon-shrimp-soup.jpg`
- `tomato-beef-soba.jpg`
- `cumin-beef-sweet-potato-bowl.jpg`

Image source notes should be recorded in `docs/recipe-image-sources.md`.

## Data File Strategy

Use two layers:

1. Update `backend/src/main/resources/db/data.sql` so a fresh database import contains the expanded knowledge base.
2. Add an additive migration file for existing local databases:
   `backend/src/main/resources/db/migrations/2026-07-09-expand-recipe-knowledge-base.sql`

The migration should:

- insert new ingredients only if they do not already exist;
- insert new recipes with stable IDs after the current maximum ID;
- insert recipe ingredients after recipes and ingredients exist;
- avoid truncating user, recommendation history, shopping lists, or favorites.

For MySQL 8, implementation can use explicit IDs and `INSERT IGNORE` where appropriate. If a recipe may need updating, prefer a clear delete-and-reinsert block scoped only to the new recipe IDs, not a whole-table reset.

## Recommendation Impact

The scoring code should not need changes. The expansion should improve behavior through better data:

- More eligible recipes after hard filters.
- More varied matches for common ingredients.
- Better target-goal distribution.
- More useful shopping-list output because recipes have richer ingredients.

DeepSeek prompt and response contract remain unchanged. It receives a richer scored context but still only explains database results.

## Validation

### Automated Checks

Add focused tests or data-validation utilities where practical:

- All `recipe.image_url` values point to an existing local asset or the default recipe SVG.
- All `recipe_ingredient.ingredient_id` values refer to existing ingredients.
- Each new recipe has at least 3 ingredient rows.
- Each new recipe has at least 3 steps.
- Each existing goal has at least 15 active recipes after seed import.

### Manual Demo Checks

After import, test these inputs:

- Available: 鸡蛋, 番茄
- Available: 鸡胸肉, 西兰花
- Available: 豆腐, 菠菜
- Available: 牛肉, 洋葱
- Available: 虾仁, 荞麦面
- Available: 红薯, 鸡腿肉
- Excluded: 花生
- Excluded: 辣椒

Expected behavior:

- Recommendation still returns up to 5 recipes.
- `aiGenerated` can be true when DeepSeek is configured.
- Recipes are visually plausible.
- Detail pages load.
- Shopping lists contain missing ingredients and exclude available ingredients.

## Non-Goals

- No new profile fields.
- No new diet-goal enum values.
- No medical-diet recommendations.
- No dynamic AI-generated recipe records.
- No dynamic AI-generated images.
- No admin bulk import UI in this phase.

## Risks and Mitigations

### Risk: Nutrition data looks arbitrary

Mitigation: Use plausible rounded values, keep them internally consistent, and avoid precise medical claims.

### Risk: Image does not match recipe exactly

Mitigation: Prefer bowl/soup/rice-plate images that visually match dish type. Use default SVG only when reuse would be misleading.

### Risk: SQL migration damages user data

Mitigation: Migration must be additive and scoped to new ingredient and recipe IDs. It must not truncate user data, histories, favorites, or shopping lists.

### Risk: Data grows but recommendation quality does not improve

Mitigation: Choose recipes around common input ingredients and ensure each goal has enough active recipes.

## Definition of Done

- Expanded seed data contains about 45 active recipes.
- Existing three diet goals each have at least 15 active recipes.
- New ingredients are used by at least one recipe.
- Fresh import from `schema.sql` + `data.sql` succeeds.
- Existing database migration succeeds without truncating user data.
- Backend tests pass.
- Frontend build passes.
- Demo flow works from recommendation to detail to shopping list to history.
