package com.shanzai.recipe.modules.recommendation;

import java.util.List;

public record AiRecommendationContext(
    String dietGoal,
    List<String> availableIngredients,
    List<String> excludedIngredients,
    Integer cookingTime,
    List<RecipeSnapshot> recipes
) {
    public record RecipeSnapshot(
        String name,
        int score,
        Integer calories,
        String protein,
        List<String> matchedIngredients,
        List<String> tags
    ) {
    }
}
