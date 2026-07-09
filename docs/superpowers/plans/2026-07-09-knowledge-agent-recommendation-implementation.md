# Knowledge Agent Recommendation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert 膳哉 recommendation into a knowledge-base-enhanced AI Agent: recipes stay database-backed, while DeepSeek generates structured recommendation analysis.

**Architecture:** The backend remains the source of truth for recipes, nutrition, images, ingredients, recommendation scoring, and shopping-list calculation. DeepSeek receives only the scored recommendation context and returns structured text fields; frontend displays whether the text came from AI or local fallback.

**Tech Stack:** Spring Boot 3, Java 17, MyBatis-Plus, MySQL 8, JUnit 5, Vue 3, TypeScript, Naive UI, DeepSeek Chat Completions API.

---

## Source Documents

- Design spec: `G:\CODE\短学期\docs\superpowers\specs\2026-07-09-knowledge-agent-recommendation-design.md`
- Existing product memory: `G:\CODE\短学期\docs\superpowers\specs\2026-07-06-smart-recipe-assistant-design.md`
- API contract: `G:\CODE\短学期\docs\api-contract.md`

## File Structure

### Backend

- Modify `G:\CODE\短学期\backend\src\main\resources\db\schema.sql`: add AI analysis columns to `recommendation_history`.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationHistoryEntity.java`: add `aiHealthTip`, `aiShoppingTip`, `aiGenerated`.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationResponse.java`: add `aiHealthTip`, `aiShoppingTip`, `aiGenerated`.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\AiRecommendationAnalysis.java`: structured AI output used by service layer.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\AiRecommendationContext.java`: prompt context for DeepSeek.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\AiRecommendationText.java`: represent DeepSeek JSON with `summary`, `healthTip`, `shoppingTip`, `topRecipeReason`.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\DeepSeekClient.java`: generate structured analysis from `AiRecommendationContext`.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\HttpDeepSeekClient.java`: update prompt and JSON parsing.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\DisabledDeepSeekClient.java`: return empty result.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\AiRecommendationService.java`: produce AI or fallback analysis.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationService.java`: call analysis once per recommendation and persist fields.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationHistorySummaryResponse.java`: expose AI analysis fields.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationHistoryDetailResponse.java`: expose AI analysis fields.
- Modify `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationHistoryService.java`: map new fields.
- Test `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\modules\recommendation\AiRecommendationServiceTest.java`
- Test `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\modules\recommendation\HttpDeepSeekClientTest.java`
- Test `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\modules\recommendation\RecommendationServiceTest.java`

### Frontend

- Modify `G:\CODE\短学期\frontend\src\types.ts`: add `aiHealthTip`, `aiShoppingTip`, `aiGenerated` to recommendation response and history types.
- Modify `G:\CODE\短学期\frontend\src\views\user\RecommendView.vue`: display AI status, summary, health tip, shopping tip, and recipe cards.
- Modify `G:\CODE\短学期\frontend\src\views\user\RecommendationHistoryView.vue`: show persisted AI analysis in history detail.

### Docs

- Modify `G:\CODE\短学期\docs\api-contract.md`: update recommendation response examples.
- Modify `G:\CODE\短学期\docs\runbook.md`: document `DEEPSEEK_API_KEY` behavior and fallback mode.

---

### Task 1: Backend Response Contract and Persistence

**Files:**
- Modify: `backend/src/main/resources/db/schema.sql`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryEntity.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationResponse.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecommendationServiceTest.java`

- [ ] **Step 1: Add failing test for new response fields**

In `RecommendationServiceTest.recommendBuildsCandidatesAndPersistsHistoryAndLogs`, add assertions after `RecommendationResponse response = ...`:

```java
assertTrue(response.aiSummary().contains("推荐"));
assertTrue(response.aiHealthTip().contains("建议") || response.aiHealthTip().contains("搭配"));
assertTrue(response.aiShoppingTip().contains("购物") || response.aiShoppingTip().contains("清单"));
assertEquals(false, response.aiGenerated());
```

Also assert persisted history:

```java
assertTrue(historyCaptor.getValue().getAiHealthTip().contains("建议")
    || historyCaptor.getValue().getAiHealthTip().contains("搭配"));
assertTrue(historyCaptor.getValue().getAiShoppingTip().contains("购物")
    || historyCaptor.getValue().getAiShoppingTip().contains("清单"));
