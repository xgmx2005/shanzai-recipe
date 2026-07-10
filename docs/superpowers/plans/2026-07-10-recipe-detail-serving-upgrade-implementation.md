# 菜谱详情与份量缩放 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用稳定知识库补全每道菜的功效与详细步骤，并让详情、营养和购物清单按照 1 至 12 人份使用同一套后端缩放规则。

**Architecture:** `recipe_benefit` 与 `recipe_step` 保存可维护的结构化知识，`RecipeServingScaler` 是详情接口和购物清单共享的唯一份量计算边界。菜谱主表中的营养值继续表示“每人份”，原 `steps` 字段在过渡期保留兼容；公共详情接口返回基础人数、目标人数、缩放食材、每人份营养与总营养，管理端始终按基础人数编辑原始数据。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、JUnit 5、Mockito、Vue 3、TypeScript、Naive UI、Vitest。

---

## 执行前提

先依次完成 `2026-07-10-conversational-recommendation-core-implementation.md` 与 `2026-07-10-recommendation-evidence-explanation-implementation.md`。本计划复用核心计划保存的 `conversationContext`，并在已经包含联网讲解面板的独立结果页上补充人数与购物入口。

## 文件结构

### 后端新增

- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeBenefitEntity.java`：菜谱稳定功效条目。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeBenefitMapper.java`：功效数据访问。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeBenefitRequest.java`：管理端功效输入。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeBenefitResponse.java`：用户端功效输出。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeStepEntity.java`：结构化步骤实体。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeStepMapper.java`：步骤数据访问。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeStepRequest.java`：管理端步骤输入。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeStepResponse.java`：用户端步骤输出。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeNutritionResponse.java`：热量和三大营养素值对象。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeServingScaler.java`：食材和总营养的唯一缩放器。
- `backend/src/main/java/com/shanzai/recipe/modules/shopping/AvailableShoppingIngredientRequest.java`：带可选数量的已有食材输入。
- `backend/src/main/resources/db/migrations/2026-07-10-add-recipe-content-and-serving.sql`：结构与购物项备注迁移。
- `backend/src/main/resources/db/migrations/2026-07-10-seed-recipe-detail-content.sql`：45 道菜的稳定功效和详细步骤。

### 后端修改

- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeDetailResponse.java`：返回基础人数、目标人数、结构化知识和双口径营养。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeSaveRequest.java`：管理端提交功效和详细步骤。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeService.java`：加载、保存结构化内容并调用缩放器。
- `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeController.java`：支持 `servings` 查询参数。
- `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListCreateRequest.java`：接收目标人数和库存数量。
- `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListCalculator.java`：扣减已知库存并保留未知数量提醒。
- `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListService.java`：按每道菜基础人数缩放后合并。
- `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListEntity.java`：持久化清单目标人数。
- `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListResponse.java`：返回清单目标人数。
- `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingNeed.java`：携带库存提醒。
- `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListItemEntity.java`：持久化库存提醒。
- `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListItemResponse.java`：返回库存提醒。
- `backend/src/main/resources/db/schema.sql`：同步全量建库结构。
- `backend/src/main/resources/db/data.sql`：全量初始化时导入稳定详情内容。

### 前端新增

- `frontend/src/components/recipe/ServingStepper.vue`：1 至 12 人份步进器。
- `frontend/src/components/recipe/RecipeBenefitSection.vue`：稳定功效展示。
- `frontend/src/components/recipe/RecipeStepTimeline.vue`：详细步骤时间线。
- `frontend/src/components/shopping/InventoryIngredientEditor.vue`：紧凑的库存名称、数量和单位输入。

### 前端修改

- `frontend/src/types.ts`：补充详情、营养、步骤、功效和库存类型。
- `frontend/src/api/recipe.ts`：详情请求携带人数。
- `frontend/src/views/user/RecipeDetailView.vue`：提高信息密度并接入份量联动。
- `frontend/src/views/admin/RecipesView.vue`：维护功效和结构化步骤。
- `frontend/src/views/user/RecommendationResultView.vue`：进入详情时传递推荐人数。
- `frontend/src/views/user/ShoppingListsView.vue`：手工建清单时选择人数和数量化库存。
- `frontend/src/views/user/FavoritesView.vue`：收藏批量建清单时选择人数和数量化库存。
- `frontend/src/views/user/RecommendationHistoryView.vue`：历史建清单时复用上下文人数和库存。

### 测试新增或修改

- `backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeDetailSchemaTest.java`
- `backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeDetailSeedCoverageTest.java`
- `backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeServingScalerTest.java`
- `backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeServiceTest.java`
- `backend/src/test/java/com/shanzai/recipe/modules/shopping/ShoppingListServiceTest.java`
- `frontend/src/components/recipe/ServingStepper.spec.ts`
- `frontend/src/components/recipe/RecipeBenefitSection.spec.ts`
- `frontend/src/components/recipe/RecipeStepTimeline.spec.ts`
- `frontend/src/views/user/RecipeDetailView.spec.ts`
- `frontend/src/views/user/recommendationResultView.test.ts`
- `frontend/src/views/user/shoppingCreateContract.test.ts`

## 统一数据约定

- `recipe.servings` 是知识库原配方的基础人数；取值 1 至 12。
- `recipe.calories/protein/fat/carbs` 是每人份营养，推荐排序继续读取这些字段，不改变现有含义。
- 公共详情响应的 `servings` 是用户目标人数，`baseServings` 是原配方人数。
- 食材数量按 `目标人数 / 基础人数` 缩放；`个、只、片、张` 向上取整，其余单位保留两位小数并去除末尾零。
- 单位为“适量”的食材不制造虚假数值，缩放后仍显示“适量”；盐、油等调料的数字前端显示“约”。
- `nutritionPerServing` 直接使用主表营养；`nutritionTotal` 等于每人份营养乘目标人数。
- 购物清单只扣减名称匹配且单位兼容的已知库存。只有名称、没有数量时不删除该项，写入“库存数量未知，请采购前确认”。
- 原 `recipe.steps` 在本轮保留，由管理端保存时同步写入结构化步骤的 `instruction` 列表；公共详情优先读取 `recipe_step`，旧数据缺行时才回退 `steps`。

### Task 1: 建立结构化功效与步骤表

**Files:**
- Create: `backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeDetailSchemaTest.java`
- Create: `backend/src/main/resources/db/migrations/2026-07-10-add-recipe-content-and-serving.sql`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeBenefitEntity.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeBenefitMapper.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeStepEntity.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeStepMapper.java`
- Modify: `backend/src/main/resources/db/schema.sql`

