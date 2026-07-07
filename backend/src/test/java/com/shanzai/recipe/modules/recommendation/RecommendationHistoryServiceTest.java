package com.shanzai.recipe.modules.recommendation;

import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationHistoryServiceTest {
    private RecommendationHistoryMapper historyMapper;
    private RecipeMapper recipeMapper;
    private RecommendationHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyMapper = mock(RecommendationHistoryMapper.class);
        recipeMapper = mock(RecipeMapper.class);
        historyService = new RecommendationHistoryService(historyMapper, recipeMapper);
    }

    @Test
    void detailIncludesRecipeNamesInSavedOrder() {
        when(historyMapper.selectById(5L)).thenReturn(history(5L, 7L, "2,1"));
        when(recipeMapper.selectBatchIds(List.of(2L, 1L))).thenReturn(List.of(
            recipe(1L, "番茄鸡蛋"),
            recipe(2L, "鸡胸肉西兰花轻食碗")
        ));

        RecommendationHistoryDetailResponse response = historyService.getHistory(7L, 5L);

        assertEquals(5L, response.id());
        assertEquals(2, response.recipes().size());
        assertEquals("鸡胸肉西兰花轻食碗", response.recipes().get(0).name());
        assertEquals("番茄鸡蛋", response.recipes().get(1).name());
    }

    private RecommendationHistoryEntity history(Long id, Long userId, String resultRecipeIds) {
        RecommendationHistoryEntity history = new RecommendationHistoryEntity();
        history.setId(id);
        history.setUserId(userId);
        history.setInputIngredients("鸡胸肉,西兰花");
        history.setExcludedIngredients("");
        history.setDietGoal("FAT_LOSS");
        history.setCookingTime(30);
        history.setServings(1);
        history.setResultRecipeIds(resultRecipeIds);
        history.setAiSummary("推荐轻食");
        history.setCreatedAt(LocalDateTime.of(2026, 7, 7, 10, 0));
        return history;
    }

    private RecipeEntity recipe(Long id, String name) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setName(name);
        recipe.setImageUrl("/images/recipes/test.jpg");
        recipe.setCalories(360);
        recipe.setProtein(new BigDecimal("28.00"));
        return recipe;
    }
}
