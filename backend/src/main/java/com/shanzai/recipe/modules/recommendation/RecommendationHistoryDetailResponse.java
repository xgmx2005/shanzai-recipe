package com.shanzai.recipe.modules.recommendation;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationHistoryDetailResponse(
    Long id,
    List<String> inputIngredients,
    List<String> excludedIngredients,
    String dietGoal,
    Integer cookingTime,
    Integer servings,
    List<Long> resultRecipeIds,
    String aiSummary,
    List<RecommendationHistoryRecipeResponse> recipes,
    LocalDateTime createdAt
) {
}
