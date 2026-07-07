package com.shanzai.recipe.modules.ingredient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record IngredientResponse(
    Long id,
    String name,
    String category,
    String unit,
    Integer caloriesPer100g,
    BigDecimal proteinPer100g,
    BigDecimal fatPer100g,
    BigDecimal carbsPer100g,
    List<String> aliases,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