assertEquals(false, historyCaptor.getValue().getAiGenerated());
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd backend
mvn -B -ntp -Dtest=RecommendationServiceTest test
```

Expected: compilation fails because `aiHealthTip`, `aiShoppingTip`, and `aiGenerated` do not exist.

- [ ] **Step 3: Extend schema**

In `schema.sql`, update `recommendation_history`:

```sql
CREATE TABLE recommendation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    input_ingredients TEXT,
    excluded_ingredients TEXT,
    diet_goal VARCHAR(30) NOT NULL,
    cooking_time INT,
    servings INT NOT NULL DEFAULT 1,
    result_recipe_ids VARCHAR(255),
    ai_summary TEXT,
    ai_health_tip TEXT,
    ai_shopping_tip TEXT,
    ai_generated TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_recommendation_history_user (user_id),
    KEY idx_recommendation_history_goal (diet_goal),
    CONSTRAINT fk_recommendation_history_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 4: Extend Java DTO and entity**

`RecommendationResponse`:

```java
public record RecommendationResponse(
    Long historyId,
    String aiSummary,
    String aiHealthTip,
    String aiShoppingTip,
    boolean aiGenerated,
    List<RecommendedRecipeResponse> recipes
) {
}
```

Add fields and getters/setters to `RecommendationHistoryEntity`:

```java
private String aiHealthTip;
private String aiShoppingTip;
private Boolean aiGenerated;
```

Use `Boolean` in the entity because MyBatis maps nullable tinyint values more safely.

- [ ] **Step 5: Run test**

Run:

```powershell
mvn -B -ntp -Dtest=RecommendationServiceTest test
```

Expected: tests still fail until service mapping is implemented in Task 3.

---

