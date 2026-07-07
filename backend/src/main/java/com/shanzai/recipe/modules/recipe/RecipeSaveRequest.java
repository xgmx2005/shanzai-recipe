package com.shanzai.recipe.modules.recipe;

import com.shanzai.recipe.common.DietGoal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record RecipeSaveRequest(
    @NotBlank String name,
    @NotBlank String description,
    String imageUrl,
    @NotNull @Min(1) Integer cookingTime,
    @NotBlank String difficulty,
    @NotNull @Min(1) Integer servings,
    @NotNull @Min(0) Integer calories,
    @NotNull @DecimalMin("0.0") BigDecimal protein,
    @NotNull @DecimalMin("0.0") BigDecimal fat,
    @NotNull @DecimalMin("0.0") BigDecimal carbs,
    List<String> tasteTags,
    List<String> healthTags,
    @NotEmpty List<DietGoal> targetGoals,
    @NotEmpty List<String> steps,
    @Valid @NotEmpty List<RecipeIngredientRequest> ingredients
) {
}
