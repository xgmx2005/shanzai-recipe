package com.shanzai.recipe.modules.recommendation;

import java.util.List;

public record RecommendationResponse(
    Long historyId,
    String aiSummary,
    List<RecommendedRecipeResponse> recipes
) {
}