### Task 2: AI Analysis Service Contract

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/AiRecommendationAnalysis.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/AiRecommendationContext.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/AiRecommendationText.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/DeepSeekClient.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/DisabledDeepSeekClient.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/AiRecommendationService.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/AiRecommendationServiceTest.java`

- [ ] **Step 1: Write fallback test**

Add:

```java
@Test
void fallbackAnalysisWorksWhenApiUnavailable() {
    AiRecommendationService service = new AiRecommendationService(new DisabledDeepSeekClient());
    AiRecommendationContext context = new AiRecommendationContext(
        "FAT_LOSS",
        List.of("鸡胸肉", "西兰花"),
        List.of("花生"),
        30,
        List.of(new AiRecommendationContext.RecipeSnapshot(
            "鸡胸肉西兰花轻食碗",
            92,
            420,
            "35.00",
            List.of("鸡胸肉", "西兰花"),
            List.of("低脂", "高蛋白")
        ))
    );

    AiRecommendationAnalysis analysis = service.generateAnalysis(context);

    assertEquals(false, analysis.generated());
    assertTrue(analysis.summary().contains("鸡胸肉西兰花轻食碗") || analysis.summary().contains("减脂"));
    assertTrue(analysis.healthTip().contains("建议") || analysis.healthTip().contains("蛋白"));
    assertTrue(analysis.shoppingTip().contains("购物") || analysis.shoppingTip().contains("清单"));
    assertTrue(analysis.topRecipeReason().contains("鸡胸肉西兰花轻食碗"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd backend
mvn -B -ntp -Dtest=AiRecommendationServiceTest test
```

Expected: compilation fails because new records and method do not exist.

- [ ] **Step 3: Add records**

`AiRecommendationAnalysis`:

```java
package com.shanzai.recipe.modules.recommendation;

public record AiRecommendationAnalysis(
    String summary,
    String healthTip,
    String shoppingTip,
    String topRecipeReason,
    boolean generated
) {
}
```

`AiRecommendationContext`:

```java
package com.shanzai.recipe.modules.recommendation;

import java.util.List;

public record AiRecommendationContext(
    String dietGoal,
    List<String> availableIngredients,
    List<String> excludedIngredients,
    Integer cookingTime,
    List<RecipeSnapshot> recipes
) {
    public record RecipeSnapshot(
        String name,
        int score,
        Integer calories,
        String protein,
        List<String> matchedIngredients,
        List<String> tags
    ) {
    }
}
```

`AiRecommendationText`:

```java
package com.shanzai.recipe.modules.recommendation;

public record AiRecommendationText(
    String summary,
    String healthTip,
    String shoppingTip,
    String topRecipeReason
) {
}
```

- [ ] **Step 4: Update DeepSeek client contract**

`DeepSeekClient`:

```java
package com.shanzai.recipe.modules.recommendation;

import java.util.Optional;

@FunctionalInterface
public interface DeepSeekClient {
    Optional<AiRecommendationText> generateRecommendationText(AiRecommendationContext context);
}
```

`DisabledDeepSeekClient` returns `Optional.empty()`.

- [ ] **Step 5: Implement fallback service**

`AiRecommendationService.generateAnalysis(AiRecommendationContext context)` must:

- call DeepSeek once with the full context;
- return `generated=true` when DeepSeek returns all required text fields;
- return local fallback with `generated=false` when DeepSeek is unavailable;
- include the top recipe name and diet goal in fallback text.

- [ ] **Step 6: Run AI service test**

Run:

```powershell
mvn -B -ntp -Dtest=AiRecommendationServiceTest test
```

Expected: `AiRecommendationServiceTest` passes.

---

### Task 3: DeepSeek HTTP Prompt and Parsing

**Files:**
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/HttpDeepSeekClient.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/HttpDeepSeekClientTest.java`

- [ ] **Step 1: Add successful JSON parsing test**

In `HttpDeepSeekClientTest`, add an HTTP server test returning:

```json
{
  "choices": [
    {
      "message": {
        "content": "{\"summary\":\"AI总结\",\"healthTip\":\"AI健康建议\",\"shoppingTip\":\"AI购物建议\",\"topRecipeReason\":\"AI推荐理由\"}"
      }
    }
  ]
}
```

Assert:

```java
Optional<AiRecommendationText> result = client.generateRecommendationText(context);
assertTrue(result.isPresent());
assertEquals("AI总结", result.get().summary());
assertEquals("AI健康建议", result.get().healthTip());
assertEquals("AI购物建议", result.get().shoppingTip());
assertEquals("AI推荐理由", result.get().topRecipeReason());
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd backend
mvn -B -ntp -Dtest=HttpDeepSeekClientTest test
```

Expected: fails until HTTP client contract and parser are updated.

- [ ] **Step 3: Update prompt**

System prompt must require JSON only:

```text
你是膳哉的知识库增强菜谱推荐助手。菜谱、营养和图片已经由系统数据库确定，你只能解释推荐结果，不能编造新菜谱、图片、热量或购物清单食材。必须只返回 JSON，不要输出 Markdown。
JSON 字段必须是 summary、healthTip、shoppingTip、topRecipeReason。
```

User prompt must include:

- diet goal;
- available ingredients;
- excluded ingredients;
- cooking time;
- recommended recipe snapshots.

- [ ] **Step 4: Update parsing**

`parseResponse` must reject missing or blank:

- `summary`
- `healthTip`
- `shoppingTip`
- `topRecipeReason`

If any field is blank, return `Optional.empty()`.

- [ ] **Step 5: Run HTTP client tests**

Run:

```powershell
mvn -B -ntp -Dtest=HttpDeepSeekClientTest test
```

Expected: timeout fallback test and successful parsing test both pass.

---

### Task 4: Recommendation Service Integration

**Files:**
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationService.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistorySummaryResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryDetailResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryService.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecommendationServiceTest.java`

- [ ] **Step 1: Build AI context after scoring**

In `RecommendationService.recommend`, after `recommendedRecipes` is built:

- build `AiRecommendationContext`;
- pass diet goal, available ingredients, excluded ingredients, cooking time;
- include recipe snapshots with matched ingredients and tags.

- [ ] **Step 2: Use analysis result**

Use:

```java
AiRecommendationAnalysis analysis = aiRecommendationService.generateAnalysis(context);
```

Set first recipe `reason` to `analysis.topRecipeReason()` when recipes are not empty. Other recipe reasons can stay local fallback.

- [ ] **Step 3: Save history fields**

Persist:

```java
history.setAiSummary(analysis.summary());
history.setAiHealthTip(analysis.healthTip());
history.setAiShoppingTip(analysis.shoppingTip());
history.setAiGenerated(analysis.generated());
```

- [ ] **Step 4: Return response fields**

Return:

```java
return new RecommendationResponse(
    history.getId(),
    analysis.summary(),
    analysis.healthTip(),
    analysis.shoppingTip(),
    analysis.generated(),
    recommendedRecipes
);
```

- [ ] **Step 5: Map history responses**

Add `aiHealthTip`, `aiShoppingTip`, and `aiGenerated` to summary and detail response records and service mapping.

- [ ] **Step 6: Run recommendation tests**

Run:

```powershell
cd backend
mvn -B -ntp -Dtest=RecommendationServiceTest test
```

Expected: recommendation tests pass.

---

### Task 5: Frontend Recommendation Result Display

**Files:**
- Modify: `frontend/src/types.ts`
- Modify: `frontend/src/views/user/RecommendView.vue`
- Modify: `frontend/src/views/user/RecommendationHistoryView.vue`

- [ ] **Step 1: Update TypeScript types**

`RecommendationResponse`:

```ts
export interface RecommendationResponse {
  historyId: number
  aiSummary: string
  aiHealthTip: string
  aiShoppingTip: string
  aiGenerated: boolean
  recipes: RecommendedRecipe[]
}
```

`RecommendationHistorySummary` must include the same AI fields.

- [ ] **Step 2: Update recommendation result panel**

In `RecommendView.vue`, display:

- status label: `AI 已生成分析` when `result.aiGenerated`, otherwise `规则推荐分析`;
- `result.aiSummary`;
- `result.aiHealthTip`;
- `result.aiShoppingTip`;
- existing recipe cards and shopping-list button.

- [ ] **Step 3: Keep recipe images database-backed**

Ensure recommendation cards continue using:

```ts
resolveRecipeImage(recipe.imageUrl)
```

Do not generate image URLs from AI text.

- [ ] **Step 4: Update history detail**

In `RecommendationHistoryView.vue`, show:

- AI summary;
- health tip;
- shopping tip;
- AI/fallback status.

- [ ] **Step 5: Build frontend**

Run:

```powershell
cd frontend
npm run build
```

Expected: TypeScript and Vite build succeed.

---

### Task 6: API Contract and Runbook

**Files:**
- Modify: `docs/api-contract.md`
- Modify: `docs/runbook.md`

- [ ] **Step 1: Update API contract**

Change recommendation response example to include:

```json
{
  "historyId": 17,
  "aiSummary": "本次推荐总结",
  "aiHealthTip": "健康建议",
  "aiShoppingTip": "购物建议",
  "aiGenerated": true,
  "recipes": []
}
```

- [ ] **Step 2: Document fallback behavior**

In `runbook.md`, add:

```markdown
## DeepSeek 配置

后端通过环境变量读取 API Key：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
```

未配置时，系统仍会基于数据库和规则评分返回推荐，并显示为“规则推荐分析”。
```

- [ ] **Step 3: Run full verification**

Run backend:

```powershell
cd backend
mvn -B -ntp test
```

Run frontend:

```powershell
cd frontend
npm run build
```

Expected:

- backend tests pass;
- frontend build succeeds;
- recommendation page displays AI/fallback analysis fields.

---

## Self-Review

### Spec Coverage

- Knowledge-base Agent positioning: covered by backend context, AI prompt, and frontend status display.
- No AI random recipe generation: covered by design constraints and prompt guardrails.
- Image consistency: covered by frontend image strategy and recipe database ownership.
- AI summary, health tip, shopping tip: covered by response contract, persistence, frontend display, and history display.
- DeepSeek fallback: covered by `aiGenerated=false` and service tests.
- Shopping list trust boundary: unchanged and still database-based.

### Empty Section Scan

The plan uses exact file paths, field names, commands, expected results, and concrete code shapes. There are no deferred implementation sections.

### Type Consistency

- Backend response fields: `aiSummary`, `aiHealthTip`, `aiShoppingTip`, `aiGenerated`.
- Frontend fields use the same camelCase names.
- DeepSeek JSON fields: `summary`, `healthTip`, `shoppingTip`, `topRecipeReason`.
- Database columns use snake_case: `ai_summary`, `ai_health_tip`, `ai_shopping_tip`, `ai_generated`.

## Execution Options

Plan complete. The recommended execution route is:

1. Use `superpowers:executing-plans` in this session because the affected files are tightly coupled.
2. Commit after backend contract passes, then commit after frontend build passes.
