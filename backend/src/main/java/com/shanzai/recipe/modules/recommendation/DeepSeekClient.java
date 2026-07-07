package com.shanzai.recipe.modules.recommendation;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface DeepSeekClient {
    Optional<AiRecommendationText> generateRecommendationText(
        String recipeName,
        String dietGoal,
        List<String> matchedIngredients
    );
}
