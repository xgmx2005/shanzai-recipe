package com.shanzai.recipe.modules.recommendation;

import java.util.List;

public record RecommendationRequestModel(
    List<String> availableIngredients,
    List<String> excludedIngredients,
    List<String> blockedIngredients,
    String dietGoal,
    List<String> tastePreferences,
    Integer cookingTime
) {
}
