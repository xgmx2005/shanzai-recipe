package com.shanzai.recipe.modules.recipe;

import java.math.BigDecimal;
import java.util.List;

public record RecipeSummaryResponse(
    Long id,
    String name,
    String description,
    String imageUrl,
    Integer cookingTime,
    String difficulty,
    Integer servings,
    Integer calories,
    BigDecimal protein,
    BigDecimal fat,
    BigDecimal carbs,
    List<String> tasteTags,
    List<String> healthTags,
    List<String> targetGoals,
    Integer status
) {
}
