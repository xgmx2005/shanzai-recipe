package com.shanzai.recipe.modules.profile;

import java.math.BigDecimal;

public record ProfileSummaryResponse(
    boolean hasProfile,
    String dietGoal,
    BigDecimal bmi,
    String bmiStatus,
    Integer dailyCalorieTarget,
    Integer cookingTimePreference,
    boolean profileCompleted
) {
}
