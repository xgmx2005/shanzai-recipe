package com.shanzai.recipe.modules.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.common.DietGoal;
import com.shanzai.recipe.modules.ingredient.IngredientEntity;
import com.shanzai.recipe.modules.ingredient.IngredientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeServiceTest {
    private RecipeMapper recipeMapper;
    private RecipeIngredientMapper recipeIngredientMapper;
    private IngredientMapper ingredientMapper;
    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeMapper = mock(RecipeMapper.class);
        recipeIngredientMapper = mock(RecipeIngredientMapper.class);
        ingredientMapper = mock(IngredientMapper.class);
        recipeService = new RecipeService(recipeMapper, recipeIngredientMapper, ingredientMapper, new ObjectMapper());
    }

    @Test
    void listRecipesFiltersActiveRecipesByKeywordGoalAndTag() {
        when(recipeMapper.selectList(any())).thenReturn(List.of(
            recipe(1L, "鸡胸肉西兰花轻食碗", "低脂高蛋白轻食碗", "清淡,轻食", "低脂,高蛋白", "FAT_LOSS,MUSCLE_GAIN", 1),
            recipe(2L, "番茄炒蛋", "经典家常菜", "家常,清淡", "营养均衡", "BALANCED", 1),
            recipe(3L, "下架鸡胸肉套餐", "不可展示", "清淡", "高蛋白", "FAT_LOSS", 0)
        ));

        List<RecipeSummaryResponse> responses = recipeService.listRecipes("鸡胸肉", DietGoal.FAT_LOSS, "高蛋白");

        assertEquals(1, responses.size());
        RecipeSummaryResponse response = responses.get(0);
        assertEquals(1L, response.id());
        assertEquals(List.of("清淡", "轻食"), response.tasteTags());
        assertEquals(List.of("低脂", "高蛋白"), response.healthTags());
        assertEquals(List.of("FAT_LOSS", "MUSCLE_GAIN"), response.targetGoals());
    }

    @Test
    void getRecipeDetailIncludesStepsAndIngredientMetadata() {
        RecipeEntity recipe = recipe(1L, "鸡胸肉西兰花轻食碗", "低脂高蛋白轻食碗", "清淡,轻食", "低脂,高蛋白", "FAT_LOSS", 1);
        recipe.setSteps("[\"鸡胸肉切块。\",\"西兰花焯水。\"]");
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(recipeIngredientMapper.selectList(any())).thenReturn(List.of(
            recipeIngredient(1L, 1L, new BigDecimal("180"), "g", true),
            recipeIngredient(1L, 9L, new BigDecimal("150"), "g", true)
        ));
        when(ingredientMapper.selectBatchIds(List.of(1L, 9L))).thenReturn(List.of(
            ingredient(1L, "鸡胸肉", "肉蛋奶"),
            ingredient(9L, "西兰花", "蔬菜")
        ));

        RecipeDetailResponse detail = recipeService.getRecipeDetail(1L);

        assertEquals(2, detail.steps().size());
        assertTrue(detail.steps().get(0).contains("鸡胸肉切块。"));
        assertTrue(detail.steps().get(0).contains("先擦干表面水分"));
        assertTrue(detail.steps().get(1).contains("西兰花焯水。"));
        assertTrue(detail.steps().get(1).contains("保持小火到中火"));
        assertEquals(2, detail.ingredients().size());
        assertEquals("鸡胸肉", detail.ingredients().get(0).name());
        assertEquals("肉蛋奶", detail.ingredients().get(0).category());
        assertEquals(new BigDecimal("180"), detail.ingredients().get(0).quantity());
        assertTrue(detail.ingredients().get(0).core());
    }

    @Test
    void getRecipeDetailRejectsInactiveRecipe() {
        when(recipeMapper.selectById(1L)).thenReturn(recipe(1L, "下架菜谱", "不可展示", "清淡", "低脂", "FAT_LOSS", 0));

        assertThrows(BusinessException.class, () -> recipeService.getRecipeDetail(1L));
    }

    @Test
    void createRecipeStoresRecipeAndIngredientRows() {
        when(ingredientMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(ingredient(1L, "鸡胸肉", "肉蛋奶")));
        when(recipeMapper.insert(any(RecipeEntity.class))).thenAnswer(invocation -> {
            RecipeEntity recipe = invocation.getArgument(0);
            recipe.setId(99L);
            return 1;
        });

        RecipeDetailResponse response = recipeService.createRecipe(
            2L,
            saveRequest(List.of(new RecipeIngredientRequest(1L, new BigDecimal("180"), "g", true)))
        );

        ArgumentCaptor<RecipeEntity> recipeCaptor = ArgumentCaptor.forClass(RecipeEntity.class);
        ArgumentCaptor<RecipeIngredientEntity> ingredientCaptor = ArgumentCaptor.forClass(RecipeIngredientEntity.class);
        verify(recipeMapper).insert(recipeCaptor.capture());
        verify(recipeIngredientMapper).insert(ingredientCaptor.capture());

        assertEquals(99L, response.id());
        assertEquals(2L, recipeCaptor.getValue().getCreatedBy());
        assertEquals("[\"鸡胸肉煎熟。\",\"西兰花焯水。\"]", recipeCaptor.getValue().getSteps());
        assertEquals(99L, ingredientCaptor.getValue().getRecipeId());
        assertEquals(1L, ingredientCaptor.getValue().getIngredientId());
        assertTrue(ingredientCaptor.getValue().getCore());
    }

    @Test
    void deleteRecipeMarksStatusInactive() {
        when(recipeMapper.selectById(1L)).thenReturn(recipe(1L, "鸡胸肉西兰花轻食碗", "低脂高蛋白轻食碗", "清淡", "低脂", "FAT_LOSS", 1));

        recipeService.deleteRecipe(1L);

        ArgumentCaptor<RecipeEntity> captor = ArgumentCaptor.forClass(RecipeEntity.class);
        verify(recipeMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    private RecipeSaveRequest saveRequest(List<RecipeIngredientRequest> ingredients) {
        return new RecipeSaveRequest(
            "鸡胸肉西兰花轻食碗",
            "低脂高蛋白轻食碗",
            "/images/recipes/chicken-broccoli-bowl.jpg",
            25,
            "EASY",
            1,
            420,
            new BigDecimal("35.00"),
            new BigDecimal("9.00"),
            new BigDecimal("45.00"),
            List.of("清淡", "轻食"),
            List.of("低脂", "高蛋白"),
            List.of(DietGoal.FAT_LOSS),
            List.of("鸡胸肉煎熟。", "西兰花焯水。"),
            ingredients
        );
    }

    private RecipeEntity recipe(
        Long id,
        String name,
        String description,
        String tasteTags,
        String healthTags,
        String targetGoals,
        Integer status
    ) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setName(name);
        recipe.setDescription(description);
        recipe.setImageUrl("/images/recipes/test.jpg");
        recipe.setCookingTime(25);
        recipe.setDifficulty("EASY");
        recipe.setServings(1);
        recipe.setCalories(420);
        recipe.setProtein(new BigDecimal("35.00"));
        recipe.setFat(new BigDecimal("9.00"));
        recipe.setCarbs(new BigDecimal("45.00"));
        recipe.setTasteTags(tasteTags);
        recipe.setHealthTags(healthTags);
        recipe.setTargetGoals(targetGoals);
        recipe.setSteps("[\"步骤一\"]");
        recipe.setStatus(status);
        return recipe;
    }

    private RecipeIngredientEntity recipeIngredient(
        Long recipeId,
        Long ingredientId,
        BigDecimal quantity,
        String unit,
        boolean core
    ) {
        RecipeIngredientEntity recipeIngredient = new RecipeIngredientEntity();
        recipeIngredient.setRecipeId(recipeId);
        recipeIngredient.setIngredientId(ingredientId);
        recipeIngredient.setQuantity(quantity);
        recipeIngredient.setUnit(unit);
        recipeIngredient.setCore(core);
        return recipeIngredient;
    }

    private IngredientEntity ingredient(Long id, String name, String category) {
        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setId(id);
        ingredient.setName(name);
        ingredient.setCategory(category);
        ingredient.setUnit("g");
        ingredient.setCaloriesPer100g(100);
        ingredient.setProteinPer100g(new BigDecimal("10.00"));
        ingredient.setFatPer100g(new BigDecimal("1.00"));
        ingredient.setCarbsPer100g(new BigDecimal("2.00"));
        return ingredient;
    }
}
