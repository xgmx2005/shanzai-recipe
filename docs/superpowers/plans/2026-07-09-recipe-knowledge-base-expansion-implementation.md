# Recipe Knowledge Base Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the seed recipe knowledge base to about 45 recipes while keeping recommendations database-backed and limited to the existing three diet goals.

**Architecture:** This is a data-first expansion. `data.sql` remains the source for fresh imports, an additive migration supports existing local databases, and a focused backend test validates the SQL seed quality without requiring MySQL. The recommendation algorithm and DeepSeek contract stay unchanged.

**Tech Stack:** Spring Boot 3, Java 17, JUnit 5, MySQL 8 SQL seed files, Vue 3 static image assets.

---

## File Structure

- Create `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecipeKnowledgeBaseSeedDataTest.java`
  - Parses `backend/src/main/resources/db/data.sql`.
  - Verifies seed counts, goal distribution, ingredient references, recipe ingredient counts, JSON steps, and image asset existence.
- Modify `backend/src/main/resources/db/data.sql`
  - Add common ingredients used by the new recipes.
  - Add recipes 19-45.
  - Add recipe_ingredient rows for recipes 19-45.
- Create `backend/src/main/resources/db/migrations/2026-07-09-expand-recipe-knowledge-base.sql`
  - Additive migration for existing local databases.
  - Inserts ingredients, recipes, and recipe_ingredient rows without truncating user data.
- Modify `docs/recipe-image-sources.md`
  - Record which new recipes reuse existing local images or use the default image.
- Optional modify `frontend/public/images/recipes/`
  - Only if a small number of additional real images are added. Otherwise reuse existing plausible local photos and default SVG.

## Task 1: Add Seed Data Quality Test

