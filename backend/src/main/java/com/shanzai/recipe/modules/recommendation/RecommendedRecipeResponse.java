package com.shanzai.recipe.modules.recommendation;

import java.math.BigDecimal;
import java.util.List;

public record RecommendedRecipeResponse(
    Long id,
    String name,
    int score,
    String reason,
    Integer calories,
    BigDecimal protein,
    String imageUrl,
    List<String> matchedIngredients,
    List<String> missingIngredients
) {
    public RecommendedRecipeResponse(
        Long id,
        String name,
        int score,
        String reason,
        Integer calories,
        BigDecimal protein,
        String imageUrl
    ) {
        this(id, name, score, reason, calories, protein, imageUrl, List.of(), List.of());
    }

    public RecommendedRecipeResponse {
        matchedIngredients = matchedIngredients == null ? List.of() : List.copyOf(matchedIngredients);
        missingIngredients = missingIngredients == null ? List.of() : List.copyOf(missingIngredients);
    }
}
