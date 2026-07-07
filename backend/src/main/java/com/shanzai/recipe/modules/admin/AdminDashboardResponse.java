package com.shanzai.recipe.modules.admin;

public record AdminDashboardResponse(
    Long userCount,
    Long recipeCount,
    Long ingredientCount,
    Long recommendationCount
) {
}
