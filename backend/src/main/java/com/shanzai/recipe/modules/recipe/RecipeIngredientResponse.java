package com.shanzai.recipe.modules.recipe;

import java.math.BigDecimal;

public record RecipeIngredientResponse(
    Long ingredientId,
    String name,
    String category,
    BigDecimal quantity,
    String unit,
    Boolean core
) {
}
