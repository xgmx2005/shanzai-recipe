package com.shanzai.recipe.modules.ingredient;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record IngredientSaveRequest(
    @NotBlank String name,
    @NotBlank String category,
    @NotBlank String unit,
    @NotNull @Min(0) Integer caloriesPer100g,
    @NotNull @DecimalMin("0.0") BigDecimal proteinPer100g,
    @NotNull @DecimalMin("0.0") BigDecimal fatPer100g,
    @NotNull @DecimalMin("0.0") BigDecimal carbsPer100g,
    List<String> aliases
) {
}
