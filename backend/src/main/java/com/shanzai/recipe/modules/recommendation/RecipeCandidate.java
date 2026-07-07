package com.shanzai.recipe.modules.recommendation;

import java.util.List;

public record RecipeCandidate(
    Long id,
    String name,
    List<String> ingredients,
    List<String> coreIngredients,
    List<String> targetGoals,
    List<String> tags,
    Integer cookingTime,
    Integer popularity
) {
}