- [ ] **Step 1: 写结构失败测试**

```java
class RecipeDetailSchemaTest {
    @Test
    void schemaDefinesStructuredRecipeContentAndShoppingNote() throws IOException {
        String schema = Files.readString(Path.of("src/main/resources/db/schema.sql"));

        assertTrue(schema.contains("recipe_benefit ("));
        assertTrue(schema.contains("UNIQUE KEY uk_recipe_benefit_order (recipe_id, display_order)"));
        assertTrue(schema.contains("recipe_step ("));
        assertTrue(schema.contains("UNIQUE KEY uk_recipe_step_no (recipe_id, step_no)"));
        assertTrue(Pattern.compile(
            "CREATE TABLE shopping_list \\(.*?servings INT NOT NULL DEFAULT 1",
            Pattern.DOTALL
        ).matcher(schema).find());
        assertTrue(schema.contains("inventory_note VARCHAR(255)"));
    }
}
```

- [ ] **Step 2: 运行并确认失败**

Run: `mvn -B -ntp -Dtest=RecipeDetailSchemaTest test`

Expected: FAIL，`schema.sql` 尚未定义 `recipe_benefit`。

- [ ] **Step 3: 新增迁移和全量建库结构**

迁移和 `schema.sql` 使用相同定义：

```sql
CREATE TABLE IF NOT EXISTS recipe_benefit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    title VARCHAR(80) NOT NULL,
    description VARCHAR(500) NOT NULL,
    display_order INT NOT NULL,
    UNIQUE KEY uk_recipe_benefit_order (recipe_id, display_order),
    KEY idx_recipe_benefit_recipe (recipe_id),
    CONSTRAINT fk_recipe_benefit_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recipe_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    title VARCHAR(80) NOT NULL,
    instruction VARCHAR(1000) NOT NULL,
    duration_minutes INT,
    heat_level VARCHAR(30),
    completion_cue VARCHAR(255),
    tips VARCHAR(500),
    UNIQUE KEY uk_recipe_step_no (recipe_id, step_no),
    KEY idx_recipe_step_recipe (recipe_id),
    CONSTRAINT fk_recipe_step_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_inventory_note = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'shopping_list_item'
      AND COLUMN_NAME = 'inventory_note'
);
SET @inventory_sql = IF(
    @has_inventory_note = 0,
    'ALTER TABLE shopping_list_item ADD COLUMN inventory_note VARCHAR(255)',
    'SELECT 1'
);
PREPARE inventory_stmt FROM @inventory_sql;
EXECUTE inventory_stmt;
DEALLOCATE PREPARE inventory_stmt;

SET @has_shopping_servings = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'shopping_list'
      AND COLUMN_NAME = 'servings'
);
SET @servings_sql = IF(
    @has_shopping_servings = 0,
    'ALTER TABLE shopping_list ADD COLUMN servings INT NOT NULL DEFAULT 1 AFTER source_recipe_ids',
    'SELECT 1'
);
PREPARE servings_stmt FROM @servings_sql;
EXECUTE servings_stmt;
DEALLOCATE PREPARE servings_stmt;
```

迁移用上面的 `information_schema.COLUMNS` + `PREPARE/EXECUTE` 分别判断 `inventory_note` 与 `shopping_list.servings` 是否存在，保证重复执行不会因重复列失败；两张新表使用 `CREATE TABLE IF NOT EXISTS`。全量 `schema.sql` 直接把 `servings` 放在 `shopping_list.source_recipe_ids` 后。

- [ ] **Step 4: 添加实体与 Mapper**

`RecipeBenefitEntity` 映射 `recipe_benefit`，字段为 `id/recipeId/title/description/displayOrder`；`RecipeStepEntity` 映射 `recipe_step`，字段为 `id/recipeId/stepNo/title/instruction/durationMinutes/heatLevel/completionCue/tips`。两个 Mapper 均为无自定义 SQL 的 `BaseMapper<T>`。

- [ ] **Step 5: 运行结构测试**

Run: `mvn -B -ntp -Dtest=RecipeDetailSchemaTest test`

Expected: PASS。

- [ ] **Step 6: 提交结构变更**

```bash
git add backend/src/main/resources/db backend/src/main/java/com/shanzai/recipe/modules/recipe backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeDetailSchemaTest.java
git commit -m "feat: 添加菜谱功效与详细步骤结构"
```

### Task 2: 为全部知识库菜谱补齐稳定详情内容

**Files:**
- Create: `backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeDetailSeedCoverageTest.java`
- Create: `backend/src/main/resources/db/migrations/2026-07-10-seed-recipe-detail-content.sql`
- Modify: `backend/src/main/resources/db/data.sql`

- [ ] **Step 1: 写 45 道菜覆盖测试**

```java
class RecipeDetailSeedCoverageTest {
    private String seed;

    @BeforeEach
    void loadSeed() throws IOException {
        seed = Files.readString(Path.of(
            "src/main/resources/db/migrations/2026-07-10-seed-recipe-detail-content.sql"
        ));
    }

    @Test
    void everyRecipeHasAtLeastTwoBenefitsAndFourDetailedSteps() {
        for (long recipeId = 1; recipeId <= 45; recipeId++) {
            assertTrue(countRows("RECIPE_BENEFIT", recipeId) >= 2, "缺少功效 recipe=" + recipeId);
            assertTrue(countRows("RECIPE_STEP", recipeId) >= 4, "缺少步骤 recipe=" + recipeId);
        }
    }

    @Test
    void detailedStepsContainCompletionCueAndNoMedicalPromise() {
        assertFalse(Pattern.compile("治疗|治愈|替代药物").matcher(seed).find());
        assertFalse(seed.contains("'', '', NULL, NULL, '', ''"));
        assertTrue(seed.contains("火候"));
        assertTrue(seed.contains("完成判断"));
    }

    private long countRows(String marker, long recipeId) {
        Pattern pattern = Pattern.compile("/\\* " + marker + ":" + recipeId + " \\*/");
        return pattern.matcher(seed).results().count();
    }
}
```

