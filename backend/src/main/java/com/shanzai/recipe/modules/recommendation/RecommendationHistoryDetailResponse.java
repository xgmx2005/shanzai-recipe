package com.shanzai.recipe.modules.recommendation;

import com.shanzai.recipe.modules.recommendation.conversation.RecommendationConversationContext;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationHistoryDetailResponse(
    Long id,
    List<String> inputIngredients,
    List<String> excludedIngredients,
    RecommendationConversationContext conversationContext,
    String dietGoal,
    Integer cookingTime,
    Integer servings,
    List<Long> resultRecipeIds,
    String aiSummary,
    String aiHealthTip,
    String aiShoppingTip,
    boolean aiGenerated,
    List<RecommendationHistoryRecipeResponse> recipes,
    LocalDateTime createdAt
) {
    public RecommendationHistoryDetailResponse {
        inputIngredients = inputIngredients == null ? List.of() : List.copyOf(inputIngredients);
        excludedIngredients = excludedIngredients == null ? List.of() : List.copyOf(excludedIngredients);
        resultRecipeIds = resultRecipeIds == null ? List.of() : List.copyOf(resultRecipeIds);
        recipes = recipes == null ? List.of() : List.copyOf(recipes);
    }
}
