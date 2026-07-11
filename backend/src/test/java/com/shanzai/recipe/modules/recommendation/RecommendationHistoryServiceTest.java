package com.shanzai.recipe.modules.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recommendation.conversation.AvailableIngredientInput;
import com.shanzai.recipe.modules.recommendation.conversation.RecommendationConversationContext;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationHistoryServiceTest {
    private RecommendationHistoryMapper historyMapper;
    private RecipeMapper recipeMapper;
    private RecommendationHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyMapper = mock(RecommendationHistoryMapper.class);
        recipeMapper = mock(RecipeMapper.class);
        historyService = new RecommendationHistoryService(historyMapper, recipeMapper, new ObjectMapper());
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


    @Test
    void detailIncludesConversationContextWhenSnapshotExists() {
        RecommendationConversationContext context = new RecommendationConversationContext(
            "清淡晚餐",
            "FAT_LOSS",
            List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
            List.of("辣椒"),
            List.of("花生"),
            30,
            2,
            List.of(),
            List.of(),
            true
        );
        RecommendationHistoryEntity history = history(5L, 7L, "2,1");
        history.setConversationContextJson("{\"intentText\":\"清淡晚餐\",\"dietGoal\":\"FAT_LOSS\",\"availableIngredients\":[{\"name\":\"鸡胸肉\",\"quantity\":300,\"unit\":\"g\",\"quantityKnown\":true}],\"excludedIngredients\":[\"辣椒\"],\"allergyIngredients\":[\"花生\"],\"cookingTime\":30,\"servings\":2,\"unknownTerms\":[],\"conflicts\":[],\"restrictionsConfirmed\":true}");
        when(historyMapper.selectById(5L)).thenReturn(history);
        when(recipeMapper.selectBatchIds(List.of(2L, 1L))).thenReturn(List.of());

        RecommendationHistoryDetailResponse response = historyService.getHistory(7L, 5L);

        assertEquals(context, response.conversationContext());
    }

    @Test
    void attachConversationContextSerializesSnapshotForOwnedHistory() throws Exception {
        RecommendationHistoryEntity history = history(5L, 7L, "2,1");
        when(historyMapper.selectById(5L)).thenReturn(history);
        RecommendationConversationContext context = new RecommendationConversationContext(
            "清淡晚餐",
            "FAT_LOSS",
            List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
            List.of(),
            List.of(),
            30,
            2,
            List.of(),
            List.of(),
            true
        );

        historyService.attachConversationContext(7L, 5L, context);

        verify(historyMapper).updateById(history);
        assertEquals("清淡晚餐", new ObjectMapper().readValue(
            history.getConversationContextJson(), RecommendationConversationContext.class).intentText());
    }

    @Test
    void attachConversationContextRejectsHistoryOwnedByAnotherUser() {
        when(historyMapper.selectById(5L)).thenReturn(history(5L, 8L, "2,1"));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> historyService.attachConversationContext(7L, 5L, RecommendationConversationContext.empty()));

        assertEquals("推荐历史不存在", exception.getMessage());
    }

    @Test
    void getRecommendationResponseRebuildsResponseFromExistingHistory() {
        when(historyMapper.selectById(5L)).thenReturn(history(5L, 7L, "2,1"));
        when(recipeMapper.selectBatchIds(List.of(2L, 1L))).thenReturn(List.of(
            recipe(1L, "番茄鸡蛋"),
            recipe(2L, "鸡胸肉西兰花轻食碗")
        ));

        RecommendationResponse response = historyService.getRecommendationResponse(7L, 5L);

        assertEquals(5L, response.historyId());
        assertEquals("推荐轻食", response.aiSummary());
        assertEquals(List.of(2L, 1L), response.recipes().stream().map(RecommendedRecipeResponse::id).toList());
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
