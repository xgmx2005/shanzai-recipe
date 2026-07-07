package com.shanzai.recipe.modules.recommendation;

import java.math.BigDecimal;

public record RecommendedRecipeResponse(
    Long id,
    String name,
    int score,
    String reason,
    Integer calories,
    BigDecimal protein,
    String imageUrl
) {
}