每条数据行前保留 `/* RECIPE_BENEFIT:菜谱ID */` 或 `/* RECIPE_STEP:菜谱ID */`，让测试不依赖脆弱的 SQL 拆分器。

- [ ] **Step 2: 运行并确认失败**

Run: `mvn -B -ntp -Dtest=RecipeDetailSeedCoverageTest test`

Expected: FAIL，种子迁移尚不存在。

- [ ] **Step 3: 编写幂等内容迁移**

迁移先按知识库范围删除再插入，确保内容修订可重复执行：

```sql
DELETE FROM recipe_benefit WHERE recipe_id BETWEEN 1 AND 45;
DELETE FROM recipe_step WHERE recipe_id BETWEEN 1 AND 45;

/* RECIPE_BENEFIT:1 */
INSERT INTO recipe_benefit (recipe_id, title, description, display_order)
VALUES (1, '优质蛋白搭配', '鸡胸肉提供蛋白质，搭配西兰花和玉米，适合作为结构清晰的一餐。', 1);
/* RECIPE_BENEFIT:1 */
INSERT INTO recipe_benefit (recipe_id, title, description, display_order)
VALUES (1, '控制烹调油脂', '以煎制和焯水为主，按配方控制用油量，便于管理整餐热量。', 2);

/* RECIPE_STEP:1 */
INSERT INTO recipe_step
    (recipe_id, step_no, title, instruction, duration_minutes, heat_level, completion_cue, tips)
VALUES
    (1, 1, '处理鸡胸肉', '鸡胸肉切成约 2 厘米小块，加入生抽和黑胡椒抓匀，静置入味。', 5, '无需加热', '肉块大小基本一致，表面均匀裹上调味料。', '顺纹切条后再切块，成熟度更均匀。'),
/* RECIPE_STEP:1 */
    (1, 2, '焯熟蔬菜', '水沸后放入西兰花和玉米，保持沸腾至西兰花颜色鲜绿后捞出。', 3, '大火', '西兰花可用筷子穿过梗部但仍有脆度。', '捞出后沥干，避免餐盘积水。'),
/* RECIPE_STEP:1 */
    (1, 3, '煎熟鸡肉', '平底锅预热后薄刷油，铺入鸡胸肉，中火煎至两面上色并翻炒至熟。', 7, '中火', '切开最大肉块后中心完全变白且没有粉红色。', '不要一次铺得过密，以免鸡肉出水。'),
/* RECIPE_STEP:1 */
    (1, 4, '组合装盘', '将鸡肉、西兰花和玉米分区装盘，按口味补少量黑胡椒。', 2, '无需加热', '食材沥干、温度适口、分量完整。', '需要带餐时先放凉再密封。');
```

对菜谱 2 至 45 逐一写入至少 2 条功效与 4 至 8 个步骤，必须满足：功效只解释已有食材和做法，不写医疗结论；每步包含动作、时间或完成判断；煎炒步骤写火候；肉蛋水产写可观察的熟制判断；总步骤时长不明显超过 `recipe.cooking_time`。将同一批 `INSERT` 追加到 `data.sql`，全量建库和增量迁移结果一致。

- [ ] **Step 4: 运行覆盖测试**

Run: `mvn -B -ntp -Dtest=RecipeDetailSeedCoverageTest test`

Expected: PASS，菜谱 1 至 45 均满足覆盖和安全词检查。

- [ ] **Step 5: 用临时库验证 SQL**

Run: `mysql -u root -p --default-character-set=utf8mb4 shanzai_recipe < backend/src/main/resources/db/migrations/2026-07-10-add-recipe-content-and-serving.sql`

Run: `mysql -u root -p --default-character-set=utf8mb4 shanzai_recipe < backend/src/main/resources/db/migrations/2026-07-10-seed-recipe-detail-content.sql`

Expected: 两个命令无 SQL 错误；查询 `recipe_benefit` 至少 90 行，`recipe_step` 至少 180 行。

- [ ] **Step 6: 提交知识内容**

```bash
git add backend/src/main/resources/db backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeDetailSeedCoverageTest.java
git commit -m "data: 补齐全部菜谱功效与详细步骤"
```

### Task 3: 实现统一份量缩放器

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeNutritionResponse.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeServingScaler.java`
- Create: `backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeServingScalerTest.java`

- [ ] **Step 1: 写食材和营养缩放失败测试**

```java
class RecipeServingScalerTest {
    private final RecipeServingScaler scaler = new RecipeServingScaler();

    @Test
    void scalesContinuousAndDiscreteIngredientsFromTwoToThreeServings() {
        List<RecipeIngredientResponse> result = scaler.scaleIngredients(List.of(
            ingredient(1L, "鸡胸肉", new BigDecimal("300"), "g"),
            ingredient(2L, "鸡蛋", new BigDecimal("2"), "个")
        ), 2, 3);

        assertEquals(new BigDecimal("450"), result.get(0).quantity());
        assertEquals(new BigDecimal("3"), result.get(1).quantity());
    }

    @Test
    void roundsDiscreteIngredientUpWhenReducingServings() {
        RecipeIngredientResponse result = scaler.scaleIngredients(
            List.of(ingredient(2L, "鸡蛋", new BigDecimal("1"), "个")), 2, 1
        ).get(0);

        assertEquals(new BigDecimal("1"), result.quantity());
    }

    @Test
    void keepsPerServingNutritionAndCalculatesTotal() {
        RecipeNutritionResponse perServing = new RecipeNutritionResponse(
            new BigDecimal("420"), new BigDecimal("35"), new BigDecimal("9"), new BigDecimal("45")
        );

        assertEquals(new BigDecimal("1260"), scaler.totalNutrition(perServing, 3).calories());
        assertEquals(new BigDecimal("105"), scaler.totalNutrition(perServing, 3).protein());
    }
}
```

- [ ] **Step 2: 运行并确认失败**

Run: `mvn -B -ntp -Dtest=RecipeServingScalerTest test`

Expected: FAIL，`RecipeServingScaler` 不存在。

- [ ] **Step 3: 写最小缩放实现**

```java
@Component
public class RecipeServingScaler {
    private static final Set<String> DISCRETE_UNITS = Set.of("个", "只", "片", "张");

