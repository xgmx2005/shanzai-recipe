package com.shanzai.recipe.modules.recommendation;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationHistoryRecipeResponse(
    Long id,
    String name,
    String imageUrl,
    Integer calories,
    BigDecimal protein,
    int score,
    String reason,
    List<String> matchedIngredients,
    List<String> missingIngredients
) {
    public RecommendationHistoryRecipeResponse(
        Long id,
        String name,
        String imageUrl,
        Integer calories,
        BigDecimal protein
    ) {
        this(id, name, imageUrl, calories, protein, 0, "", List.of(), List.of());
    }

    public RecommendationHistoryRecipeResponse {
        matchedIngredients = matchedIngredients == null ? List.of() : List.copyOf(matchedIngredients);
        missingIngredients = missingIngredients == null ? List.of() : List.copyOf(missingIngredients);
    }
}
