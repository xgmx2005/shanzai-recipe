package com.shanzai.recipe.modules.profile;

import com.shanzai.recipe.common.DietGoal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProfileRequest(
    @Size(max = 10) String gender,
    @Min(1) @Max(120) Integer age,
    @DecimalMin("1.0") BigDecimal heightCm,
    @DecimalMin("1.0") BigDecimal weightKg,
    DietGoal dietGoal,
    List<@Size(max = 30) String> tastePreferences,
    List<@Size(max = 30) String> avoidIngredients,
    List<@Size(max = 30) String> allergyIngredients,
    @Min(1) @Max(240) Integer cookingTimePreference,
    Boolean profileCompleted
) {
}
