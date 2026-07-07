package com.shanzai.recipe.modules.recommendation;

import java.math.BigDecimal;

public record RecommendationHistoryRecipeResponse(
    Long id,
    String name,
    String imageUrl,
    Integer calories,
    BigDecimal protein
) {
}
