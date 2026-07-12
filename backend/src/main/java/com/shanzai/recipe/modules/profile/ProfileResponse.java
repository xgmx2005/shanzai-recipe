package com.shanzai.recipe.modules.profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProfileResponse(
    Long id,
    Long userId,
    String gender,
    Integer age,
    BigDecimal heightCm,
    BigDecimal weightKg,
    BigDecimal bmi,
    String dietGoal,
    List<String> tastePreferences,
    List<String> avoidIngredients,
    List<String> allergyIngredients,
    Integer cookingTimePreference,
    Integer dailyCalorieTarget,
    Boolean profileCompleted,
    LocalDateTime updatedAt
) {
}
