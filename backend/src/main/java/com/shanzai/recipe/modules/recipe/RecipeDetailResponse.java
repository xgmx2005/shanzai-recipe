package com.shanzai.recipe.modules.recipe;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RecipeDetailResponse(
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
    List<String> steps,
    List<RecipeIngredientResponse> ingredients,
    Integer status,
    Long createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
