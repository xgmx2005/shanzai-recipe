package com.shanzai.recipe.modules.admin;

public record PopularRecipeStatResponse(
    Long recipeId,
    String recipeName,
    Long recommendationCount
) {
}