    public List<RecipeIngredientResponse> scaleIngredients(
        List<RecipeIngredientResponse> ingredients,
        int baseServings,
        int targetServings
    ) {
        validateServings(baseServings, targetServings);
        BigDecimal factor = BigDecimal.valueOf(targetServings)
            .divide(BigDecimal.valueOf(baseServings), 8, RoundingMode.HALF_UP);
        return ingredients.stream().map(item -> new RecipeIngredientResponse(
            item.ingredientId(), item.name(), item.category(),
            scaleQuantity(item.quantity(), item.unit(), factor), item.unit(), item.core()
        )).toList();
    }

    public RecipeNutritionResponse totalNutrition(RecipeNutritionResponse perServing, int targetServings) {
        validateServings(1, targetServings);
        BigDecimal factor = BigDecimal.valueOf(targetServings);
        return new RecipeNutritionResponse(
            normalize(perServing.calories().multiply(factor)),
            normalize(perServing.protein().multiply(factor)),
            normalize(perServing.fat().multiply(factor)),
            normalize(perServing.carbs().multiply(factor))
        );
    }

    private BigDecimal scaleQuantity(BigDecimal quantity, String unit, BigDecimal factor) {
        if ("适量".equals(unit)) return quantity;
        BigDecimal scaled = quantity.multiply(factor);
        if (DISCRETE_UNITS.contains(unit)) return scaled.setScale(0, RoundingMode.CEILING);
        return normalize(scaled);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private void validateServings(int base, int target) {
        if (base < 1 || target < 1 || target > 12) {
            throw new BusinessException("用餐人数必须在 1 到 12 之间");
        }
    }
}
```

- [ ] **Step 4: 运行缩放测试**

Run: `mvn -B -ntp -Dtest=RecipeServingScalerTest test`

Expected: PASS。

- [ ] **Step 5: 提交缩放器**

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeNutritionResponse.java backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeServingScaler.java backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeServingScalerTest.java
git commit -m "feat: 添加统一菜谱份量缩放器"
```

### Task 4: 升级菜谱详情 API 与管理端保存事务

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeBenefitRequest.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeBenefitResponse.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeStepRequest.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeStepResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeDetailResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeSaveRequest.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeService.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recipe/RecipeController.java`
- Modify: `backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeServiceTest.java`

- [ ] **Step 1: 写详情人数与结构化内容失败测试**

在 `RecipeServiceTest` 的 mock 中加入 `RecipeBenefitMapper`、`RecipeStepMapper` 和 `RecipeServingScaler`，并增加：

```java
@Test
void getRecipeDetailScalesIngredientsAndReturnsStableKnowledge() {
    RecipeEntity recipe = recipe(1L, "鸡胸肉西兰花轻食碗", "低脂高蛋白轻食碗",
        "清淡,轻食", "低脂,高蛋白", "FAT_LOSS", 1);
    recipe.setServings(2);
    when(recipeMapper.selectById(1L)).thenReturn(recipe);
    when(recipeIngredientMapper.selectList(any())).thenReturn(List.of(
        recipeIngredient(1L, 1L, new BigDecimal("300"), "g", true)
    ));
    when(ingredientMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(
        ingredient(1L, "鸡胸肉", "肉蛋奶")
    ));
    when(recipeBenefitMapper.selectList(any())).thenReturn(List.of(
        benefit(1L, 1, "优质蛋白搭配", "组成清晰的一餐。")
    ));
    when(recipeStepMapper.selectList(any())).thenReturn(List.of(
        step(1L, 1, "处理鸡肉", "切块并腌制。", 5, "无需加热", "均匀裹料", "切块一致")
    ));

    RecipeDetailResponse detail = recipeService.getRecipeDetail(1L, 3);

    assertEquals(2, detail.baseServings());
    assertEquals(3, detail.servings());
    assertEquals(new BigDecimal("450"), detail.ingredients().get(0).quantity());
    assertEquals("优质蛋白搭配", detail.benefits().get(0).title());
    assertEquals("均匀裹料", detail.detailedSteps().get(0).completionCue());
    assertEquals(new BigDecimal("1260"), detail.nutritionTotal().calories());
}

@Test
void getRecipeDetailRejectsServingsOutsideSupportedRange() {
    assertThrows(BusinessException.class, () -> recipeService.getRecipeDetail(1L, 0));
    assertThrows(BusinessException.class, () -> recipeService.getRecipeDetail(1L, 13));
}
```

- [ ] **Step 2: 运行并确认失败**

Run: `mvn -B -ntp -Dtest=RecipeServiceTest test`

Expected: FAIL，详情服务没有人数参数和结构化字段。

- [ ] **Step 3: 定义请求与响应类型**

```java
public record RecipeBenefitRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull @Min(1) Integer displayOrder
) {}

public record RecipeBenefitResponse(String title, String description, Integer displayOrder) {}

public record RecipeStepRequest(
    @NotNull @Min(1) Integer stepNo,
    @NotBlank String title,
    @NotBlank String instruction,
    @Min(0) Integer durationMinutes,
    String heatLevel,
    @NotBlank String completionCue,
    String tips
) {}

public record RecipeStepResponse(
    Integer stepNo,
    String title,
    String instruction,
    Integer durationMinutes,
    String heatLevel,
    String completionCue,
    String tips
) {}

