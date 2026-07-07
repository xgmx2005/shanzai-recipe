package com.shanzai.recipe.modules.admin;

import com.shanzai.recipe.modules.auth.UserMapper;
import com.shanzai.recipe.modules.ingredient.IngredientMapper;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import com.shanzai.recipe.modules.recommendation.RecommendationHistoryMapper;
import com.shanzai.recipe.modules.recommendation.RecommendationLogEntity;
import com.shanzai.recipe.modules.recommendation.RecommendationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {
    private UserMapper userMapper;
    private RecipeMapper recipeMapper;
    private IngredientMapper ingredientMapper;
    private RecommendationHistoryMapper historyMapper;
    private RecommendationLogMapper logMapper;
    private AdminDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        recipeMapper = mock(RecipeMapper.class);
        ingredientMapper = mock(IngredientMapper.class);
        historyMapper = mock(RecommendationHistoryMapper.class);
        logMapper = mock(RecommendationLogMapper.class);
        dashboardService = new AdminDashboardService(
            userMapper,
            recipeMapper,
            ingredientMapper,
            historyMapper,
            logMapper
        );
    }

    @Test
    void popularRecipesAreGroupedByRecommendationLogCount() {
        when(logMapper.selectList(any())).thenReturn(List.of(
            log(2L),
            log(1L),
            log(2L)
        ));
        when(recipeMapper.selectBatchIds(List.of(2L, 1L))).thenReturn(List.of(
            recipe(1L, "番茄鸡蛋"),
            recipe(2L, "鸡胸肉西兰花轻食碗")
        ));

        List<PopularRecipeStatResponse> response = dashboardService.listPopularRecipes(5);

        assertEquals(2, response.size());
        assertEquals(2L, response.get(0).recipeId());
        assertEquals("鸡胸肉西兰花轻食碗", response.get(0).recipeName());
        assertEquals(2L, response.get(0).recommendationCount());
    }

    private RecommendationLogEntity log(Long recipeId) {
        RecommendationLogEntity log = new RecommendationLogEntity();
        log.setRecipeId(recipeId);
        return log;
    }

    private RecipeEntity recipe(Long id, String name) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setName(name);
        return recipe;
    }
}
