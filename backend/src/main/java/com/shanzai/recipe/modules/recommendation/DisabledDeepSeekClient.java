package com.shanzai.recipe.modules.recommendation;

import java.util.List;
import java.util.Optional;

public class DisabledDeepSeekClient implements DeepSeekClient {
    @Override
    public Optional<AiRecommendationText> generateRecommendationText(
        String recipeName,
        String dietGoal,
        List<String> matchedIngredients
    ) {
        return Optional.empty();
    }
}
