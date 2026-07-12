package com.shanzai.recipe.modules.recommendation;

import com.shanzai.recipe.common.DietGoal;
import jakarta.validation.constraints.Min;

import java.util.List;

public record RecommendationRequest(
    List<String> availableIngredients,
    List<String> excludedIngredients,
    DietGoal dietGoal,
    @Min(1) Integer cookingTime,
    @Min(1) Integer servings
) {
}