public record RecipeNutritionResponse(
    BigDecimal calories,
    BigDecimal protein,
    BigDecimal fat,
    BigDecimal carbs
) {}
```

在 `RecipeSaveRequest` 中用 `@Valid @NotEmpty List<RecipeBenefitRequest> benefits` 和 `@Valid @NotEmpty List<RecipeStepRequest> detailedSteps` 替换外部提交的 `List<String> steps`；`RecipeDetailResponse` 保留现有顶层营养字段以兼容卡片，并新增：

```java
Integer baseServings,
RecipeNutritionResponse nutritionPerServing,
RecipeNutritionResponse nutritionTotal,
List<RecipeBenefitResponse> benefits,
List<RecipeStepResponse> detailedSteps
```

- [ ] **Step 4: 实现详情加载和回退**

`getRecipeDetail(Long id, Integer requestedServings)` 先验证菜谱状态，再以 `requestedServings == null ? recipe.getServings() : requestedServings` 解析目标人数。分别按 `display_order` 和 `step_no` 查询新表；没有结构化步骤时，把旧 `parseSteps(recipe.getSteps())` 转为标题“步骤 N”、完成判断“按描述完成”的兼容响应。公共接口调用 `RecipeServingScaler` 缩放食材并计算总营养，管理端 `getAdminRecipeDetail` 固定使用基础人数。

Controller 改为：

```java
@GetMapping("/{id}")
public ApiResponse<RecipeDetailResponse> getRecipe(
    @PathVariable Long id,
    @RequestParam(required = false) @Min(1) @Max(12) Integer servings
) {
    return ApiResponse.ok(recipeService.getRecipeDetail(id, servings));
}
```

- [ ] **Step 5: 实现新增和编辑的原子保存**

`createRecipe` 和 `updateRecipe` 保持 `@Transactional`。保存主表与食材后，调用 `replaceBenefits`、`replaceSteps`：先按 `recipe_id` 删除旧行，再按请求顺序插入；同时把 `detailedSteps.stream().map(RecipeStepRequest::instruction)` 序列化回旧 `recipe.steps`，确保推荐历史和旧代码仍可读取。拒绝重复 `displayOrder`、重复 `stepNo`、少于 2 条功效、少于 4 个步骤，并检查步骤编号必须从 1 连续递增。

- [ ] **Step 6: 运行服务测试**

Run: `mvn -B -ntp -Dtest=RecipeServiceTest test`

Expected: PASS，旧列表行为、详情缩放、管理保存和下架校验均通过。

- [ ] **Step 7: 提交详情接口**

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recipe backend/src/test/java/com/shanzai/recipe/modules/recipe/RecipeServiceTest.java
git commit -m "feat: 升级菜谱详情与知识库维护接口"
```

### Task 5: 让购物清单按人数缩放并安全扣减库存

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/shopping/AvailableShoppingIngredientRequest.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListCreateRequest.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingNeed.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListCalculator.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListService.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListEntity.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListItemEntity.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/shopping/ShoppingListItemResponse.java`
- Modify: `backend/src/test/java/com/shanzai/recipe/modules/shopping/ShoppingListServiceTest.java`

- [ ] **Step 1: 写购物数量失败测试**

```java
@Test
void scalesNeedsBeforeMergingAndSubtractsKnownInventory() {
    ShoppingListCalculator calculator = new ShoppingListCalculator();
    List<RecipeNeed> scaled = List.of(
        new RecipeNeed(1L, "鸡胸肉", "肉蛋奶", new BigDecimal("600"), "g")
    );
    List<AvailableShoppingIngredientRequest> inventory = List.of(
        new AvailableShoppingIngredientRequest("鸡胸肉", new BigDecimal("200"), "g", true)
    );

    List<ShoppingNeed> result = calculator.calculate(scaled, inventory);

    assertEquals(new BigDecimal("400"), result.get(0).quantity());
    assertNull(result.get(0).inventoryNote());
}

@Test
void keepsNeedWhenInventoryQuantityIsUnknown() {
    List<ShoppingNeed> result = new ShoppingListCalculator().calculate(
        List.of(new RecipeNeed(2L, "鸡蛋", "肉蛋奶", new BigDecimal("3"), "个")),
        List.of(new AvailableShoppingIngredientRequest("鸡蛋", null, null, false))
    );

    assertEquals(new BigDecimal("3"), result.get(0).quantity());
    assertEquals("库存数量未知，请采购前确认", result.get(0).inventoryNote());
}

@Test
void createShoppingListScalesEachRecipeFromItsOwnBaseServings() {
    // 菜谱基础 2 人份、食材 300g，目标 3 人份，应保存 450g。
    when(recipeMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(recipeWithServings(1L, "鸡肉餐", 2)));
    when(recipeIngredientMapper.selectList(any())).thenReturn(List.of(recipeIngredient(1L, 1L, "300", "g")));
    when(ingredientMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(ingredient(1L, "鸡胸肉", "肉蛋奶")));

    shoppingListService.createShoppingList(7L, new ShoppingListCreateRequest(
        List.of(1L), 3, List.of(), "三人份采购清单"
    ));

    ArgumentCaptor<ShoppingListItemEntity> captor = ArgumentCaptor.forClass(ShoppingListItemEntity.class);
    verify(itemMapper).insert(captor.capture());
    assertEquals(new BigDecimal("450"), captor.getValue().getQuantity());
    ArgumentCaptor<ShoppingListEntity> listCaptor = ArgumentCaptor.forClass(ShoppingListEntity.class);
    verify(shoppingListMapper).insert(listCaptor.capture());
    assertEquals(3, listCaptor.getValue().getServings());
}
```

- [ ] **Step 2: 运行并确认失败**

Run: `mvn -B -ntp -Dtest=ShoppingListServiceTest test`

Expected: FAIL，购物请求没有人数和数量化库存。

- [ ] **Step 3: 定义兼容的购物请求**

```java
public record AvailableShoppingIngredientRequest(
    @NotBlank String name,
    @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
    String unit,
    boolean quantityKnown
) {}