**Files:**
- Create: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecipeKnowledgeBaseSeedDataTest.java`

- [ ] **Step 1: Write failing test file**

Create `RecipeKnowledgeBaseSeedDataTest.java` with these tests:

```java
package com.shanzai.recipe.modules.recommendation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeKnowledgeBaseSeedDataTest {
    private static final Path DATA_SQL = Path.of("src/main/resources/db/data.sql");
    private static final Path FRONTEND_PUBLIC = Path.of("../frontend/public");

    @Test
    void seedDataHasExpandedRecipeCoverage() throws IOException {
        SeedData seedData = parseSeedData();

        assertTrue(seedData.recipes().size() >= 45, "seed data should contain at least 45 recipes");
        assertTrue(countGoal(seedData, "FAT_LOSS") >= 15, "FAT_LOSS should have at least 15 active recipes");
        assertTrue(countGoal(seedData, "BALANCED") >= 15, "BALANCED should have at least 15 active recipes");
        assertTrue(countGoal(seedData, "MUSCLE_GAIN") >= 15, "MUSCLE_GAIN should have at least 15 active recipes");
    }

    @Test
    void seedDataRecipeIngredientsReferenceExistingIngredients() throws IOException {
        SeedData seedData = parseSeedData();

        for (RecipeIngredient row : seedData.recipeIngredients()) {
            assertTrue(seedData.ingredients().contains(row.ingredientId()),
                "missing ingredient id " + row.ingredientId() + " for recipe " + row.recipeId());
            assertTrue(seedData.recipes().containsKey(row.recipeId()),
                "missing recipe id " + row.recipeId());
        }
    }

    @Test
    void seedDataRecipesHaveEnoughIngredientsStepsAndImages() throws IOException {
        SeedData seedData = parseSeedData();
        Map<Long, Integer> ingredientCounts = new HashMap<>();
        for (RecipeIngredient row : seedData.recipeIngredients()) {
            ingredientCounts.merge(row.recipeId(), 1, Integer::sum);
        }

        for (RecipeSeed recipe : seedData.recipes().values()) {
            assertTrue(ingredientCounts.getOrDefault(recipe.id(), 0) >= 3,
                recipe.name() + " should have at least 3 ingredients");
            assertTrue(recipe.steps().startsWith("[") && recipe.steps().endsWith("]"),
                recipe.name() + " steps should be a JSON array text");
            assertTrue(recipe.steps().split("\",\"").length >= 3,
                recipe.name() + " should have at least 3 steps");
            if (recipe.imageUrl().startsWith("/images/")) {
                Path asset = FRONTEND_PUBLIC.resolve(recipe.imageUrl().substring(1));
                assertTrue(Files.exists(asset), "missing image asset " + asset);
            }
        }
    }

    private int countGoal(SeedData seedData, String goal) {
        return (int) seedData.recipes().values().stream()
            .filter(recipe -> recipe.targetGoals().contains(goal))
            .count();
    }

    private SeedData parseSeedData() throws IOException {
        String sql = Files.readString(DATA_SQL);
        return new SeedData(parseIngredientIds(sql), parseRecipes(sql), parseRecipeIngredients(sql));
    }

    private Set<Long> parseIngredientIds(String sql) {
        String values = valuesBlock(sql, "ingredient");
        Pattern pattern = Pattern.compile("\\((\\d+),\\s*'");
        Matcher matcher = pattern.matcher(values);
        Set<Long> ids = new HashSet<>();
        while (matcher.find()) {
            ids.add(Long.parseLong(matcher.group(1)));
        }
        return ids;
    }

    private Map<Long, RecipeSeed> parseRecipes(String sql) {
        String values = valuesBlock(sql, "recipe");
        List<String> rows = splitRows(values);
        Map<Long, RecipeSeed> recipes = new HashMap<>();
        for (String row : rows) {
            List<String> columns = splitSqlColumns(row);
            Long id = Long.valueOf(columns.get(0));
            recipes.put(id, new RecipeSeed(
                id,
                unquote(columns.get(1)),
                unquote(columns.get(3)),
                unquote(columns.get(12)),
                unquote(columns.get(13)),
                unquote(columns.get(14))
            ));
        }
        return recipes;
    }

    private List<RecipeIngredient> parseRecipeIngredients(String sql) {
        String values = valuesBlock(sql, "recipe_ingredient");
        Pattern pattern = Pattern.compile("\\((\\d+),\\s*(\\d+),\\s*([0-9.]+),\\s*'[^']+',\\s*[01]\\)");
        Matcher matcher = pattern.matcher(values);
        List<RecipeIngredient> rows = new ArrayList<>();
        while (matcher.find()) {
            rows.add(new RecipeIngredient(
                Long.parseLong(matcher.group(1)),
                Long.parseLong(matcher.group(2)),
                new BigDecimal(matcher.group(3))
            ));
        }
        return rows;
    }

    private String valuesBlock(String sql, String table) {
        Pattern pattern = Pattern.compile("INSERT INTO " + table + " .*? VALUES\\s*(.*?);", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        assertTrue(matcher.find(), "missing INSERT block for " + table);
        return matcher.group(1);
    }

    private List<String> splitRows(String values) {
        List<String> rows = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        int start = -1;
        for (int i = 0; i < values.length(); i++) {
            char current = values.charAt(i);
            char previous = i == 0 ? '\0' : values.charAt(i - 1);
            if (current == '\'' && previous != '\\') {
                inString = !inString;
            }
            if (!inString && current == '(') {
                if (depth == 0) {
                    start = i + 1;
                }
                depth++;
            } else if (!inString && current == ')') {
                depth--;
                if (depth == 0 && start >= 0) {
                    rows.add(values.substring(start, i));
                }
            }
        }
        return rows;
    }

    private List<String> splitSqlColumns(String row) {
        List<String> columns = new ArrayList<>();
        boolean inString = false;
        int start = 0;
        for (int i = 0; i < row.length(); i++) {
            char current = row.charAt(i);
            char previous = i == 0 ? '\0' : row.charAt(i - 1);
            if (current == '\'' && previous != '\\') {
                inString = !inString;
            }
            if (!inString && current == ',') {
                columns.add(row.substring(start, i).trim());
                start = i + 1;
            }
        }
        columns.add(row.substring(start).trim());
        return columns;
    }

    private String unquote(String value) {
        assertFalse(value.isBlank());
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private record SeedData(
        Set<Long> ingredients,
        Map<Long, RecipeSeed> recipes,
        List<RecipeIngredient> recipeIngredients
    ) {
    }

    private record RecipeSeed(
        Long id,
        String name,
        String imageUrl,
        String healthTags,
        String targetGoals,
        String steps
    ) {
    }

    private record RecipeIngredient(Long recipeId, Long ingredientId, BigDecimal quantity) {
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
cd backend
mvn -B -ntp -Dtest=RecipeKnowledgeBaseSeedDataTest test
```

Expected: test compiles and fails because current seed data has only 18 recipes and each goal has fewer than 15 recipes.

## Task 2: Expand Fresh Seed Data

**Files:**
- Modify: `backend/src/main/resources/db/data.sql`

- [ ] **Step 1: Add ingredients 37-61**

Append these rows to the existing `INSERT INTO ingredient` block, changing the previous final row `(36, ...)` from semicolon to comma:

```sql
(37, '鸡腿肉', '肉蛋奶', 'g', 181, 19.00, 11.00, 0.00, '去皮鸡腿肉'),
(38, '猪里脊', '肉蛋奶', 'g', 155, 20.20, 7.90, 0.70, '里脊肉'),
(39, '巴沙鱼', '肉蛋奶', 'g', 90, 18.00, 2.00, 0.00, '鱼柳'),
(40, '牛奶', '肉蛋奶', 'ml', 54, 3.40, 3.20, 5.00, '纯牛奶'),
(41, '无糖酸奶', '肉蛋奶', 'g', 72, 4.00, 3.00, 7.00, '酸奶'),
(42, '毛豆', '其他', 'g', 131, 13.10, 5.00, 10.50, '青豆'),
(43, '菠菜', '蔬菜', 'g', 23, 2.90, 0.40, 3.60, ''),
(44, '白菜', '蔬菜', 'g', 20, 1.50, 0.20, 3.20, '大白菜'),
(45, '洋葱', '蔬菜', 'g', 40, 1.10, 0.10, 9.30, ''),
(46, '彩椒', '蔬菜', 'g', 31, 1.00, 0.30, 6.00, '甜椒'),
(47, '南瓜', '其他', 'g', 23, 0.70, 0.10, 5.30, ''),
(48, '菌菇', '蔬菜', 'g', 28, 2.50, 0.30, 5.00, '蘑菇,蟹味菇'),
(49, '冬瓜', '蔬菜', 'g', 12, 0.40, 0.20, 2.60, ''),
(50, '西葫芦', '蔬菜', 'g', 19, 1.20, 0.20, 3.10, ''),
(51, '红薯', '主食', 'g', 86, 1.60, 0.10, 20.10, '地瓜'),
(52, '荞麦面', '主食', 'g', 99, 5.10, 0.70, 21.00, '荞麦挂面'),
(53, '杂粮饭', '主食', 'g', 118, 3.00, 0.80, 24.00, '杂粮米饭'),
(54, '玉米面', '主食', 'g', 340, 8.10, 3.30, 73.00, ''),
(55, '紫薯', '主食', 'g', 82, 1.90, 0.20, 18.40, ''),
(56, '姜', '调料', 'g', 80, 1.80, 0.80, 18.00, ''),
(57, '蒜', '调料', 'g', 149, 6.40, 0.50, 33.10, '大蒜'),
(58, '料酒', '调料', 'ml', 66, 1.00, 0.00, 4.00, ''),
(59, '醋', '调料', 'ml', 21, 0.00, 0.00, 3.00, ''),
(60, '番茄酱', '调料', 'g', 112, 1.70, 0.20, 25.80, ''),
(61, '孜然粉', '调料', 'g', 375, 17.80, 22.30, 44.20, '孜然');
```

- [ ] **Step 2: Add recipes 19-45**

Append recipes 19-45 to the existing `INSERT INTO recipe` block, changing recipe 18 from semicolon to comma. Use image paths that already exist or the default SVG:

```sql
(19, '冬瓜虾仁汤', '冬瓜和虾仁煮成清淡汤品，热量低且适合晚餐。', '/images/recipes/shrimp-tofu-soup.jpg', 18, 'EASY', 1, 260, 24.00, 5.00, 24.00, '清淡,汤类', '低卡,高蛋白,少油', 'FAT_LOSS,BALANCED', '["冬瓜去皮切片，虾仁洗净。","锅中加水煮开后放入冬瓜。","冬瓜变软后加入虾仁煮至变色。","用盐、姜和葱调味后出锅。"]', 1, 2),
(20, '菠菜鸡蛋豆腐汤', '菠菜、鸡蛋和豆腐搭配，清淡快手又有蛋白质。', '/images/recipes/egg-tofu-custard.jpg', 15, 'EASY', 1, 290, 23.00, 14.00, 16.00, '清淡,快手', '低卡,高蛋白,易做', 'FAT_LOSS,BALANCED', '["菠菜洗净切段，豆腐切块。","锅中加水煮开后放入豆腐。","加入菠菜煮软。","倒入蛋液形成蛋花并调味。"]', 1, 2),
(21, '巴沙鱼西兰花轻食盘', '巴沙鱼和西兰花组成低脂轻食盘，适合控制热量。', '/images/recipes/cod-asparagus.jpg', 22, 'EASY', 1, 380, 34.00, 8.00, 38.00, '清淡,轻食', '低脂,高蛋白,低卡', 'FAT_LOSS', '["巴沙鱼用柠檬汁、盐和黑胡椒腌制。","西兰花焯水备用。","平底锅少油煎熟巴沙鱼。","搭配米饭和西兰花装盘。"]', 1, 2),
(22, '鸡腿肉南瓜沙拉', '去皮鸡腿肉搭配南瓜和生菜，兼顾饱腹与清爽。', '/images/recipes/chicken-avocado-wrap.jpg', 25, 'EASY', 1, 430, 31.00, 14.00, 45.00, '清爽,轻食', '高蛋白,饱腹,适中热量', 'FAT_LOSS,BALANCED', '["鸡腿肉去皮切块并煎熟。","南瓜蒸熟切块。","生菜洗净沥干。","全部混合后加少量醋和黑胡椒。"]', 1, 2),
(23, '番茄豆腐菌菇汤', '番茄、豆腐和菌菇煮汤，酸甜清淡，适合轻食。', '/images/recipes/shrimp-tofu-soup.jpg', 18, 'EASY', 1, 310, 20.00, 12.00, 28.00, '清淡,汤类', '低卡,均衡,素食', 'FAT_LOSS,BALANCED', '["番茄切块，豆腐切块，菌菇洗净。","番茄先煮出汤汁。","加入豆腐和菌菇煮熟。","用盐和葱调味。"]', 1, 2),
(24, '西葫芦鸡胸肉炒蛋', '西葫芦、鸡胸肉和鸡蛋快炒，蛋白质充足且少油。', '/images/recipes/chicken-tofu-protein.jpg', 18, 'EASY', 1, 390, 36.00, 15.00, 24.00, '家常,快手', '低脂,高蛋白,低卡', 'FAT_LOSS,MUSCLE_GAIN', '["鸡胸肉切片腌制。","西葫芦切片，鸡蛋打散。","先炒鸡蛋后盛出。","鸡胸肉和西葫芦炒熟后加入鸡蛋调味。"]', 1, 2),
(25, '紫薯酸奶轻食碗', '紫薯和无糖酸奶搭配，适合作为轻量早餐。', '/images/recipes/default-recipe.svg', 12, 'EASY', 1, 340, 13.00, 6.00, 60.00, '清爽,早餐', '饱腹,低脂,快手', 'FAT_LOSS,BALANCED', '["紫薯蒸熟切块。","碗中加入无糖酸奶。","放入紫薯和少量玉米粒。","按口味加入少量燕麦增加饱腹感。"]', 1, 2),
(26, '黄瓜虾仁鸡蛋杯', '黄瓜、虾仁和鸡蛋做成快手轻食杯。', '/images/recipes/tuna-lettuce-salad.jpg', 12, 'EASY', 1, 320, 29.00, 14.00, 18.00, '清爽,快手', '低卡,高蛋白,少油', 'FAT_LOSS', '["虾仁煮熟，鸡蛋煮熟切块。","黄瓜切丁。","将虾仁、鸡蛋和黄瓜放入碗中。","加入少量柠檬汁和黑胡椒拌匀。"]', 1, 2),
(27, '白菜豆腐鸡肉汤', '白菜、豆腐和鸡胸肉煮汤，清淡又有饱腹感。', '/images/recipes/shrimp-tofu-soup.jpg', 22, 'EASY', 1, 360, 34.00, 11.00, 24.00, '清淡,汤类', '低脂,高蛋白,饱腹', 'FAT_LOSS,BALANCED', '["白菜洗净切段，豆腐切块。","鸡胸肉切片并焯水。","锅中加水煮白菜和豆腐。","加入鸡胸肉煮熟并调味。"]', 1, 2),
(28, '洋葱彩椒牛肉饭', '洋葱和彩椒搭配牛肉和米饭，适合日常均衡午餐。', '/images/recipes/blackpepper-beef-brownrice.jpg', 25, 'EASY', 1, 620, 36.00, 20.00, 72.00, '家常,下饭', '均衡,高蛋白', 'BALANCED,MUSCLE_GAIN', '["牛肉切片用生抽和黑胡椒腌制。","洋葱和彩椒切条。","牛肉炒熟后加入蔬菜翻炒。","搭配米饭装盘。"]', 1, 2),
(29, '香菇鸡腿肉杂粮饭', '鸡腿肉、香菇和杂粮饭组合，适合健康便当。', '/images/recipes/mushroom-tofu-rice.jpg', 30, 'MEDIUM', 1, 610, 34.00, 18.00, 76.00, '家常,便当', '均衡,饱腹,高蛋白', 'BALANCED', '["鸡腿肉去皮切块腌制。","香菇切片。","鸡腿肉煎熟后加入香菇翻炒。","搭配杂粮饭装入餐盒。"]', 1, 2),
(30, '番茄牛肉荞麦面', '番茄牛肉汤底搭配荞麦面，酸甜开胃。', '/images/recipes/seaweed-egg-noodle.jpg', 25, 'EASY', 1, 560, 32.00, 16.00, 72.00, '汤面,家常', '均衡,高蛋白', 'BALANCED', '["番茄切块煮出汤汁。","牛肉片快速焯熟。","荞麦面煮熟放入碗中。","倒入番茄汤和牛肉片调味。"]', 1, 2),
(31, '南瓜鸡蛋杂粮粥', '南瓜、鸡蛋和杂粮饭煮成温和饱腹粥。', '/images/recipes/tomato-egg-oatmeal.jpg', 20, 'EASY', 1, 420, 19.00, 10.00, 66.00, '清淡,早餐', '均衡,饱腹,易做', 'BALANCED,FAT_LOSS', '["南瓜切块蒸软。","杂粮饭加水煮开。","加入南瓜压散。","倒入蛋液搅成蛋花并调味。"]', 1, 2),
(32, '西红柿豆腐炖牛肉', '番茄、豆腐和牛肉炖煮，营养均衡。', '/images/recipes/beef-potato-carrot.jpg', 35, 'MEDIUM', 2, 640, 40.00, 24.00, 58.00, '家常,炖菜', '均衡,高蛋白', 'BALANCED,MUSCLE_GAIN', '["牛肉切块焯水。","番茄炒出汤汁。","加入牛肉炖煮至变软。","放入豆腐继续炖熟。"]', 1, 2),
(33, '白菜猪里脊炒饭', '白菜和猪里脊搭配米饭，做法快且适合作为一餐。', '/images/recipes/mushroom-tofu-rice.jpg', 20, 'EASY', 1, 590, 30.00, 18.00, 76.00, '家常,快手', '均衡,饱腹', 'BALANCED', '["猪里脊切丝腌制。","白菜切碎。","先炒猪里脊至变色。","加入白菜和米饭翻炒调味。"]', 1, 2),
(34, '菌菇青菜鸡蛋面', '菌菇、青菜和鸡蛋搭配面条，快手均衡。', '/images/recipes/seaweed-egg-noodle.jpg', 15, 'EASY', 1, 520, 24.00, 15.00, 72.00, '汤面,快手', '均衡,易做', 'BALANCED', '["面条煮熟。","菌菇和青菜煮软。","打入鸡蛋形成蛋花。","加入面条并调味。"]', 1, 2),
(35, '彩椒豆腐鸡肉盖饭', '彩椒、豆腐和鸡肉搭配米饭，颜色丰富且营养均衡。', '/images/recipes/chicken-tofu-protein.jpg', 25, 'EASY', 1, 600, 38.00, 18.00, 70.00, '家常,盖饭', '均衡,高蛋白', 'BALANCED,MUSCLE_GAIN', '["鸡胸肉切丁腌制。","豆腐和彩椒切块。","鸡肉和豆腐煎香。","加入彩椒翻炒后盖在米饭上。"]', 1, 2),
(36, '玉米鸡蛋牛奶粥', '玉米、鸡蛋和牛奶煮成快手早餐粥。', '/images/recipes/tomato-egg-oatmeal.jpg', 15, 'EASY', 1, 450, 21.00, 14.00, 62.00, '早餐,清淡', '均衡,快手,饱腹', 'BALANCED', '["玉米粒煮熟。","牛奶小火加热。","加入玉米和燕麦煮软。","倒入蛋液搅拌至熟。"]', 1, 2),
(37, '孜然牛肉红薯碗', '牛肉和红薯组合，适合训练后补充蛋白和碳水。', '/images/recipes/beef-egg-quinoa.jpg', 30, 'MEDIUM', 1, 720, 44.00, 22.00, 86.00, '训练后,碗餐', '高蛋白,增肌,饱腹', 'MUSCLE_GAIN', '["红薯蒸熟切块。","牛肉切片用孜然粉和生抽腌制。","牛肉煎熟。","搭配红薯和蔬菜装碗。"]', 1, 2),
(38, '鸡腿排杂粮饭', '去皮鸡腿排搭配杂粮饭，适合增肌便当。', '/images/recipes/chicken-broccoli-bowl.jpg', 30, 'MEDIUM', 1, 700, 42.00, 24.00, 78.00, '便当,家常', '高蛋白,增肌,饱腹', 'MUSCLE_GAIN,BALANCED', '["鸡腿肉去皮拍平腌制。","杂粮饭提前煮熟。","鸡腿排煎熟切块。","搭配西兰花和杂粮饭装盘。"]', 1, 2),
(39, '虾仁牛油果荞麦面', '虾仁、牛油果和荞麦面组合，适合训练后轻负担补给。', '/images/recipes/shrimp-egg-pasta.jpg', 22, 'EASY', 1, 650, 38.00, 22.00, 76.00, '训练后,清爽', '高蛋白,增肌,均衡', 'MUSCLE_GAIN,BALANCED', '["荞麦面煮熟过凉。","虾仁煮熟。","牛油果切片。","将荞麦面、虾仁和牛油果拌匀调味。"]', 1, 2),
(40, '猪里脊鸡蛋蛋白餐', '猪里脊和鸡蛋组成高蛋白餐，适合增肌期。', '/images/recipes/chicken-tofu-protein.jpg', 25, 'EASY', 1, 640, 48.00, 24.00, 52.00, '蛋白餐,家常', '高蛋白,增肌', 'MUSCLE_GAIN', '["猪里脊切片腌制。","鸡蛋煮熟或煎熟。","猪里脊煎熟。","搭配米饭和青菜装盘。"]', 1, 2),
(41, '牛肉毛豆藜麦饭', '牛肉、毛豆和藜麦提供蛋白质与优质碳水。', '/images/recipes/beef-egg-quinoa.jpg', 30, 'MEDIUM', 1, 760, 50.00, 24.00, 82.00, '训练后,碗餐', '高蛋白,增肌,均衡', 'MUSCLE_GAIN', '["藜麦煮熟。","毛豆煮熟备用。","牛肉切片煎熟。","全部放入碗中并调味。"]', 1, 2),
(42, '鸡胸肉酸奶全麦卷', '鸡胸肉搭配无糖酸奶和全麦饼，便携高蛋白。', '/images/recipes/chicken-avocado-wrap.jpg', 18, 'EASY', 1, 620, 44.00, 16.00, 72.00, '便携,西式', '高蛋白,增肌,快手', 'MUSCLE_GAIN,FAT_LOSS', '["鸡胸肉煎熟切条。","无糖酸奶加少量黑胡椒调成酱。","全麦饼加热。","放入鸡胸肉、生菜和酸奶酱后卷起。"]', 1, 2),
(43, '巴沙鱼红薯训练餐', '巴沙鱼和红薯搭配，脂肪较低且适合训练餐。', '/images/recipes/cod-asparagus.jpg', 25, 'EASY', 1, 610, 42.00, 10.00, 86.00, '训练后,轻食', '高蛋白,低脂,增肌', 'MUSCLE_GAIN,FAT_LOSS', '["红薯蒸熟切块。","巴沙鱼用盐和黑胡椒腌制。","巴沙鱼煎熟。","搭配西兰花和红薯装盘。"]', 1, 2),
(44, '双蛋牛肉杂粮饭', '牛肉和双蛋搭配杂粮饭，蛋白质充足。', '/images/recipes/beef-egg-quinoa.jpg', 25, 'EASY', 1, 780, 52.00, 28.00, 82.00, '训练后,便当', '高蛋白,增肌,饱腹', 'MUSCLE_GAIN', '["杂粮饭加热。","牛肉切片煎熟。","两个鸡蛋煎熟。","将牛肉、鸡蛋和杂粮饭装盘。"]', 1, 2),
(45, '虾仁豆腐高蛋白盖饭', '虾仁和豆腐搭配米饭，口味清淡且蛋白质充足。', '/images/recipes/shrimp-tofu-soup.jpg', 20, 'EASY', 1, 620, 44.00, 18.00, 72.00, '盖饭,清淡', '高蛋白,增肌,均衡', 'MUSCLE_GAIN,BALANCED', '["虾仁炒至变色。","豆腐切块煎香。","加入少量生抽调味。","盖在米饭上并撒葱花。"]', 1, 2);
```

- [ ] **Step 3: Add recipe_ingredient rows for recipes 19-45**

Append rows for recipes 19-45 to the existing `INSERT INTO recipe_ingredient` block, changing recipe 18's final semicolon to comma. Use this exact block:

```sql
(19, 49, 220, 'g', 1), (19, 4, 120, 'g', 1), (19, 56, 3, 'g', 0), (19, 30, 5, 'g', 0),
(20, 43, 120, 'g', 1), (20, 2, 1, '个', 1), (20, 8, 160, 'g', 1), (20, 26, 1, 'g', 0),
(21, 39, 180, 'g', 1), (21, 9, 150, 'g', 1), (21, 17, 120, 'g', 1), (21, 29, 0.50, '个', 0),
(22, 37, 160, 'g', 1), (22, 47, 160, 'g', 1), (22, 11, 100, 'g', 1), (22, 59, 5, 'ml', 0),
(23, 10, 180, 'g', 1), (23, 8, 160, 'g', 1), (23, 48, 120, 'g', 1), (23, 30, 5, 'g', 0),
(24, 50, 180, 'g', 1), (24, 1, 150, 'g', 1), (24, 2, 1, '个', 1), (24, 27, 6, 'ml', 0),
(25, 55, 180, 'g', 1), (25, 41, 160, 'g', 1), (25, 20, 25, 'g', 0), (25, 31, 40, 'g', 0),
(26, 16, 160, 'g', 1), (26, 4, 120, 'g', 1), (26, 2, 1, '个', 1), (26, 29, 0.50, '个', 0),
(27, 44, 180, 'g', 1), (27, 8, 150, 'g', 1), (27, 1, 120, 'g', 1), (27, 56, 3, 'g', 0),
(28, 45, 100, 'g', 1), (28, 46, 120, 'g', 1), (28, 3, 180, 'g', 1), (28, 17, 180, 'g', 1),
(29, 37, 170, 'g', 1), (29, 14, 100, 'g', 1), (29, 53, 180, 'g', 1), (29, 27, 8, 'ml', 0),
(30, 10, 180, 'g', 1), (30, 3, 150, 'g', 1), (30, 52, 120, 'g', 1), (30, 30, 5, 'g', 0),
(31, 47, 180, 'g', 1), (31, 2, 1, '个', 1), (31, 53, 160, 'g', 1), (31, 26, 1, 'g', 0),
(32, 10, 180, 'g', 1), (32, 8, 160, 'g', 1), (32, 3, 180, 'g', 1), (32, 27, 8, 'ml', 0),
(33, 44, 150, 'g', 1), (33, 38, 150, 'g', 1), (33, 17, 180, 'g', 1), (33, 57, 3, 'g', 0),
(34, 48, 120, 'g', 1), (34, 12, 120, 'g', 1), (34, 2, 1, '个', 1), (34, 21, 110, 'g', 1),
(35, 46, 120, 'g', 1), (35, 8, 150, 'g', 1), (35, 1, 150, 'g', 1), (35, 17, 160, 'g', 1),
(36, 31, 100, 'g', 1), (36, 2, 1, '个', 1), (36, 40, 200, 'ml', 1), (36, 20, 30, 'g', 1),
(37, 3, 200, 'g', 1), (37, 51, 220, 'g', 1), (37, 61, 3, 'g', 0), (37, 9, 100, 'g', 0),
(38, 37, 220, 'g', 1), (38, 53, 180, 'g', 1), (38, 9, 120, 'g', 1), (38, 27, 8, 'ml', 0),
(39, 4, 150, 'g', 1), (39, 33, 80, 'g', 1), (39, 52, 120, 'g', 1), (39, 29, 0.50, '个', 0),
(40, 38, 180, 'g', 1), (40, 2, 2, '个', 1), (40, 17, 140, 'g', 1), (40, 12, 100, 'g', 0),
(41, 3, 190, 'g', 1), (41, 42, 100, 'g', 1), (41, 19, 120, 'g', 1), (41, 25, 2, 'g', 0),
(42, 1, 180, 'g', 1), (42, 41, 80, 'g', 1), (42, 24, 1, '张', 1), (42, 11, 80, 'g', 0),
(43, 39, 200, 'g', 1), (43, 51, 220, 'g', 1), (43, 9, 120, 'g', 1), (43, 25, 2, 'g', 0),
(44, 3, 180, 'g', 1), (44, 2, 2, '个', 1), (44, 53, 180, 'g', 1), (44, 27, 8, 'ml', 0),
(45, 4, 150, 'g', 1), (45, 8, 180, 'g', 1), (45, 17, 180, 'g', 1), (45, 30, 5, 'g', 0);
```

- [ ] **Step 4: Run seed data test**

Run:

```powershell
cd backend
mvn -B -ntp -Dtest=RecipeKnowledgeBaseSeedDataTest test
```

Expected: all tests pass.

## Task 3: Add Existing Database Migration

**Files:**
- Create: `backend/src/main/resources/db/migrations/2026-07-09-expand-recipe-knowledge-base.sql`

- [ ] **Step 1: Create additive migration**

Create a migration that:

1. Starts with `SET NAMES utf8mb4;`
2. Inserts ingredients 37-61 using `INSERT IGNORE INTO ingredient (...) VALUES ...;`
3. Deletes recipe_ingredient rows for recipe IDs 19-45 only:
   `DELETE FROM recipe_ingredient WHERE recipe_id BETWEEN 19 AND 45;`
4. Deletes recipe rows for recipe IDs 19-45 only:
   `DELETE FROM recipe WHERE id BETWEEN 19 AND 45;`
5. Inserts recipes 19-45 using the exact same rows as `data.sql`.
6. Inserts recipe_ingredient rows for recipes 19-45 using the exact same rows as `data.sql`.

- [ ] **Step 2: Verify migration does not contain destructive global operations**

Run:

```powershell
rg -n "TRUNCATE|DROP TABLE|DELETE FROM user|DELETE FROM recommendation_history|DELETE FROM shopping_list|DELETE FROM favorite" backend/src/main/resources/db/migrations/2026-07-09-expand-recipe-knowledge-base.sql
```

Expected: no output.

## Task 4: Update Image Source Documentation

**Files:**
- Modify: `docs/recipe-image-sources.md`

- [ ] **Step 1: Add knowledge base expansion note**

Append a section:

```markdown
## Knowledge Base Expansion Phase 1

Recipes 19-45 reuse existing local images by dish type where a visually close photo already exists. Distinct missing photos use `/images/recipes/default-recipe.svg` until a matching real photo is collected.

- Soup recipes reuse `shrimp-tofu-soup.jpg` or `egg-tofu-custard.jpg`.
- Bowl and rice recipes reuse existing bowl/rice photos.
- Fish recipes reuse `cod-asparagus.jpg`.
- Wrap recipes reuse `chicken-avocado-wrap.jpg`.
- `紫薯酸奶轻食碗` currently uses `default-recipe.svg`.
```

## Task 5: Full Verification and Commit

**Files:**
- All modified files in this plan.

- [ ] **Step 1: Run backend tests**

Run:

```powershell
cd backend
mvn -B -ntp test
```

Expected: build success, 34 tests or more, 0 failures.

- [ ] **Step 2: Run frontend build**

Run:

```powershell
cd frontend
npm run build
```

Expected: TypeScript and Vite build succeed.

- [ ] **Step 3: Sync CodeGraph**

Run:

```powershell
codegraph sync
```

Expected: sync completes without errors.

- [ ] **Step 4: Commit**

Run:

```powershell
git add backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecipeKnowledgeBaseSeedDataTest.java backend/src/main/resources/db/data.sql backend/src/main/resources/db/migrations/2026-07-09-expand-recipe-knowledge-base.sql docs/recipe-image-sources.md docs/superpowers/plans/2026-07-09-recipe-knowledge-base-expansion-implementation.md
git commit -m "feat: 扩充菜谱知识库种子数据"
```

Expected: one commit with the seed data expansion, migration, validation test, and docs.

## Self-Review

### Spec Coverage

- Existing three diet goals only: Task 1 validates target-goal distribution and Task 2 uses only existing enum strings.
- About 45 recipes: Task 1 validates at least 45 recipes and Task 2 adds recipes 19-45.
- Additive migration: Task 3 creates scoped insert/delete SQL and checks against broad destructive statements.
- Image strategy: Task 1 validates image paths and Task 4 documents reuse.
- No scoring or DeepSeek changes: no tasks touch recommendation scoring or AI contracts.

### Placeholder Scan

No TBD/TODO placeholders are present. SQL rows and validation code are specified explicitly.

### Type Consistency

The plan uses existing database column names: `image_url`, `target_goals`, `recipe_ingredient`, `is_core`, and existing diet-goal values.
