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
    private static final Path EXPANSION_MIGRATION_SQL =
        Path.of("src/main/resources/db/migrations/2026-07-09-expand-recipe-knowledge-base.sql");
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
    void seedDataIngredientUnitsRemainDomainUnits() throws IOException {
        String values = valuesBlock(Files.readString(DATA_SQL), "ingredient");
        Set<String> allowedUnits = Set.of("g", "ml", "个", "张", "片");

        for (String row : splitRows(values)) {
            List<String> columns = splitSqlColumns(row);
            String name = unquote(columns.get(1));
            String unit = unquote(columns.get(3));
            assertTrue(allowedUnits.contains(unit), name + " should use a real ingredient unit");
            assertFalse(unit.startsWith("/images/"), name + " unit should not contain an image path");
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

    @Test
    void expansionMigrationRecipeColumnsMatchSeedData() throws IOException {
        String seedDataSql = Files.readString(DATA_SQL);
        String migrationSql = Files.readString(EXPANSION_MIGRATION_SQL);

        assertTrue(
            insertColumns(seedDataSql, "recipe").equals(insertColumns(migrationSql, "recipe")),
            "recipe migration insert columns should match data.sql"
        );
    }

    @Test
    void expandedRecipesUseDedicatedVisualAssets() throws IOException {
        SeedData seedData = parseSeedData();
        Map<Long, String> expectedImages = Map.ofEntries(
            Map.entry(22L, "/images/recipes/pexels-recipe-22-chicken-salad-bowl.jpg"),
            Map.entry(25L, "/images/recipes/pexels-recipe-25-sweet-potato-yogurt-bowl.jpg"),
            Map.entry(26L, "/images/recipes/pexels-recipe-26-cucumber-shrimp-egg-cup.jpg"),
            Map.entry(30L, "/images/recipes/pexels-recipe-30-tomato-beef-soba-noodle.jpg"),
            Map.entry(31L, "/images/recipes/pexels-recipe-31-pumpkin-egg-porridge.jpg"),
            Map.entry(33L, "/images/recipes/pexels-recipe-33-pork-cabbage-fried-rice.jpg"),
            Map.entry(37L, "/images/recipes/pexels-recipe-37-buddha-sweet-potato-bowl.jpg"),
            Map.entry(39L, "/images/recipes/pexels-recipe-39-shrimp-avocado-salad.jpg"),
            Map.entry(41L, "/images/recipes/pexels-recipe-41-quinoa-chicken-bowl.jpg"),
            Map.entry(43L, "/images/recipes/pexels-recipe-43-fish-sweet-potato-plate.jpg")
        );

        for (Map.Entry<Long, String> expected : expectedImages.entrySet()) {
            RecipeSeed recipe = seedData.recipes().get(expected.getKey());
            assertTrue(recipe.imageUrl().equals(expected.getValue()),
                recipe.name() + " should use dedicated image " + expected.getValue());
        }
    }

    @Test
    void highVisibilityRecipesUsePremiumPexelsLightMealImages() throws IOException {
        SeedData seedData = parseSeedData();
        Map<Long, String> expectedImages = Map.ofEntries(
            Map.entry(1L, "/images/recipes/pexels-recipe-01-chicken-broccoli-bowl.jpg"),
            Map.entry(4L, "/images/recipes/pexels-recipe-04-tuna-egg-salad-bowl.jpg"),
            Map.entry(10L, "/images/recipes/pexels-recipe-10-salmon-poke-quinoa-bowl.jpg"),
            Map.entry(13L, "/images/recipes/pexels-recipe-13-bulgogi-beef-rice-bowl.jpg"),
            Map.entry(16L, "/images/recipes/pexels-recipe-16-tofu-broccoli-protein-bowl.jpg"),
            Map.entry(22L, "/images/recipes/pexels-recipe-22-chicken-salad-bowl.jpg"),
            Map.entry(28L, "/images/recipes/pexels-recipe-28-bibimbap-beef-rice-bowl.jpg"),
            Map.entry(35L, "/images/recipes/pexels-recipe-35-chicken-kale-rice-bowl.jpg"),
            Map.entry(37L, "/images/recipes/pexels-recipe-37-buddha-sweet-potato-bowl.jpg"),
            Map.entry(39L, "/images/recipes/pexels-recipe-39-shrimp-avocado-salad.jpg"),
            Map.entry(41L, "/images/recipes/pexels-recipe-41-quinoa-chicken-bowl.jpg"),
            Map.entry(45L, "/images/recipes/pexels-recipe-45-shrimp-poke-bowl.jpg")
        );

        for (Map.Entry<Long, String> expected : expectedImages.entrySet()) {
            RecipeSeed recipe = seedData.recipes().get(expected.getKey());
            assertTrue(recipe.imageUrl().equals(expected.getValue()),
                recipe.name() + " should use premium Pexels image " + expected.getValue());
        }
    }

    @Test
    void allSeedRecipesUseFullPexelsImageLibrary() throws IOException {
        SeedData seedData = parseSeedData();
        Set<String> imageUrls = new HashSet<>();

        for (RecipeSeed recipe : seedData.recipes().values()) {
            String imageUrl = recipe.imageUrl();
            assertTrue(imageUrl.startsWith("/images/recipes/pexels-recipe-"),
                recipe.name() + " should use a full Pexels library image");
            assertTrue(imageUrl.endsWith(".jpg"),
                recipe.name() + " should use a jpg Pexels library image");
            Path asset = FRONTEND_PUBLIC.resolve(imageUrl.substring(1));
            assertTrue(Files.exists(asset), "missing Pexels image asset " + asset);
            assertTrue(imageUrls.add(imageUrl), "duplicate recipe image " + imageUrl);
        }

        assertEquals(seedData.recipes().size(), imageUrls.size(), "each recipe should have a distinct image");
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

    private List<String> insertColumns(String sql, String table) {
        Pattern pattern = Pattern.compile("INSERT(?: IGNORE)? INTO " + table + " \\((.*?)\\) VALUES", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        assertTrue(matcher.find(), "missing INSERT columns for " + table);
        return List.of(matcher.group(1).replaceAll("\\s+", "").split(","));
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