public record ShoppingListCreateRequest(
    @NotEmpty(message = "请选择菜谱") List<Long> recipeIds,
    @NotNull @Min(1) @Max(12) Integer servings,
    @Valid List<AvailableShoppingIngredientRequest> availableIngredients,
    String title
) {}
```

所有前端调用在同一个提交内切换到新结构，因此不保留名称字符串数组的双协议。推荐对话中的 `AvailableIngredientInput` 在 Controller/Service 边界显式映射为该 shopping DTO，不让 shopping 模块依赖 conversation 包。

- [ ] **Step 4: 实现库存扣减规则**

`ShoppingListCalculator.calculate` 先按“规范化名称 + 单位”合并需求，再查同名库存：

1. `quantityKnown=false`：保留完整采购量并设置提醒。
2. 数量已知且单位相同：`max(需求量 - 库存量, 0)`，结果为 0 时不生成条目。
3. 数量已知但单位不同：不猜测换算，保留完整采购量并设置“库存单位不一致，请采购前确认”。
4. 不允许负数量；名称使用 `trim().toLowerCase(Locale.ROOT)`，中文别名在进入计算器前由食材词典规范化。

将 `inventoryNote` 加入 `ShoppingNeed`、`ShoppingListItemEntity` 和 `ShoppingListItemResponse`，并在 `insertItem` 中持久化。将 `servings` 加入 `ShoppingListEntity`、`ShoppingListResponse` 和列表摘要响应；创建清单时执行 `list.setServings(request.servings())`，让刷新后的清单仍能说明采购口径。

- [ ] **Step 5: 在 Service 中复用份量缩放器**

为 `ShoppingListService` 注入 `RecipeServingScaler`。把 `loadActiveRecipes` 的结果转成 `Map<Long, RecipeEntity>`，每个 `RecipeIngredientEntity` 根据所属菜谱的 `recipe.servings` 与请求 `servings` 调用缩放器；缩放后再传入 Calculator，不能先合并再缩放。

- [ ] **Step 6: 运行购物测试**

Run: `mvn -B -ntp -Dtest=ShoppingListServiceTest test`

Expected: PASS，包含多菜谱缩放、已知库存扣减、未知库存提醒和单位冲突。

- [ ] **Step 7: 提交购物链路**

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/shopping backend/src/test/java/com/shanzai/recipe/modules/shopping/ShoppingListServiceTest.java backend/src/main/resources/db
git commit -m "feat: 按人数生成可核对的购物清单"
```

### Task 6: 重构用户详情页的信息密度与人数交互

**Files:**
- Create: `frontend/src/components/recipe/ServingStepper.vue`
- Create: `frontend/src/components/recipe/RecipeBenefitSection.vue`
- Create: `frontend/src/components/recipe/RecipeStepTimeline.vue`
- Create: `frontend/src/components/shopping/InventoryIngredientEditor.vue`
- Create: `frontend/src/components/recipe/ServingStepper.spec.ts`
- Create: `frontend/src/components/recipe/RecipeBenefitSection.spec.ts`
- Create: `frontend/src/components/recipe/RecipeStepTimeline.spec.ts`
- Modify: `frontend/src/types.ts`
- Modify: `frontend/src/api/recipe.ts`
- Modify: `frontend/src/views/user/RecipeDetailView.vue`
- Modify: `frontend/src/views/user/RecommendationResultView.vue`
- Modify: `frontend/src/views/user/ShoppingListsView.vue`
- Modify: `frontend/src/views/user/FavoritesView.vue`
- Modify: `frontend/src/views/user/RecommendationHistoryView.vue`
- Create: `frontend/src/views/user/RecipeDetailView.spec.ts`
- Modify: `frontend/src/views/user/recommendationResultView.test.ts`
- Create: `frontend/src/views/user/shoppingCreateContract.test.ts`

- [ ] **Step 1: 写人数步进器失败测试**

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ServingStepper from './ServingStepper.vue'

describe('ServingStepper', () => {
  it('在 1 到 12 之间增减并发送目标人数', async () => {
    const wrapper = mount(ServingStepper, { props: { modelValue: 2 } })
    await wrapper.get('[aria-label="增加人数"]').trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([3])

    await wrapper.setProps({ modelValue: 1 })
    expect(wrapper.get('[aria-label="减少人数"]').attributes('disabled')).toBeDefined()
  })
})
```

- [ ] **Step 2: 写功效与步骤失败测试**

```ts
it('展示知识库功效而不渲染医疗承诺', () => {
  const wrapper = mount(RecipeBenefitSection, {
    props: { benefits: [{ title: '优质蛋白搭配', description: '组成清晰的一餐。', displayOrder: 1 }] },
  })
  expect(wrapper.text()).toContain('优质蛋白搭配')
  expect(wrapper.text()).not.toMatch(/治疗|治愈|替代药物/)
})

it('展示步骤动作、火候、完成判断和提示', () => {
  const wrapper = mount(RecipeStepTimeline, {
    props: { steps: [{
      stepNo: 1,
      title: '煎熟鸡肉',
      instruction: '平底锅薄刷油后放入鸡肉。',
      durationMinutes: 7,
      heatLevel: '中火',
      completionCue: '中心完全变白。',
      tips: '不要铺得过密。',
    }] },
  })
  expect(wrapper.text()).toContain('中火')
  expect(wrapper.text()).toContain('中心完全变白')
  expect(wrapper.text()).toContain('不要铺得过密')
})
```

- [ ] **Step 3: 运行并确认失败**

Run: `npm run test:unit -- --run frontend/src/components/recipe/ServingStepper.spec.ts frontend/src/components/recipe/RecipeBenefitSection.spec.ts frontend/src/components/recipe/RecipeStepTimeline.spec.ts`

Expected: FAIL，组件尚不存在。

- [ ] **Step 4: 补齐 TypeScript 类型与 API**

```ts
export interface RecipeNutrition {
  calories: number
  protein: number
  fat: number
  carbs: number
}

export interface RecipeBenefit {
  title: string
  description: string
  displayOrder: number
}

export interface RecipeStep {
  stepNo: number
  title: string
  instruction: string
  durationMinutes?: number
  heatLevel?: string
  completionCue: string
  tips?: string
}

