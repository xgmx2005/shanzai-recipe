package com.shanzai.recipe.modules.recipe;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecipeIngredientRequest(
    @NotNull Long ingredientId,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
    @NotBlank String unit,
    Boolean core
) {
}