export interface RecipeDetail extends RecipeSummary {
  baseServings: number
  nutritionPerServing: RecipeNutrition
  nutritionTotal: RecipeNutrition
  benefits: RecipeBenefit[]
  detailedSteps: RecipeStep[]
  ingredients: RecipeIngredient[]
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface AvailableShoppingIngredient {
  name: string
  quantity?: number
  unit?: string
  quantityKnown: boolean
}

export interface ShoppingListCreateRequest {
  recipeIds: number[]
  servings: number
  availableIngredients: AvailableShoppingIngredient[]
  title: string
}
```

同时把 `servings: number` 直接加入现有 `ShoppingList` 与 `ShoppingListSummary`，把 `inventoryNote?: string` 直接加入 `ShoppingListItem`。

`getRecipe` 改为：

```ts
export function getRecipe(id: number, servings?: number) {
  return http.get<RecipeDetail>(`/recipes/${id}`, { params: { servings } }).then((res) => res.data)
}
```

- [ ] **Step 5: 实现三个展示组件**

`ServingStepper` 使用两个仅图标按钮（`Minus`、`Plus`）和固定宽度人数值，按钮带 `aria-label` 与 tooltip，组件本身不发网络请求。`RecipeBenefitSection` 是一个全宽、非嵌套卡片区，按 `displayOrder` 排序；`RecipeStepTimeline` 使用稳定编号轨道，步骤主体依次显示 instruction、时长/火候、完成判断、tips，空的可选字段不占位。

- [ ] **Step 6: 实现库存编辑器**

`InventoryIngredientEditor` 每行包含食材名、可选数量和单位；用户只填名称时输出 `quantityKnown=false`，填写数量时必须同时填写单位并输出 `quantityKnown=true`。最多 20 行，重复名称在前端合并提示但最终由后端校验。使用 `Plus` 添加、`Trash2` 删除，保持购物弹窗宽度 `420px`、移动端 `max-width: calc(100vw - 32px)`。

- [ ] **Step 7: 写详情页联动失败测试**

```ts
it('从查询参数读取人数并在变更后重新加载详情', async () => {
  vi.mocked(useRoute).mockReturnValue({ params: { id: '1' }, query: { servings: '3' } } as never)
  vi.mocked(getRecipe).mockResolvedValue(recipeDetailFixture({ servings: 3 }))
  const wrapper = mount(RecipeDetailView)
  await flushPromises()

  expect(getRecipe).toHaveBeenCalledWith(1, 3)
  await wrapper.get('[aria-label="增加人数"]').trigger('click')
  await flushPromises()
  expect(getRecipe).toHaveBeenLastCalledWith(1, 4)
})

it('生成购物清单时提交当前人数和库存数量', async () => {
  // 打开弹窗，填写鸡蛋 2 个并确认。
  expect(createShoppingList).toHaveBeenCalledWith(expect.objectContaining({
    recipeIds: [1],
    servings: 3,
    availableIngredients: [{ name: '鸡蛋', quantity: 2, unit: '个', quantityKnown: true }],
  }))
})

it('推荐结果使用历史上下文创建同人数购物清单', async () => {
  const history = recommendationHistoryFixture({
    conversationContext: {
      servings: 3,
      availableIngredients: [{ name: '鸡蛋', quantity: 2, unit: '个', quantityKnown: true }],
    },
  })
  vi.mocked(getRecommendationHistory).mockResolvedValue(history)

  const wrapper = mount(RecommendationResultView)
  await flushPromises()
  await wrapper.get('[data-testid="create-result-shopping-list"]').trigger('click')

  expect(createShoppingList).toHaveBeenCalledWith(expect.objectContaining({
    servings: 3,
    availableIngredients: history.conversationContext?.availableIngredients,
  }))
})
```

新增 `shoppingCreateContract.test.ts` 锁定其他三个入口：

```ts
it.each(['ShoppingListsView.vue', 'FavoritesView.vue', 'RecommendationHistoryView.vue'])(
  '%s 使用人数和结构化库存创建清单',
  (file) => {
    const source = readFileSync(new URL(`./${file}`, import.meta.url), 'utf8')
    expect(source).toContain('servings:')
    expect(source).toContain('availableIngredients:')
    expect(source).toContain('InventoryIngredientEditor')
  },
)
```

- [ ] **Step 8: 重排详情页**

`RecipeDetailView` 执行以下固定布局：

1. Hero 改为紧凑的 `minmax(0, 0.9fr) minmax(360px, 1.1fr)`，图片桌面高度约 360px，不再占据整屏。
2. 标题区加入 `ServingStepper`，人数改变后更新路由 `?servings=N` 并调用 `getRecipe(id, N)`；请求期间保留旧内容并只禁用步进器，避免整页闪烁。
3. 营养区明确标注“每人份”和“本餐 N 人合计”，不再让用户猜口径。
4. 功效区放在营养下方，使用知识库内容；不显示推荐页联网来源。
5. 食材与详细步骤保持两列，右列按内容自然高度，不用固定 `min-height` 填空。
6. 购物弹窗使用 `InventoryIngredientEditor` 并提交当前人数。
7. 从推荐结果进入详情时，`RecommendationResultView` 链接携带 `servings` 和 `historyId`，返回时仍回到独立结果页。
8. 食材单位为“适量”时只显示“适量”；分类为“调料”的数值数量显示为“约 N 单位”。
9. 结果页“生成整单”从历史 `conversationContext` 读取人数和带数量库存；旧历史没有快照时默认人数 1、库存空数组，并在确认弹窗中允许用户修改。
10. `ShoppingListsView` 和 `FavoritesView` 的建单弹窗加入 `ServingStepper` 与 `InventoryIngredientEditor`，默认 1 人；`RecommendationHistoryView` 默认使用 `detail.servings` 和 `detail.conversationContext.availableIngredients`。三个入口都提交新的结构化协议，不再把字符串数组当作“库存完全足够”。

- [ ] **Step 9: 运行组件和详情页测试**

Run: `npm run test:unit -- --run frontend/src/components/recipe frontend/src/views/user/RecipeDetailView.spec.ts frontend/src/views/user/recommendationResultView.test.ts frontend/src/views/user/shoppingCreateContract.test.ts`

Expected: PASS。

- [ ] **Step 10: 提交用户详情页**

```bash
git add frontend/src/components/recipe frontend/src/components/shopping frontend/src/types.ts frontend/src/api/recipe.ts frontend/src/views/user/RecipeDetailView.vue frontend/src/views/user/RecommendationResultView.vue frontend/src/views/user/ShoppingListsView.vue frontend/src/views/user/FavoritesView.vue frontend/src/views/user/RecommendationHistoryView.vue frontend/src/views/user/RecipeDetailView.spec.ts frontend/src/views/user/recommendationResultView.test.ts frontend/src/views/user/shoppingCreateContract.test.ts
git commit -m "feat: 完善菜谱详情与人数联动展示"
```

### Task 7: 升级管理端菜谱知识维护

**Files:**
- Modify: `frontend/src/types.ts`
- Modify: `frontend/src/views/admin/RecipesView.vue`
- Create: `frontend/src/views/admin/RecipesView.spec.ts`

- [ ] **Step 1: 写管理表单失败测试**

```ts
it('编辑菜谱时加载并提交结构化功效与步骤', async () => {
  vi.mocked(getAdminRecipe).mockResolvedValue(recipeDetailFixture({
    benefits: [
      { title: '优质蛋白搭配', description: '组成清晰的一餐。', displayOrder: 1 },
      { title: '控制烹调油脂', description: '按配方控制用油。', displayOrder: 2 },
    ],
    detailedSteps: detailedStepFixtures(4),
  }))

  const wrapper = mount(RecipesView)
  await openFirstRecipeEditor(wrapper)
  expect(wrapper.findAll('[data-testid="benefit-row"]')).toHaveLength(2)
  expect(wrapper.findAll('[data-testid="step-row"]')).toHaveLength(4)

  await wrapper.get('[data-testid="save-recipe"]').trigger('click')
  expect(updateAdminRecipe).toHaveBeenCalledWith(expect.any(Number), expect.objectContaining({
    benefits: expect.arrayContaining([expect.objectContaining({ title: '优质蛋白搭配' })]),
    detailedSteps: expect.arrayContaining([expect.objectContaining({ stepNo: 1 })]),
  }))
})
```

- [ ] **Step 2: 运行并确认失败**

Run: `npm run test:unit -- --run frontend/src/views/admin/RecipesView.spec.ts`

Expected: FAIL，管理表单仍使用字符串标签式步骤。

- [ ] **Step 3: 修改管理端请求类型**

`RecipeSaveRequest` 删除 `steps: string[]`，新增：

```ts
benefits: RecipeBenefit[]
detailedSteps: RecipeStep[]
```

- [ ] **Step 4: 实现功效编辑区**

每条功效使用标题输入和多行描述，顺序由数组位置生成 `displayOrder`；至少 2 条、最多 6 条。新增按钮使用 `Plus`，删除使用 `Trash2`，保存前拦截空标题、空描述和医疗承诺词“治疗、治愈、替代药物”。

- [ ] **Step 5: 实现结构化步骤编辑区**

把 `n-dynamic-tags` 替换为可展开步骤列表。每步包含标题、做法说明、分钟、火候、完成判断、提示；拖拽不在本轮实现，使用上移/下移图标并在 `buildPayload` 中按数组位置重排 `stepNo`。至少 4 步、最多 12 步，肉蛋水产步骤的完成判断由维护者填写，不在前端自动生成。

- [ ] **Step 6: 运行管理端测试与构建**

Run: `npm run test:unit -- --run frontend/src/views/admin/RecipesView.spec.ts`

Expected: PASS。

Run: `npm run build`

Expected: TypeScript 与 Vite 构建成功。

- [ ] **Step 7: 提交管理端维护功能**

```bash
git add frontend/src/types.ts frontend/src/views/admin/RecipesView.vue frontend/src/views/admin/RecipesView.spec.ts
git commit -m "feat: 支持维护菜谱功效与详细步骤"
```

### Task 8: 全链路回归与数据库验收

**Files:**
- Modify only if tests expose a defect in files listed by Tasks 1-7.

- [ ] **Step 1: 运行后端全量测试**

Run: `mvn -B -ntp test`

Expected: 全部测试通过，0 failures，0 errors。

- [ ] **Step 2: 运行前端全量测试和构建**

Run: `npm run test:unit -- --run`

Expected: 全部测试通过。

Run: `npm run build`

Expected: 构建成功，无 TypeScript 错误。

- [ ] **Step 3: 在本地 MySQL 执行迁移并核对数据**

在 IDEA Database Console 中依次执行：

```sql
SOURCE G:/CODE/短学期/backend/src/main/resources/db/migrations/2026-07-10-add-recipe-content-and-serving.sql;
SOURCE G:/CODE/短学期/backend/src/main/resources/db/migrations/2026-07-10-seed-recipe-detail-content.sql;

SELECT COUNT(DISTINCT recipe_id) AS benefit_recipe_count FROM recipe_benefit;
SELECT COUNT(DISTINCT recipe_id) AS step_recipe_count FROM recipe_step;
SELECT MIN(step_count), MAX(step_count)
FROM (SELECT recipe_id, COUNT(*) AS step_count FROM recipe_step GROUP BY recipe_id) counts;
```

Expected: 两个 distinct count 均为 45；最少步骤数不小于 4，最多不大于 8。

- [ ] **Step 4: 手工验证关键流程**

1. 推荐 3 人份菜谱，进入详情后 URL 保留 `servings=3`，食材为基础配方的 3/基础人数倍。
2. 人数从 3 改为 4，食材和总营养变化，每人份营养不变。
3. 输入库存“鸡胸肉 200 g”生成购物清单，数量按 4 人份需求减 200g。
4. 仅输入“鸡蛋”不填数量，购物清单仍保留鸡蛋并显示确认提醒。
5. 详情页展示至少 2 条稳定功效和 4 个详细步骤；刷新不调用 DeepSeek 或 Tavily。
6. 管理端修改一条功效和步骤，保存后用户详情立即读取新内容。

- [ ] **Step 5: 检查响应式布局**

使用浏览器分别检查 1440×900、1024×768、390×844：无横向滚动；Hero 图片不挤压标题；步进器不换行；食材和步骤在 980px 以下切为单列；购物弹窗不超过视口且按钮文字完整。

- [ ] **Step 6: 提交联调修复**

```bash
git add backend frontend
git commit -m "fix: 修复菜谱详情与份量联调问题"
```

若没有代码变化，不创建空提交。
